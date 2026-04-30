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
    "languagePreference": "ko"
  },
  "message": "OK"
}
4.2 Update My Profile
PATCH /api/users/me

Request:

{
  "nickname": "new_bee_user",
  "languagePreference": "en"
}
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
  "message": "Place created"
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

6.4 Get Places by Region
GET /api/places?cityId={cityId}&districtId={districtId}&dongId={dongId}
6.5 Search Places
GET /api/places/search?keyword={keyword}
6.6 Get Popular Places
GET /api/places/popular?regionType={regionType}&regionId={regionId}
6.7 Update Place
PATCH /api/places/{placeId}

Notes:

only place owner or admin may update
server must validate permissions
6.8 Delete Place
DELETE /api/places/{placeId}

Notes:

delete may be logical delete
only place owner or admin may delete
6.9 Get My Registered Places
GET /api/users/me/places
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
- GPS distance within `system_policies.visit.radius_meter`
- no active cooldown from `system_policies.visit.cooldown_hour`

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
`expGained` currently returns 0 until the trust/level growth system is implemented.
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
- one visible comment per user/place

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
14. Notification API

This can be added after MVP, but should be reserved in API design.

14.1 Get Notifications
GET /api/notifications
14.2 Mark Notification as Read
PATCH /api/notifications/{notificationId}/read
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
15.2 Admin User Management
GET /api/admin/users

Get user list.

GET /api/admin/users/{userId}

Get user detail.

POST /api/admin/users/{userId}/sanctions

Apply sanction.

PATCH /api/admin/users/{userId}/trust

Adjust trust info.

PATCH /api/admin/users/{userId}/recommend-weight

Adjust recommendation weight.

15.3 Admin Place Management
GET /api/admin/places

Get place list.

GET /api/admin/places/{placeId}

Get place detail.

PATCH /api/admin/places/{placeId}/exposure

Hide or restore a place.

PATCH /api/admin/places/{placeId}/approval

Change approval status.

PATCH /api/admin/places/{placeId}/franchise-status

Set franchise review result.

PATCH /api/admin/places/{placeId}/score-adjustment

Adjust score manually if needed.

15.4 Admin Recommendation and Visit Management
GET /api/admin/recommendations

Get recommendation logs.

PATCH /api/admin/recommendations/{recommendationId}/invalidate

Invalidate recommendation.

GET /api/admin/visits

Get visit logs.

PATCH /api/admin/visits/{visitId}/invalidate

Invalidate visit.

15.5 Admin Comment Management
GET /api/admin/comments

Get comment list.

PATCH /api/admin/comments/{commentId}/blind

Blind comment.

DELETE /api/admin/comments/{commentId}

Delete comment.

15.6 Admin Report Management
GET /api/admin/reports

Get report list.

GET /api/admin/reports/{reportId}

Get report detail.

PATCH /api/admin/reports/{reportId}

Process report.

Request:

{
  "status": "APPROVED",
  "reviewNote": "Franchise confirmed and place hidden"
}
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
- one active season is allowed at a time in the MVP admin flow
- admin ranking actions are logged to `admin_action_logs`

PATCH /api/admin/rankings/places/{placeId}/exclude

Exclude place from ranking.

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

Typical policy keys:

recommend.daily_limit
visit.radius_meter
visit.cooldown_hour
ranking.recommend_weight
ranking.visit_weight
ranking.comment_weight
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
