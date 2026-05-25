# 🧠 Architectural Decisions

This document records key decisions made during the design of Honeytong.

Its purpose is to:
- explain WHY decisions were made
- prevent inconsistent changes later
- provide context for future development
- guide refactoring and scaling

---

## 1. Monolithic Architecture (MVP)

### Decision
Use Spring Boot monolithic architecture.

### Reason
- faster MVP development
- simpler deployment
- easier coordination of policies and business rules

### Trade-off
- less scalable than microservices

### Future Plan
- separate batch processing if needed
- split domains only when complexity increases

---

## 2. Domain-Oriented Structure

### Decision
Organize code by domain, not by technical layer.

### Reason
- improves maintainability
- aligns with business logic
- easier to scale features independently

---

## 3. Policy-Driven System

### Decision
All critical values must be stored in DB policies.

### Examples
- recommendation limits
- visit radius
- cooldown durations
- ranking weights

### Reason
- admin must control system behavior without code changes
- enables fast iteration and tuning

### Trade-off
- increased complexity in implementation

---

## 4. Aggregation-Based Ranking

### Decision
Ranking is NOT calculated in real-time.

### Instead
- store activity events
- aggregate scores periodically
- read from precomputed tables

### Reason
- performance optimization
- predictable ranking results
- easier admin control

---

## 5. Trust-Based Influence System

### Decision
User actions are weighted by trust.

### Reason
- prevent abuse
- reward active and reliable users
- maintain ranking quality

### Trade-off
- requires additional tracking and calculation

---

## 6. Visit Verification with GPS

### Decision
Visits require GPS-based validation.

### Reason
- ensure real-world interaction
- increase credibility of recommendations

### Trade-off
- GPS spoofing risk
- device dependency

### Mitigation
- cooldown limits
- anomaly detection (future)

---

## 7. One Action Per User Rule

### Decision
- one recommendation per user per place
- one comment per user per place

### Reason
- prevent spam
- simplify logic
- maintain fairness

---

## 8. Cooldown-Based Controls

### Decision
Use cooldown instead of unlimited actions.

### Examples
- visit cooldown (24h)
- region change cooldown
- daily recommendation limit

### Reason
- control abuse
- encourage natural usage patterns

---

## 9. Region-Based System (Hyperlocal)

### Decision
System is built around:
- city
- district
- dong

### Reason
- hyperlocal discovery is core value
- aligns with Korean user behavior (당근 스타일)

---

## 10. Place Registration Limitation

### Decision
Users have limited place registration capacity.

### Reason
- prevent spam content
- increase quality of entries
- encourage thoughtful contribution

---

## 11. Franchise Restriction Policy

### Decision
Franchise places are restricted.

### Reason
- maintain "local hidden gem" concept
- differentiate from generic review platforms

### Trade-off
- requires moderation
- potential ambiguity

---

## 12. Admin-Centric Control System

### Decision
Admin has strong control over:
- users
- places
- reports
- policies
- rankings

### Reason
- early-stage system needs manual intervention
- abuse must be controllable

---

## 13. Mobile-First Design

### Decision
System is designed primarily for mobile.

### Reason
- location-based behavior
- real-time interaction
- map-based UX

---

## 14. Web as Secondary Platform

### Decision
Web supports browsing and admin usage.

### Reason
- mobile is primary usage
- admin tools require larger interface

---

## 15. Redis for Runtime Control

### Decision
Use Redis for:
- cooldown
- daily limits
- temporary states
- caching

### Reason
- fast access
- reduces DB load

---

## 16. Separation of User and Admin APIs

### Decision
Admin APIs must be separated from user APIs.

### Reason
- security
- clarity
- easier permission control

---

## 16.1 Access Token and Refresh Token Structure

### Decision
Use short-lived JWT access tokens and server-managed refresh tokens.

### Rules
- store only hashed refresh tokens
- rotate refresh tokens on every successful refresh
- revoke refresh tokens on logout
- keep token durations in configuration, not code

### Reason
- access tokens keep normal API requests stateless
- refresh tokens can be revoked when sessions must end
- rotation reduces damage from leaked refresh tokens

---

## 16.2 Phone Verification Delivery

### Decision
Use a sender interface for phone verification code delivery.

### Current Implementation
- development sender logs code issuance without the raw code at INFO level
- `PHONE_VERIFICATION_SENDER_PROVIDER=dev` keeps local delivery on the development sender by default
- `PHONE_VERIFICATION_SENDER_PROVIDER=solapi` enables the SOLAPI SMS adapter
- `PHONE_VERIFICATION_SENDER_PROVIDER=naver-sens` enables the Naver Cloud SENS SMS adapter
- the production profile defaults the sender provider to `solapi` so production does not silently use the development sender and can support non-business-account MVP testing
- the SOLAPI adapter signs requests with HMAC-SHA256 using the configured API secret and posts to `/messages/v4/send-many/detail`
- the SENS adapter signs requests with the provider's Signature V2 HMAC-SHA256 headers and posts to `/sms/v2/services/{serviceId}/messages`
- provider credentials, sender number, country code, API base URL, timeouts, and message template are environment-driven
- verification codes are stored as hashes
- code length, expiry, and max attempts are configurable
- core actions can use the server-side `@RequirePhoneVerified` guard
- the verification code cache is written only after sender delivery succeeds, so failed SMS delivery does not leave a latest-code cache entry

### Future Plan
- run a live credential smoke test before marking MVP phone verification complete
- keep the sender interface so another provider can replace SENS if cost, reliability, or compliance requirements change

### Reason
- backend validation can be built before choosing the SMS provider
- production credentials do not need to be committed to the repository
- sender implementation can change without changing controller or service logic
- SOLAPI personal accounts can be used for early MVP validation when Naver Cloud SENS business-account restrictions block setup

---

## 16.3 Region GPS Resolver Boundary

### Decision
Separate GPS-to-administrative-dong resolution behind a `RegionCoordinateResolver` interface.

### Current Implementation
- region lookup APIs read from `region_city`, `region_district`, and `region_dong`
- GPS region verification flow saves the resolved dong as the user's primary region
- Kakao Local API `coord2regioncode` is used when `app.maps.provider=kakao`
- Kakao administrative region documents (`region_type = H`) are preferred
- Kakao region codes are matched against `region_dong.code`

### Reason
- administrative dong mapping requires reliable region boundary data or provider integration
- hardcoding coordinate-to-region rules would be inaccurate and difficult to maintain
- region verification must remain server-side and replaceable

---

## 16.4 Map Provider for MVP

### Decision
Use Kakao Map and Kakao Local API as the MVP map provider.

### Current Preparation
- API keys are loaded from local environment variables
- `.env` is ignored by Git and `.env.example` documents required variable names
- backend reads `KAKAO_REST_API_KEY`, `KAKAO_JAVASCRIPT_KEY`, and `KAKAO_LOCAL_BASE_URL`
- frontend reads `VITE_KAKAO_MAP_JAVASCRIPT_KEY` for the Kakao Maps JavaScript SDK, with `VITE_KAKAO_JAVASCRIPT_KEY` kept as a legacy local-development fallback
- frontend `vite.config.ts` sets `envDir` to the repository root so `cd frontend && npm run dev` can read the root `.env`
- Home and place Detail use a shared Kakao Maps JavaScript SDK loader when the frontend Kakao JavaScript key is present
- Home renders markers from place API latitude/longitude fields, and Detail renders the selected place coordinate when available
- When the frontend map key is missing, map surfaces show Korean configuration states instead of a fixed placeholder map
- Kakao Developers JavaScript SDK domains must include the actual frontend origins that load the SDK, such as `http://localhost:5173` and `http://127.0.0.1:5173`; add any additional deployed, staging, LAN, or alternate dev-server origins before using them
- After changing `.env` values for Vite, restart `npm run dev` because Vite only exposes environment variables from the dev-server process environment at startup
- Flutter mobile initializes both Kakao Maps and Kakao Login when `KAKAO_NATIVE_APP_KEY` is supplied by dart define; placeholder native app keys must not be committed.
- Flutter mobile Home uses `kakao_maps_flutter` for the actual Kakao native map when the native app key is configured, and falls back to a Korean configuration state when the key is missing.
- The map renders current-location and place markers from API coordinates. Marker clicks route to the existing place detail flow.
- Android native map setup includes the Kakao Maps Maven repository in Gradle, but each runtime environment still needs Kakao platform registration, native app key, and key hash registration.
- Development-only mock Kakao login is disabled by default and can only be enabled explicitly with `--dart-define=HONEY_ALLOW_MOCK_KAKAO_LOGIN=true`.

### Reason
- Kakao Local API directly supports coordinate-to-region-code conversion
- Kakao Local API also supports keyword/category place search with coordinates and radius
- this fits region verification and nearby place discovery before the place domain is implemented
- Place discovery should use API-backed coordinates for map markers instead of hardcoded visual marker positions

---

## 16.5 Region Seed Import

### Decision
Use an opt-in CSV import path for region seed data.

### Current Implementation
- import is disabled by default
- `REGION_SEED_ENABLED=true` enables import on backend startup
- `REGION_SEED_LOCATION` points to a UTF-8 CSV file
- CSV rows use city, district, and administrative dong codes
- `region_dong.code` must match Kakao administrative dong codes returned by `coord2regioncode`
- the full seed CSV is generated from `KIKcd_H.20260325.xlsx` using `scripts/convert_korean_region_seed.py`

### Reason
- region data is operational reference data and should not change accidentally on every development run
- the Kakao resolver needs stable administrative dong codes in DB
- the import path lets the project add a complete national seed file later without changing service logic

---

## 16.6 Region Change Policy

### Decision
Region change cooldown must be read from `system_policies`.

### Current Implementation
- `PATCH /api/regions/me` changes the user's primary region by `dongId`
- `GET /api/regions/me/change-policy` returns cooldown availability
- the required policy key is `region.change_cooldown_day`
- if the policy is missing or invalid, the server rejects region change
- GPS verification through `POST /api/regions/verify` must also respect the same cooldown when the resolved dong differs from the user's current primary dong.
- Redis locking is used only as a concurrency guard; if Redis is unavailable, region changes continue through DB-backed policy checks instead of failing solely because the lock store is down.

### Reason
- region change is a business policy and must remain admin-adjustable
- place registration, recommendation, visit, and ranking depend on stable primary region behavior

---

## 16.7 Policy Seed Import and Admin Policy Management

### Decision
Default system policies can be imported from a UTF-8 CSV seed file, and admins manage policies through separated admin APIs.

### Current Implementation
- Local development enables missing-only policy seed import by default so empty development databases have required policies after startup
- Production keeps policy seed import disabled by default unless `POLICY_SEED_ENABLED=true` is set
- `POLICY_SEED_LOCATION` defaults to `classpath:policy/policy-defaults.csv`
- `GET /api/admin/policies` is available to ADMIN and SUPER_ADMIN users
- policy updates are restricted to SUPER_ADMIN users
- policy updates are logged to `admin_action_logs`

### Reason
- business policy values must live in the database, not application logic
- seed import gives local and initial environments a repeatable bootstrap path
- missing-only import avoids overwriting values changed by admins
- missing-only import prevents login/setup flows from failing on empty local databases
- policy changes need an audit trail because they affect platform rules

---

## 16.8 MVP Place Registration Policy

### Decision
Place registration uses a simple MVP policy model: one per-user registration limit and one region scope.

### Current Implementation
- `POST /api/places` requires phone verification
- user primary region verification is required before place registration
- `place.registration_limit` controls how many active places a user can register
- `region.registration_scope` controls whether registration is allowed within DONG, DISTRICT, or CITY
- place stats are initialized when a place is created

### Reason
- this keeps the first place API small and testable
- registration remains policy-driven from the beginning
- the model can later expand to level-based or region-specific limits without changing the core place table

---

## 16.9 MVP Recommendation Policy

### Decision
Recommendation is a positive-only user action with one active recommendation per user/place.

### Current Implementation
- `POST /api/places/{placeId}/recommend` requires phone verification
- `DELETE /api/places/{placeId}/recommend` cancels an active recommendation
- duplicate active recommendations are rejected
- `recommend.daily_limit` controls the daily recommendation limit
- `user_trust.recommend_weight` is stored on the recommendation and applied to place stats
- recommend/cancel updates `place_stats.recommend_count`, `score_total`, and `trust_weighted_score`

### Reason
- positive-only recommendation keeps MVP interaction simple and low-conflict
- daily limits and phone verification reduce basic abuse
- storing the weight on each recommendation preserves the action's historical influence for later ranking aggregation

---

## 16.10 Local Admin Bootstrap

### Decision
Provide a disabled-by-default local admin bootstrap path for development and MVP testing.

### Current Implementation
- `ADMIN_BOOTSTRAP_ENABLED=false` keeps the flow inactive by default
- when enabled, the backend creates or promotes one LOCAL account to ADMIN or SUPER_ADMIN on startup
- optional phone verification can be applied to the bootstrap account for later core-action testing
- existing accounts keep their password unless `ADMIN_BOOTSTRAP_RESET_PASSWORD=true`
- bootstrap actions are written to `admin_action_logs`

### Reason
- admin policy testing should not require manual database edits
- later place registration tests need a repeatable privileged test account path
- no public admin account creation API is exposed for MVP safety

---

## 16.11 MVP Visit Verification Policy

### Decision
Visit verification uses stored GPS coordinates, policy-driven radius, and policy-driven cooldown.

### Current Implementation
- `POST /api/places/{placeId}/visits` requires phone verification
- `GET /api/users/me/visits` returns the authenticated user's valid visits
- `GET /api/places/{placeId}/visits/summary` returns aggregate visit count and latest valid visit time
- `visit.radius_meter` controls the maximum allowed GPS distance from the place
- `visit.cooldown_hour` controls when the same user can verify the same place again
- `ranking.visit_weight` is applied when a valid visit updates `place_stats`
- valid visits are stored in `visits` with `is_valid = true`
- valid visits delegate EXP and trust score updates to `UserGrowthService`

### Reason
- visits are the most trusted MVP action and need server-side validation
- radius and cooldown must remain admin-adjustable
- ranking should read from aggregated stats later instead of calculating raw events on every request

---

## 16.12 MVP Comment Policy

### Decision
Comments are short, owner-controlled context attached to a place, with one visible comment per user/place.

### Current Implementation
- `POST /api/places/{placeId}/comments` requires phone verification
- `PATCH /api/comments/{commentId}` requires phone verification and comment ownership
- `DELETE /api/comments/{commentId}` marks the comment deleted
- `GET /api/places/{placeId}/comments` returns visible comments for a place
- `GET /api/users/me/comments` returns visible comments written by the authenticated user
- a deleted comment row is restored when the same user writes again for the same place
- `ranking.comment_weight` is applied when visible comments update `place_stats`

### Reason
- short comments add useful local context without turning the MVP into a long-form review product
- one visible comment per user/place prevents spam and keeps moderation simpler
- restoring deleted rows respects the database unique key while allowing users to write again later

---

## 16.13 MVP Ranking Read Foundation

### Decision
Public place ranking reads use precomputed season score rows only.

### Current Implementation
- `GET /api/rankings/places` reads `place_season_scores` by season, region type, and region ID
- `GET /api/rankings/seasons/current` returns the active season from `seasons`
- supported ranking region types are DONG, DISTRICT, and CITY
- hidden or deleted places are excluded from ranking responses
- audience tags return an empty list until audience aggregation is implemented

### Reason
- ranking reads must stay fast and predictable
- raw recommendation, visit, and comment rows should feed aggregation jobs, not public read queries
- adding the read contract now lets the frontend and later admin/batch work connect without reshaping the API

---

## 16.14 MVP Ranking Aggregation and Admin Season Control

### Decision
MVP ranking recalculation is an admin-triggered operation that rewrites season score rows from aggregate place stats.

### Current Implementation
- admins can list, create, and update ranking seasons through `/api/admin/seasons`
- admins can trigger place ranking recalculation through `POST /api/admin/rankings/recalculate`
- one ACTIVE season is allowed at a time in the MVP admin flow
- recalculation reads `place_stats`, not raw recommendation, visit, or comment rows
- recalculation writes dong, district, and city rows into `place_season_scores`
- ranking weights are loaded from `system_policies` keys `ranking.recommend_weight`, `ranking.visit_weight`, and `ranking.comment_weight`
- the top place in dong/district/city receives star levels 1/2/3 for that season result
- admin season and recalculation actions are logged to `admin_action_logs`

### Reason
- public ranking reads need precomputed rows to stay fast and predictable
- the MVP needs a controlled manual recalculation path before adding a scheduler
- keeping the trigger under admin APIs preserves operator control and auditability

---

## 16.15 MVP Report Foundation

### Decision
User-created reports are stored as moderation queue items and do not apply automatic sanctions or visibility changes.

### Current Implementation
- `POST /api/reports` creates a PENDING report for PLACE, COMMENT, or USER targets
- `GET /api/users/me/reports` returns reports created by the authenticated user
- report creation validates that the reporter is active
- PLACE and COMMENT targets must exist and be visible
- USER targets must exist, be active, and cannot be the reporter

### Reason
- reports are important trust signals but should be reviewed before action in the MVP
- keeping creation separate from admin processing prevents accidental automated punishment
- the target-type model lets admin moderation later connect place hiding, comment deletion, and user sanctions without reshaping the user API

---

## 16.16 MVP Admin Report Processing

### Decision
Admin report processing records review decisions first and keeps moderation effects as separate workflows.

### Current Implementation
- `GET /api/admin/reports` lists reports and can filter by status
- `GET /api/admin/reports/{reportId}` returns report detail
- `PATCH /api/admin/reports/{reportId}` processes PENDING reports as APPROVED or REJECTED
- processed reports record `reviewed_by`, `reviewed_at`, and `review_note`
- processing writes a `REPORT_PROCESS` row to `admin_action_logs`
- already processed reports cannot be processed again

### Reason
- report review should be auditable before destructive moderation actions exist
- separating review from hide/delete/sanction reduces accidental user-impacting actions
- admin tools can later apply explicit follow-up actions while preserving the original review trail

---

## 16.17 MVP Admin Dashboard Metrics

### Decision
Admin dashboard metrics are read-only count queries over existing aggregate/event tables.

### Current Implementation
- `GET /api/admin/dashboard` returns today non-deleted new users, today active recommendations, today valid visits, pending reports, and today non-deleted new places
- today is calculated from the server's current local date using `[00:00, next day 00:00)`
- pending reports count all PENDING reports, not only today's reports
- the dashboard API is separated under `/api/admin` and requires ADMIN or SUPER_ADMIN

### Reason
- MVP operations need a quick health snapshot without complex analytics
- count queries avoid loading moderation or activity lists into memory
- keeping the endpoint read-only avoids unnecessary admin action logs

---

## 16.18 MVP Admin User Read Foundation

### Decision
Admin user management starts with read-only list and detail APIs.

### Current Implementation
- `GET /api/admin/users` returns core user rows ordered by creation time descending
- `GET /api/admin/users/{userId}` returns core user data plus trust and level snapshots when those rows exist
- both APIs are separated under `/api/admin` and require ADMIN or SUPER_ADMIN
- the read-only APIs do not write `admin_action_logs`; later sanction and trust changes must be logged

### Reason
- operators need user visibility before applying sanctions or trust changes
- exposing trust and level snapshots supports report investigation without changing user state
- keeping moderation actions as separate APIs reduces accidental user-impacting changes

---

## 16.19 MVP Admin User Sanction Foundation

### Decision
User sanctions are recorded as auditable admin actions before adding broader enforcement automation.

### Current Implementation
- `POST /api/admin/users/{userId}/sanctions` creates an ACTIVE `user_sanctions` row
- supported sanction types are WARNING, TEMPORARY_RESTRICTION, and PERMANENT_RESTRICTION
- the endpoint rejects self-sanctioning and admin-account sanctioning
- when a target user's trust row exists, `user_trust.sanction_count` is incremented
- sanction creation writes a `USER_SANCTION` row to `admin_action_logs`

### Reason
- operators need a clear moderation record before automated restriction behavior is widened
- audit logging is mandatory for user-impacting admin actions
- keeping enforcement as a separate follow-up lets the product define exactly which actions each active sanction should block

---

## 16.20 MVP Active User Sanction Enforcement

### Decision
Active blocking sanctions prevent new user participation actions, while non-destructive undo actions remain allowed.

### Current Implementation
- `TEMPORARY_RESTRICTION` and `PERMANENT_RESTRICTION` with status ACTIVE block core write actions when `start_at <= now` and `end_at` is null or in the future
- blocked actions are place creation, recommendation creation, visit verification, comment creation, and comment update
- `WARNING` is treated as an audit/trust signal and does not block actions
- recommendation cancellation and comment deletion remain allowed so users can undo participation
- enforcement is centralized through `@RequireNoActiveSanction`

### Reason
- server-side enforcement is required for abuse prevention
- warnings should not unintentionally lock users out of participation
- allowing cancellation/deletion reduces harm and helps users clean up their own activity
- expiration status automation can be added later without changing the core enforcement rule

---

## 16.21 MVP Admin Trust Adjustment Foundation

### Decision
Admin trust changes are explicit, audited operations over existing `user_trust` fields.

### Current Implementation
- `PATCH /api/admin/users/{userId}/trust` updates `trust_score` and `trust_grade`
- `PATCH /api/admin/users/{userId}/recommend-weight` updates `recommend_weight`
- both endpoints require ADMIN or SUPER_ADMIN through separated admin routes and service validation
- targets must be active normal USER accounts; self-adjustment and admin-account adjustment are rejected
- trust adjustment writes `USER_TRUST_ADJUST` to `admin_action_logs`
- recommendation weight adjustment writes `USER_RECOMMEND_WEIGHT_ADJUST` to `admin_action_logs`
- recommendation weight is validated against the current `DECIMAL(4,2)` schema shape

### Reason
- trust values affect ranking influence and need a clear audit trail
- keeping trust score/grade adjustment separate from recommendation weight adjustment makes operator intent explicit
- future policy-driven trust formulas can be added without removing the manual admin override path

---

## 16.22 MVP Admin Place Read Foundation

### Decision
Admin place management starts with read-only list and detail APIs before mutation workflows.

### Current Implementation
- `GET /api/admin/places` returns non-deleted places ordered by creation time descending
- `GET /api/admin/places/{placeId}` returns one non-deleted place detail
- both APIs require ADMIN or SUPER_ADMIN through separated admin routes and service validation
- responses include creator, region, approval status, exposure status, franchise review status, image URLs, and place_stats aggregate values
- the read-only APIs do not write `admin_action_logs`; later exposure, approval, and franchise status changes must be logged

### Reason
- operators need visibility into moderation state before changing place visibility or approval
- hidden or pending/rejected places must remain visible to admins even when user-facing place APIs hide them
- keeping state changes as separate APIs reduces accidental moderation changes and preserves audit clarity

---

## 16.23 MVP Admin Place Moderation Status Control

### Decision
Place moderation state changes are explicit, separated admin operations.

### Current Implementation
- `PATCH /api/admin/places/{placeId}/exposure` updates `places.exposure_status`
- `PATCH /api/admin/places/{placeId}/approval` updates `places.approval_status`
- `PATCH /api/admin/places/{placeId}/franchise-status` updates `places.franchise_review_status`
- all three APIs require ADMIN or SUPER_ADMIN through separated admin routes and service validation
- actual state changes write `admin_action_logs` with action types `PLACE_EXPOSURE_UPDATE`, `PLACE_APPROVAL_UPDATE`, and `PLACE_FRANCHISE_REVIEW_UPDATE`
- before and after log values store approval, exposure, and franchise review status snapshots
- no-op requests that keep the same status return the current state without writing a duplicate action log

### Reason
- exposure, approval, and franchise review mean different operational decisions and should remain auditable separately
- automatic side effects could hide or publish places accidentally, so MVP moderation keeps each state change explicit
- no-op idempotency prevents repeated client retries from creating misleading moderation history

---

## 16.24 MVP Admin Place Score Adjustment Foundation

### Decision
Manual place score adjustments are stored separately from automatic activity aggregates.

### Current Implementation
- `PATCH /api/admin/places/{placeId}/score-adjustment` changes `place_stats.manual_adjustment_score`
- the endpoint accepts a signed decimal `scoreDelta` and rejects zero or values with more than two decimal places
- `score_total`, recommendation count, visit count, comment count, and trust-weighted score are not changed by the manual adjustment API
- each adjustment writes `PLACE_SCORE_ADJUST` to `admin_action_logs` with before and after score snapshots
- admin-triggered ranking recalculation adds `manual_adjustment_score` after automatic score components and clamps the final total score at zero

### Reason
- operators need a small override path for early-stage data imbalance and moderation decisions
- separating manual adjustment from automatic aggregates keeps activity data auditable and reversible
- applying the adjustment during recalculation preserves the aggregation-based ranking architecture and avoids public read-time scoring

---

## 16.25 MVP Admin Ranking Exclusion Control

### Decision
Ranking exclusion is an explicit place-level admin flag that is independent from normal exposure status.

### Current Implementation
- `PATCH /api/admin/rankings/places/{placeId}/exclude` updates `places.ranking_excluded`
- excluded places are skipped by admin-triggered ranking recalculation
- excluding a place immediately resets `places.current_star_level` to 0
- restoring a place does not immediately calculate a star; the next ranking recalculation determines its rank and star
- actual state changes write `PLACE_RANKING_EXCLUSION_UPDATE` to `admin_action_logs`
- no-op requests that keep the same exclusion state return the current state without writing a duplicate action log

### Reason
- operators need a ranking fairness control that does not necessarily hide a place from discovery
- keeping exclusion separate from exposure avoids mixing moderation visibility with ranking eligibility
- recalculation remains aggregation-based while still respecting operator controls

---

## 16.26 MVP Admin Recommendation and Visit Read Foundation

### Decision
Admin recommendation and visit management starts with read-only log lists before invalidation workflows.

### Current Implementation
- `GET /api/admin/recommendations` returns the latest 50 recommendation rows
- `GET /api/admin/visits` returns the latest 50 visit rows, including valid and invalid rows
- responses include user, place, status or validity, score weight or GPS context, and timestamps
- both APIs require ADMIN or SUPER_ADMIN through separated admin routes and service validation
- read-only list APIs do not write `admin_action_logs`; later invalidation actions must be logged

### Reason
- operators need investigation context before invalidating activity that affects ranking and place stats
- keeping read and mutation workflows separate reduces accidental ranking-impacting changes
- showing both valid and invalid visit rows helps review GPS, image, and distance behavior without changing data

---

## 16.27 MVP Admin Recommendation and Visit Invalidation Foundation

### Decision
Recommendation and visit invalidation are explicit admin workflows that reverse active aggregate effects.

### Current Implementation
- `PATCH /api/admin/recommendations/{recommendationId}/invalidate` changes ACTIVE recommendations to INVALIDATED
- recommendation invalidation subtracts the stored recommendation weight from `place_stats`
- `PATCH /api/admin/visits/{visitId}/invalidate` changes valid visits to invalid with `ADMIN_INVALIDATED`
- visit invalidation subtracts one visit and the current `ranking.visit_weight` policy value from `place_stats`
- inactive recommendations and invalid visits are treated as no-op requests and do not write duplicate logs
- actual changes write `RECOMMENDATION_INVALIDATE` or `VISIT_INVALIDATE` rows to `admin_action_logs`

### Reason
- operators need to remove manipulated activity without deleting the original event row
- reversing `place_stats` keeps the aggregation-based ranking model consistent for later recalculation
- no-op behavior makes the endpoints safe for retries and prevents misleading audit history

---

## 16.28 MVP Admin Comment Moderation Foundation

### Decision
Admin comment moderation uses explicit blind and delete workflows with audited state transitions.

### Current Implementation
- `GET /api/admin/comments` returns the latest 50 comment rows across all statuses
- `PATCH /api/admin/comments/{commentId}/blind` changes visible comments to BLINDED
- `DELETE /api/admin/comments/{commentId}` changes non-deleted comments to DELETED
- blind and visible delete subtract one comment and the current `ranking.comment_weight` policy value from `place_stats`
- deleting a blinded comment logs the state transition without subtracting stats again
- already non-visible blind requests and already deleted delete requests are no-ops without duplicate logs
- actual changes write `COMMENT_BLIND` or `COMMENT_DELETE` rows to `admin_action_logs`
- BLINDED comments cannot be restored by user rewrite

### Reason
- operators need a moderation option that hides harmful comments without deleting the original record immediately
- preserving the event row and audit log supports report investigation and later abuse analysis
- keeping report processing separate from direct comment moderation avoids accidental side effects

---

## 16.29 MVP Admin Action Log Read Foundation

### Decision
Admin action logs are exposed through a read-only admin API for audit review.

### Current Implementation
- `GET /api/admin/action-logs` returns the latest 50 `admin_action_logs` rows
- responses include admin user, action type, target type/id, before/after values, memo, and created timestamp
- the API requires ADMIN or SUPER_ADMIN through separated admin routes and service validation
- reading action logs does not create another admin action log

### Reason
- operators need to inspect moderation, policy, ranking, and user-impacting changes without direct database access
- keeping the endpoint read-only avoids recursive audit noise
- returning raw before/after snapshots preserves the exact context written by each action workflow

---

## 16.30 MVP Place Update and Delete Foundation

### Decision
Place owners can edit and logically delete their own places, and admins can use the same mutation path with audit logging.

### Current Implementation
- `PATCH /api/places/{placeId}` partially updates mutable place fields and can replace the image URL list
- `DELETE /api/places/{placeId}` sets `places.deleted_at` and resets `current_star_level` to 0
- both endpoints require the authenticated user to be the place owner, ADMIN, or SUPER_ADMIN
- normal owner updates require phone verification and no active blocking sanction
- normal owner region changes validate the existing `system_policies.region.registration_scope`
- admin updates write `PLACE_UPDATE` only when state changes
- admin deletes write `PLACE_DELETE`

### Reason
- users need a safe way to correct their own place registrations without database intervention
- logical deletion preserves history for ranking, reports, and future audit workflows
- allowing admin override keeps operations practical while preserving the admin action log requirement
- keeping registration count limits out of update avoids treating correction as a new registration

---

## 16.31 MVP Admin Report Follow-up Action Foundation

### Decision
Report processing records the review decision first, and approved reports can then receive explicit follow-up actions.

### Current Implementation
- `POST /api/admin/reports/{reportId}/actions` applies one follow-up action from an APPROVED report
- PLACE reports can trigger `HIDE_PLACE` or `DELETE_PLACE`
- COMMENT reports can trigger `BLIND_COMMENT` or `DELETE_COMMENT`
- USER reports can trigger `SANCTION_USER`
- follow-up actions delegate to existing admin place, comment, user sanction, or place delete workflows
- the affected target is audited by its domain action log, and the report link is audited as `REPORT_FOLLOW_UP`
- PENDING and REJECTED reports cannot receive follow-up actions

### Reason
- report review and destructive moderation should remain separate operator decisions
- reusing existing domain workflows avoids duplicating moderation logic and keeps aggregate rollback behavior consistent
- the extra report follow-up log connects the original report to the explicit operational action for later audit review

---

## 16.32 MVP Visit Growth and Trust Signal Foundation

### Decision
Valid visit verification grants policy-driven EXP and policy-driven trust score through the user growth domain.

### Current Implementation
- `UserGrowthService` updates `user_level.exp`, `user_level.total_exp`, and `user_trust.trust_score`
- visit EXP is read from `system_policies.growth.visit_exp`
- valid-visit trust score is read from `system_policies.trust.valid_visit_score`
- missing user trust or level rows are initialized defensively before applying visit growth
- `POST /api/places/{placeId}/visits` returns the EXP granted by the valid visit in `expGained`
- trust grade evaluation, level-up thresholds, and level history writes remain separate follow-up workflows

### Reason
- visits are high-trust real-world actions and should contribute to both growth and reliability
- keeping the policy values in `system_policies` preserves admin control and avoids hardcoded business values
- delegating the update out of `VisitService` keeps visit verification focused on validation, event storage, and place stats

---

## 16.33 MVP Trust Grade and Level-up Policy Foundation

### Decision
Trust grade, recommendation weight, and level-up thresholds are evaluated from system policies instead of application constants.

### Current Implementation
- `growth.level_exp_thresholds` maps the current level to the EXP required for the next level
- level-up consumes the configured EXP threshold and leaves remaining EXP toward the next level
- `user_level_history` receives one `VALID_VISIT` row when a valid visit causes an actual level change
- `trust.grade_thresholds` maps trust grades to minimum trust scores
- `trust.recommend_weight_by_grade` maps trust grades to recommendation weights
- valid visit growth recalculates trust grade and recommendation weight after trust score changes
- `GET /api/users/me/status` returns `nextLevelExp` from `growth.level_exp_thresholds`, or 0 when no next threshold exists
- policy list values use semicolon-separated `key:value` entries to remain CSV seed friendly

### Reason
- trust influence and growth pacing are business policies that operators must tune without code changes
- keeping thresholds in policies avoids hidden hardcoded progression rules
- writing history only on real level changes keeps the level log meaningful and retry-safe

---

## 16.34 MVP User Activity Summary Counts

### Decision
User activity summary reads real repository counts while keeping the summary reader replaceable.

### Current Implementation
- `GET /api/users/me/activity-summary` delegates to `UserActivitySummaryReader`
- the repository-backed reader counts ACTIVE recommendations
- the repository-backed reader counts valid visits
- the repository-backed reader counts VISIBLE comments with `deleted_at` null
- the repository-backed reader counts places created by the user whose `deleted_at` is null

### Reason
- users need their profile/status screen to reflect real participation instead of placeholder zeros
- counting only active/valid/visible/non-deleted rows keeps the summary aligned with user-facing activity
- retaining the reader boundary allows later caching or analytics aggregation without changing `UserService`

---

## 16.35 MVP OAuth Login Foundation

### Decision
OAuth login uses provider-verified identities through replaceable provider clients and links accounts by provider plus provider user id.

### Current Implementation
- `POST /api/auth/oauth/{provider}` supports KAKAO, NAVER, and GOOGLE providers
- provider access tokens are verified through `OAuthProviderClient` implementations
- Kakao, Naver, and Google user info endpoints are configurable through `app.oauth`
- first OAuth login creates a normal USER account, a provider `user_auth` row, and initial `user_trust` and `user_level` rows
- existing provider links issue Honeytong access and refresh tokens through the same token path as local login
- LOCAL is explicitly rejected on the OAuth endpoint
- accounts are not automatically merged by email alone

### Reason
- provider token verification is external integration logic and should stay outside controller and core auth flow
- provider user ids are the stable account link key; email-only linking can attach the wrong account when provider email trust differs
- reusing the same token issuance and growth initialization keeps local and OAuth accounts consistent

---

## 16.36 MVP User Action Log Foundation

### Decision
Core user participation events are recorded in `user_action_logs` as operational telemetry after successful domain transactions.

### Current Implementation
- `user_action_logs` is mapped through `UserActionLog` and `UserActionLogRepository`
- `UserActionLogService` records logs after the surrounding transaction commits
- log writes use a separate transaction through `UserActionLogWriter`
- logging failures are swallowed so a completed domain action is not broken by telemetry storage
- current action types are `PLACE_CREATE`, `RECOMMENDATION_CREATE`, `RECOMMENDATION_CANCEL`, `VISIT_VERIFY`, `COMMENT_CREATE`, `COMMENT_UPDATE`, `COMMENT_DELETE`, and `REPORT_CREATE`
- event-specific details are stored in `metadata_json`

### Reason
- user action logs are useful for abuse analysis and operations, but they are not the source of truth for the user action itself
- writing after commit avoids logging rolled-back actions
- keeping telemetry failure isolated prevents monitoring infrastructure from harming normal participation flows

---

## 16.37 MVP Admin User Action Log Read Foundation

### Decision
User action logs are exposed through a read-only admin API for operation and abuse investigation.

### Current Implementation
- `GET /api/admin/user-action-logs` returns the latest 50 `user_action_logs` rows
- responses include user id, nickname, action type, target type/id, client context, metadata JSON, and created timestamp
- the API requires ADMIN or SUPER_ADMIN through separated admin routes and service validation
- reading user action logs does not write another user action log or admin action log

### Reason
- operators need to inspect participation patterns without direct database access
- keeping the endpoint read-only avoids recursive logging noise
- returning metadata JSON preserves event-specific context while the domain tables remain the source of truth

---

## 16.38 Application Logging Baseline

### Decision
Application logs are for runtime diagnostics, while business and audit events remain in database log tables.

### Current Implementation
- backend log levels are configurable through `LOG_LEVEL_ROOT`, `LOG_LEVEL_APP`, `LOG_LEVEL_SECURITY`, `LOG_LEVEL_SQL`, and `LOG_LEVEL_SQL_BIND`
- file logging uses `LOG_FILE`, `LOG_FILE_MAX_SIZE`, and `LOG_FILE_MAX_HISTORY`
- SQL bind logging is disabled by default
- the development phone verification sender logs code issuance at INFO without the raw code and only prints the raw code at DEBUG

### Reason
- operators need useful runtime diagnostics without mixing them with audit history
- SQL bind values, tokens, and verification codes can expose sensitive data if logged casually
- environment-driven logging keeps local debugging flexible while making production defaults conservative

---

## 16.39 Basic Monitoring Health Readiness

### Decision
Expose a minimal unauthenticated health endpoint for deployment and runtime readiness checks.

### Current Implementation
- Spring Boot Actuator is included in the backend
- `/actuator/health` and nested health probe paths are permitted without authentication
- actuator web exposure defaults to `health` only through `MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE`
- health details default to `never` through `MANAGEMENT_HEALTH_SHOW_DETAILS`
- liveness and readiness probe support is enabled through `MANAGEMENT_HEALTH_PROBES_ENABLED`

### Reason
- deployment platforms and load balancers need a lightweight health endpoint that does not require a user token
- exposing only health avoids broad management surface area in the MVP
- hiding details by default prevents leaking database, disk, or dependency diagnostics to public callers

---

## 16.40 Production Profile Baseline

### Decision
Use an explicit `prod` Spring profile for production runtime configuration.

### Current Implementation
- `application-prod.yml` requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` from the environment
- production defaults `spring.jpa.hibernate.ddl-auto` to `validate` through `JPA_DDL_AUTO`
- local `application.yml` keeps its development defaults, including local DB URL fallback and `ddl-auto: update`
- region seed import, policy seed import, and local admin bootstrap remain disabled by default in production
- actuator web exposure remains limited to health and health details remain hidden by default

### Reason
- production must not depend on committed secrets or development JWT defaults
- schema auto-update is convenient locally but risky in production without an explicit migration process
- bootstrap and seed flows are operational tools and should only run when intentionally enabled

---

## 16.41 Database Migration Strategy Baseline

### Decision
Use Flyway as the MVP database migration tool.

### Current Implementation
- Flyway dependencies are included in the backend Gradle build
- migration files live under `backend/src/main/resources/db/migration`
- `V1__baseline_schema.sql` captures the current backend core schema
- local development keeps Flyway disabled by default through `FLYWAY_ENABLED=false`
- production enables Flyway by default through `application-prod.yml`
- production keeps Hibernate in `validate` mode so schema changes are applied by migrations, not by runtime entity auto-update

### Reason
- production schema changes must be repeatable, reviewable, and ordered
- Flyway fits the Spring Boot monolith without adding a separate migration service
- keeping local Flyway opt-in avoids breaking existing developer databases while the project transitions from Hibernate auto-update
- future schema changes need an explicit SQL artifact alongside entity and documentation updates

---

## 16.42 Redis Configuration Baseline

### Decision
Add Redis connection configuration before wiring Redis into business behavior.

### Current Implementation
- `spring-boot-starter-data-redis` is included in the backend build
- Redis connection settings are environment-driven through `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE`, and `REDIS_TIMEOUT`
- Spring cache type defaults to `none`
- app-level Redis usage defaults to disabled through `APP_REDIS_ENABLED=false`
- Redis repositories are disabled because the MVP uses JPA repositories for persistent data
- Redis health checks default to disabled so local and current runtime health does not require Redis yet

### Reason
- Redis is part of the target architecture for cooldowns, limits, policy caching, and hot ranking cache
- adding connection settings first keeps later domain adoption smaller and easier to test
- keeping Redis-backed behavior disabled avoids changing current DB-backed behavior before a domain-specific cache boundary exists

---

## 16.43 Policy Redis Cache Adapter

### Decision
Add a Redis-backed cache boundary to policy reads while keeping `system_policies` as the source of truth.

### Current Implementation
- `PolicyService` reads through `PolicyCache` before loading an active policy row from the database
- cached policy entries store both value and `PolicyValueType` so typed policy validation is preserved
- Redis policy caching is enabled only when `APP_REDIS_ENABLED=true`
- disabled Redis usage uses a no-op cache, so local/default behavior falls back to DB reads
- `POLICY_CACHE_TTL` controls the Redis policy cache TTL and defaults to 10 minutes
- admin policy updates evict the changed policy cache key after updating the DB value and writing the admin action log
- Redis read/write/evict failures do not break policy reads or committed admin policy updates

### Reason
- policy values are read frequently by recommendation, visit, ranking, region, and growth flows
- Redis reduces repeated policy DB lookups without moving policy ownership out of the database
- explicit invalidation keeps admin policy changes visible to subsequent requests
- the no-op boundary lets the project adopt Redis incrementally without requiring Redis in every local environment

---

## 16.44 Recommendation Daily Limit Redis Counter

### Decision
Add a Redis-backed boundary for per-user daily recommendation limit checks while keeping recommendation rows as the durable source of truth.

### Current Implementation
- `RecommendationService` reads daily used count through `RecommendationDailyCounter`
- disabled Redis usage uses a no-op counter that executes the existing DB count query
- Redis usage stores the DB-backed count under `recommend:daily:user:{userId}:{yyyyMMdd}`
- successful recommend and cancel operations evict the user's current-day counter key
- admin recommendation invalidation also evicts the affected user's current-day counter key when it changes an active recommendation
- Redis read/write/evict failures fall back to DB or are ignored so recommendation workflows are not broken by cache availability

### Reason
- daily recommendation limit checks happen on recommend and policy reads, so they are a natural Redis adoption point
- recommendation rows must remain authoritative for audits, admin invalidation, and fallback behavior
- evicting on state changes keeps Redis from becoming a second source of truth
- the no-op boundary preserves local/default behavior while allowing production-like environments to opt in with `APP_REDIS_ENABLED=true`

---

## 16.45 Visit Cooldown Redis Adapter

### Decision
Add a Redis-backed boundary for per-user/place visit cooldown checks while keeping visit rows as the durable source of truth.

### Current Implementation
- `VisitService` reads cooldown state through `VisitCooldownCache`
- disabled Redis usage uses a no-op cache that executes the existing DB latest-visit lookup
- Redis usage stores active cooldown-until timestamps under `visit:cooldown:user:{userId}:place:{placeId}`
- the Redis key TTL is the remaining cooldown duration
- successful valid visits and admin visit invalidation evict the affected user/place cooldown key when visit state changes
- Redis read/write/evict failures fall back to DB or are ignored so visit workflows are not broken by cache availability

### Reason
- visit cooldown checks happen on verification and policy reads, so they are a useful Redis adoption point
- visit rows must remain authoritative for audit, admin invalidation, and fallback behavior
- evicting on state changes avoids making Redis a second source of truth
- the no-op boundary preserves local/default behavior while allowing production-like environments to opt in with `APP_REDIS_ENABLED=true`

---

## 16.46 Phone Verification Redis Adapter

### Decision
Add a Redis-backed boundary for phone verification code lookup and attempt tracking while keeping verification rows as the durable source of truth.

### Current Implementation
- `PhoneVerificationService` reads latest unverified code state through `PhoneVerificationCache`
- disabled Redis usage uses a no-op cache that executes the existing DB latest-code lookup
- Redis usage stores active code state under `phone:verification:user:{userId}:phone:{phone}`
- the Redis key TTL is the remaining verification-code lifetime
- successful phone verification evicts the affected user/phone key after DB state is updated
- Redis read/write/evict failures fall back to DB or are ignored so phone verification workflows are not broken by cache availability

### Reason
- phone verification is a temporary-state flow and is a natural Redis adoption point
- verification rows must remain authoritative for fallback, audit, and account state updates
- evicting after success avoids making Redis a second source of truth
- the no-op boundary preserves local/default behavior while allowing production-like environments to opt in with `APP_REDIS_ENABLED=true`

---

## 16.47 Ranking Hot Cache Adapter

### Decision
Add a Redis-backed cache boundary for public place ranking reads while keeping `place_season_scores` as the authoritative ranking read model.

### Current Implementation
- `RankingService` reads public place ranking responses through `RankingCache`
- disabled Redis usage uses a no-op cache and the existing `place_season_scores` query path
- Redis usage stores public place ranking responses under `ranking:places:{seasonCode}:{regionType}:{regionId}`
- `RANKING_CACHE_TTL` controls the Redis ranking cache TTL and defaults to 5 minutes
- admin ranking recalculation evicts public place ranking cache entries after rewriting season score rows
- admin ranking exclusion state changes evict public place ranking cache entries when state actually changes
- Redis read/write/evict failures fall back to DB or are ignored so public ranking reads and admin ranking workflows are not broken by cache availability

### Reason
- public ranking reads are a hot endpoint and already have a stable aggregate read model
- `place_season_scores` must remain authoritative because ranking is aggregation-based, not real-time
- explicit invalidation after ranking mutations keeps cached responses aligned with admin operations
- the no-op boundary preserves local/default behavior while allowing production-like environments to opt in with `APP_REDIS_ENABLED=true`

---

## 16.48 Ranking Scheduler Foundation

### Decision
Add a disabled-by-default scheduled ranking recalculation path that reuses the same core aggregation service as admin manual recalculation.

### Current Implementation
- `RankingRecalculationService` owns the aggregation logic that rewrites `place_season_scores` from `place_stats`
- `AdminRankingService` delegates to `RankingRecalculationService` and continues writing `RANKING_RECALCULATE` rows to `admin_action_logs`
- `ScheduledRankingRecalculationJob` delegates to `RankingRecalculationService` when `app.ranking.scheduler.enabled=true`
- `RANKING_SCHEDULER_ENABLED` defaults to `false`
- `RANKING_SCHEDULER_CRON` defaults to `0 0 4 * * *`
- `RANKING_SCHEDULER_ZONE` defaults to `Asia/Seoul`
- blank `RANKING_SCHEDULER_SEASON_CODE` means the active season is recalculated
- scheduled recalculation writes application logs, not `admin_action_logs`, because no admin user performed the action

### Reason
- the MVP still needs a safe manual admin control path before relying on automation
- keeping the scheduler disabled by default prevents unexpected local or production ranking mutations
- sharing one recalculation service avoids drift between manual and scheduled ranking results
- schedule settings are operational configuration, not ranking business policy, so they live in environment-driven app config rather than `system_policies`

---

## 16.49 Ranking History Finalization Foundation

### Decision
Add an explicit admin-controlled operation that finalizes a season's current ranking score rows into `place_ranking_history`.

### Current Implementation
- `POST /api/admin/rankings/seasons/{seasonId}/finalize-history` finalizes one season
- `RankingHistoryFinalizationService` reads `place_season_scores` for the selected season and maps them to `place_ranking_history`
- seasons without score rows are rejected before deleting existing history
- existing history rows for the season are deleted before inserting the current snapshot
- repeated finalization of the same season snapshot does not create duplicate history rows
- admin finalization writes `RANKING_HISTORY_FINALIZE` to `admin_action_logs`
- public ranking reads remain backed by `place_season_scores`; public history reads remain a follow-up API task

### Reason
- ranking history should preserve finalized snapshots, not recalculate historical results from changing activity aggregates
- manual admin control is safer for MVP season closing than automatic finalization
- delete-then-rewrite keeps the operation retry-friendly while the schema remains simple
- separating finalization from current ranking reads avoids changing user-facing ranking behavior before the history API is built

---

## 16.50 Place Ranking History Read API

### Decision
Expose finalized place ranking history through the public place API while keeping it separate from current ranking reads.

### Current Implementation
- `GET /api/places/{placeId}/ranking-history` returns finalized history for one visible place
- the API reads `place_ranking_history` rows and does not read current `place_season_scores`
- response items include season metadata, region type and id, rank, star level, total score, and finalization time
- hidden or deleted places return `RESOURCE_NOT_FOUND` to match public place detail visibility behavior
- visible places with no finalized history return an empty `items` list

### Reason
- ranking history should show finalized snapshots, not mutable current score rows
- matching the existing public place visibility rule avoids exposing moderated or deleted places through history endpoints
- an empty history list lets the frontend render new or never-finalized places without treating that state as an error

---

## 16.51 Ranking Query Index Migration

### Decision
Add explicit Flyway-managed indexes for the ranking read and ranking history read paths.

### Current Implementation
- `V2__add_ranking_query_indexes.sql` adds `idx_place_season_scores_region_rank` on `place_season_scores`
- `idx_place_season_scores_region_rank` supports season/region filtered ranking reads ordered by `rank_no`
- `V2__add_ranking_query_indexes.sql` adds place-based and season-based composite indexes on `place_ranking_history`
- the place-based history index supports `GET /api/places/{placeId}/ranking-history`
- the season-based history index supports season finalization cleanup and future admin season history reads
- existing baseline indexes remain unchanged to avoid destructive migration behavior

### Reason
- ranking reads are a hot path and should stay aligned with the aggregate table query shape
- finalized history reads should not scan all history rows as seasons accumulate
- keeping the change in Flyway makes production schema changes reviewable and repeatable

---

## 16.52 Ranking Frontend Read Views

### Decision
Connect the mobile frontend ranking list and place detail ranking history views directly to the public ranking APIs.

### Current Implementation
- The ranking page reads `GET /api/rankings/seasons/current` and `GET /api/rankings/places`.
- The place detail page reads `GET /api/places/{placeId}/ranking-history`.
- The ranking screen derives its default dong/district/city region from loaded place region data until a dedicated user-region frontend state is added.
- Korean UI copy in touched frontend screens and `ko.json` is restored to valid UTF-8.

### Reason
- ranking is now backed by aggregate read models and should be shown from backend data
- finalized place history belongs on the place detail read view
- frontend read states need Korean loading, empty, and error copy before participation actions are wired

---

## 16.53 Place Participation Frontend Actions

### Decision
Connect place detail participation controls to the existing user APIs while leaving all business eligibility decisions on the backend.

### Current Implementation
- The shared frontend API client attaches a stored access token to authenticated requests when one exists.
- The place detail page calls recommend/cancel, visit verification, comment list/create/update/delete, and user profile APIs.
- Recommendation and visit policy reads are used for display state only; server responses remain authoritative for action success or failure.
- Browser geolocation supplies visit coordinates, and the backend validates radius and cooldown.
- Server error messages and known error codes are surfaced as Korean action messages.

### Reason
- recommendation, visit, and comment are core user flows and should now use the implemented backend APIs
- the frontend must not duplicate policy rules such as daily limits, visit radius, cooldown, or phone-verification eligibility
- the next usability gap is a first-class auth and phone verification UI, because these action APIs require an access token and verified phone state

---

## 16.54 Auth and Phone Verification Frontend Flow

### Decision
Use My Page as the MVP entry point for local login, signup, logout, and phone verification.

### Current Implementation
- The frontend calls local signup/login/logout APIs from My Page.
- Successful login stores the access token under `honeytong-access-token` and the refresh token under `honeytong-refresh-token`.
- The shared frontend API client reads the stored access token and attaches it to authenticated requests.
- My Page reads `/api/users/me`, `/api/users/me/status`, and `/api/users/me/activity-summary` after login.
- My Page calls phone verification send, verify, and status APIs and displays Korean success/error states.

### Reason
- participation actions already require authenticated and phone-verified users, so the app needs a reachable recovery path before broader frontend workflows expand
- keeping the MVP flow in My Page avoids adding a heavier auth routing structure before the mobile frontend layout settles
- the backend remains authoritative for password rules, token issuance, phone code validation, and core-action eligibility

---

## 16.55 Region Verification Frontend Flow

### Decision
Use My Page as the MVP entry point for GPS-based primary region verification.

### Current Implementation
- My Page reads `/api/regions/me` and `/api/regions/me/change-policy` after login.
- My Page calls `/api/regions/verify` with browser geolocation coordinates when the user requests current-location verification.
- Missing current-region data does not break the rest of the authenticated My Page state.
- Region change policy data is displayed from the backend response only; the frontend does not decide eligibility rules.

### Reason
- place registration and other local participation flows depend on verified primary region state
- GPS-to-region resolution and region-change policy validation must remain server-side
- keeping this flow in My Page gives users a reachable setup path before place registration UI is connected

---

## 16.56 Place Registration Frontend Flow

### Decision
Use a dedicated mobile frontend route for MVP place registration and submit directly to the existing user place API.

### Current Implementation
- Home links to `/places/new` for place registration.
- The registration screen reads `/api/regions/me` and `/api/places/registration-policy` before showing the form.
- New places are submitted to `POST /api/places` with the current verified dong id and user-entered place details.
- Browser geolocation can fill latitude and longitude, but server-side region, phone verification, sanction, and policy checks remain authoritative.
- After successful creation, the frontend refreshes the place list and navigates to the created place detail page.

### Reason
- place registration is a core MVP flow and now has the required auth, phone, and region setup paths
- the frontend should guide users without duplicating registration limit or region-scope policy decisions
- a dedicated route keeps the form reachable without reshaping the current mobile navigation

---

## 16.57 My Registered Places Frontend Read Flow

### Decision
Expose the current user's registered places through a user-scoped read API and render them in My Page.

### Current Implementation
- `GET /api/users/me/places` returns non-deleted places created by the authenticated user.
- The endpoint reuses the public place list item DTO shape so frontend cards can share the same mapper.
- My Page reads registered places together with profile, status, activity summary, and region state after login.
- The registered-place section links each item to the existing place detail page and provides a route to register a new place.

### Reason
- users need a simple way to return to places they contributed before owner edit/delete flows are added
- keeping the read user-scoped avoids exposing owner-specific views through public list endpoints
- reusing the existing list DTO keeps the MVP frontend smaller while the dedicated owner management UI is still pending

---

## 16.58 Local Tool Session Bootstrap

### Decision
Use project-owned PowerShell bootstrap scripts to normalize local Windows tool paths before running development commands.

### Current Implementation
- `scripts/dev-env.ps1` resolves Node/npm, Git, Java 21, and Gradle user home for the current PowerShell process.
- `scripts/check-dev-env.ps1` verifies the resolved toolchain.
- `scripts/run-backend-gradle.ps1` runs the backend Gradle wrapper after loading the normalized environment.
- `scripts/run-frontend-npm.ps1` runs frontend npm commands through `npm.cmd` after loading the normalized environment.
- `scripts/dev-env.ps1` normalizes duplicate process environment keys such as `PATH` and `Path` before wrappers run so Windows process launch APIs do not fail on case-insensitive key collisions.
- `docs/dev-environment.md` documents the standard commands and restart rules for Codex, terminals, backend, Vite, and the in-app browser.

### Reason
- Windows terminals and long-running processes keep their own environment snapshot, so changing system `PATH` or `JAVA_HOME` does not fix already-open sessions.
- Git, Node/npm, Java, and Gradle failures were repeatedly caused by inconsistent per-session path resolution rather than application code.
- Explicit project wrappers make local verification repeatable without relying on Git hooks.

---

## 16.59 Place Owner Edit/Delete Frontend Flow

### Decision
Expose owner edit and delete entry points from My Page while keeping all ownership and eligibility checks on the backend.

### Current Implementation
- My Page renders edit and delete actions for places returned by `GET /api/users/me/places`.
- The edit route `/places/{placeId}/edit` reuses the place registration form in edit mode and submits to `PATCH /api/places/{placeId}`.
- The delete action calls `DELETE /api/places/{placeId}` after an explicit browser confirmation.
- Frontend delete success removes the place from local browse/wishlist state and refreshes My Page data.
- Normal owner phone verification, blocking sanction checks, owner/admin authorization, and region-scope validation remain enforced by the backend.

### Reason
- Users need a reachable way to manage places they contributed before a larger owner dashboard exists.
- Reusing the registration form keeps the MVP UI compact and avoids introducing a second divergent place form.
- The frontend should provide clear entry points and confirmation, but the backend remains authoritative for permission and business rules.

---

## 16.60 Report Frontend User Flow

### Decision
Expose user report entry points from place detail surfaces and show the current user's submitted reports in My Page.

### Current Implementation
- Place detail can submit PLACE reports for the visible place.
- Place detail can submit COMMENT reports from each comment item except the current user's own comment in the UI.
- Report creation calls `POST /api/reports` with target type, target id, reason code, and optional reason text.
- My Page reads `GET /api/users/me/reports` and displays the latest submitted report status.
- The frontend reason options are display choices only; target existence, reporter status, self-report prevention, and moderation effects remain server-side.

### Reason
- Users need a reachable trust/safety path from the content they are viewing.
- Showing submitted report status reduces uncertainty after a report is created.
- Keeping moderation effects out of user report creation preserves the existing admin review model.

---

## 16.61 Admin Report Frontend Management Flow

### Decision
Expose the existing admin report moderation APIs through a separated frontend route at `/admin/reports`.

### Current Implementation
- The admin report page reads `GET /api/admin/reports` with PENDING/APPROVED/REJECTED/ALL filters.
- Selecting a report reads `GET /api/admin/reports/{reportId}` for the detailed moderation snapshot.
- PENDING reports can be processed as APPROVED or REJECTED through `PATCH /api/admin/reports/{reportId}`.
- APPROVED reports expose target-matched follow-up actions through `POST /api/admin/reports/{reportId}/actions`.
- Follow-up actions are limited by target type in the UI, while authorization, valid transitions, and actual moderation effects remain server-side.

### Reason
- Operators need a direct manual workflow for MVP trust and safety handling.
- Separating review from follow-up action keeps the admin UI aligned with the backend audit model.
- Keeping the route under `/admin` preserves clear separation from user-facing discovery screens.

---

## 16.62 Admin Dashboard Frontend Read Flow

### Decision
Expose the existing admin dashboard metrics API through separated frontend routes at `/admin` and `/admin/dashboard`.

### Current Implementation
- The admin dashboard page reads `GET /api/admin/dashboard`.
- Dashboard cards show today's new users, active recommendations, valid visits, pending reports, and new places.
- The page links to `/admin/reports` so operators can move from pending report counts into the moderation workflow.
- The dashboard is read-only and does not duplicate admin authorization or metric calculation rules in the frontend.

### Reason
- Operators need a compact entry point before drilling into report and policy screens.
- Keeping the page read-only preserves the backend dashboard API's no-audit-log behavior.
- Using `/admin` as the admin landing route keeps admin workflows separated from user-facing discovery.

---

## 16.63 Admin Policy Frontend Management Flow

### Decision
Expose the existing admin policy APIs through a separated frontend route at `/admin/policies`.

### Current Implementation
- The admin policy page reads `GET /api/admin/policies` and shows policy groups, keys, value types, current values, active state, and update metadata.
- Selecting a policy can submit `PATCH /api/admin/policies/{policyKey}` with a new value and optional memo.
- The page also reads and updates the region policy shortcut through `GET /api/admin/policies/region` and `PATCH /api/admin/policies/region`.
- The frontend only performs basic empty/shape checks for form usability; policy value parsing, role authorization, cache eviction, and audit logging remain server-side.
- The admin dashboard links to `/admin/policies` as an operator entry point.

### Reason
- Policy control is a core MVP admin requirement and should be reachable without database edits.
- Keeping validation and authorization in the backend preserves the policy-driven rule model and avoids hardcoding business limits in the UI.
- The route stays under `/admin` so policy operations remain separated from user-facing discovery flows.

---

## 16.64 Admin User Frontend Management Flow

### Decision
Expose the existing admin user management APIs through a separated frontend route at `/admin/users`.

### Current Implementation
- The admin user page reads `GET /api/admin/users` and filters the loaded list by role and status for operator scanning.
- Selecting a user reads `GET /api/admin/users/{userId}` and displays core account, trust, and level snapshots.
- Active normal USER accounts can submit `POST /api/admin/users/{userId}/sanctions`, `PATCH /api/admin/users/{userId}/trust`, and `PATCH /api/admin/users/{userId}/recommend-weight`.
- The frontend limits mutation controls for non-active or admin accounts for usability, while target eligibility, self-action prevention, period validation, schema limits, and audit logging remain server-side.
- Admin dashboard, reports, and policies pages link to the user management route.

### Reason
- Operators need a direct user investigation and manual moderation workflow after report handling is connected.
- Trust and recommendation influence changes affect abuse control and ranking quality, so the backend must remain authoritative.
- Keeping the route under `/admin` preserves separation from user-facing profile and discovery flows.

---

## 16.65 Admin Place Frontend Management Flow

### Decision
Expose the existing admin place moderation and ranking-exclusion APIs through a separated frontend route at `/admin/places`.

### Current Implementation
- The admin place page reads `GET /api/admin/places` and filters the loaded list by approval status, exposure status, franchise review status, ranking participation, and keyword.
- Selecting a place reads `GET /api/admin/places/{placeId}` and displays creator, region, addresses, activity counts, score snapshots, current stars, image URLs, and moderation state.
- Operators can submit exposure, approval, franchise review, score adjustment, and ranking exclusion changes from the detail panel.
- The frontend keeps action inputs and memo capture lightweight; status transitions, score effects, star reset behavior, duplicate-log prevention, authorization, and audit logging remain server-side.
- Admin dashboard, reports, policies, and user management pages link to the place management route.

### Reason
- Place moderation directly affects discovery quality and ranking fairness, so operators need a dedicated workflow before activity-level moderation screens are connected.
- Ranking exclusion is independent from normal exposure and approval, and the UI should reflect that separation instead of hiding the place.
- Keeping the route under `/admin` preserves separation from user-facing place browsing and owner edit flows.

---

## 16.66 Admin Activity Moderation Frontend Flow

### Decision
Expose the existing admin recommendation, visit, and comment moderation APIs through a separated frontend route at `/admin/activities`.

### Current Implementation
- The admin activity page reads `GET /api/admin/recommendations`, `GET /api/admin/visits`, and `GET /api/admin/comments`.
- Operators can switch between recommendation, visit, and comment tabs and filter loaded records by active or already-moderated state.
- Recommendation records can submit `PATCH /api/admin/recommendations/{recommendationId}/invalidate`.
- Visit records can submit `PATCH /api/admin/visits/{visitId}/invalidate`.
- Comment records can submit `PATCH /api/admin/comments/{commentId}/blind` or `DELETE /api/admin/comments/{commentId}` with an operation memo.
- The frontend only limits controls for obvious inactive states; aggregate stat changes, cooldown/cache invalidation, idempotency, authorization, and audit logging remain server-side.
- Admin dashboard, places, reports, policies, and user management pages link to the activity moderation route.

### Reason
- Activity-level moderation is the operator path for fixing manipulation or unsafe content after place and report workflows are connected.
- Recommendation, visit, and comment actions directly influence place stats and rankings, so the backend must remain authoritative for side effects.
- Keeping the route under `/admin` preserves separation from user-facing participation and place detail flows.

---

## 16.67 Admin Audit Log Frontend Flow

### Decision
Expose administrator and user action logs through a separated read-only admin frontend route at `/admin/audit-logs`.

### Current Implementation
- The admin audit page reads `GET /api/admin/action-logs` and `GET /api/admin/user-action-logs`.
- Operators can switch between administrator action logs and user participation logs.
- Loaded records can be filtered by action type, target type, keyword, user/admin nickname, and target id.
- Log detail views show before/after snapshots, metadata JSON, memo, actor, target, and created time.
- The page does not create, update, or delete logs; all log creation remains inside existing backend domain workflows.
- The admin dashboard links to the audit log route as an operator investigation entry point.
- Existing admin pages include a Korean `감사 로그` navigation label so operators can reach audit review from any admin workflow.

### Reason
- Audit review is an operations workflow and must remain separated from user-facing pages.
- Filtering on the loaded latest records is enough for the MVP while preserving the existing backend read contract.
- Keeping the page read-only prevents accidental audit mutation and keeps domain services responsible for log creation.

---

## 17. Server-Side Validation Only

### Decision
All critical validation must be done on server.

### Reason
- frontend cannot be trusted
- prevents abuse

---

## 18. No Hardcoded Business Rules

### Decision
Business rules must not be hardcoded.

### Reason
- flexibility
- easier tuning
- admin control

---

## 19. Event-Driven Data Collection

### Decision
Record user actions as events.

### Examples
- recommendation
- visit
- comment

### Reason
- enables analytics
- supports ranking aggregation

---

## 20. Simple MVP, Expandable Structure

### Decision
Keep MVP simple but structure scalable.

### Reason
- reduce initial complexity
- allow future expansion without redesign

---

## 21. Ranking Priority Design

### Decision
visit > recommendation > comment

### Reason
- real-world action is most valuable
- recommendation is secondary signal
- comments are supportive signals

---

## 22. Audience Tag System (Future-Ready)

### Decision
Design system to support demographic-based tags.

### Examples
- "Popular among men in their 30s"
- "Popular among women in their 20s"

### Reason
- enhances discovery
- adds insight beyond simple ranking

---

## 23. Season-Based Ranking System

### Decision
Ranking is calculated per season.

### Reason
- freshness
- competition
- historical tracking

---

## 24. Logging for Critical Actions

### Decision
Log:
- user actions
- admin actions

### Reason
- debugging
- monitoring
- abuse detection

---

## 25. Growth System (Level & EXP)

### Decision
Users gain EXP and level up.

### Reason
- gamification
- increase engagement

---

## 26. Soft Control Before Hard Restriction

### Decision
Start with flexible policies, tighten later.

### Reason
- early-stage needs data
- avoid blocking growth

---

## 27. Final Principle

The system is designed to:

- encourage exploration
- reward trust
- prevent abuse
- allow admin control
- remain flexible

---

## 28. Search Scale Strategy

### Decision
Keep the MVP place search endpoint as a broad visible-place keyword match, but do not treat additional B-tree indexes as the production scaling strategy for expanded text search.

Before production-scale data, introduce a dedicated place search document or a database full-text strategy that is validated for Korean text and keeps region/category visibility filters explicit.

### Reason
- the current search uses multiple `%keyword%` text predicates across place and region fields
- ordinary indexes do not solve leading-wildcard text matching
- a dedicated search shape keeps exploration UX flexible without hardcoding business policy into query text

---

## 29. Brand Favicon Color Update

### Decision
Update the brand favicon to use the primary purple color (#6750a4) with a white "H" logo.

### Reason
- align with the core Material 3 primary color (`--m3-sys-primary`)
- improve brand consistency across the platform
- enhance visual clarity at small icon sizes

---

## 30. Comment Length Policy

### Decision
Enforce the user comment content length through `system_policies.comment.max_length` in the comment service instead of a hardcoded DTO size constraint.

The database column remains capped at 300 characters as a storage constraint, and policy values above that physical column limit are rejected as policy violations.

### Reason
- comment length is a business rule that admins should be able to adjust
- server-side service validation keeps the rule consistent across create and update flows
- the storage limit still protects persistence from oversized configured values

---

## 31. Community Post Length Policy

### Decision
Enforce community post title and content lengths through `system_policies.community.post_title_max_length` and `system_policies.community.post_content_max_length` in the community post service instead of hardcoded DTO size constraints.

The database columns remain capped at 120 characters for title and 2000 characters for content as storage constraints. Policy values above those physical limits are rejected as policy violations.

### Reason
- free-board title/content limits are operational content policy
- service-layer validation keeps create and update behavior consistent
- admins can tune community posting limits without changing code

---

## 32. Place Text Length Policy

### Decision
Enforce place recommended menu, short recommendation, and feature text lengths through `system_policies.place.recommended_menu_max_length`, `system_policies.place.short_recommendation_max_length`, and `system_policies.place.feature_text_max_length` in the place service instead of DTO size constraints.

The database columns remain capped at 255 characters for recommended menu and short recommendation, and 500 characters for feature text. Policy values above those physical limits are rejected as policy violations.

### Reason
- these fields shape user-facing place content and are operational policy
- service-layer validation keeps place create and update behavior consistent
- admins can tune content length without code changes while the database still protects persistence

---

## 33. Report Text Length Policy

### Decision
Enforce user report reason text and admin report review note lengths through `system_policies.report.reason_text_max_length` and `system_policies.report.review_note_max_length` in the report services instead of DTO size constraints.

The report table columns remain capped at 255 characters. Policy values above that physical limit are rejected as policy violations.

### Reason
- report details and review notes are operational moderation policy
- service-layer validation keeps API behavior consistent before persistence and audit logging
- admins can tune reporting text limits without code changes while the database still protects storage

---

## 34. Report Follow-Up Text Length Policy

### Decision
Enforce admin report follow-up reason and memo lengths through `system_policies.report.follow_up_reason_max_length` and `system_policies.report.follow_up_memo_max_length` in the admin report service instead of DTO size constraints.

The follow-up reason maps into the downstream user sanction reason when a user sanction action is applied. The follow-up memo is validated after adding report context such as `Report #id` so downstream admin action log memo columns remain protected by the same policy boundary. The physical storage limit remains 255 characters.

### Reason
- report follow-up actions are moderation operations and must stay operator-tunable
- service-layer validation protects downstream admin workflows before they receive the request
- validating the contextual memo avoids accepting a raw memo that becomes too long after audit context is added

---

## 35. Admin Operation Text Length Policy

### Decision
Enforce admin user sanction reason length through `system_policies.admin.sanction_reason_max_length`.

Enforce admin operation memo length through `system_policies.admin.action_memo_max_length` for admin user trust/recommend-weight changes, user sanctions, place moderation/score changes, and ranking/season operations.

The `user_sanctions.reason` and `admin_action_logs.memo` columns remain capped at 255 characters as storage constraints. Policy values above that physical limit are rejected as policy violations.

### Reason
- administrator notes and sanction reasons are operational policy, not controller-level constants
- service-layer validation protects audit writes before persistence
- admins can tune moderation note length without changing code while database limits still protect storage

---

## 36. Image URL Length Policy

### Decision
Enforce place image URL length through `system_policies.place.image_url_max_length` in the place service instead of DTO size constraints.

Enforce visit verification image URL length through `system_policies.visit.image_url_max_length` in the visit service instead of DTO size constraints.

The `place_images.image_url` and `visits.image_url` columns remain capped at 255 characters as storage constraints. Policy values above that physical limit are rejected as policy violations.

### Reason
- image URL length is an operational storage/input policy that should remain adjustable
- service-layer validation protects create/update and visit flows before persistence
- validating replacement place image URLs before deleting old rows avoids losing existing images on invalid input

---

## 37. Place Address Length Policy

### Decision
Enforce place road-address and jibun-address lengths through `system_policies.place.address_max_length` in the place service instead of DTO size constraints.

The `places.address_road` and `places.address_jibun` columns remain capped at 255 characters as storage constraints. Policy values above that physical limit are rejected as policy violations.

### Reason
- place address text is operational input policy and should remain adjustable
- service-layer validation keeps create and update behavior consistent before persistence
- the storage limit still protects the database from oversized configured values

---

## 38. Mobile Saved Places (Wishlist) Client-side Parallel Detail Mapping

### Decision
For the mobile saved places (wishlist) tab, retrieve the simple saved places metadata from the user recommendations API, then concurrently fetch the detailed information of each place on the client-side via asynchronous parallel calls (`Future.wait`) using the public place detail endpoint.

### Reason
- The user recommendations API `/api/users/me/recommendations` returns only basic identifiers (placeId, name, savedAt). Doing client-side parallel fetching avoids writing a heavy backend DTO custom-join for this tab.
- Using `Future.wait` speeds up the combined loading time relative to serial requests.
- Keeping authentication, empty placeholders, and swipe-to-unsave interactions client-side enhances mobile responsiveness.

---

## 39. Mobile Community (Tab 3) Integration and Policy Verification Flow

### Decision
For the mobile community tab, integrate the client-side screens (`CommunityScreen`, `CommunityCreateScreen`, `CommunityDetailScreen`, `CommunityEditScreen`) with the backend community endpoints (`/api/community/posts`).
Enforce phone verification client-side before allowed post creation actions by referencing `authProvider.userProfile?.phoneVerified` instead of `userStatus`, and redirect unverified users to the MyPage verification flow (`onTabSwitch(4)`).
For guest users, render an appealing inline login prompt card that routes users to the native Flutter Kakao/local login screen.

### Reason
- Placing a client-side check on `userProfile?.phoneVerified` avoids unnecessary API network failure rounds for unverified or guest users.
- Standardizing route index matching (MyPage is index 4) ensures predictable, user-friendly redirection tabs.
- Fixing early static analysis compiler errors on `userStatus` and `MainAxisAlignment.between` guarantees native build stability across emulators and devices.

---

## 40. Place Audience Tag System

### Decision
Implement the **Audience Tag System** in Honeytong to dynamically aggregate demographic metrics (age, gender, nationality) from recommendations and visits, and map them to localized popular tags (e.g., "외국인 인기", "20대 여성 선호").
- Demographic fields (`birthYear`, `gender`, `nationalityCode`) are added to the user profile.
- A dedicated `place_audience_stats` database table tracks demographic counts per place.
- Demographics recalculation triggers on user actions (active recommendation, valid visit verification) and admin activity invalidations.
- Audience tags are generated using ratio thresholds ($N \ge 1$, foreigner ratio $\ge 30\%$, dominant gender ratio $\ge 60\%$, dominant age group ratio $\ge 40\%$).
- Generated tags are exposed on the frontend through place details and ranking lists.

### Reason
- Provides exploration-first, trust-based personalization without exposing sensitive, individual user demographics directly.
- Running aggregation dynamically on participation updates ensures tags remain current.
- Mapped Korean labels match localization constraints while protecting personal data.

---

## 41. Redis Caching for Hot Endpoints (Active Missions and Current Season)

### Decision
Implement Redis caching for hot endpoints (`GET /api/missions` and `GET /api/rankings/seasons/current`) to improve performance and throughput.
- Separate caching boundaries (`MissionCache` and `SeasonCache` interfaces) are introduced to support both `Redis` and `NoOp` implementations based on the `app.redis.enabled` flag.
- Default TTL configurations are set to 10 minutes for active missions and 1 hour for current season data.
- Safe serialization using Jackson `ObjectMapper` with `JavaTimeModule` is used.
- Exceptions during Redis read/write operations are caught and logged, allowing a silent fallback to the database.
- Cache invalidation (evict) hook is integrated in `AdminRankingService` to clear the cached current season whenever a season is created or modified.

### Reason
- Highly concurrent endpoints like active missions and current season queries can place unnecessary load on the database. Caching them reduces latency and DB traffic.
- Transparent database fallback ensures high availability, even if Redis experiences connection issues.
- Manual cache eviction on admin mutations keeps the current season cache consistent.

---

## 42. Decoupled Demographics Recalculation for Place Stats

### Decision
Decouple the heavy demographic statistics recalculation query from the main transactional request-response path (recommendation, visit, invalidation) using Spring Application Events and asynchronous event listeners.
- Enable Spring's asynchronous processing (`@EnableAsync`) with a dedicated `ThreadPoolTaskExecutor` bean `taskExecutor` under `AsyncConfig` with a thread name prefix `Honey-Async-`.
- Define a custom event `PlaceDemographicsRecalculateEvent` carrying the `placeId`.
- Modify `PlaceAudienceStatsService.recalculateStats(Long placeId)` to publish this event using an autowired optional `ApplicationEventPublisher`.
- If the event publisher is not present (such as during mock-based service unit tests), fall back to synchronous execution to ensure test stability.
- Create `PlaceDemographicsEventListener` to listen to this event using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` combined with `@Async("taskExecutor")`. The listener executes `PlaceAudienceStatsService.recalculateStatsSync(Long placeId)` asynchronously in a separate thread.

### Reason
- Running the heavy SQL query `findDemographicsByPlaceId` inside the request-response transaction holds a pessimistic write lock on the `place_stats` row for the entire duration of the query (often tens of milliseconds).
- Decoupling this recalculation to run asynchronously after the main transaction commits releases the lock on `place_stats` immediately (reducing lock duration to milliseconds), preventing lock contention and database serialization bottlenecks under concurrent traffic.
- Running the async listener after the main transaction commits preserves consistency since it ensures we only recalculate statistics for successfully committed recommendations and visits.

---

## 43. Database Index Optimization for Heavy Queries

### Decision
Implement database indexes to optimize heavy read and write operations, including audit logs sorting, active sanction validations, and covering index for demographic aggregations:
1. **User Action Logs**: Add index `idx_user_action_logs_created_at` on `(created_at DESC)` to optimize `findTop50ByOrderByCreatedAtDesc()`.
2. **Admin Action Logs**: Add index `idx_admin_action_logs_created_at` on `(created_at DESC)` to optimize `findTop50ByOrderByCreatedAtDesc()`.
3. **User Sanctions**: Add index `idx_user_sanctions_active_check` on `(user_id, status, sanction_type, start_at, end_at)` to optimize `existsBlockingSanction(...)`.
4. **Visits**: Add index `idx_visits_place_valid_user` on `(place_id, is_valid, user_id)` as a covering index to optimize user demographic aggregation queries.
5. **Users**: Add index `idx_users_created_at` on `(created_at DESC)` to optimize `findAllByOrderByCreatedAtDesc()`.

### Reason
- **Sorting Performance**: Action logs (`user_action_logs` and `admin_action_logs`) and user lists (`users`) accumulate huge numbers of records. Indexing on `created_at DESC` removes CPU-intensive filesort operations for operator dashboard scanning.
- **Critical Path Latency**: The active sanction check is queried before every core user write action. A composite index covering target fields minimizes check duration and prevents transactional delays.
- **Covering Index Optimization**: Joining visits in demographic recalculations is optimized by creating a composite covering index, enabling the DB to resolve the query entirely from the index tree without loading table data pages.

---

## 44. Notification System Implementation

### Decision
Implement an in-app database-backed notification system to alert users about key updates, interactions, and events (comments on registered places, completed missions, processed reports).
- Created a Flyway schema migration (V7) for the `notifications` table.
- Implemented core domain classes (`Notification` entity, `NotificationRepository` with custom query methods, and `NotificationResponse` DTO).
- Built user APIs: `GET /api/notifications` (for listing user's notifications in descending order of creation time) and `PATCH /api/notifications/{notificationId}/read` (for reading notifications after validating user ownership).
- Designed the notification event architecture using Spring's `ApplicationEventPublisher` to decouple domains: defined `CommentCreatedEvent`, `MissionCompletedEvent`, and `ReportProcessedEvent`.
- Implemented `NotificationEventListener` to handle events and generate custom localized Korean notification messages.
- Integrated event publishing hooks into existing service layers (`CommentService`, `MissionService`, and `AdminReportService`).

### Reason
- In-app database notifications provide a baseline history of events for users without requiring real-time setup (like FCM/WebSockets) which is out of the scope of MVP.
- Decoupling notification trigger logic from the core business domains using Spring Application Events avoids circular dependencies and enforces architectural separation.
- Handling localized messaging within the event listener preserves Korean language policies while keeping other service domains language-agnostic.

---

## 45. Advanced Analytics System

### Decision
Implement an Advanced Analytics System in Honeytong to track user preferences, hyperlocal regional trends, and daily platform activity trends.
- Structured response mappers through custom DTO records (`UserAnalyticsResponse`, `RegionalTrendsResponse`, `AdminAnalyticsResponse`).
- Implemented `AnalyticsRepository` with optimized Native SQL queries to aggregate data dynamically across different entities (`places`, `visits`, `recommendations`, `comments`, `users`).
- Constructed two service layers: `AnalyticsService` for user statistics (category/price preferences percentage calculations, and 30-day activity count padding) and regional trends; `AdminAnalyticsService` for global platform activity trends.
- Designed `AnalyticsCache` (supporting optional Redis and NoOp configurations) to cache regional trends (TTL: 1 hour) and admin dashboard trends (TTL: 2 hours).
- Exposed REST API endpoints: `GET /api/analytics/me` (authenticated), `GET /api/analytics/regions/{dongId}/trends` (public/permitted), and `GET /api/admin/analytics/activity-trends` (restricted to ADMIN/SUPER_ADMIN role).

### Reason
- Real-time logging queries can stress the DB under heavy traffic. Using custom Native Queries with existing optimized indexes combined with Redis caching limits latency and database serialization overhead.
- Grouping user-specific trends client-side after returning small date maps reduces payload size, while zero-count date padding is handled elegantly in Java to prevent UI chart failures.
- Separating public trends from authenticated endpoints maintains exploration-first UX benefits for guest users while securing user data.

## 46. Fraud Detection System Implementation

### Decision
Implement a Fraud Detection System to detect abnormal recommendation or visit activity patterns (rapid participation, GPS teleportation, IP spam), record them in the `fraud_alerts` table, alert admins, and automatically penalize the user's trust score.
- Created Flyway schema migration (V8) for the `fraud_alerts` table.
- Defined `FraudAlertType` with rules: `RAPID_PARTICIPATION` (actions within a short interval), `GPS_TELEPORTATION` (physically impossible movement between consecutive visits), and `IP_SPAM` (multiple accounts operating under the same IP address).
- Implemented `FraudDetectionService` to perform abnormal pattern scans.
- Configured real-time detection via `@Async` and `TransactionalEventListener` to isolate the scan from core business transactions, preventing delays or rollback propagation.
- Automatically subtracts a policy-driven trust penalty from the user's trust score upon alert generation, triggering a recalculation of their trust grade and recommendation weight.
- Rapid participation windows and thresholds, IP spam windows and thresholds, GPS teleportation thresholds, fraud risk scores, and the trust penalty are loaded from `system_policies.fraud.*`.
- Implemented admin endpoints: `GET /api/admin/fraud/alerts` (list of all alerts) and `GET /api/admin/fraud/suspicious-users` (users with highest alert counts).

### Reason
- **Transaction Safety**: To prevent potential lock contention or transaction rollbacks from affecting normal user operations, the detection logic is processed asynchronously only after the parent business transaction successfully commits.
- **Automatic Penalization with Manual Sanctions**: While trust scores are automatically penalized by a policy-driven amount to limit immediate ranking manipulation, full account suspension (Sanction) is kept as a manual admin-only action to prevent false-positive automated bans.

---

## 46.1 Audience Tag Policy Thresholds

### Decision
Audience tag generation thresholds are system policies, not code constants.

### Current Implementation
- `audience.minimum_participants` controls the minimum aggregate count before tags are generated.
- `audience.foreigner_ratio_threshold`, `audience.gender_ratio_threshold`, and `audience.age_ratio_threshold` control demographic tag thresholds.
- Missing or invalid audience policies raise `POLICY_VIOLATION` rather than silently using embedded defaults.

### Reason
- Audience tags affect discovery and ranking perception, so operators must be able to tune thresholds without redeploying.
- Keeping the values in `system_policies` satisfies the rule that business policy values are not hardcoded in services.

---

## 47. Recommendation & Ranking Scoring Optimization

### Decision
Implement Shannon Entropy for demographic diversity bonus and timeframe-based recency bonus calculation in the ranking recalculation service, driven dynamically by system policies.
- **Demographic Diversity Score**: Shannon Entropy is calculated over user birth years (grouped by decades) and gender cohorts. The entropy value is normalized between 0.0 and 1.0 based on active count ratio, then multiplied by the dynamic policy `ranking.diversity_weight`.
- **Recency Score**: Based on a configurable timeframe (`ranking.recency_days`), query and count recommendations, valid visits, and comments. Calculate `recentScore` using dynamic policy weights (`ranking.recency_recommend_weight`, `ranking.recency_visit_weight`, `ranking.recency_comment_weight`).
- **Integration**: Apply both bonuses to the `PlaceStats` model using an atomic update `updateBonuses(recentScore, diversityScore)`. The combined score is persistent in the database.

### Reason
- **Exploration-First UX**: Encourages diversity by ranking places that attract varied demographics higher, aligning with the platform's core discovery identity.
- **System Policy Control**: Adheres to the non-negotiable rule of avoiding hardcoded business rules, allowing administrators to tune scoring dynamics in real time.
- **Transactional Consistency**: Keeps database operations optimized and isolated within the recalculation phase.

---

## 48. Place Search Optimization with FULLTEXT ngram Index

### Decision
Optimize the place search keyword matching query by introducing a MySQL FULLTEXT index with the CJK `ngram` parser, combined with a hybrid search execution strategy.
- **Database Indexing**: Drop the old prefix B-tree index on `place_search_documents.search_text` and create a `FULLTEXT INDEX` using the native `ngram` token parser.
- **Hybrid Service Routing**: 
  - For keywords >= 2 characters, execute a native SQL Full-Text Search query utilizing `MATCH(d.search_text) AGAINST(:keyword IN BOOLEAN MODE)`. Keywords are preprocessed into FTS Boolean terms (e.g. `+term1* +term2*`) to perform precise substring and multi-word matching.
  - For 1-character keywords, fallback to a traditional `LIKE CONCAT('%', :keyword, '%')` wildcard search to bypass CJK tokenization limitations of the ngram parser.

### Reason
- **Search Scale Performance**: Wildcard prefix LIKE queries perform full table scans which cannot scale with search volumes. FTS utilizes specialized index trees for rapid, indexed word lookup.
- **CJK Token Boundaries**: CJK (Chinese, Japanese, Korean) text has no native space-based boundaries, so normal full-text parsers fail. The `ngram` parser indexes overlapping character groups, enabling accurate Korean substring searches.
- **Single-Character Usability**: Fallback routing ensures that searches for single Korean letters (e.g., "닭") continue to yield results, avoiding empty sets that FTS would return due to default minimum token length limits.

---

## 49. Spatial Index & Spatial Query for Nearby Places

### Decision
Optimize the nearby places API (`GET /api/places/nearby`) by introducing a native MySQL `SPATIAL INDEX` and refactoring coordinates range query execution to the database layer.
- **Database Indexing**: Define a `location` point column on `places` as a `STORED` Generated Column derived from `longitude` and `latitude`. Create a `SPATIAL INDEX` on this column. This ensures no third-party Java geometry dependencies (e.g. `hibernate-spatial`) are introduced to the core JPA mapping.
- **Query Optimization**: Implement a native SQL query `findNearbyPlaces` utilizing native spatial functions `ST_Distance_Sphere` and `ST_PointFromText` to filter, sort by proximity, and limit (50 items) inside MySQL.
- **Hybrid Distance Representation**: JVM computes the precise Haversine distance in memory *only* for the returned 50 places to populate the DTO response, completely bypassing full table Haversine scans.
- **MySQL Axis Order**: All WKT point parsing for SRID 4326 must pass `axis-order=long-lat`, and the generated `location` column must be `NOT NULL`, because MySQL 8 otherwise interprets 4326 coordinates as latitude/longitude and rejects Korean longitudes as invalid latitudes before creating the spatial index.

### Reason
- **Platform Scale**: Fetching the entire places table into JVM memory to calculate distance is highly inefficient and creates CPU and memory exhaustion risks. DB-side spatial indexing ensures sub-millisecond range scans even with large datasets.
- **Code Decoupling**: Generated columns allow the JPA application code to insert and update normal `latitude`/`longitude` columns without mapping JTS or Geolatte geometry types in Java.
- **H2 Test Compatibility**: Because Java entities remain decoupled from spatial column mappings, H2 database schemas in the test profile are auto-created without spatial generated column features, preventing test context startup crashes.
- **Runtime Correctness**: Explicit axis order keeps Kakao-style longitude/latitude coordinates compatible with MySQL spatial functions across local development and production migration runs.

---

## 50. Non-Intrusive Lightweight Localization Architecture

### Decision
Implement a lightweight, dependency-free localization system (i18n) for React web frontend and Flutter mobile app to prevent dependency version conflicts.
- **React Frontend**: Created `LocaleContext` and custom hook `useTranslation()` utilizing native React Context/Hooks to manage locale transitions dynamically. Stored locale selections in `localStorage`.
- **Flutter Mobile**: Created a lightweight `Localization` singleton ChangeNotifier and String extension method (`tr`) utilizing SharedPreferences to persist local settings.
- **Resources**: Organized translations in JSON format (`ko.json`, `en.json`, `ja.json`) and Dart maps, prioritizing Korean as the default locale source.
- **In-App Dynamic Selection**: Formally rejected multi-package app store builds (separate builds for English/Japanese installers) due to high maintenance overhead. Adopted in-app locale switches that toggle translation keys instantly without page reloads.

### Reason
- **Dependency Isolation**: Adding external multi-language frameworks (e.g. `react-i18next`, `easy_localization`) can break builds during compiler updates or when packages clash with core framework versions (React 19 or Flutter 3.x).
- **Zero Refresh Switch**: Utilizing React Context and Flutter ChangeNotifier allows the application UI to switch languages instantly without refreshing pages or rebooting.
- **Client-Side Autonomy**: Keeps translation concerns on the client side, decoupled from backend systems to avoid API-level translations.
- **Maintenance & Scalability**: Bundling translation files (several KBs) in a single package is lightweight and allows effortless translation expansion (e.g., adding Chinese) later by simply introducing a new JSON/map asset without changing the core layout structure.

---

## 51. Redis-Based Concurrency Lock for User Region Change

### Decision
Implement a Redis-based distributed lock (`SETNX`) on `changeMyRegion` inside `RegionService` to prevent concurrent region change requests from bypassing cooldown policies.
- **Lock Key**: `lock:region:change:user:{userId}` with a 10-second expiry duration.
- **Transaction Safety**: Utilize Spring's `TransactionSynchronizationManager` to release the Redis lock inside the `afterCompletion` callback, guaranteeing that locks are only freed after the database transaction has committed or rolled back.

### Reason
- **Cooldown Policy Integrity**: Without lock enforcement, concurrent regional requests can execute parallel check-queries, bypassing the 7-day cooldown threshold and creating duplicate primary regions.
- **Race Condition Prevention**: Relies on Redis atomic set-if-absent capabilities to immediately reject concurrent write attempts and preserve data consistency across instances.

---

## 52. Address-Resolved Place Coordinates

### Decision
When creating or updating a place with a road or jibun address, the backend resolves the address through Kakao Local address search before persistence.
- The resolved Kakao `y` value is stored as latitude and `x` as longitude.
- The resolved coordinate is also passed through the existing coordinate-to-region resolver, and that administrative dong is used for registration-scope validation and place persistence.
- Client-supplied latitude/longitude remain fallback values only for requests without an address.
- On place update, address-derived coordinates and administrative dong take precedence over a client-provided `dongId` whenever a next address is present.

### Reason
- **Location Correctness**: Mobile registration initially populated coordinates from the user's current GPS position, which could place a restaurant marker next to the user even when the typed address was elsewhere.
- **Edit Consistency**: The same mistake must not reappear when an owner edits a restaurant address.
- **Server-Side Validation**: Address-to-coordinate and coordinate-to-region decisions must be enforced server-side rather than trusting mobile clients.
- **Region-Aware Rules**: Registration scope must be evaluated against the actual address location, not only the user's currently verified dong or a client-provided `dongId`.

---

## Final Note

Every new feature must respect these decisions.

If a change conflicts with a decision:
- document it here
- explain the reason
- update related systems accordingly

