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
- development sender logs verification codes
- verification codes are stored as hashes
- code length, expiry, and max attempts are configurable
- core actions can use the server-side `@RequirePhoneVerified` guard

### Future Plan
- replace development sender with a production SMS provider adapter
- required external values will include provider access key, secret key, sender number, and message template settings

### Reason
- backend validation can be built before choosing the SMS provider
- production credentials do not need to be committed to the repository
- sender implementation can change without changing controller or service logic

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
- frontend can read `VITE_KAKAO_JAVASCRIPT_KEY`

### Reason
- Kakao Local API directly supports coordinate-to-region-code conversion
- Kakao Local API also supports keyword/category place search with coordinates and radius
- this fits region verification and nearby place discovery before the place domain is implemented

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

### Reason
- region change is a business policy and must remain admin-adjustable
- place registration, recommendation, visit, and ranking depend on stable primary region behavior

---

## 16.7 Policy Seed Import and Admin Policy Management

### Decision
Default system policies can be imported from an opt-in UTF-8 CSV seed file, and admins manage policies through separated admin APIs.

### Current Implementation
- `POLICY_SEED_ENABLED=true` enables missing-only policy seed import on backend startup
- `POLICY_SEED_LOCATION` defaults to `classpath:policy/policy-defaults.csv`
- `GET /api/admin/policies` is available to ADMIN and SUPER_ADMIN users
- policy updates are restricted to SUPER_ADMIN users
- policy updates are logged to `admin_action_logs`

### Reason
- business policy values must live in the database, not application logic
- seed import gives local and initial environments a repeatable bootstrap path
- missing-only import avoids overwriting values changed by admins
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
- growth EXP is returned as `0` until the trust/level growth system is implemented

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

## Final Note

Every new feature must respect these decisions.

If a change conflicts with a decision:
- document it here
- explain the reason
- update related systems accordingly
