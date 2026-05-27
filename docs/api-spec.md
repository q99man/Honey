# API Specification

This document defines the API structure for Honeytong.

The API is designed for:
- mobile-first product usage
- web-based browsing and admin usage
- trust-based access control
- policy-driven business rules
- future extension without major redesign

Base path:
- User API: `/api`
- Admin API: `/api/admin`

All responses should follow a consistent envelope format.

Example success response:
```json
{
  "success": true,
  "data": {},
  "message": "OK"
}
```
Example error response:

```json
{
  "success": false,
  "errorCode": "PHONE_VERIFICATION_REQUIRED",
  "message": "Phone verification is required."
}
```

1. Authentication API

These APIs handle signup, login, token refresh, and phone verification.

1.1 Signup
POST /api/auth/signup

Create a local user account.

Request:

{
  "email": "user@example.com",
  "password": "password1234",
  "nickname": "bee_user"
}

Response:

{
  "success": true,
  "data": {
    "userId": 1,
    "nickname": "bee_user"
  },
  "message": "Signup completed"
}
1.2 Login
POST /api/auth/login

Login with local credentials.

Request:

{
  "email": "user@example.com",
  "password": "password1234"
}

Response:

{
  "success": true,
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token"
  },
  "message": "Login successful"
}
1.3 OAuth Login
POST /api/auth/oauth/{provider}

Login with social provider.

Path variables:

provider: kakao, naver, google

Request:

{
  "accessToken": "provider-token"
}

Response:

{
  "success": true,
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "jwt-refresh-token"
  },
  "message": "OAuth login successful"
}

MVP note:
The backend verifies the provider access token through a replaceable OAuth provider client for Kakao, Naver, or Google.
On first successful provider verification, the backend creates a normal USER account, inserts a `user_auth` row for the provider and provider user id, and initializes `user_trust` and `user_level`.
On later logins, the existing `user_auth` link is used to issue Honeytong access and refresh tokens.
The OAuth flow does not automatically merge accounts by email alone; linking is based on the provider and provider user id returned by the provider.
1.4 Logout
POST /api/auth/logout

Invalidate current session or refresh token.

Request:

{
  "refreshToken": "jwt-refresh-token"
}

Response:

{
  "success": true,
  "data": null,
  "message": "Logout completed"
}

1.5 Refresh Token
POST /api/auth/refresh

Reissue access token using refresh token.
The refresh token is rotated on each successful refresh.
The server stores only a hash of the refresh token.

Request:

{
  "refreshToken": "jwt-refresh-token"
}

Response:

{
  "success": true,
  "data": {
    "accessToken": "jwt-access-token",
    "refreshToken": "new-refresh-token"
  },
  "message": "Token refreshed"
}
2. Phone Verification API

These APIs handle phone verification for core actions.
These APIs require a valid access token.

MVP note:
Verification code rows remain the durable source of truth.
When `APP_REDIS_ENABLED=true`, the backend can cache the latest unverified code state and attempt count in Redis while preserving DB fallback behavior.
Phone code delivery defaults to the development sender in local development.
Production delivery defaults to the SOLAPI sender and can be enabled explicitly with `PHONE_VERIFICATION_SENDER_PROVIDER=solapi` plus `SOLAPI_API_KEY`, `SOLAPI_API_SECRET`, and `SOLAPI_FROM`.
Naver Cloud SENS remains available with `PHONE_VERIFICATION_SENDER_PROVIDER=naver-sens` when a business account can provide `NAVER_SENS_SERVICE_ID`, `NAVER_SENS_ACCESS_KEY`, `NAVER_SENS_SECRET_KEY`, and `NAVER_SENS_FROM`.
The sender uses the configured message template and must not write raw verification codes to normal application logs.

2.1 Send Verification Code
POST /api/auth/phone/send-code

Request:

{
  "phone": "01012345678"
}

Response:

{
  "success": true,
  "data": {
    "sent": true
  },
  "message": "Verification code sent"
}
2.2 Verify Code
POST /api/auth/phone/verify-code

Request:

{
  "phone": "01012345678",
  "code": "123456"
}

Response:

{
  "success": true,
  "data": {
    "phoneVerified": true
  },
  "message": "Phone verification completed"
}
2.3 Verification Status
GET /api/auth/phone/status

Response:

{
  "success": true,
  "data": {
    "phoneVerified": true
  },
  "message": "OK"
}
3. Password Recovery API
3.1 Request Password Reset
POST /api/auth/password/reset-request

Request:

{
  "email": "user@example.com"
}
3.2 Reset Password
POST /api/auth/password/reset

Request:

{
  "email": "user@example.com",
  "resetCode": "123456",
  "newPassword": "newPassword1234"
}
4. User API

These APIs provide user profile, trust, level, and activity information.

4.1 Get My Profile
GET /api/users/me

Response:

{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "bee_user",
    "phoneVerified": true,
    "languagePreference": "ko",
    "birthYear": 1995,
    "gender": "FEMALE",
    "nationalityCode": "KR",
    "profileImageUrl": "https://api.example.com/uploads/images/profiles/profile.jpg"
  },
  "message": "OK"
}
4.2 Update My Profile
PATCH /api/users/me

Request:

{
  "nickname": "new_bee_user",
  "languagePreference": "en",
  "birthYear": 1995,
  "gender": "FEMALE",
  "nationalityCode": "KR",
  "profileImageUrl": "https://api.example.com/uploads/images/profiles/profile.jpg"
}

Response:

{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "new_bee_user",
    "phoneVerified": true,
    "languagePreference": "en",
    "birthYear": 1995,
    "gender": "FEMALE",
    "nationalityCode": "KR",
    "profileImageUrl": "https://api.example.com/uploads/images/profiles/profile.jpg"
  },
  "message": "Profile updated"
}

4.2.1 Upload Image
POST /api/uploads/images

Requires:
- authenticated user
- multipart form data
- jpg, png, or webp image file
- file size within the configured backend upload limit

Request:

multipart/form-data fields:
- file: image file
- target: PLACE, PROFILE, or VISIT

Response:

{
  "success": true,
  "data": {
    "imageUrl": "https://api.example.com/uploads/images/places/abc123.jpg",
    "originalFilename": "menu.jpg",
    "contentType": "image/jpeg",
    "size": 12345
  },
  "message": "이미지가 업로드되었습니다."
}

MVP note:
The backend stores binary image files in the configured upload storage path and stores only returned URLs in relational rows such as `place_images`, `visits.image_url`, and `users.profile_image_url`. Image URL storage supports up to 2048 characters. The default local implementation uses a server-local filesystem path and can be replaced by object storage later without changing the mobile upload contract.
4.3 Get My Status
GET /api/users/me/status

Purpose:
Returns user trust, level, and key participation status.

Response:

{
  "success": true,
  "data": {
    "level": 4,
    "exp": 135,
    "nextLevelExp": 200,
    "trustGrade": "ACTIVE_BEE",
    "recommendWeight": 1.10,
    "phoneVerified": true,
    "regionVerified": true
  },
  "message": "OK"
}
MVP note:
`nextLevelExp` is read from `system_policies.growth.level_exp_thresholds`.
If the current level has no configured threshold, `nextLevelExp` returns 0.
Trust grade and recommendation weight are recalculated from trust policies after valid visit growth events.
4.4 Get My Growth Info
GET /api/users/me/growth

Response:

{
  "success": true,
  "data": {
    "level": 4,
    "exp": 135,
    "title": "Active Bee",
    "avatarStage": "BEE_STAGE_4"
  },
  "message": "OK"
}
4.5 Get My Activity Summary
GET /api/users/me/activity-summary

Response:

{
  "success": true,
  "data": {
    "recommendedCount": 24,
    "visitCount": 18,
    "commentCount": 11,
    "registeredPlaceCount": 3
  },
  "message": "OK"
}
MVP note:
Activity summary counts active recommendations, valid visits, visible non-deleted comments, and non-deleted registered places.
5. Region API

These APIs support region lookup, region verification, and primary region management.

5.1 Get Cities
GET /api/regions/cities

This lookup API is public.

5.2 Get Districts by City
GET /api/regions/cities/{cityId}/districts

This lookup API is public.

5.3 Get Dongs by District
GET /api/regions/districts/{districtId}/dongs

This lookup API is public.

5.4 Verify Region by GPS
POST /api/regions/verify

This API requires a valid access token.
The GPS-to-administrative-dong resolver is an implementation boundary and requires region mapping data.
If the resolved dong differs from the user's current primary region, the same `region.change_cooldown_day` policy used by `PATCH /api/regions/me` is enforced server-side.

Request:

{
  "latitude": 37.123456,
  "longitude": 127.123456
}

Response:

{
  "success": true,
  "data": {
    "cityId": 1,
    "districtId": 12,
    "dongId": 123,
    "cityName": "Bucheon-si",
    "districtName": "Wonmi-gu",
    "dongName": "Sang-dong",
    "verified": true
  },
  "message": "Region verified"
}
5.5 Get My Primary Region
GET /api/regions/me

This API requires a valid access token.

Response:

{
  "success": true,
  "data": {
    "cityId": 1,
    "districtId": 12,
    "dongId": 123,
    "cityName": "서울특별시",
    "districtName": "마포구",
    "dongName": "서교동",
    "verified": true
  },
  "message": "OK"
}

5.6 Change My Primary Region
PATCH /api/regions/me

Request:

{
  "dongId": 123
}

Note:

cooldown and eligibility must be checked on server side
region change policy must be policy-driven
the current policy key is region.change_cooldown_day in system_policies

Response:

{
  "success": true,
  "data": {
    "cityId": 1,
    "districtId": 12,
    "dongId": 123,
    "cityName": "서울특별시",
    "districtName": "마포구",
    "dongName": "서교동",
    "verified": true
  },
  "message": "Region changed"
}

5.7 Get Region Change Policy
GET /api/regions/me/change-policy

Response:

{
  "success": true,
  "data": {
    "changeAllowed": true,
    "cooldownDays": 7,
    "nextAvailableAt": null
  },
  "message": "OK"
}
6. Place API

These APIs manage flowers (restaurants).

6.1 Create Place
POST /api/places

Requires:
- authenticated user
- phone verification
- no active blocking user sanction
- primary region verification
- place registration policy eligibility
- road/jibun address length within `system_policies.place.address_max_length`
- recommended menu length within `system_policies.place.recommended_menu_max_length`
- short recommendation length within `system_policies.place.short_recommendation_max_length`
- feature text length within `system_policies.place.feature_text_max_length`
- each image URL length within `system_policies.place.image_url_max_length`
- when a road or jibun address is present, the server resolves coordinates through Kakao Local address search and uses the resolved coordinate/administrative dong for persistence and registration-scope validation. Client-supplied latitude/longitude are fallback values only when no address is present.

Request:

{
  "name": "Sangdong Sundaeguk",
  "categoryCode": "KOREAN",
  "dongId": 123,
  "addressRoad": "Some road address",
  "addressJibun": "Some jibun address",
  "latitude": 37.123456,
  "longitude": 127.123456,
  "priceRangeCode": "10000_20000",
  "recommendedMenu": "Sundaeguk",
  "shortRecommendation": "A place worth revisiting in this area",
  "featureText": "Rich soup and strong local vibe",
  "imageUrls": [
    "https://image.example.com/1.jpg"
  ]
}

Response:

{
  "success": true,
  "data": {
    "placeId": 1001,
    "approvalStatus": "APPROVED"
  },
  "message": "맛집이 등록되었습니다."
}
6.2 Get Place Detail
GET /api/places/{placeId}

Response:

{
  "success": true,
  "data": {
    "id": 1001,
    "name": "Sangdong Sundaeguk",
    "categoryCode": "KOREAN",
    "starLevel": 1,
    "flowerGrade": "GOLD",
    "recommendCount": 32,
    "visitCount": 19,
    "commentCount": 11,
    "audienceTags": [
      "Popular among men in their 30s and 40s"
    ],
    "myRecommended": true,
    "myVisitedToday": false
  },
  "message": "OK"
}
6.3 Get Nearby Places
GET /api/places/nearby?lat={lat}&lng={lng}&radius={radius}

Purpose:
Returns nearby flowers for map-based exploration.

Response items include `distanceMeter` when distance is calculated.
Place list-shaped responses include `regionName`, `address`, `starLevel`, top-level aggregate counts, and `representativeImageUrl`; mobile clients should map these fields directly instead of relying on detail-only fields such as `addressRoad`, `currentStarLevel`, nested `stats`, or `imageUrls`.

6.4 Get Places by Region
GET /api/places?cityId={cityId}&districtId={districtId}&dongId={dongId}
6.5 Search Places
GET /api/places/search?keyword={keyword}

Purpose:
Returns visible, non-deleted places matching a user keyword for map discovery.

Current searchable fields:
- place name
- recommended menu
- short recommendation
- feature text
- road address
- jibun address
- Korean and English city, district, and dong names

Notes:
- blank keywords are rejected with `INVALID_REQUEST`
- the response uses the same place list item shape as normal place list reads
- frontend search reset should call the normal place list flow by submitting an empty keyword
- scale note: current search is a broad keyword match for MVP discovery; before production-scale data, move to a dedicated search-document strategy or database full-text strategy validated for Korean text rather than relying on more `%keyword%` predicates

6.6 Get Popular Places
GET /api/places/popular?regionType={regionType}&regionId={regionId}
6.7 Update Place
PATCH /api/places/{placeId}

Requires:
- authenticated user
- place owner or admin permission
- phone verification and no active blocking sanction for normal place owners
- road/jibun address length within `system_policies.place.address_max_length`
- recommended menu length within `system_policies.place.recommended_menu_max_length`
- short recommendation length within `system_policies.place.short_recommendation_max_length`
- feature text length within `system_policies.place.feature_text_max_length`
- each image URL length within `system_policies.place.image_url_max_length` when image URLs are replaced

Request:

{
  "name": "Sangdong Sundaeguk Updated",
  "categoryCode": "KOREAN",
  "dongId": 123,
  "addressRoad": "Updated road address",
  "addressJibun": "Updated jibun address",
  "latitude": 37.123456,
  "longitude": 127.123456,
  "priceRangeCode": "10000_20000",
  "recommendedMenu": "Sundaeguk",
  "shortRecommendation": "Updated local recommendation",
  "featureText": "Updated feature text",
  "franchise": false,
  "imageUrls": [
    "https://image.example.com/updated-1.jpg"
  ]
}

Response:

{
  "success": true,
  "data": {
    "placeId": 1001,
    "name": "Sangdong Sundaeguk Updated",
    "categoryCode": "KOREAN",
    "cityId": 1,
    "districtId": 12,
    "dongId": 123,
    "addressRoad": "Updated road address",
    "addressJibun": "Updated jibun address",
    "latitude": 37.123456,
    "longitude": 127.123456,
    "priceRangeCode": "10000_20000",
    "recommendedMenu": "Sundaeguk",
    "shortRecommendation": "Updated local recommendation",
    "featureText": "Updated feature text",
    "franchise": false,
    "imageUrls": [
      "https://image.example.com/updated-1.jpg"
    ]
  },
  "message": "Place updated"
}

Notes:
- request fields are partial; omitted fields keep the current value
- if `dongId` changes for a normal owner, the server validates `system_policies.region.registration_scope`
- if `addressRoad` or `addressJibun` is present, the server resolves the next address through Kakao Local address search and stores the address-derived coordinate instead of trusting client-supplied latitude/longitude
- address-derived coordinates are converted back to an administrative dong and validated against `system_policies.region.registration_scope` for normal owners
- `latitude` and `longitude` must be updated together
- when `imageUrls` is present, existing place images are replaced by the provided list
- admin updates through this endpoint write `PLACE_UPDATE` to `admin_action_logs` only when state changes
6.8 Delete Place
DELETE /api/places/{placeId}

Requires:
- authenticated user
- place owner or admin permission

Response:

{
  "success": true,
  "data": {
    "placeId": 1001,
    "deleted": true
  },
  "message": "Place deleted"
}

Notes:
- delete is a logical delete using `places.deleted_at`
- deleted places are excluded from user-facing place lists and details
- deleting a place resets `current_star_level` to 0
- admin deletes through this endpoint write `PLACE_DELETE` to `admin_action_logs`
6.9 Get My Registered Places
GET /api/users/me/places

Response:

{
  "success": true,
  "data": [
    {
      "id": 1001,
      "name": "Sangdong Sundaeguk",
      "categoryCode": "KOREAN",
      "cityId": 1,
      "districtId": 12,
      "dongId": 123,
      "regionName": "서교동",
      "address": "Some road address",
      "latitude": 37.123456,
      "longitude": 127.123456,
      "shortRecommendation": "A place worth revisiting in this area",
      "starLevel": 0,
      "flowerGrade": "SEED",
      "recommendCount": 0,
      "visitCount": 0,
      "commentCount": 0,
      "distanceMeter": null,
      "representativeImageUrl": null
    }
  ],
  "message": "OK"
}

MVP note:
This authenticated read returns non-deleted places created by the current user, ordered by newest first.
Place list-shaped responses include `latitude` and `longitude` so the frontend can render real map markers from API data instead of fixed placeholder positions.
6.10 Get Registration Policy
GET /api/places/registration-policy

Purpose:
Returns current registration availability and scope.

Response:

{
  "success": true,
  "data": {
    "canRegister": true,
    "registrationScope": "DISTRICT",
    "registrationLimit": 5,
    "currentUsage": 1
  },
  "message": "OK"
}

MVP note:
The current backend uses one active registration scope policy and one per-user registration limit.
7. Recommendation API
7.1 Recommend Place
POST /api/places/{placeId}/recommend

Requires:
- authenticated user
- phone verification
- no active blocking user sanction
- daily recommendation limit policy

Response:

{
  "success": true,
  "data": {
    "recommended": true,
    "recommendCount": 33,
    "myWeight": 1.10
  },
  "message": "Recommendation completed"
}
7.2 Cancel Recommendation
DELETE /api/places/{placeId}/recommend

Response:

{
  "success": true,
  "data": {
    "recommended": false,
    "recommendCount": 32,
    "myWeight": 1.10
  },
  "message": "Recommendation canceled"
}
7.3 Get My Recommendations
GET /api/users/me/recommendations
7.4 Get Recommendation Policy
GET /api/places/{placeId}/recommend-policy

Response:

{
  "success": true,
  "data": {
    "canRecommend": true,
    "reason": null,
    "dailyRemainingCount": 12
  },
  "message": "OK"
}
8. Visit API
8.1 Verify Visit
POST /api/places/{placeId}/visits

Requires:
- authenticated user
- phone verification
- no active blocking user sanction
- GPS distance within `system_policies.visit.radius_meter`
- no active cooldown from `system_policies.visit.cooldown_hour`
- image URL length within `system_policies.visit.image_url_max_length` when imageUrl is supplied

Request:

{
  "latitude": 37.123456,
  "longitude": 127.123456,
  "imageUrl": "https://image.example.com/visit.jpg"
}

Response:

{
  "success": true,
  "data": {
    "verified": true,
    "distanceMeter": 42,
    "expGained": 2,
    "visitCount": 20
  },
  "message": "Visit verified"
}
MVP note:
Valid visits add EXP from `system_policies.growth.visit_exp` and increase trust score from `system_policies.trust.valid_visit_score`.
The visit API returns the EXP granted by this valid visit.
Level-up uses `system_policies.growth.level_exp_thresholds` and writes `user_level_history` only when the level changes.
Trust grade and recommendation weight are recalculated from `system_policies.trust.grade_thresholds` and `system_policies.trust.recommend_weight_by_grade`.
8.2 Get Visit Policy
GET /api/places/{placeId}/visit-policy

Response:

{
  "success": true,
  "data": {
    "canVisitNow": false,
    "radiusMeter": 70,
    "cooldownUntil": "2026-04-22T13:00:00"
  },
  "message": "OK"
}
8.3 Get My Visits
GET /api/users/me/visits

Response:

{
  "success": true,
  "data": [
    {
      "visitId": 701,
      "placeId": 1001,
      "placeName": "Sangdong Sundaeguk",
      "categoryCode": "KOREAN",
      "address": "Some road address",
      "distanceMeter": 42,
      "imageUrl": "https://image.example.com/visit.jpg",
      "visitedAt": "2026-04-22T13:00:00"
    }
  ],
  "message": "OK"
}
8.4 Get Place Visit Summary
GET /api/places/{placeId}/visits/summary

Response:

{
  "success": true,
  "data": {
    "placeId": 1001,
    "visitCount": 20,
    "lastVisitedAt": "2026-04-22T13:00:00"
  },
  "message": "OK"
}
9. Comment API
9.1 Create Comment
POST /api/places/{placeId}/comments

Requires:
- authenticated user
- phone verification
- no active blocking user sanction
- one visible comment per user/place
- content length within `system_policies.comment.max_length`

Request:

{
  "content": "Rich soup and reliable quality"
}

Response:

{
  "success": true,
  "data": {
    "commentId": 501
  },
  "message": "Comment created"
}
9.2 Update Comment
PATCH /api/comments/{commentId}

Requires:
- authenticated comment owner
- phone verification
- no active blocking user sanction
- content length within `system_policies.comment.max_length`

Request:

{
  "content": "Rich soup and strong local taste"
}

Response:

{
  "success": true,
  "data": {
    "commentId": 501,
    "placeId": 1001,
    "placeName": "Sangdong Sundaeguk",
    "userId": 1,
    "nickname": "bee_user",
    "content": "Rich soup and strong local taste",
    "createdAt": "2026-04-22T13:00:00",
    "updatedAt": "2026-04-22T13:10:00"
  },
  "message": "Comment updated"
}
9.3 Delete Comment
DELETE /api/comments/{commentId}

Response:

{
  "success": true,
  "data": {
    "commentId": 501,
    "deleted": true,
    "commentCount": 10
  },
  "message": "Comment deleted"
}
9.4 Get Place Comments
GET /api/places/{placeId}/comments

Response:

{
  "success": true,
  "data": [
    {
      "commentId": 501,
      "placeId": 1001,
      "placeName": "Sangdong Sundaeguk",
      "userId": 1,
      "nickname": "bee_user",
      "content": "Rich soup and reliable quality",
      "createdAt": "2026-04-22T13:00:00",
      "updatedAt": "2026-04-22T13:00:00"
    }
  ],
  "message": "OK"
}
9.5 Get My Comments
GET /api/users/me/comments

Response:

{
  "success": true,
  "data": [
    {
      "commentId": 501,
      "placeId": 1001,
      "placeName": "Sangdong Sundaeguk",
      "userId": 1,
      "nickname": "bee_user",
      "content": "Rich soup and reliable quality",
      "createdAt": "2026-04-22T13:00:00",
      "updatedAt": "2026-04-22T13:00:00"
    }
  ],
  "message": "OK"
}

MVP note:
Deleting a comment marks it deleted. Writing again for the same place restores the existing row to respect the unique user/place rule.
Create and update trim comment content and enforce the active `comment.max_length` policy value on the server before persistence.

9.6 Community Post API

GET /api/community/posts

Returns visible community posts newest first. `mine` is true when the authenticated viewer is the author.

GET /api/community/posts/{postId}

Returns one visible community post.

POST /api/community/posts

Requires:
- authenticated user
- phone verification
- no active blocking user sanction
- title length within `system_policies.community.post_title_max_length`
- content length within `system_policies.community.post_content_max_length`

Request:

{
  "title": "동네 소식",
  "content": "오늘 새 가게가 열었어요."
}

Response:

{
  "success": true,
  "data": {
    "postId": 100
  },
  "message": "Community post created"
}

PATCH /api/community/posts/{postId}

Requires:
- authenticated post author
- phone verification
- no active blocking user sanction
- title length within `system_policies.community.post_title_max_length`
- content length within `system_policies.community.post_content_max_length`

DELETE /api/community/posts/{postId}

Deletes the author's visible community post.

GET /api/users/me/community-posts

Returns visible community posts written by the authenticated user.

MVP note:
Create and update trim community post title/content and enforce the active community length policies on the server before persistence. The database title/content column lengths remain the physical upper bounds for configured policy values.
10. Ranking API

These APIs return seasonal ranking data.

10.1 Get Place Ranking by Region
GET /api/rankings/places?regionType={regionType}&regionId={regionId}

Query parameters:

regionType: dong, district, city
regionId: region identifier
optional seasonCode

Example:
GET /api/rankings/places?regionType=dong&regionId=123&seasonCode=2026-04

Response:

{
  "success": true,
  "data": {
    "seasonCode": "2026-04",
    "regionType": "dong",
    "regionName": "Sang-dong",
    "items": [
      {
        "rank": 1,
        "placeId": 1001,
        "name": "Sangdong Sundaeguk",
        "starLevel": 1,
        "totalScore": 72.5,
        "audienceTags": [
          "Popular among men in their 30s and 40s"
        ]
      }
    ]
  },
  "message": "OK"
}
10.2 Get User Ranking by Region
GET /api/rankings/users?regionType={regionType}&regionId={regionId}
10.3 Get Place Ranking History
GET /api/places/{placeId}/ranking-history

Purpose:
Returns star and rank history by season.

Response:

{
  "success": true,
  "data": {
    "placeId": 1001,
    "name": "Sangdong Sundaeguk",
    "items": [
      {
        "seasonId": 1,
        "seasonCode": "2026-04",
        "seasonName": "April 2026",
        "seasonType": "MONTHLY",
        "seasonStartAt": "2026-04-01T00:00:00",
        "seasonEndAt": "2026-04-30T23:59:59",
        "regionType": "dong",
        "regionId": 123,
        "rank": 1,
        "starLevel": 1,
        "totalScore": 72.5,
        "finalizedAt": "2026-05-01T12:00:00"
      }
    ]
  },
  "message": "OK"
}

MVP note:
Place ranking history reads are public and read finalized rows from `place_ranking_history`, not current `place_season_scores`.
Hidden or deleted places return `RESOURCE_NOT_FOUND` through the same public visibility rule as place detail.
Visible places with no finalized history return an empty `items` list.

10.4 Get Current Season Info
GET /api/rankings/seasons/current

Response:

{
  "success": true,
  "data": {
    "seasonCode": "2026-04",
    "seasonName": "2026년 4월",
    "seasonType": "MONTHLY",
    "startAt": "2026-04-01T00:00:00",
    "endAt": "2026-04-30T23:59:59",
    "status": "ACTIVE"
  },
  "message": "OK"
}

MVP note:
Place ranking reads are public and read from `place_season_scores`.
They must not recalculate scores from recommendation, visit, or comment rows on each request.
Place ranking history reads are separate and read from finalized `place_ranking_history` snapshots.
When `APP_REDIS_ENABLED=true`, public place ranking reads can be served from Redis while `place_season_scores` remains the authoritative read model.
Audience tags currently return an empty list until audience aggregation is implemented.
11. Mission API
11.1 Get Active Missions
GET /api/missions
11.2 Get My Mission Progress
GET /api/users/me/missions
11.3 Claim Mission Reward
POST /api/users/me/missions/{missionId}/claim
12. Audience Tag API
12.1 Get Place Audience Tags
GET /api/places/{placeId}/audience-tags

Response:

{
  "success": true,
  "data": {
    "tags": [
      "Popular among women in their 20s",
      "Popular among foreigners"
    ]
  },
  "message": "OK"
}
12.2 Get Regional Popular Tags
GET /api/regions/{dongId}/popular-tags
13. Report API
13.1 Create Report
POST /api/reports

Requires:
- authenticated user
- phone verification
- no active blocking user sanction
- reason text length within `system_policies.report.reason_text_max_length`

Request:

{
  "targetType": "PLACE",
  "targetId": 1001,
  "reasonCode": "FRANCHISE",
  "reasonText": "This appears to be a chain restaurant"
}

Response:

{
  "success": true,
  "data": {
    "reportId": 901
  },
  "message": "Report submitted"
}
13.2 Get My Reports
GET /api/users/me/reports

Response:

{
  "success": true,
  "data": [
    {
      "reportId": 901,
      "targetType": "PLACE",
      "targetId": 1001,
      "reasonCode": "FRANCHISE",
      "reasonText": "This appears to be a chain restaurant",
      "status": "PENDING",
      "createdAt": "2026-04-22T13:00:00",
      "reviewedAt": null,
      "reviewNote": null
    }
  ],
  "message": "OK"
}

MVP note:
Report creation currently validates active reporter and existing visible target.
Supported targetType values are PLACE, COMMENT, and USER.
Reason text is trimmed and enforced by `report.reason_text_max_length` before persistence.
Reports are stored as PENDING for later admin review.
Automated sanctions or moderation actions are not applied by user report creation.
Users cannot report themselves as USER targets.
14. Notification API

14.1 Get Notifications
GET /api/notifications

Returns list of notifications for the authenticated user, sorted by creation date in descending order.

Response:
{
  "success": true,
  "data": [
    {
      "id": 100,
      "type": "NEW_COMMENT",
      "title": "새로운 댓글이 등록되었습니다.",
      "content": "[맛집 이름] 맛집에 새로운 댓글이 작성되었습니다: \"정말 맛있는 맛집이네요!\"",
      "targetId": 10,
      "isRead": false,
      "createdAt": "2026-05-23T14:16:00"
    }
  ],
  "message": "OK"
}

14.2 Mark Notification as Read
PATCH /api/notifications/{notificationId}/read

Marks a notification as read.

Response:
{
  "success": true,
  "data": null,
  "message": "Notification marked as read"
}
15. Admin API

Admin APIs must be separated from normal user APIs.

All admin actions must:

require admin role
be logged
validate permission on server side

MVP bootstrap note:
Local admin/test account creation is handled by disabled-by-default startup configuration, not by a public API endpoint.
15.1 Admin Dashboard
GET /api/admin/dashboard

Response:

{
  "success": true,
  "data": {
    "todayNewUsers": 12,
    "todayRecommendations": 85,
    "todayVisits": 41,
    "pendingReports": 7,
    "newPlaces": 9
  },
  "message": "OK"
}

MVP note:
Dashboard metrics are read-only count values.
`todayNewUsers`, `todayRecommendations`, `todayVisits`, and `newPlaces` use the server's current local day from 00:00 inclusive to the next day 00:00 exclusive.
`todayNewUsers` counts non-deleted users created today.
`todayRecommendations` counts ACTIVE recommendations created today.
`todayVisits` counts valid visits created today.
`pendingReports` counts all PENDING reports.
15.2 Admin User Management
GET /api/admin/users

Get user list.

Response:

{
  "success": true,
  "data": [
    {
      "userId": 1,
      "nickname": "bee_user",
      "email": "user@example.com",
      "phoneVerified": true,
      "status": "ACTIVE",
      "role": "USER",
      "languagePreference": "ko",
      "createdAt": "2026-05-01T10:00:00",
      "updatedAt": "2026-05-01T10:00:00",
      "deletedAt": null
    }
  ],
  "message": "OK"
}

GET /api/admin/users/{userId}

Get user detail.

Response:

{
  "success": true,
  "data": {
    "userId": 1,
    "nickname": "bee_user",
    "email": "user@example.com",
    "phone": "01012345678",
    "phoneVerified": true,
    "status": "ACTIVE",
    "role": "USER",
    "languagePreference": "ko",
    "marketingAgreed": false,
    "createdAt": "2026-05-01T10:00:00",
    "updatedAt": "2026-05-01T10:00:00",
    "deletedAt": null,
    "trust": {
      "trustScore": 0,
      "trustGrade": "SEED_BEE",
      "recommendWeight": 1.00,
      "sanctionCount": 0,
      "reportReceivedCount": 0,
      "reportConfirmedCount": 0,
      "abnormalActivityScore": 0,
      "phoneVerified": true,
      "regionVerified": false,
      "lastEvaluatedAt": null
    },
    "level": {
      "level": 1,
      "exp": 0,
      "totalExp": 0,
      "title": null,
      "avatarStage": null,
      "rankScore": 0
    }
  },
  "message": "OK"
}

MVP note:
Admin user management currently provides read-only list and detail APIs.
User sanctions, trust adjustment, and recommendation-weight adjustment remain separate state-changing admin workflows.
The detail response includes trust and level data when the corresponding rows exist.

POST /api/admin/users/{userId}/sanctions

Apply sanction.

Requires:
- reason length within `system_policies.admin.sanction_reason_max_length` when reason is supplied
- memo length within `system_policies.admin.action_memo_max_length` when memo is supplied

Request:

{
  "sanctionType": "TEMPORARY_RESTRICTION",
  "reason": "Repeated spam reports confirmed",
  "startAt": "2026-05-01T10:00:00",
  "endAt": "2026-05-08T10:00:00",
  "memo": "Operator memo"
}

Response:

{
  "success": true,
  "data": {
    "sanctionId": 1001,
    "userId": 1,
    "nickname": "bee_user",
    "sanctionType": "TEMPORARY_RESTRICTION",
    "reason": "Repeated spam reports confirmed",
    "startAt": "2026-05-01T10:00:00",
    "endAt": "2026-05-08T10:00:00",
    "status": "ACTIVE",
    "createdByUserId": 10,
    "createdByNickname": "admin_user",
    "createdAt": "2026-05-01T10:00:00",
    "updatedAt": "2026-05-01T10:00:00"
  },
  "message": "User sanction created"
}

MVP note:
Supported sanctionType values are WARNING, TEMPORARY_RESTRICTION, and PERMANENT_RESTRICTION.
New sanctions are stored as ACTIVE and logged to admin_action_logs with action type USER_SANCTION.
The backend rejects self-sanctioning and admin-account sanctioning through this MVP endpoint.
If `user_trust` exists, `sanction_count` is incremented.
Active TEMPORARY_RESTRICTION and PERMANENT_RESTRICTION sanctions block place creation, recommendation creation, visit verification, comment creation, and comment update while `startAt <= now` and `endAt` is null or in the future.
WARNING sanctions do not block actions.
Cancellation/deletion actions are not blocked in the MVP so users can undo participation.
Reason and memo are trimmed and enforced by admin text length policies before persistence and audit logging.
Expiration status automation remains a follow-up workflow.

PATCH /api/admin/users/{userId}/trust

Adjust trust info.

Request:

{
  "trustScore": 20,
  "trustGrade": "LOCAL_BEE",
  "memo": "Manual review after confirmed healthy activity"
}

Response:

{
  "success": true,
  "data": {
    "userId": 1,
    "nickname": "bee_user",
    "trustScore": 20,
    "trustGrade": "LOCAL_BEE",
    "recommendWeight": 1.00,
    "lastEvaluatedAt": "2026-05-01T10:00:00"
  },
  "message": "User trust adjusted"
}

MVP note:
Admin trust adjustment updates `user_trust.trust_score` and `user_trust.trust_grade`.
The target must be an active normal USER account.
Self-adjustment and admin-account adjustment are rejected.
The action is logged to admin_action_logs with action type USER_TRUST_ADJUST.
The optional memo is trimmed and enforced by `system_policies.admin.action_memo_max_length`.

PATCH /api/admin/users/{userId}/recommend-weight

Adjust recommendation weight.

Request:

{
  "recommendWeight": 1.25,
  "memo": "Manual recommendation influence adjustment"
}

Response:

{
  "success": true,
  "data": {
    "userId": 1,
    "nickname": "bee_user",
    "trustScore": 20,
    "trustGrade": "LOCAL_BEE",
    "recommendWeight": 1.25,
    "lastEvaluatedAt": "2026-05-01T10:00:00"
  },
  "message": "User recommendation weight adjusted"
}

MVP note:
Recommendation weight adjustment updates `user_trust.recommend_weight`.
The value must fit the `DECIMAL(4,2)` schema shape.
The target must be an active normal USER account.
The action is logged to admin_action_logs with action type USER_RECOMMEND_WEIGHT_ADJUST.
The optional memo is trimmed and enforced by `system_policies.admin.action_memo_max_length`.

15.3 Admin Place Management
GET /api/admin/places

Get place list.

Response:

{
  "success": true,
  "data": [
    {
      "placeId": 1001,
      "name": "Sangdong Sundaeguk",
      "categoryCode": "KOREAN",
      "createdByUserId": 1,
      "createdByNickname": "bee_user",
      "cityId": 1,
      "districtId": 12,
      "dongId": 123,
      "cityName": "Seoul",
      "districtName": "Mapo-gu",
      "dongName": "Seogyo-dong",
      "franchise": false,
      "franchiseReviewStatus": "PENDING",
      "approvalStatus": "APPROVED",
      "exposureStatus": "VISIBLE",
      "rankingExcluded": false,
      "starLevel": 0,
      "flowerGrade": "SEED",
      "recommendCount": 0,
      "visitCount": 0,
      "commentCount": 0,
      "createdAt": "2026-05-01T10:00:00",
      "updatedAt": "2026-05-01T10:00:00"
    }
  ],
  "message": "OK"
}

GET /api/admin/places/{placeId}

Get place detail.

Response:

{
  "success": true,
  "data": {
    "placeId": 1001,
    "name": "Sangdong Sundaeguk",
    "categoryCode": "KOREAN",
    "createdByUserId": 1,
    "createdByNickname": "bee_user",
    "cityId": 1,
    "districtId": 12,
    "dongId": 123,
    "cityName": "Seoul",
    "districtName": "Mapo-gu",
    "dongName": "Seogyo-dong",
    "addressRoad": "Some road address",
    "addressJibun": "Some jibun address",
    "latitude": 37.123456,
    "longitude": 127.123456,
    "priceRangeCode": "10000_20000",
    "recommendedMenu": "Sundaeguk",
    "shortRecommendation": "A place worth revisiting in this area",
    "featureText": "Rich soup and strong local vibe",
    "franchise": false,
    "franchiseReviewStatus": "PENDING",
    "approvalStatus": "APPROVED",
    "exposureStatus": "VISIBLE",
    "rankingExcluded": false,
    "starLevel": 0,
    "flowerGrade": "SEED",
    "recommendCount": 0,
    "visitCount": 0,
    "commentCount": 0,
    "uniqueUserCount": 0,
    "scoreTotal": 0,
    "manualAdjustmentScore": 0,
    "recentScore": 0,
    "diversityScore": 0,
    "trustWeightedScore": 0,
    "lastActivityAt": null,
    "imageUrls": [],
    "createdAt": "2026-05-01T10:00:00",
    "updatedAt": "2026-05-01T10:00:00"
  },
  "message": "OK"
}

MVP note:
Admin place read APIs return non-deleted places, including hidden or pending/rejected places, so operators can investigate moderation state.
These read-only APIs do not write admin action logs.

PATCH /api/admin/places/{placeId}/exposure

Hide or restore a place.

Request:

{
  "exposureStatus": "HIDDEN",
  "memo": "Hide while reviewing report"
}

Response:

{
  "success": true,
  "data": {
    "placeId": 1001,
    "name": "Sangdong Sundaeguk",
    "approvalStatus": "APPROVED",
    "exposureStatus": "HIDDEN",
    "franchiseReviewStatus": "PENDING"
  },
  "message": "Place exposure status changed"
}

PATCH /api/admin/places/{placeId}/approval

Change approval status.

Request:

{
  "approvalStatus": "REJECTED",
  "memo": "Rejected after manual moderation"
}

Response:

{
  "success": true,
  "data": {
    "placeId": 1001,
    "name": "Sangdong Sundaeguk",
    "approvalStatus": "REJECTED",
    "exposureStatus": "VISIBLE",
    "franchiseReviewStatus": "PENDING"
  },
  "message": "Place approval status changed"
}

PATCH /api/admin/places/{placeId}/franchise-status

Set franchise review result.

Request:

{
  "franchiseReviewStatus": "APPROVED",
  "memo": "Local independent shop confirmed"
}

Response:

{
  "success": true,
  "data": {
    "placeId": 1001,
    "name": "Sangdong Sundaeguk",
    "approvalStatus": "APPROVED",
    "exposureStatus": "VISIBLE",
    "franchiseReviewStatus": "APPROVED"
  },
  "message": "Place franchise review status changed"
}

MVP note:
Place moderation status changes are explicit and independent.
Changing approval or franchise review status does not automatically change exposure status in the MVP.
Each actual state change writes an admin_action_logs row with before and after status snapshots.

PATCH /api/admin/places/{placeId}/score-adjustment

Adjust score manually if needed.

Requires:
- memo length within `system_policies.admin.action_memo_max_length` when memo is supplied

Request:

{
  "scoreDelta": 1.25,
  "memo": "Manual adjustment after operator review"
}

Response:

{
  "success": true,
  "data": {
    "placeId": 1001,
    "name": "Sangdong Sundaeguk",
    "scoreTotal": 12.5,
    "manualAdjustmentScore": 1.25
  },
  "message": "Place score adjusted"
}

MVP note:
Manual score adjustment updates `place_stats.manual_adjustment_score` only.
It does not change recommendation, visit, comment, or `score_total` automatic aggregates.
Admin-triggered ranking recalculation adds `manual_adjustment_score` to the computed total score after automatic score components and clamps the final total score at zero.
Each adjustment writes a `PLACE_SCORE_ADJUST` row to `admin_action_logs` with before and after score snapshots.
The optional memo is trimmed and enforced by `system_policies.admin.action_memo_max_length`.

15.4 Admin Recommendation and Visit Management
GET /api/admin/recommendations

Get recommendation logs.

Response:

{
  "success": true,
  "data": [
    {
      "recommendationId": 501,
      "userId": 1,
      "nickname": "bee_user",
      "placeId": 1001,
      "placeName": "Sangdong Sundaeguk",
      "categoryCode": "KOREAN",
      "status": "ACTIVE",
      "recommendWeight": 1.10,
      "createdAt": "2026-05-01T10:00:00",
      "updatedAt": "2026-05-01T10:00:00"
    }
  ],
  "message": "OK"
}

MVP note:
Admin recommendation list returns the latest 50 recommendation rows.
This read-only API does not write admin action logs.

PATCH /api/admin/recommendations/{recommendationId}/invalidate

Invalidate recommendation.

Request:

{
  "memo": "Confirmed manipulation after report review"
}

Response:

{
  "success": true,
  "data": {
    "recommendationId": 501,
    "userId": 1,
    "nickname": "bee_user",
    "placeId": 1001,
    "placeName": "Sangdong Sundaeguk",
    "status": "INVALIDATED",
    "recommendCount": 32
  },
  "message": "Recommendation invalidated"
}

MVP note:
Admin recommendation invalidation changes ACTIVE recommendations to INVALIDATED.
It subtracts the stored recommendation weight from `place_stats.recommend_count`, `score_total`, and `trust_weighted_score`.
Already inactive recommendations return their current state without writing duplicate admin action logs.
Actual invalidation writes a `RECOMMENDATION_INVALIDATE` row to `admin_action_logs`.

GET /api/admin/visits

Get visit logs.

Response:

{
  "success": true,
  "data": [
    {
      "visitId": 701,
      "userId": 1,
      "nickname": "bee_user",
      "placeId": 1001,
      "placeName": "Sangdong Sundaeguk",
      "categoryCode": "KOREAN",
      "latitude": 37.123456,
      "longitude": 127.123456,
      "distanceMeter": 42,
      "imageUrl": "https://image.example.com/visit.jpg",
      "valid": true,
      "validReason": "WITHIN_RADIUS",
      "createdAt": "2026-05-01T10:00:00",
      "updatedAt": "2026-05-01T10:00:00"
    }
  ],
  "message": "OK"
}

MVP note:
Admin visit list returns the latest 50 visit rows, including valid and invalid rows.
This read-only API does not write admin action logs.

PATCH /api/admin/visits/{visitId}/invalidate

Invalidate visit.

Request:

{
  "memo": "GPS evidence rejected after manual review"
}

Response:

{
  "success": true,
  "data": {
    "visitId": 701,
    "userId": 1,
    "nickname": "bee_user",
    "placeId": 1001,
    "placeName": "Sangdong Sundaeguk",
    "valid": false,
    "validReason": "ADMIN_INVALIDATED",
    "visitCount": 19
  },
  "message": "Visit invalidated"
}

MVP note:
Admin visit invalidation changes valid visits to invalid and stores `ADMIN_INVALIDATED` as the validity reason.
It subtracts one visit and the current `ranking.visit_weight` policy value from `place_stats.visit_count`, `score_total`, and `trust_weighted_score`.
Already invalid visits return their current state without writing duplicate admin action logs.
Actual invalidation writes a `VISIT_INVALIDATE` row to `admin_action_logs`.

15.5 Admin Comment Management
GET /api/admin/comments

Get comment list.

Response:

{
  "success": true,
  "data": [
    {
      "commentId": 501,
      "userId": 1,
      "nickname": "bee_user",
      "placeId": 1001,
      "placeName": "Sangdong Sundaeguk",
      "categoryCode": "KOREAN",
      "content": "Rich soup and reliable quality",
      "status": "VISIBLE",
      "reportCount": 0,
      "createdAt": "2026-05-01T10:00:00",
      "updatedAt": "2026-05-01T10:00:00",
      "deletedAt": null
    }
  ],
  "message": "OK"
}

MVP note:
Admin comment list returns the latest 50 comment rows across all statuses for moderation investigation.
This read-only API does not write admin action logs.

PATCH /api/admin/comments/{commentId}/blind

Blind comment.

Request:

{
  "memo": "Offensive content hidden after review"
}

Response:

{
  "success": true,
  "data": {
    "commentId": 501,
    "userId": 1,
    "nickname": "bee_user",
    "placeId": 1001,
    "placeName": "Sangdong Sundaeguk",
    "content": "Rich soup and reliable quality",
    "status": "BLINDED",
    "commentCount": 10
  },
  "message": "Comment blinded"
}

MVP note:
Admin blind changes visible comments to `BLINDED`.
It subtracts one comment and the current `ranking.comment_weight` policy value from `place_stats.comment_count`, `score_total`, and `trust_weighted_score`.
Already non-visible comments return their current state without writing duplicate admin action logs.
Actual blind actions write a `COMMENT_BLIND` row to `admin_action_logs`.
Blinded comments cannot be restored by a user's later comment write.

DELETE /api/admin/comments/{commentId}

Delete comment.

Request:

{
  "memo": "Delete after confirmed report"
}

Response:

{
  "success": true,
  "data": {
    "commentId": 501,
    "userId": 1,
    "nickname": "bee_user",
    "placeId": 1001,
    "placeName": "Sangdong Sundaeguk",
    "content": "Rich soup and reliable quality",
    "status": "DELETED",
    "commentCount": 10
  },
  "message": "Comment deleted"
}

MVP note:
Admin delete marks comments as `DELETED`.
Deleting a visible comment subtracts one comment and the current `ranking.comment_weight` policy value from place stats.
Deleting an already blinded comment records the delete transition but does not subtract stats again.
Already deleted comments return their current state without writing duplicate admin action logs.
Actual delete actions write a `COMMENT_DELETE` row to `admin_action_logs`.

15.6 Admin Report Management
GET /api/admin/reports

Get report list.
Optional query:

status: PENDING, APPROVED, REJECTED

GET /api/admin/reports/{reportId}

Get report detail.

PATCH /api/admin/reports/{reportId}

Process report.

Request:

{
  "status": "APPROVED",
  "reviewNote": "Franchise confirmed and place hidden"
}

Response:

{
  "success": true,
  "data": {
    "reportId": 901,
    "reporterUserId": 1,
    "reporterNickname": "bee_user",
    "targetType": "PLACE",
    "targetId": 1001,
    "reasonCode": "FRANCHISE",
    "reasonText": "This appears to be a chain restaurant",
    "status": "APPROVED",
    "reviewedByUserId": 10,
    "reviewedByNickname": "admin_user",
    "reviewedAt": "2026-04-22T14:00:00",
    "reviewNote": "Franchise confirmed and place hidden",
    "createdAt": "2026-04-22T13:00:00",
    "updatedAt": "2026-04-22T14:00:00"
  },
  "message": "Report processed"
}

MVP note:
Admin report processing currently records review status only.
Allowed processing statuses are APPROVED and REJECTED.
PENDING reports cannot be changed back to PENDING through the processing API.
Already processed reports cannot be processed again.
Review note is trimmed and enforced by `report.review_note_max_length` before persistence.
Processing writes a REPORT_PROCESS row to admin_action_logs.

POST /api/admin/reports/{reportId}/actions

Apply an explicit follow-up action from a processed report.

Requires:
- approved report
- action type matching report target type
- reason length within `system_policies.report.follow_up_reason_max_length` when reason is supplied
- final action memo length within `system_policies.report.follow_up_memo_max_length` after report context is added

Request:

{
  "actionType": "HIDE_PLACE",
  "sanctionType": null,
  "reason": null,
  "startAt": null,
  "endAt": null,
  "memo": "Hide confirmed reported place"
}

Response:

{
  "success": true,
  "data": {
    "reportId": 901,
    "actionType": "HIDE_PLACE",
    "reportTargetType": "PLACE",
    "reportTargetId": 1001,
    "resultTargetType": "PLACE",
    "resultTargetId": 1001,
    "applied": true
  },
  "message": "Report follow-up action applied"
}

Supported actionType values:
- HIDE_PLACE for PLACE reports
- DELETE_PLACE for PLACE reports
- BLIND_COMMENT for COMMENT reports
- DELETE_COMMENT for COMMENT reports
- SANCTION_USER for USER reports

MVP note:
Report follow-up actions are only allowed after a report is APPROVED.
Processing a report still does not automatically hide, delete, or sanction anything.
Follow-up reason and memo are trimmed and enforced by report follow-up policy values before delegating to downstream admin workflows.
Each follow-up action delegates to the existing admin place, comment, user, or place delete workflow so the affected target is audited with its domain action log.
The report follow-up request also writes a REPORT_FOLLOW_UP row to admin_action_logs to connect the report to the explicit follow-up action.
15.7 Admin Ranking Management
GET /api/admin/seasons

Get season list.

Response:

{
  "success": true,
  "data": [
    {
      "id": 1,
      "seasonCode": "2026-04",
      "seasonName": "2026년 4월",
      "seasonType": "MONTHLY",
      "startAt": "2026-04-01T00:00:00",
      "endAt": "2026-04-30T23:59:59",
      "status": "ACTIVE"
    }
  ],
  "message": "OK"
}

POST /api/admin/seasons

Create season.

Requires:
- memo length within `system_policies.admin.action_memo_max_length` when memo is supplied

Request:

{
  "seasonCode": "2026-04",
  "seasonName": "2026년 4월",
  "seasonType": "MONTHLY",
  "startAt": "2026-04-01T00:00:00",
  "endAt": "2026-04-30T23:59:59",
  "status": "ACTIVE",
  "memo": "MVP launch season"
}

PATCH /api/admin/seasons/{seasonId}

Change season status or period.

Requires:
- memo length within `system_policies.admin.action_memo_max_length` when memo is supplied

Request:

{
  "seasonName": "2026년 4월",
  "seasonType": "MONTHLY",
  "startAt": "2026-04-01T00:00:00",
  "endAt": "2026-04-30T23:59:59",
  "status": "ACTIVE",
  "memo": "Open current season"
}

POST /api/admin/rankings/recalculate

Trigger ranking recalculation.

Requires:
- memo length within `system_policies.admin.action_memo_max_length` when memo is supplied

Request:

{
  "seasonCode": "2026-04",
  "memo": "Manual MVP recalculation"
}

Response:

{
  "success": true,
  "data": {
    "seasonCode": "2026-04",
    "placeCount": 25,
    "scoreCount": 75
  },
  "message": "Ranking recalculated"
}

Notes:
- ranking recalculation reads `place_stats`, not raw recommendation, visit, or comment rows
- `ranking.recommend_weight`, `ranking.visit_weight`, and `ranking.comment_weight` are read from `system_policies`
- places with `ranking_excluded = true` are skipped by recalculation without changing their exposure status
- one active season is allowed at a time in the MVP admin flow
- admin ranking actions are logged to `admin_action_logs`
- admin ranking and season operation memos are trimmed and enforced by `system_policies.admin.action_memo_max_length`
- the disabled-by-default ranking scheduler reuses the same recalculation service, but it is not an admin API call and does not write `admin_action_logs`

POST /api/admin/rankings/seasons/{seasonId}/finalize-history

Finalize ranking history for a season.

Requires:
- memo length within `system_policies.admin.action_memo_max_length` when memo is supplied

Request:

{
  "memo": "Finalize April ranking history"
}

Response:

{
  "success": true,
  "data": {
    "seasonId": 1,
    "seasonCode": "2026-04",
    "historyCount": 75
  },
  "message": "Ranking history finalized"
}

Notes:
- ranking history finalization reads current `place_season_scores` rows for the selected season
- seasons without score rows are rejected so an existing history snapshot is not accidentally cleared
- existing `place_ranking_history` rows for that season are deleted before inserting the current snapshot
- this makes repeated finalization idempotent for the same season snapshot and prevents duplicate season/region/place history rows
- finalization does not change public ranking reads, which continue to use `place_season_scores`
- actual finalization actions are logged to `admin_action_logs` with action type `RANKING_HISTORY_FINALIZE`
- the optional memo is trimmed and enforced by `system_policies.admin.action_memo_max_length`

PATCH /api/admin/rankings/places/{placeId}/exclude

Exclude place from ranking.

Requires:
- memo length within `system_policies.admin.action_memo_max_length` when memo is supplied

Request:

{
  "excluded": true,
  "memo": "Exclude while reviewing ranking manipulation report"
}

Response:

{
  "success": true,
  "data": {
    "placeId": 1001,
    "name": "Sangdong Sundaeguk",
    "rankingExcluded": true,
    "starLevel": 0
  },
  "message": "Place ranking exclusion changed"
}

MVP note:
Ranking exclusion updates `places.ranking_excluded`.
It does not change approval status, exposure status, or automatic aggregate scores.
When a place is excluded, its current star level is reset to 0 immediately.
Restoring a place does not calculate a star immediately; it becomes eligible on the next admin-triggered ranking recalculation.
Actual state changes write a `PLACE_RANKING_EXCLUSION_UPDATE` row to `admin_action_logs`.
The optional memo is trimmed and enforced by `system_policies.admin.action_memo_max_length`.

15.8 Admin Policy Management
GET /api/admin/policies

Get all policies.

PATCH /api/admin/policies/{policyKey}

Update policy value.
Requires SUPER_ADMIN.

Request:

{
  "value": "20",
  "memo": "Increase launch-period recommendation limit"
}

Notes:
- one active recommendation per user/place is allowed
- daily limit is read from `system_policies.recommend.daily_limit`
- recommendation weight is read from `user_trust.recommend_weight`
- recommend/cancel updates `place_stats`
- list-style policies such as level thresholds and trust weights use semicolon-separated `key:value` entries

Typical policy keys:

recommend.daily_limit
visit.radius_meter
visit.cooldown_hour
visit.image_url_max_length
growth.visit_exp
growth.level_exp_thresholds
trust.valid_visit_score
trust.grade_thresholds
trust.recommend_weight_by_grade
ranking.recommend_weight
ranking.visit_weight
ranking.comment_weight
fraud.rapid_participation_window_minutes
fraud.rapid_participation_threshold
fraud.rapid_participation_risk_score
fraud.ip_spam_window_minutes
fraud.ip_spam_distinct_user_threshold
fraud.ip_spam_risk_score
fraud.gps_teleport_speed_kmh
fraud.gps_teleport_zero_second_distance_km
fraud.gps_teleport_risk_score
fraud.alert_trust_penalty
audience.minimum_participants
audience.foreigner_ratio_threshold
audience.gender_ratio_threshold
audience.age_ratio_threshold
admin.sanction_reason_max_length
admin.action_memo_max_length
place.image_url_max_length
place.address_max_length
region.change_cooldown_day
15.9 Admin Region Policy Management
GET /api/admin/policies/region

Get region policy settings.

PATCH /api/admin/policies/region

Update region policy settings.

Request:

{
  "regionChangeCooldownDays": 7,
  "registrationScope": "DISTRICT"
}

Notes:
- policy changes are logged to admin_action_logs
- default policy rows can be bootstrapped by setting POLICY_SEED_ENABLED=true
15.10 Admin Action Logs
GET /api/admin/action-logs

Get admin action history.

Response:

{
  "success": true,
  "data": [
    {
      "logId": 9001,
      "adminUserId": 10,
      "adminNickname": "admin_user",
      "actionType": "COMMENT_BLIND",
      "targetType": "COMMENT",
      "targetId": 501,
      "beforeValue": "{\"status\":\"VISIBLE\"}",
      "afterValue": "{\"status\":\"BLINDED\"}",
      "memo": "Offensive content hidden after review",
      "createdAt": "2026-05-02T10:30:00"
    }
  ],
  "message": "OK"
}

MVP note:
Admin action log list returns the latest 50 audit rows.
It is read-only and does not write another admin action log.

15.11 Admin User Action Logs
GET /api/admin/user-action-logs

Get user participation action history.

Response:

{
  "success": true,
  "data": [
    {
      "logId": 7001,
      "userId": 1,
      "nickname": "bee_user",
      "actionType": "VISIT_VERIFY",
      "targetType": "PLACE",
      "targetId": 1001,
      "ipAddress": null,
      "userAgent": null,
      "metadataJson": "{\"distanceMeter\":42}",
      "createdAt": "2026-05-02T11:20:00"
    }
  ],
  "message": "OK"
}

MVP note:
Admin user action log list returns the latest 50 user action rows.
It is read-only and does not write admin_action_logs or user_action_logs.
The endpoint is separated under `/api/admin` and is intended for operation, abuse investigation, and participation audit review.

15.12 Admin Fraud Management
GET /api/admin/fraud/alerts

Get all fraud alerts flagged by the system.

Response:

{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 1,
      "alertType": "RAPID_PARTICIPATION",
      "riskScore": 1.0,
      "description": "동일 유저가 5.0초 내에 연속된 행동을 감지했습니다. (직전: PLACE_CREATE, 현재: RECOMMENDATION_CREATE)",
      "ipAddress": "127.0.0.1",
      "targetId": 100,
      "createdAt": "2026-05-23T14:30:00"
    }
  ],
  "message": "OK"
}

GET /api/admin/fraud/suspicious-users

Get list of suspicious users with their cumulative fraud alert count.

Response:

{
  "success": true,
  "data": [
    {
      "userId": 1,
      "nickname": "spammer_bee",
      "email": "spammer@example.com",
      "phone": "01012345678",
      "alertCount": 8,
      "trustScore": 75,
      "status": "ACTIVE"
    }
  ],
  "message": "OK"
}

15.13 Operational Health Check
GET /actuator/health

Purpose:
Returns the backend runtime health status for local checks, deployment probes, and load balancer health checks.

Response:

{
  "status": "UP"
}

MVP note:
Only the health actuator endpoint is exposed by default.
Health detail output is hidden by default with `management.endpoint.health.show-details=never`.
Liveness and readiness probe groups are enabled for deployment environments through Spring Boot Actuator.
Other actuator endpoints must remain disabled unless explicitly needed and reviewed.

16. Error Code Recommendations

The following error codes are recommended for consistency:

UNAUTHORIZED
FORBIDDEN
PHONE_VERIFICATION_REQUIRED
REGION_VERIFICATION_REQUIRED
DAILY_RECOMMEND_LIMIT_EXCEEDED
RECOMMEND_ALREADY_EXISTS
COMMENT_ALREADY_EXISTS
VISIT_COOLDOWN_ACTIVE
OUT_OF_VISIT_RADIUS
PLACE_REGISTRATION_LIMIT_EXCEEDED
POLICY_VIOLATION
INVALID_REGION_CHANGE
NOT_PLACE_OWNER
ADMIN_ONLY
RESOURCE_NOT_FOUND
INVALID_REQUEST
USER_SANCTION_ACTIVE
17. API Development Priority

Recommended implementation order:

Phase 1: Core MVP
auth
users/me
regions/verify
places
place detail
nearby places
recommend
visit
comment
place rankings
reports
admin dashboard
admin reports
admin policies
Phase 2: Growth and Operations
growth info
missions
user rankings
ranking history
audience tags
admin recommendation/visit controls
Phase 3: Expansion
notifications
advanced analytics endpoints
richer admin controls
automated abuse tooling support
18. Final Notes

This API is intentionally organized by domain and policy responsibility.

Important principles:

server must validate all critical rules
frontend should not decide business validity
admin APIs must be separate
ranking should use aggregated score tables
policy-based settings must remain adjustable without code changes
