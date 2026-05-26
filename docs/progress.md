# ?뱢 Development Progress

This document tracks the current development state of Honeytong.

It is used to:
- maintain development continuity
- inform Codex of current progress
- prevent duplicate or conflicting implementations
- guide next steps

---

## 1. Current Phase

Phase: FLUTTER REFACTOR STABILIZATION
Status: Backend policy and region guards are passing regression; Flutter data contract and Kakao map readiness issues are being stabilized.

The system design, API structure, database schema, and rules are defined.
Backend MVP implementation is progressing in small API increments.

---

## 2. Completed Documents

The following documents are finalized:

- [x] prd.md
- [x] rules.md
- [x] agent.md
- [x] db-schema.md
- [x] api-spec.md
- [x] architecture.md
- [x] tasks.md

All documents are consistent with each other.

---

## 3. Implementation Status

### Harness / Repository Setup
- [x] Root Git repository initialized
- [x] Root GitHub remote configured
- [x] UTF-8 editor and Git attributes configured
- [x] Root README added with Windows dev quickstart and docs pointers
- [x] Frontend lint/build checks pass
- [x] CI updated for current frontend-first state and future backend Gradle project
- [x] Agent harness now requires next recommended task and matching reasoning level after work
- [x] Agent workflow skill added to adapt local andrej-skills guidance under Honeytong rules
- [x] Repository ignore rules now exclude local `tmp/` QA artifacts such as screenshots and browser profile/cache output
- [x] Backend Spring Boot project initialized
- [x] Backend Gradle Wrapper generated and verified
- [x] Backend default DB schema set to `honey`
- [x] Backend application context startup verified after admin dashboard constructor injection fix
- [x] Backend CORS is enabled through Spring Security and allows local frontend dev origins
- [x] Backend application logging baseline configured with environment-driven levels, file output, rotation, and safe SQL bind defaults
- [x] Backend health readiness baseline configured with Spring Boot Actuator `/actuator/health`
- [x] Backend production profile baseline added with external DB/JWT requirements and schema auto-update disabled by default
- [x] Backend Flyway migration baseline added for production schema management
- [x] Backend Redis connection baseline added with Redis-backed behavior disabled by default
- [x] Windows tool session bootstrap added for consistent Git, Node/npm, Java, Gradle, and Codex command behavior
- [x] Local MVP smoke confirmed backend health, frontend route loading, signup/login, user status, policy-dependent status APIs, and GPS region verification after local policy and region seed bootstrap
- [x] Local place creation smoke confirmed unauthenticated phone state is blocked before write actions
- [x] Local phone-verified bootstrap account smoke confirmed place creation, place detail, my registered places, dong place list, and audit log reads
- [x] Local phone-verified participation smoke confirmed recommendation, visit verification, comment creation, place aggregate counters, and user action log reads
- [x] Local ranking smoke confirmed season creation, admin recalculation, dong/district/city ranking reads, history finalization, and public place ranking history reads
- [x] Local moderation smoke confirmed report creation, admin approval, report follow-up hide action, public hidden-state behavior, exposure restore, and admin action log reads
- [x] Public place ranking history security rule aligned with the documented public read API
- [x] Local abuse-prevention smoke confirmed duplicate recommendation rejection, visit cooldown rejection, duplicate comment rejection, and server-side aggregate preservation
- [x] Local admin activity moderation smoke confirmed recommendation invalidation, visit invalidation, comment blind, aggregate counter rollback, public comment hiding, and admin action log reads
- [x] Full local regression passed after accumulated MVP smoke testing: backend tests, frontend build, frontend lint, diff check, UI language check, Korean encoding check, and document sync check
- [x] Final MVP release-readiness regression passed after live SOLAPI SMS smoke: backend tests, frontend build, frontend lint, diff check, UI language check, Korean encoding check, doc sync check, and sensitive smoke-data scan

### Authentication
- [x] Local signup API implemented
- [x] Local login API implemented
- [x] OAuth login API implemented for Kakao, Naver, and Google
- [x] OAuth provider token verification is separated behind replaceable provider clients
- [x] OAuth first login creates user, user_auth, user_trust, and user_level rows
- [x] Access token / refresh token structure implemented
- [x] Refresh token rotation implemented
- [x] Logout revokes refresh token
- [x] Phone verification code send API implemented
- [x] Phone verification code verify API implemented
- [x] Phone verification status API implemented
- [x] Phone verification guard annotation/aspect implemented for future core actions
- [x] Phone verification unit tests added
- [x] Frontend login/signup token storage flow connected to local auth APIs
- [x] Naver Cloud SENS production SMS sender adapter implemented behind `PhoneVerificationSender`
- [x] SOLAPI production SMS sender adapter implemented behind `PhoneVerificationSender`
- [x] SOLAPI and Naver SENS sender beans verified with explicit Spring constructor injection coverage
- [x] Live SOLAPI SMS delivery smoke passed against local backend and real handset

### Phone Verification
- [x] DB-backed verification code state implemented
- [x] Development sender logs code issuance without raw code at INFO level
- [x] Server-side phone verification guard mechanism implemented
- [x] Phone verification guard coverage verified for implemented core actions
- [x] Place owner updates enforce phone verification and blocking-sanction checks while admin overrides remain allowed
- [x] Phone verification code lookup and attempt tracking can use Redis when `APP_REDIS_ENABLED=true`
- [x] Frontend phone verification send, verify, and status flow connected from My Page
- [x] Production SMS provider integration added for Naver Cloud SENS through `PHONE_VERIFICATION_SENDER_PROVIDER=naver-sens`
- [x] Production SMS provider integration added for SOLAPI through `PHONE_VERIFICATION_SENDER_PROVIDER=solapi`
- [x] Production profile defaults phone verification delivery to SOLAPI instead of the development sender
- [x] SMS delivery failure no longer writes the latest verification code state to cache
- [x] Live phone verification smoke runbook and PowerShell script added without logging raw codes or full phone numbers
- [x] Spring bean creation coverage added for SOLAPI and Naver SENS sender adapters
- [x] Live SMS delivery smoke with SOLAPI provider credentials passed

### User System
- [x] User profile API implemented
- [x] User profile update API implemented
- [x] User status API implemented
- [x] User growth API implemented
- [x] User activity summary API implemented with replaceable repository-backed reader
- [x] User service unit tests added
- [x] Activity summary counts active recommendations, valid visits, visible comments, and non-deleted registered places

### Region System
- [x] Region city/district/dong entities and repositories implemented
- [x] Region lookup APIs implemented
- [x] My primary region API implemented
- [x] GPS verification service boundary implemented
- [x] Kakao Map/Local API environment variable placeholders prepared
- [x] Kakao coordinate-to-region resolver implemented
- [x] Region seed CSV import path implemented
- [x] Region seed sample CSV added for Kakao administrative dong code format
- [x] Full Korean administrative dong seed CSV generated from `KIKcd_H.20260325.xlsx`
- [x] Region seed conversion script added
- [x] Region seed imported into local MySQL schema `honey`
- [x] Live Kakao GPS region verification smoke test passed for Seoul Mapo-gu Seogyo-dong
- [x] Region change API implemented
- [x] Region change policy API implemented
- [x] Region change cooldown reads `system_policies.region.change_cooldown_day`
- [x] Region service unit tests added
- [x] Kakao region resolver unit test added
- [x] Production seed file aligns `region_dong.code` with Korean administrative dong codes used by Kakao
- [x] Frontend region verification flow connected from My Page

### Policy System
- [x] `system_policies` entity and repository implemented
- [x] Required integer policy loader implemented
- [x] Region change cooldown wired to policy service
- [x] Required string policy loader implemented
- [x] Admin policy list/update API implemented at `/api/admin/policies`
- [x] Admin region policy API implemented at `/api/admin/policies/region`
- [x] Admin policy frontend management flow connected at `/admin/policies`
- [x] Opt-in policy seed import path added with `POLICY_SEED_ENABLED`
- [x] Local development seeds missing default policies by default while production remains opt-in
- [x] Redis policy cache boundary implemented with DB fallback and admin update invalidation
- [x] JWT expiration policies (access/refresh token validity) and phone verification policies (code length, expiry, max attempts) wired to policy service
- [x] Visit cooldown checks can use Redis when `APP_REDIS_ENABLED=true` and fall back to visit rows when disabled
- [x] Valid visits update `place_stats.visit_count`, `score_total`, and `trust_weighted_score`
- [x] Valid visits update user EXP and trust score through `UserGrowthService`
- [x] Visit EXP reads `system_policies.growth.visit_exp`
- [x] Valid-visit trust score reads `system_policies.trust.valid_visit_score`
- [x] Visit response `expGained` returns the policy-driven EXP granted by the visit
- [x] Place detail frontend visit verification action connected to browser GPS and backend visit API

### Trust & Level System
- [x] Signup initializes user trust and level rows
- [x] Phone verification marks the trust phone signal
- [x] Region verification marks the trust region signal
- [x] Valid visits add policy-driven EXP to `user_level.exp` and `user_level.total_exp`
- [x] Valid visits add policy-driven score to `user_trust.trust_score`
- [x] Level-up reads `system_policies.growth.level_exp_thresholds`
- [x] Level-up subtracts consumed EXP and keeps remaining EXP toward the next level
- [x] Level changes write `user_level_history` with reason `VALID_VISIT`
- [x] Trust grade reads `system_policies.trust.grade_thresholds`
- [x] Recommendation weight reads `system_policies.trust.recommend_weight_by_grade`
- [x] User status `nextLevelExp` reads growth policy thresholds

### Comment System
- [x] `comments` entity/repository implemented
- [x] Create comment API implemented at `POST /api/places/{placeId}/comments`
- [x] Update comment API implemented at `PATCH /api/comments/{commentId}`
- [x] Delete comment API implemented at `DELETE /api/comments/{commentId}`
- [x] Place comment list API implemented at `GET /api/places/{placeId}/comments`
- [x] My comments API implemented at `GET /api/users/me/comments`
- [x] Comment create/update requires phone verification via `@RequirePhoneVerified`
- [x] One visible comment per user/place is enforced
- [x] Deleted comments can be restored by writing again without violating the DB unique key
- [x] Valid visible comments update `place_stats.comment_count`, `score_total`, and `trust_weighted_score`
- [x] Place detail frontend comment list, create, update, and delete actions connected to backend comment APIs

### Ranking System
- [x] `seasons`, `place_season_scores`, and `place_ranking_history` entities/repositories implemented
- [x] Place ranking read API implemented at `GET /api/rankings/places`
- [x] Current season API implemented at `GET /api/rankings/seasons/current`
- [x] Ranking reads use `place_season_scores` aggregate rows instead of raw recommendation/visit/comment rows
- [x] Ranking read supports dong/district/city region types and optional `seasonCode`
- [x] Admin season list/create/update APIs implemented at `/api/admin/seasons`
- [x] Admin ranking recalculation API implemented at `POST /api/admin/rankings/recalculate`
- [x] Ranking aggregation writes `place_season_scores` from `place_stats`
- [x] Ranking aggregation reads `ranking.recommend_weight`, `ranking.visit_weight`, and `ranking.comment_weight`
- [x] Ranking aggregation applies audited `place_stats.manual_adjustment_score` after automatic score components
- [x] Ranking aggregation skips places with `places.ranking_excluded = true`
- [x] Public place ranking reads can use Redis when `APP_REDIS_ENABLED=true` while `place_season_scores` remains authoritative
- [x] Disabled-by-default ranking scheduler foundation implemented with environment-driven schedule settings
- [x] Ranking history finalization foundation implemented by rewriting `place_ranking_history` from `place_season_scores` for a selected season
- [x] Place ranking history read API implemented at `GET /api/places/{placeId}/ranking-history`
- [x] Place ranking history reads finalized `place_ranking_history` rows and returns empty items when no history exists
- [x] Hidden or deleted places are rejected from public ranking history reads
- [x] Ranking query index migration added for public ranking reads, history finalization, and place history reads
- [x] Ranking exclusion control implemented
- [x] Ranking frontend read views connected to current season, regional ranking, and place ranking history APIs

### Report System
- [x] `reports` entity/repository implemented
- [x] Report target/status enums implemented for PLACE, COMMENT, USER and PENDING/APPROVED/REJECTED
- [x] Create report API implemented at `POST /api/reports`
- [x] My reports API implemented at `GET /api/users/me/reports`
- [x] Report creation validates active reporter and existing visible target
- [x] User self-reporting is rejected
- [x] Admin report list API implemented at `GET /api/admin/reports`
- [x] Admin report detail API implemented at `GET /api/admin/reports/{reportId}`
- [x] Admin report processing API implemented at `PATCH /api/admin/reports/{reportId}`
- [x] Admin report processing logs `REPORT_PROCESS` to `admin_action_logs`
- [x] Admin report follow-up action API implemented at `POST /api/admin/reports/{reportId}/actions`
- [x] Approved reports can trigger explicit follow-up actions for place hide/delete, comment blind/delete, or user sanction
- [x] Report follow-up actions delegate to existing admin domain workflows and log `REPORT_FOLLOW_UP` for report traceability
- [x] Report processing still applies no automatic hide/delete/sanction action
- [x] Frontend report create flow connected from place detail place/comment surfaces
- [x] Frontend my report read flow connected from My Page
- [x] Frontend admin report list/detail/process/follow-up flow connected

### Admin System
- [x] Admin API route protection added for `/api/admin/**`
- [x] Policy updates restricted to `SUPER_ADMIN`
- [x] Admin policy changes are logged in `admin_action_logs`
- [x] Disabled-by-default local admin bootstrap/test account flow added
- [x] Admin report list/detail/processing foundation implemented
- [x] Admin report management frontend flow connected at `/admin/reports`
- [x] Admin dashboard metrics API implemented at `GET /api/admin/dashboard`
- [x] Dashboard metrics read today new users, active recommendations, valid visits, pending reports, and new places
- [x] Admin dashboard frontend read flow connected at `/admin` and `/admin/dashboard`
- [x] Admin policy frontend management flow connected to general and region policy update APIs
- [x] Admin user list/detail read foundation implemented at `/api/admin/users`
- [x] Admin user sanction creation API implemented at `POST /api/admin/users/{userId}/sanctions`
- [x] Admin user management frontend flow connected at `/admin/users`
- [x] User sanction creation logs `USER_SANCTION` to `admin_action_logs`
- [x] User sanction creation increments `user_trust.sanction_count` when trust data exists
- [x] Active TEMPORARY_RESTRICTION and PERMANENT_RESTRICTION sanctions block core write actions
- [x] WARNING sanctions remain non-blocking trust/audit signals
- [x] Admin trust score/grade adjustment API implemented at `PATCH /api/admin/users/{userId}/trust`
- [x] Admin recommendation weight adjustment API implemented at `PATCH /api/admin/users/{userId}/recommend-weight`
- [x] Trust and recommendation weight adjustments are logged to `admin_action_logs`
- [x] Admin place list/detail read foundation implemented at `/api/admin/places`
- [x] Admin place read responses include approval, exposure, franchise review, creator, region, image, and aggregate stats data
- [x] Admin place exposure status control implemented at `PATCH /api/admin/places/{placeId}/exposure`
- [x] Admin place approval status control implemented at `PATCH /api/admin/places/{placeId}/approval`
- [x] Admin place franchise review status control implemented at `PATCH /api/admin/places/{placeId}/franchise-status`
- [x] Place moderation status changes are logged to `admin_action_logs`
- [x] Admin place score adjustment implemented at `PATCH /api/admin/places/{placeId}/score-adjustment`
- [x] Place score adjustment updates `place_stats.manual_adjustment_score` without changing automatic `score_total`
- [x] Place score adjustments are logged to `admin_action_logs`
- [x] Admin place management frontend flow connected at `/admin/places`
- [x] Admin ranking exclusion API implemented at `PATCH /api/admin/rankings/places/{placeId}/exclude`
- [x] Ranking exclusion is independent from place exposure and resets current star level to 0 when enabled
- [x] Ranking exclusion changes are logged to `admin_action_logs`
- [x] Admin ranking history finalization API implemented at `POST /api/admin/rankings/seasons/{seasonId}/finalize-history`
- [x] Ranking history finalization logs `RANKING_HISTORY_FINALIZE` to `admin_action_logs`
- [x] Admin recommendation log read API implemented at `GET /api/admin/recommendations`
- [x] Admin visit log read API implemented at `GET /api/admin/visits`
- [x] Recommendation and visit admin read APIs return latest 50 rows with user/place investigation context
- [x] Admin recommendation invalidation API implemented at `PATCH /api/admin/recommendations/{recommendationId}/invalidate`
- [x] Admin visit invalidation API implemented at `PATCH /api/admin/visits/{visitId}/invalidate`
- [x] Recommendation and visit invalidation update `place_stats` and log actual changes to `admin_action_logs`
- [x] Admin recommendation and visit moderation frontend flow connected at `/admin/activities`
- [x] Admin comment list API implemented at `GET /api/admin/comments`
- [x] Admin comment blind API implemented at `PATCH /api/admin/comments/{commentId}/blind`
- [x] Admin comment delete API implemented at `DELETE /api/admin/comments/{commentId}`
- [x] Comment blind/delete update `place_stats` when removing visible comments and log actual changes to `admin_action_logs`
- [x] Admin comment moderation frontend flow connected at `/admin/activities`
- [x] Admin action log read API implemented at `GET /api/admin/action-logs`
- [x] Admin action log read returns latest 50 audit rows with admin, target, before/after, memo, and timestamp
- [x] Admin report follow-up action API implemented for explicit hide/delete/sanction workflows from approved reports
- [x] Admin audit log frontend flow connected at `/admin/audit-logs`
- [x] Admin audit log page reads admin action logs and user action logs as read-only data
- [x] Admin audit log page filters loaded records by action type, target type, keyword, user/admin, and target ids
- [x] Existing admin screens expose a Korean `媛먯궗 濡쒓렇` navigation label for audit review

### Logging System
- [x] Application runtime logging baseline configured for diagnostics
- [x] SQL bind logging is disabled by default to avoid sensitive data exposure
- [x] Development phone verification sender no longer logs raw verification codes at INFO level
- [x] `user_action_logs` entity and repository implemented
- [x] User action log writer stores telemetry in a separate transaction after the domain transaction commits
- [x] User action log failures are swallowed so completed domain actions are not broken by telemetry storage
- [x] Admin user action log read API implemented at `GET /api/admin/user-action-logs`
- [x] Admin user action log read returns latest 50 user action rows with user, target, metadata, client context, and timestamp
- [x] Place creation logs `PLACE_CREATE`
- [x] Recommendation create/cancel logs `RECOMMENDATION_CREATE` and `RECOMMENDATION_CANCEL`
- [x] Visit verification logs `VISIT_VERIFY`
- [x] Comment create/update/delete logs `COMMENT_CREATE`, `COMMENT_UPDATE`, and `COMMENT_DELETE`
- [x] Report creation logs `REPORT_CREATE`
- [x] Focused user action log and representative domain service tests added

### Monitoring System
- [x] Spring Boot Actuator added for operational health checks
- [x] `/actuator/health` is exposed without authentication for deployment and load balancer probes
- [x] Actuator web exposure is limited to `health` by default
- [x] Health details are hidden by default while liveness/readiness probe support is enabled

### Deployment Preparation
- [x] `application-prod.yml` added as the explicit production profile baseline
- [x] Production profile requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` from the environment
- [x] Production profile defaults JPA schema handling to `validate` instead of schema auto-update
- [x] Production profile keeps region seed import, policy seed import, and local admin bootstrap disabled by default
- [x] Flyway is selected for schema migrations with migration files under `classpath:db/migration`
- [x] Local development keeps Flyway disabled by default while production enables it before Hibernate schema validation
- [x] Local development enables missing-only default policy seed import to support empty DB login and setup flows
- [x] `V1__baseline_schema.sql` captures the current backend core schema baseline
- [x] Redis connection properties are environment-driven through `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `REDIS_DATABASE`, and `REDIS_TIMEOUT`
- [x] Redis cache type, app Redis usage, Redis repositories, and Redis health checks remain disabled by default until a domain explicitly adopts Redis
- [x] PolicyService can use Redis for policy-value caching when `APP_REDIS_ENABLED=true`
- [x] Admin policy updates evict the changed policy cache key while DB policy rows remain the source of truth
- [x] Recommendation daily limit count checks can use Redis while recommendation rows remain the durable source of truth
- [x] Visit cooldown checks can use Redis while visit rows remain the durable source of truth
- [x] Phone verification code state can use Redis while verification rows remain the durable source of truth
- [x] Public place ranking reads can use Redis while `place_season_scores` remains the authoritative read model
- [x] Ranking scheduler settings are environment-driven and disabled by default through `RANKING_SCHEDULER_ENABLED=false`
- [x] MVP release candidate and deployment checklist added at `docs/mvp-release-candidate-checklist.md`
- [x] Production-profile startup rehearsal passed against disposable MySQL schema `honey_prod_rehearsal_20260504164117` with Flyway migrations V1/V2 successful, `JPA_DDL_AUTO=validate`, seed/bootstrap disabled, and `/actuator/health` returning `UP`
- [x] Staging-like seed/bootstrap rehearsal passed against disposable MySQL schema `honey_stage_rehearsal_20260504164730`; first boot imported region/policy/admin seed data, restart with seed/bootstrap disabled reached `/actuator/health`, and core auth, region, place, recommendation, visit, comment, and admin dashboard smoke APIs passed
- [x] Frontend browser smoke passed against the staging-like backend on isolated ports `18083` and `5174`; verified Korean home UI, login, My Page phone/region/status data, place detail participation surfaces, admin dashboard, and admin policy navigation with no browser console errors or warnings
- [x] One-command local staging smoke wrapper added at `scripts/run-local-staging-smoke.ps1`; it selects a staging rehearsal schema, checks occupied ports, starts backend/frontend with normalized paths, records backend/frontend/listener PIDs and log paths, and `-Stop` cleans up the recorded processes without disturbing the existing 8080 backend
- [x] MVP release runbook added at `docs/mvp-release-runbook.md`; it fixes the pre-release command order from tool normalization through backend tests, `bootJar`, frontend build/lint, document and encoding checks, staging smoke, browser smoke, stop cleanup, rollback, and rerun notes for port conflicts, missing schemas, and provider credentials
- [x] Release branch packaging prepared on `codex/prepare-mvp-release-packaging`; reviewed the dirty worktree, confirmed tracked changes are limited to release-runbook/progress/tasks docs, confirmed `.env`, local logs, build output, Gradle caches, frontend `dist`, and `node_modules` remain ignored, reran the release gate, and found no smoke credential or secret scan matches in tracked files
- [x] Release candidate documentation commit created and pushed to `origin/codex/prepare-mvp-release-packaging`; pushed branch is ready for PR creation at `https://github.com/q99man/Honey/pull/new/codex/prepare-mvp-release-packaging`
- [x] Release candidate PR draft added at `docs/mvp-release-pr-draft.md` because the local environment has no `gh` CLI or authenticated GitHub connector for direct PR creation
- [x] Release candidate PR creation page opened in the local browser and the prepared PR body was copied to the clipboard for manual submission through the user's authenticated GitHub session
- [x] Release candidate PR opened at `https://github.com/q99man/Honey/pull/1`; PR is open, not draft, currently mergeable, and the latest check-run poll found one `test` check successful and one `test` check still in progress
- [x] Release candidate PR #1 passed both `test` check runs and was squash-merged into `main` as `7759ca6 docs: prepare MVP release runbook`; local `main` was fast-forwarded to the merged remote state
- [x] Post-merge release readiness check passed on `main`: backend tests, backend `bootJar`, frontend build, frontend lint, diff check, UI language check, Korean encoding check, doc sync check, secret pattern scan, release runbook presence, and PR draft presence all passed
- [x] Merged release branch `codex/prepare-mvp-release-packaging` was deleted from `origin` and from the local repository after confirming the squash-merged content is on `main`

### Mission System
- [x] Database migration added under `db/migration/V5__add_mission_system.sql` with seed data
- [x] Refactored `UserGrowthService` to allow dynamic EXP reasoning (e.g. `MISSION_COMPLETE_...`)
- [x] Implemented core domain Entity, Repository, Service, DTOs, and REST API controller for Missions
- [x] Injected tracking hooks to trigger mission progress on user activities (Recommendation, Visit, Comment, Place registration)
- [x] Covered API security rules allowing public active missions retrieval while guarding reward claims
- [x] Wrote comprehensive unit tests and verified all tests pass successfully

### Fraud Detection System
- [x] Database migration added under `db/migration/V8__add_fraud_detection.sql` to support `fraud_alerts`
- [x] Defined `FraudAlertType` and implemented core domain Entity, Repository, and Response/User DTOs
- [x] Implemented `FraudDetectionService` with rules: RAPID_PARTICIPATION, GPS_TELEPORTATION, and IP_SPAM
- [x] Connected async `@Async` and TransactionalEventListener hooks from Place, Recommendation, Visit, and Comment domains
- [x] Applied automatic trust score penalty (-5 points) and status re-evaluation upon alert generation
- [x] Implemented admin controllers `/api/admin/fraud/alerts` and `/api/admin/fraud/suspicious-users`
- [x] Added unit and integration tests and verified all tests pass successfully

---

## 4. Key Decisions (Summary)

- System is mobile-first
- Backend is Spring Boot monolith
- Database is MySQL
- Redis is used for caching and cooldown
- Ranking is aggregation-based (not real-time)
- Policy values must NOT be hardcoded
- Admin must be able to control all key parameters

---

## 5. MVP Scope (Confirmed)

The MVP must include:

- user signup/login
- phone verification
- region verification (GPS-based)
- place registration (limited)
- recommendation system
- visit verification (GPS-based)
- comment system
- regional ranking (dong/district/city)
- report system
- admin dashboard
- admin report handling
- admin policy control

---

## 6. Development Priority

Next implementation order:

1. Authentication system
2. Phone verification
3. User system
4. Region system
5. Place system
6. Recommendation system
7. Visit system
8. Comment system
9. Ranking system
10. Policy system
11. Admin system

---

## 7. Current Next Step

Recent update:
- [x] Home hardcoded map placeholder was replaced with a Kakao Maps JavaScript SDK integration
- [x] Place list-shaped API responses now include `latitude` and `longitude` so Home can render map markers from real backend data
- [x] Frontend `Place` mapping now carries place coordinates through list, search, nearby, wishlist, ranking source data, and My Page registered-place reads
- [x] Kakao map markers navigate to `/places/{id}` through the existing detail route and existing card/wishlist/search/category flows were preserved
- [x] Home now reads `VITE_KAKAO_MAP_JAVASCRIPT_KEY` for the Kakao Maps JavaScript SDK while keeping `VITE_KAKAO_JAVASCRIPT_KEY` as a legacy local-development fallback
- [x] When the frontend Kakao JavaScript key is missing, Home shows a Korean map configuration state instead of a fixed fake map
- [x] Kakao map setup docs now record JavaScript SDK domain registration requirements and the need to restart `npm run dev` after `.env` changes
- [x] Local UI smoke restaurant sample SQL was regenerated with UTF-8 Korean place data, 10 map-ready sample places, representative images/fallback cases, and DEV ranking rows
- [x] Kakao Maps JavaScript SDK loading was moved to a shared frontend loader, Vite now reads root `.env` through `envDir`, and Detail shows a Kakao map for place coordinates
- [x] Home sample restaurant data regression was traced to root `.env` injecting `VITE_API_BASE_URL=http://localhost:5173`; Vite dev now proxies `/api` to the backend so existing list/search/category API flows keep working in local development
- [x] Real-key mobile QA covered Home and Detail at 360px, 393px, and 430px; Home card density and Detail report-button wrapping were minimally adjusted while API, routing, backend, and Home/App data state flows remained unchanged
- [x] Seeded mobile QA covered Ranking, Wishlist, and My Page at 360px, 393px, and 430px; no UI code changes were needed because list cards, ranking filters, authenticated/guest My Page states, Korean text, and bottom navigation spacing remained usable without horizontal overflow
- [x] Mobile user-flow smoke covered Home map/list load, search result and empty states, category filtering, Home/Ranking to Detail navigation, Detail wishlist toggle, Wishlist add/remove and empty state, Ranking navigation, and My Page guest/authenticated long-nickname states at 360px, 393px, and 430px; no code changes were needed because API/routing/state flows and mobile layout remained stable
- [x] MVP mobile smoke checklist fixed at `docs/mobile-smoke-checklist.md`; future UI changes should repeat smoke testing against this checklist and record results here
- [x] Home map UI was reorganized around map-first exploration: full food category tabs, emoji-based Kakao custom overlays, marker selection without immediate navigation, selected-place summary card with wishlist/detail/Kakao-map actions, and lightweight current-location/register floating actions while preserving existing APIs, routes, auth flow, and seeded place data
- [x] Home map UX was tightened to behave more like a real map app: default state keeps the map visible, marker/ranking selection opens a compact bottom detail sheet, the sheet handle closes on click or downward drag, floating map actions were compacted below the sheet layer, search/category controls were slimmed down, and the mobile bottom navigation now shows five icon-and-label items without API, routing, auth, backend, seed, or Kakao key-loader changes
- [x] Home map interaction blocker was fixed by letting the mobile overlay pass pointer events through to the Kakao map while keeping only search, category tabs, floating actions, and the bottom sheet interactive; Kakao native zoom control was added so map drag, zoom, and marker selection can work like a real map app without changing API, routing, auth, backend, seed data, or SDK key loading
- [x] Home Kakao map drag behavior was reinforced by explicitly enabling map draggable/zoomable state on the Kakao map instance and marking the map container as touch-handled so mobile browser scroll gestures do not steal map pan gestures
- [x] Home map chrome was restyled without changing Kakao base map tiles: Kakao default zoom control was removed, React Honeytong zoom/current/register controls now drive the Kakao map instance, and category CustomOverlay markers were shaped as cream/honey speech-drop markers with emoji centers and selected-marker scale/shadow/z-index emphasis
- [x] Home map drag blocker follow-up confirmed no `setDraggable(false)` usage, kept overlay wrappers pointer-events pass-through with only real controls/cards interactive, and replaced the map container `touch-none` with `touch-action: pan-x pan-y` so Kakao map touch drag is not blocked by app CSS
- [x] Home map drag follow-up simplified the actual Kakao map container to `pointer-events-auto` without `touch-action`, changed the wide-screen overlay grid to pass pointer events through empty map space, kept only real side panels/controls interactive, and added development-only DOM/Kakao drag logs for confirming which layer receives map drag starts
- [x] Home map overlay layout was tightened further by making the mobile and desktop UI chrome absolute `inset-0` pass-through layers above the Kakao map, matching the intended map-root plus overlay structure while preserving only real controls and cards as interactive targets
- [x] Home mobile map drag follow-up restored `touch-action: none` only on the actual Kakao map container after desktop-width drag worked but app-width touch drag did not, so mobile browser scroll gestures do not preempt Kakao map pan handling
- [x] Home mobile map visibility follow-up reverted the map-container `touch-action: none` change after it caused the Kakao map to disappear in app-sized view, restoring the last visible map-container structure before continuing drag debugging
- [x] Home map debug logging was expanded with map rect and overlay/control identifiers so app-width drag attempts can identify whether the pointer starts on the Kakao map, mobile header, floating actions, selected sheet, or bottom nav without changing production behavior
- [x] Home mobile-only map drag follow-up kept `touch-action` removed, converted the mobile overlay into separate absolute pass-through layers for header/actions/bottom sheet, stabilized the map parent height with `100dvh`, and added map container rect, map creation, SDK failure, relayout, draggable, zoomable, and Kakao drag logs so mobile target handling can be verified without changing API, routing, auth, backend, seeded data, or Kakao key loading
- [x] Home mobile drag logs showed pointerdown inside the Kakao map with no blocking overlay and `center_changed` firing; automatic `setBounds`, `setCenter`, and `relayout` calls are now gated to first data fit, selected-place changes, or current-location changes so user map dragging is not immediately corrected back by Home map synchronization
- [x] Home first-entry map initialization was hardened by sharing a single Kakao SDK load promise, resolving only after `kakao.maps.load`, waiting for a non-zero map container rect before creating the map, and keeping place overlay synchronization tied to later seeded/API place updates so first navigation no longer depends on a browser refresh
- [x] Home viewport switching stability was reinforced after DevTools device toolbar changes exposed stale Kakao map layout; the map container now observes resize, window resize, orientation changes, and visualViewport resize, then debounces `map.relayout()`, re-enables draggable/zoomable, and bumps a layout version so custom overlays resync after breakpoint/devicePixelRatio changes while overlay pointer-event separation remains unchanged
- [x] Home device toolbar follow-up confirmed `isInsideMap: true` and Kakao internal `svg` targets after viewport switching, so the remaining instability is treated as stale Kakao internal map handlers; viewport signatures now track window size, devicePixelRatio, breakpoint, and container size, and significant mode changes safely recreate the Kakao map instance while preserving center/level and triggering marker resync
- [x] Home device toolbar drag follow-up now forces a React-level Kakao map container remount with `mapMountKey` on significant viewport signature changes, clearing stale overlays and map refs before remount, preserving center/level for the new map, running requestAnimationFrame plus 160ms stabilization relayouts, re-enabling draggable/zoomable, and logging remount/init/marker-render steps for DevTools toggle verification
- [x] Home device toolbar zero-rect follow-up now treats `width: 0` or `height: 0` map containers as unstable and never initializes, remounts, relayouts, or renders markers in that state; reinitialization keeps the existing map alive until a stable non-zero rect is observed across frames, then safely cleans up overlays, remounts the container, initializes the new Kakao map, and stabilizes draggable/zoomable with requestAnimationFrame plus 160ms and 400ms retries
- [x] Home device toolbar mobile drag follow-up treats the remaining mobile-only no-drag state as a touch-action/pointer gesture issue after logs showed Kakao internal `svg` targets; the actual map container and Kakao-generated `div/svg/canvas` children now receive scoped gesture CSS (`touch-action: none`, disabled selection/drag, contained overscroll) after init, stabilization, and marker rendering, with expanded mouse/pointer/touch debug logs that do not call preventDefault or stopPropagation
- [x] Home device toolbar drag investigation narrowed further after touchmove events reached Kakao internal SVG but Kakao drag events still did not fire; CustomOverlay marker DOM now defaults to `pointer-events: none` with only the small marker button hit target set to `auto`, user-location overlay is non-interactive, a local `DEBUG_MAP_DISABLE_CUSTOM_OVERLAYS` switch can isolate map-only dragging, and a suspicious large `pointer-events: auto` overlay scanner logs oversized Kakao/overlay DOM after marker rendering
- [x] Home Kakao map cleanup classified the remaining Chrome DevTools Toggle device toolbar drag freeze as a development-emulation issue also reproducible on Kakao's official map site; mobile QA should enable device toolbar before page load and refresh before testing, while app code now keeps only service-relevant safeguards: non-zero container guards, ResizeObserver/window/visualViewport/orientation relayout, draggable/zoomable reassertion, scoped map-container gesture CSS, and small-hit-target CustomOverlay pointer-events
- [x] API spec and architecture decision notes were updated for API-backed Home map markers
- [x] Home desktop map UX was aligned with the latest reference prompt by adding a map-first desktop layer with a left global nav rail, left search/selected-place panel, top primary category bar with a more popover, right floating map controls, and lightweight map status chips; API, routing, auth, backend, seeded data, Kakao SDK loading, and mobile map flow were left unchanged
- [x] Home mobile app UI second refactor aligned with the provided reference: Korean app header, search action button, category carousel, larger right-side map controls, marker-driven half sheet, drag/click expanded detail sheet, photo strip, tabbed detail information rows, and bottom place action bar were added while keeping existing API, routing, policy validation, Kakao map loading, and place DTO contracts intact
- [x] Mobile bottom navigation was realigned to the five basic app destinations from the reference (`??, `??궧`, `???, `而ㅻ??덊떚`, `留덉씠`), with place registration remaining as a floating map action and a lightweight Korean community placeholder route added until the community domain is implemented
- [x] Verification for the mobile app UI second refactor passed frontend build, frontend lint, UI language check, and Korean encoding check; browser screenshot QA was not completed because the in-app browser tool was unavailable in this session and the local Vite server could not be kept reachable from the tool environment
- [x] Home mobile layout QA follow-up made minimal overlap fixes after static responsive review: right floating map actions now hide while the place sheet is open, the mobile sheet has a 44px-class close target, full-sheet max height leaves room for the top chrome and bottom navigation, and bottom navigation/sheet offsets account for mobile safe-area insets; API, routing, data contracts, and Kakao map stabilization logic were left unchanged
- [x] Home mobile browser QA was rerun with Chrome CDP mobile emulation at 360px, 393px, and 430px; the basic map screen kept `documentElement.scrollWidth` equal to the viewport width, the app header/search/category row/bottom navigation did not clip, and the right floating map controls stayed clear of the category row and bottom navigation. The local static QA server did not load real Kakao map markers, so marker-click half sheet and expanded sheet visual QA still need a real Kakao SDK/key browser pass.
- [x] Home web browser QA was rerun with Chrome CDP at 768px, 1024px, 1280px, 1440px, 1536px, and 1920px; desktop widths kept `documentElement.scrollWidth` equal to the viewport width, the left rail/panel/map/category/control structure stayed separated, the category more popover remained within the viewport, and mobile 360px/393px/430px rechecks still used the app mobile overlay. Minimal desktop fixes made the top category chips horizontally scrollable at 1024px, hid the map status chip while the more popover is open, and aligned the desktop rail labels with the web navigation scope.
- [x] Home web 2.5 detail refactor follow-up kept the desktop map fixed while converting the left rail to Home-local panel modes, added a compact left information-panel collapse/expand toggle, kept desktop map categories limited to existing Honeytong food categories, removed the lower weather/status chip, and verified 360px, 1024px, 1280px, and 1440px Chrome CDP widths without horizontal overflow.
- [x] Home web selected-place information panel detail pass added a desktop-only detail card with a fuller image area, title-side save button, compact place summary, and the confirmed tabs `??, `硫붾돱`, `?ъ쭊`, `?볤?`, `?좉퀬`; the mobile place sheet remains on the existing shared card path. Frontend lint/build passed, while an attempted Chrome CDP selected-panel screenshot timed out in the local tool session after the temporary server and browser were cleaned up.
- [x] Home web 2.5 follow-up continued without final verification by request: the hidden legacy desktop overlay was reduced to an inert placeholder, the map status chip over the desktop map was removed, the left information panel keeps collapse/expand behavior, and selected-place panel copy/encoding was normalized for Korean-first UI before the user review pass.
- [x] Home web 2.5 image follow-up applied the attached desktop QA details: the left information panel now occupies the full map height and collapses with a smooth translate/opacity transition, web map controls keep only current-location and zoom actions, selected-place panel image height/content fill were tightened, desktop category positioning follows the panel collapse state, and selected map markers use a clearer label-plus-pin active state. Build and diff whitespace checks passed; final browser QA remains pending for user review.
- [x] Home web 2.5 shape correction tightened the desktop reference match without changing panel content: the information panel is now flush against the left menu rail with no top/bottom gap, the category more button sits inside the horizontal category chip row, and active map markers render as a speech-label plus highlighted pin instead of an emoji-only marker.
- [x] Home web panel-mode flow was refined: category selection now opens a category-specific restaurant list in the left panel, list or marker selection switches the same panel to place detail, the panel close button clears selected category/place state, the lower explore category section was removed, the detail panel uses a single white surface instead of separated cards, and compact map markers now show no text until selection state is expressed by scale/color.
- [x] Home web category more popover was repositioned to open directly under the more button by keeping the button as a visible row-end control and anchoring the popover to that button wrapper; build, lint, and diff whitespace checks passed.
- [x] Home web category more button was tightened again so the category bar no longer stretches to the screen edge; the visible chip strip uses a content-sized row next to the panel and the more button remains attached immediately after the chips, with the popover anchored below the button.
- [x] Home mobile second refactor started with the notification control removed from the floating header, the expanded place sheet changed to own the bottom safe area, the mobile BottomNav hidden during full detail view, and the selected-place card reorganized so tabs, scrolling details, and action buttons stay inside the sheet; build and lint passed.
- [x] Home mobile selected-place detail was tightened again: voice search was removed, the register button now sits at the lower-right only while no place sheet is open, the mobile BottomNav hides for both half and full sheets, place markers were made more visually distinct from map POIs, and the half/full cards now share the same image, save button, web-aligned tabs, and information-area structure.
- [x] Home mobile half-sheet spacing was adjusted after the BottomNav hide change so the selected-place card sits near the bottom safe area instead of leaving the old navigation gap; half-sheet max height now scales with viewport height, and selected card tab state resets naturally when a different place is selected.
- [x] Home mobile detail QA adjustments made the half sheet flush to the left, right, and bottom edges while open, converted the selected-place sheet into a flex column so the full detail view scrolls inside the card content area, hid the full-detail scrollbar styling, and reconfirmed the top search row now contains only search icon, input, and search button.
- [x] Home mobile top search was tightened again by moving the Honeytong `H` badge into the former search-icon position and removing the visible Honeytong wordmark; the half-sheet image strip now fills the card width with no side padding while keeping the same tab and information structure.
- [x] Home mobile half-sheet was reshaped into a folded detail card: the image area takes roughly half of the half-sheet, the title/save/close controls sit in the middle, the menu tabs are pinned at the bottom, image taps expand to the full photo tab, tab taps expand to the matching full tab, and drag direction now transitions `half -> full`, `full -> half`, and `half -> closed`.
- [x] Home mobile final QA pass checked 360px, 393px, and 430px browser widths with seeded Kakao map markers: the compact search row, highlighted map markers, half-sheet layout, hidden BottomNav/register action while a sheet is open, image-to-photo-tab expansion, menu/comment tab expansion, full-sheet internal content, and Korean UI text all rendered without critical overlap. A small focus-outline polish, hidden-content accessibility cleanup, and mouse/touch drag handler fallback were applied during QA.
- [x] Home mobile final polish fixed the half-sheet title/action row so long place names cannot push the save/close buttons off the right edge, added consistent `px-2` image padding to half and full image strips, and changed mobile sheet close behavior to clear the selected place marker as well as closing the sheet.
- [x] Home UI second-refactor final visual cleanup removed glass/blur styling from the web information panel, web side rail, web category chips/popover, web map controls, mobile search row, mobile map controls, mobile place sheet, and map status card; these surfaces now use solid white backgrounds for a cleaner unified UI.
- [x] Home UI detail polish (2nd round): Kakao map markers updated to use a pure CSS teardrop pin shape (`rounded-[50%_50%_50%_0] -rotate-45`) with accurate downward shadows instead of emoji-only boxes, providing a sharper and more professional look.
- [x] Home UI detail polish (2nd round): Map background changed to `bg-[#fffaf0]` to align with the warm honey/cream design system instead of greenish `#eaf2e4`.
- [x] Home UI detail polish (2nd round): Unified mobile and desktop map controls (My Location, Zoom In, Zoom Out) into an identical UI, positioning them seamlessly below the top header/category region on both platforms.
- [x] Home UI detail polish (2nd round): Changed Kakao map zoom level transitions to use smooth animations (`animate: { duration: 300 }`) matching native app feel.
- [x] Home UI detail polish (2nd round): Expanded mobile selected-place sheet drag UX to the entire card area and added intelligent scroll-conflict prevention (`scrollTop > 0` bypass) so users can naturally swipe up/down anywhere to expand/collapse.
- [x] Sub-screen UI detail polish (2nd round): Unified Ranking, Wishlist, and MyPage UI to match the new Home honey/cream theme, adjusting card paddings, highlighting badges with `bg-[#f6b800] text-[#2b210f]`, and normalizing form field active states.
- [x] UI QA Polish (2nd round): Improved desktop panel toggle ?쒖씤??visibility) by increasing button size and adding shadows; adjusted mobile sheet vertical spacing and tab contrast; unified wishlist icons to `??/`?? across all screens; and reinforced map control borders for better contrast on bright map backgrounds.
- [x] Home selected-place comment tab now reads real place comments, shows loading/empty/error states, and provides a bottom input for immediate comment creation on both desktop and mobile detail panels while leaving authentication, phone verification, duplicate-comment, and sanction validation to the backend.
- [x] Home overlapping marker UX now groups nearby place markers by current Kakao map zoom level and opens a Korean place-selection list in the mobile bottom sheet or desktop left panel before showing a single place detail card.
- [x] Home overlapping marker detection radius was widened to better match visible marker/touch overlap, including close-zoom cases where adjacent pins visually collide.
- [x] Home overlapping marker detection was refined to use the Kakao projection screen distance and a marker-body-sized `40px` threshold, so the list opens only when marker bodies visually overlap instead of when nearby-but-separated pins are clicked.
- [x] Home category and marker visual regression was cleaned up by restoring Korean category metadata with dedicated food emoji values, rendering category chips with emoji badges, and restoring map markers to centered teardrop pins with rotated-back emoji contents.
- [x] Home mobile map controls were realigned to the upper-right area directly below the search/category header, matching the desktop control placement pattern; the selected-place sheet now supports card-surface drag gestures with scroll/input guards, and the comments tab again loads place comments and provides an inline comment input.
- [x] Home search UX now resets map selection on search, opens the result panel, shows a Korean no-result state with a full-list reset action, and keeps desktop/mobile search inputs in sync with the active keyword.
- [x] Place search API now matches visible places by name, recommended menu, recommendation text, feature text, road/jibun address, and Korean/English city/district/dong names instead of place name only.
- [x] Home mobile map browser visual QA was rerun with Chrome mobile device emulation at 360px, 393px, and 430px widths. Kakao map tiles loaded, the mobile search/category header, right-side map controls, registration FAB, and bottom navigation stayed within each viewport with no horizontal overflow (`scrollWidth` matched viewport width). App-owned place markers were not rendered in this run because the local `/api/places` response did not expose latitude/longitude values, so marker-click half sheet and expanded sheet visual QA remain blocked until the API-backed coordinate response is restored in the running target.
- [x] Home mobile map control follow-up unified the mobile current-location, zoom-in, and zoom-out buttons with the desktop map control icon set and labels. Chrome DevTools mobile emulation at 360px, 393px, and 430px confirmed no horizontal overflow, right-side controls stayed inside the viewport, each control rendered as an SVG icon button, and API-backed map markers rendered again in the QA target.
- [x] Home web/mobile UI follow-up applied the mobile bottom navigation SVG icons to the desktop side menu, restored full-surface mobile detail-sheet drag transitions for full, half, and closed states while preserving tap behavior, and added compact desktop detail/list scrollbars without visible arrow buttons.
- [x] Home desktop My menu now renders the shared login/signup auth card directly inside the web side panel when the user is not logged in, avoiding route changes from the map UI and reusing the Korean-first MyPage authentication flow.
- [x] Home desktop My panel now owns its authentication check instead of relying on a stale parent flag, so the My tab cannot fall back to the generic "留덉씠 ?뺣낫 以鍮?以? state; stale tokens are cleared on failed profile load, logged-out users see the inline auth card, and logged-in users see profile, certification, and activity summary cards inside the panel.
- [x] Home web/mobile search header polish replaced the desktop text search glyph and mobile Honeytong `H` badge with a shared magnifying-glass SVG treatment, and removed the desktop menu-panel region title/description block so region context can be reintroduced elsewhere later.
- [x] Home web follow-up moved region context back onto the desktop map as a compact overlay chip with current-location and clear-selection actions, and the desktop My panel now fetches the same profile, status, activity summary, my-region, and region-change policy APIs used by the mobile My Page after login.
- [x] Home seeded browser QA and My-panel parity follow-up ran against local backend `8080`, Vite `5173`, real Kakao SDK loading, and existing UI smoke seed data. Chrome CDP verified mobile 430px search, no-result, reset-to-all, marker click/detail sheet, desktop 1280px map markers, guest My panel auth card, and logged-in desktop My panel profile/status/region/activity data from real APIs with no horizontal overflow and no console errors.
- [x] QA setup note: the existing UI smoke seed data was usable and rendered 10 API-backed Kakao markers, but reapplying `scripts/dev-seed-honeytong-ui-smoke.sql` through the current PowerShell/mysql pipe hit a duplicate `place_stats` primary key. Treat seed idempotency cleanup as a follow-up before the next repeated seeded QA cycle.
- [x] UI smoke seed idempotency and search scale review made `scripts/dev-seed-honeytong-ui-smoke.sql` recover from repeated `place_stats` inserts, documented the Windows PowerShell `cmd.exe /c` mysql file-redirection workflow that preserves Korean text, and recorded that expanded MVP search should move to a Korean-validated search document/full-text strategy before production-scale data instead of relying on extra `%keyword%` predicates.
- [x] Community route follow-up confirmed the existing free-board backend/API/frontend CRUD slice and connected the desktop Home side navigation community entry directly to the real `/community` route instead of the old "以鍮?以? map-panel placeholder path.
- [x] Community CRUD browser QA passed against the rebuilt local backend and Vite frontend: authenticated phone-verified user login, post create, detail display, owner edit, owner delete, Korean UI text, empty state, and browser console error check all passed.
- [x] Updated brand favicon (favicon.png) to use the primary purple color (#6750a4) with a white "H" logo, matching the brand design system.
- [x] MyPage and admin shell UI follow-up aligned MyPage cards, inputs, badges, and action buttons with the existing Material 3 token classes; added a shared admin page shell for administrator headers and navigation; and applied it across dashboard, users, places, reports, policies, activities, and audit log pages while preserving existing API behavior and Korean UI copy.
- [x] Verification for the MyPage/admin shell UI follow-up passed frontend build, frontend lint, and route smoke checks for `/my`, `/admin`, `/admin/users`, `/admin/places`, `/admin/reports`, `/admin/policies`, `/admin/activities`, and `/admin/audit-logs`. Browser viewport QA was completed for `/my` and `/admin/users`; the remaining admin viewport screenshots are still pending because the in-app browser session stopped exposing an active pane.
- [x] Admin activities and audit logs inner UI polish added shared admin M3 component classes for panels, fields, segmented controls, list items, info cells, and action buttons; then applied them to `/admin/activities` and `/admin/audit-logs` while preserving the existing API behavior and Korean UI copy.
- [x] Verification for the admin activities/audit M3 polish passed frontend build, frontend lint, `git diff --check`, and route smoke checks for `/admin/activities` and `/admin/audit-logs`.
- [x] Admin users, places, reports, and policies inner UI polish applied the shared admin M3 component classes to list panels, detail panels, filters, editable fields, status chips, and action controls while keeping API calls and server-side validation behavior unchanged.
- [x] Verification for the admin users/places/reports/policies M3 polish passed frontend build, frontend lint, `git diff --check`, and route smoke checks for `/admin/users`, `/admin/places`, `/admin/reports`, and `/admin/policies`.
- [x] Admin viewport QA fallback pass checked the responsive grid and wrapping patterns statically after browser automation remained unavailable, adjusted `/admin/reports` status filters and `/admin/places` summary cells to use 2-column mobile layouts before expanding to 4 columns, and reconfirmed frontend build, frontend lint, `git diff --check`, and route smoke checks for `/admin`, `/admin/users`, `/admin/places`, `/admin/reports`, `/admin/policies`, `/admin/activities`, `/admin/audit-logs`, and `/my`.
- [x] Admin QA follow-up extended static responsive checks to admin activity tabs and status filters, changed those controls from mobile 3-column grids to stacked mobile controls that expand to 3 columns on larger screens, and verified frontend build, frontend lint, UI language check, Korean encoding check, `git diff --check`, and route smoke checks for all admin routes plus `/my`.
- [x] Mobile restaurant detail sheet follow-up improved drag behavior so the sheet tracks finger movement and snaps between full, half, and closed states; downward drag from half closes the detail selection, downward drag from full returns to half, and upward drag opens full view. The close control and save controls now use custom SVG/M3-styled icon buttons instead of plain text glyphs.
- [x] Mobile restaurant detail sheet gesture fix moved touch drag interception from React passive touch handlers to native non-passive touchmove handling on the sheet root, removing the passive `preventDefault` console warning and allowing downward drag from full view to snap back to half while preserving upward content scrolling in full view.
- [x] Card action polish moved the shared place card, mobile/desktop selected-place cards, and standalone place detail save controls onto reusable SVG M3 icon buttons; the mobile detail close button now uses an opaque M3 surface, stronger shadow, and contrast ring so it remains visible over dark restaurant images.
- [x] Mobile detail card user QA confirmed close-button contrast and drag gestures are working well on the user's device; the remaining Home place-list/detail rating glyphs were replaced with reusable SVG M3 rating stars to remove default text-symbol UI from visible cards.
- [x] Policy hardcoded-value audit started with the comment domain: removed the hardcoded `@Size(max = 300)` DTO rule, added `system_policies.comment.max_length` to the default policy seed, and moved comment create/update length enforcement into `CommentService` with focused tests and full backend build verification.
- [x] Policy hardcoded-value audit continued with community posts: removed hardcoded DTO title/content size rules, added `system_policies.community.post_title_max_length` and `system_policies.community.post_content_max_length` to the default policy seed, and moved create/update length enforcement into `CommunityPostService` with focused tests and full backend build verification.
- [x] Policy hardcoded-value audit continued with place content fields: removed DTO size rules for recommended menu, short recommendation, and feature text; added `system_policies.place.recommended_menu_max_length`, `system_policies.place.short_recommendation_max_length`, and `system_policies.place.feature_text_max_length`; and moved create/update length enforcement into `PlaceService` with focused tests and full backend build verification.
- [x] Policy hardcoded-value audit continued with report text fields: removed DTO size rules for user report reason text and admin review notes, added `system_policies.report.reason_text_max_length` and `system_policies.report.review_note_max_length`, and moved create/process length enforcement into `ReportService` and `AdminReportService` with focused tests.
- [x] Policy hardcoded-value audit continued with report follow-up text fields: added `system_policies.report.follow_up_reason_max_length` and `system_policies.report.follow_up_memo_max_length`, and moved follow-up reason/action-memo length enforcement into `AdminReportService` before downstream admin workflows run.
- [x] Policy hardcoded-value audit continued with admin operation text fields: added `system_policies.admin.sanction_reason_max_length` and `system_policies.admin.action_memo_max_length`, restored UTF-8 Korean descriptions in the default policy seed file, removed selected admin/ranking DTO memo/reason size rules, and moved user sanction reason plus admin user/place/ranking/season memo validation into service-layer policy checks with focused tests.
- [x] Policy hardcoded-value audit continued with image URL fields: added `system_policies.place.image_url_max_length` and `system_policies.visit.image_url_max_length`, removed place/visit image URL DTO size rules, moved validation into `PlaceService` and `VisitService`, and ensured place image replacement validates new URLs before deleting existing image rows.
- [x] Policy hardcoded-value audit continued with place address fields: added `system_policies.place.address_max_length`, removed place create/update DTO address size rules, and moved road/jibun address length validation into `PlaceService` before persistence.
- [x] Flutter mobile real-time ranking screen implementation: Completed Tab 1 integration, including season/region API data fetching, Dong/District/City tab toggling, custom region selector BottomSheet dropdowns, gold/silver/bronze rank badge designs, demographic audience tags chips, and navigation to PlaceDetailScreen.
- [x] Flutter mobile saved places (wishlist) screen implementation: Completed Tab 2 integration, including fetching saved places meta lists, resolving full place details concurrently via asynchronous parallel (`Future.wait`) calls, supporting interactive Dismissible swipe-to-unsave gestures, providing direct bookmark unsave buttons, custom pure Flutter pulse skeleton loading animations, guest authentication check UI redirection, and navigating to PlaceDetailScreen on item clicks.
- [x] Flutter mobile community screen (Tab 3) implementation: Connected to community backend API to fetch and display board post list (?꾩껜 ?섎떎 / ?닿? ??湲), support pull-to-refresh, show pure Flutter custom pulsing skeleton loaders, handle guest redirection to login, verify phone verification status (`userProfile?.phoneVerified`) before post creation, redirect unverified users to MyPage verification view, and support post creation, detailed view, editing, and deletion. Fix Flutter analyze compilation errors related to `userStatus` properties and `MainAxisAlignment` values.
- [x] Backend auth policy refactoring: Replaced hardcoded properties for phone verification (code length, duration, max attempts) and JWT token validity (access/refresh duration) with dynamic `PolicyService` values, updated `policy-defaults.csv` seed, verified with tests, and validated successful DB insertion on local application startup.
- [x] Flutter mobile integrated smoke verification: Verified all 5 core tabs (Home, Ranking, Save, Community, MyPage) on Android Emulator under real backend (8080) and database seed state, resolved Google location accuracy popups, took screenshots of all views (`screen.png`, `ranking.png`, `save.png`, `community.png`, `mypage.png`), and validated Korean localization.
- [x] Flutter mobile release configuration & packaging: Configured internet permission and Korean app name ('?덈땲??) in `AndroidManifest.xml`, updated package namespace/applicationId to `com.honeytong.app`, added keystore `key.properties` dynamic loading to `build.gradle.kts`, migrated `MainActivity.kt` to the new package location, and created a build automation script (`build-mobile-release.ps1`) and deployment guide (`mobile-release-guide.md`).
- [x] Flutter mobile release build verification: Executed `scripts/build-mobile-release.ps1` to successfully generate final release APK (`app-release.apk`) and App Bundle (`app-release.aab`) under debug/release keys, verifying output paths.
- [x] Place Audience Tag System Backend & Frontend: Fully implemented place audience tag system. Demographics (age, gender, nationality) are aggregated from recommendations and visits to generate tags, which are exposed via API and shown as demographic chips on the Flutter mobile ranking screen.
- [x] Mission System Architecture Design: Designed the database schema for `missions` and `user_mission_progress` tables, established reward claiming and repeatable reset policies, drafted seed data for default missions (`VISIT_3`, `RECOMMEND_5`, `COMMENT_3`), and compiled the detailed implementaton plan (`docs/mission-implementation-plan.md`).
- [x] Mission System Implementation: Implemented Flyway migration (V5), refactored `UserGrowthService` for dynamic EXP reward logs, implemented the mission domain layer (entities, repositories, services), and integrated mission tracking hooks. Verified all 265 backend tests pass successfully.
- [x] Redis Caching for Hot Endpoints: Implemented Redis caching layers for active missions list (`GET /api/missions`) and current season data (`GET /api/rankings/seasons/current`). Added Redis/NoOp implementation bounds, dynamic TTL properties configurations, ObjectMapper serialization with JavaTimeModule, exception-safe DB fallback, and admin eviction hooks for current season updates. All unit and integration tests compile and pass.
- [x] Optimize place_stats updates: Decoupled demographics recalculation from request-response transactions using Spring Application Events and asynchronous listeners. Configured a task executor thread pool, set up TransactionPhase.AFTER_COMMIT listener, and kept PlaceAudienceStatsService backwards compatible for unit tests. All backend unit and integration tests compile and pass.
- [x] Heavy query index optimization: Created Flyway migration (V6) containing indexes on user_action_logs (created_at DESC), admin_action_logs (created_at DESC), users (created_at DESC), user_sanctions (user_id, status, sanction_type, start_at, end_at), and visits (place_id, is_valid, user_id). All backend tests compile, validate, and pass successfully.
- [x] Core Services Edge-Case Unit Testing: Added and expanded unit tests for key domain services (PlaceService, RecommendationService, VisitService, CommentService, UserService, AuthService) covering validation failures, invalid state transitions, and edge cases. Resolved mock stubbing issues and strict stubbing mismatches. Verified all backend unit tests pass successfully.

- [x] Notification System Implementation: Created the Flyway DB schema migration (V7) for the notifications table. Implemented the core entity, repository, response DTOs, service, and controller for API endpoints (`GET /api/notifications` and `PATCH /api/notifications/{notificationId}/read`). Defined `CommentCreatedEvent`, `MissionCompletedEvent`, and `ReportProcessedEvent` and implemented the `NotificationEventListener` to handle localized notification generation. Integrated event publishing in `CommentService`, `MissionService`, and `AdminReportService`. Added unit and listener tests, verifying all backend tests compile, validate, and pass successfully.

- [x] Advanced Analytics System Implementation: Implemented custom DTO records, `AnalyticsRepository` with optimized Native SQL queries, and service layers (`AnalyticsService` and `AdminAnalyticsService`) to track user preferences, hyperlocal regional trends, and global activity counts. Created `AnalyticsCache` supporting Redis and NoOp configurations. Exposed endpoints: `GET /api/analytics/me` (authenticated), `GET /api/analytics/regions/{dongId}/trends` (public), and `GET /api/admin/analytics/activity-trends` (admin roles). Added unit tests, verifying all backend tests compile, validate, and pass successfully.

- [x] Recommendation & Ranking Scoring Optimization: Refactored the ranking calculation engine to dynamically compute diversity and recency bonus scores. Implemented Shannon Entropy to evaluate user demographic (age decade and gender) distribution as a normalized diversity factor (0.0 to 1.0) scaled by policy weight. Added timeframe-based counts for recommendations, visits, and comments to calculate recency bonuses. Ensured database persistence in PlaceStats, loaded all parameters from Dynamic System Policies, updated the database seed, and verified with robust Mockito lenient testing.

- [x] Place Search Optimization: Refactored the search architecture to utilize a MySQL FULLTEXT index on place_search_documents(search_text) with the CJK ngram parser. Implemented a hybrid search execution strategy in PlaceSearchDocumentService that delegates 2+ character keywords to native MATCH AGAINST Boolean Mode queries (preprocessed with + and * operators) and falls back to wildcard LIKE matching for single-character queries. Expanded unit testing and verified all backend tests compile and pass.

- [x] Spatial Index & Spatial Query Optimization: Optimized nearby places API (GET /api/places/nearby) by introducing a location POINT Generated Column on places table and a MySQL SPATIAL INDEX. Refactored coordinate range queries to use native SQL functions ST_Distance_Sphere and ST_PointFromText at the database layer. Decoupled Hibernate entities from spatial types, preserving H2 testing compatibility. Updated and verified unit tests.
- [x] Multi-Language UI (i18n): Implemented a custom lightweight localization infrastructure using React Context/Hooks for the React web frontend and a ChangeNotifier singleton for the Flutter mobile app. Created ko/en/ja localization map resources, integrated LanguageSwitcher select/dropdowns, and successfully translated core interfaces. Completed Vite build compiling, backend Gradle tests, and Flutter static analysis (`flutter analyze`), and corrected Japanese resource typos.

- [x] AI-Based Place Tagging System: Designed and implemented an asynchronous AI tagging system for places. Added flyway database migration (V11) for `place_ai_tags`. Created modular service abstractions for a default rule-based `MockAiTaggingService` and a conditional `GeminiAiTaggingService`. Integrated Spring Boot asynchronous event listeners (`@Async` and `@EventListener`) to update tags on place registration and comment additions. Integrated tags presentation (custom M3 chips) inside the React ?대뱶誘?Places dashboard and Flutter mobile's Place Detail Screen. Verified 100% success across all backend unit tests, frontend build packaging, and Flutter static analysis.

- [x] Region Change Concurrency Guard & UI/UX Detail Polishing: Implemented a Redis-based distributed lock (`SETNX` with a 10s TTL) inside `RegionService.changeMyRegion` integrated with transaction synchronization callbacks to prevent race conditions during regional change requests. Fixed potential layout and vertical/horizontal text overflow breaks under multilingual rendering (e.g. English/Japanese long titles) by wrapping critical title and name components with `Flexible`/`Expanded` and enabling ellipsis boundaries in `place_detail_screen.dart` and `ranking_screen.dart`. Verified builds across backend tests and `flutter analyze` successfully.

- [x] Mobile Deployment Flavors & Dynamic Endpoint: Configured native product flavors (dev and prod) in Android Kotlin DSL Gradle script (`build.gradle.kts`) alongside enabling `buildFeatures.resValues` for Gradle 8.0+ compatibility. Mapped dynamic string resources (`app_name`) for localized app names ("?덈땲??(媛쒕컻)" / "?덈땲??) and package ID suffixes. Refactored Flutter API client endpoint mapping (`api_endpoints.dart`) to check `appFlavor` dynamically without extra packages. Added a one-click build automation script (`scripts/build-mobile-release.ps1`) and updated release manuals (`docs/mobile-release-guide.md`). Run and verified `flutter analyze` and clean flavor APK/AAB compilations successfully.

- [x] Web Admin UI 鍮뚮뱶 寃고븿 ?섏젙 & ?ъ슜??蹂듦뎄 ?곕룞 ?꾨즺: ?ㅺ뎅??ko/en/ja) ?⑥튂 以?`AdminShell.tsx`?먯꽌 蹂寃쎈맂 `AdminNavItem` ??낆쓽 `labelKey`? `AdminDashboardPage.tsx`?먯꽌 援ы삎?쇰줈 ?ъ슜?섍퀬 ?덈뜕 `label` ?꾨뱶??遺덉씪移섎? ?닿껐?섏뿬 `tsc -b && vite build` ?뺤쟻 泥댄겕 鍮뚮뱶瑜??뺤긽?곸쑝濡??듦낵?쒗궡. ?댁쟾 'web to mobile' 而ㅻ컠 ?쒖젏????젣?섏뿀???쇰컲 ?ъ슜???섏씠吏援?`HomePage.tsx`, `DetailPage.tsx` ????蹂듦뎄 蹂듭썝 ??異⑸룎 ?놁씠 而댄뙆???⑥쓣 寃利??꾨즺.

- [x] Codex stabilization pass after Flutter refactor review: Added backend regression coverage for GPS region verification respecting region-change cooldown and for region changes continuing safely when Redis lock access is unavailable. Updated `RegionService` so GPS verification cannot bypass cooldown when moving to a different dong, and Redis lock failures fall back to DB policy enforcement instead of failing the user flow. Verified full backend tests pass.

- [x] Policy hardcoded-value audit continued with fraud detection and audience tags: Added default `fraud.*` and `audience.*` policies, moved rapid participation, IP spam, GPS teleportation, fraud trust penalty, and audience tag thresholds from code constants into `PolicyService`, and verified with focused service tests plus full backend regression.

- [x] Flutter mobile place contract stabilization: Added `Place.fromJson` coverage for backend place list and detail DTO shapes, mapped list fields (`regionName`, `address`, `starLevel`, `representativeImageUrl`, top-level aggregate counts) correctly, removed Home map/search mock-place fallback so empty API responses render as empty states instead of fake data, and moved API/Kakao key configuration to dart-define-backed `AppConfig`. Flutter CLI and Dart CLI were unavailable in the local session, so mobile tests could not be executed.

- [x] React frontend localization lint stabilization: Split `LocaleContext` runtime context from the provider component and removed translation `setState` inside an effect. Verified `npm run lint` and `npm run build` pass.

- [x] Kakao map integration diagnosis: Confirmed Flutter Home currently uses a simulated `CustomPaint` map and only has the Kakao login SDK, not a Kakao map SDK. Actual native map integration requires adding a Flutter Kakao map package, Kakao native app key/platform registration/key hash, and Android Kakao Maven repository setup before replacing the simulated map widget.

- [x] Flutter Kakao map native integration foundation: Added `kakao_maps_flutter`, configured the Kakao Maps Maven repository for Android, initialized Kakao Maps and Kakao Login from `KAKAO_NATIVE_APP_KEY`, replaced the simulated Home map with a `KakaoPlaceMap` wrapper, renders current-position and place markers from API coordinates, and keeps a Korean configuration state when the native app key is missing. Updated `.env.example`, mobile release guide, and build script so Spring/Web Kakao keys and Flutter native map keys can be managed from the same root `.env`. Verified `flutter pub get`, focused mobile tests, `flutter analyze`, and Android dev debug APK build.

- [x] Mobile Kakao runtime verification prep: Added `scripts/check-mobile-kakao.ps1` and Android SDK/ADB discovery in `scripts/dev-env.ps1`. The check confirms current `.env` has Spring/Web Kakao keys, shows `KAKAO_NATIVE_APP_KEY` is still needed for Flutter native maps, prints debug key hashes for Kakao Console registration, and reports ADB device availability without exposing secret key values.

- [x] Mobile Kakao smoke runner: Added `scripts/run-mobile-kakao-smoke.ps1` and `docs/mobile-kakao-smoke.md` so the dev Flutter app can be built with `KAKAO_NATIVE_APP_KEY`, installed on a selected ADB device, launched, and checked with filtered Kakao/Honeytong/Flutter logs. The script has a no-side-effect `-CheckOnly` mode, can start an Android Studio AVD with `-StartEmulator`, and does not print secret key values.

- [x] Mobile Kakao ABI diagnosis: Ran the dev APK on the local `Pixel_10_Pro` AVD and confirmed the app launches far enough to initialize Flutter, then crashes because Kakao Maps native `libK3fAndroid.so` is packaged for ARM (`arm64-v8a`/`armeabi-v7a`) while the current emulator is `x86_64`. Updated the smoke script to fail before build/install on x86/x86_64 devices and documented that real Kakao native map verification needs an ARM64 Android device or compatible ARM emulator image.

- [x] Mobile Kakao ARM emulator attempt: Installed `system-images;android-35;google_apis;arm64-v8a`, created `Pixel_Arm64_API35`, and attempted to boot it. The Android emulator exits immediately on this x86_64 Windows host with `Avd's CPU Architecture 'arm64' is not supported by the QEMU2 emulator on x86_64 host`, confirming local ARM64 emulator validation is not available on this machine.

- [x] Mobile Kakao native map live device smoke: Connected a Galaxy S24+ (`SM-S926N`, `arm64-v8a`) through ADB, built and installed the dev APK, launched `com.honeytong.app.dev`, granted location permission, and confirmed Kakao native map tile rendering on the device with Korean POI labels and Kakao watermark visible. This verifies the native app key, Android package/signing registration, ARM native library loading, and map view creation path. Backend-driven place markers/detail navigation still need a follow-up run with the backend listening on a LAN-reachable URL because physical Android devices cannot use the emulator-only `10.0.2.2` host alias.

- [x] Skill usage audit: Reviewed local `/skills` overlap with the Superpowers plugin and documented the recommended operating model in `docs/skill-usage-audit.md`: use Superpowers for workflow, keep `agent-workflow` as the Honeytong-specific supplemental guide, and treat the smaller domain skills as reference-only unless explicitly needed.

- [x] Mobile backend discovery API stabilization: Diagnosed the LAN mobile smoke empty-place state to local MySQL schema drift and MySQL 8 SRID axis-order behavior. Updated the spatial migration/query to force `axis-order=long-lat`, require the generated `places.location` column to be `NOT NULL` for SPATIAL indexing, added security regression coverage for public map discovery reads, and repaired the local search/spatial indexes without deleting data. The running 8080 backend still needs a restart to load the corrected nearby-place query before rerunning the phone smoke.

- [x] Flutter Kakao map recenter stabilization: Fixed the Home map current-location button so repeated taps recenter the Kakao map even when the resolved GPS coordinates are unchanged. Confirmed the physical device's current coordinates return an empty nearby-place result from the backend even at a 10km radius, while the public place list still has Hongdae/Seogyo test data; this means the current home-location empty marker state is data coverage, not a map rendering failure.

- [x] Physical-device backend connectivity diagnosis: Confirmed the Galaxy S24+ cannot reach the PC backend through `192.168.50.5:8080` from the device network, which explains failed signup/login and empty mobile API flows. Added `-UseAdbReverse` to the mobile Kakao smoke script so USB-connected physical devices can call `http://127.0.0.1:8080` on-device and have ADB forward it to the PC backend.

- [x] Flutter Kakao map controller readiness stabilization: Diagnosed a physical-device crash where `kakao_maps_flutter` exposed `onMapCreated` before the native `onMapReady` method-channel state accepted marker and camera commands. Updated the Home Kakao map wrapper to schedule marker/camera synchronization and retry briefly until the native controller is ready, preventing the early `moveCamera` assertion while preserving current-location recenter behavior.

- [x] Flutter Kakao map marker layer stabilization: Matched the `kakao_maps_flutter` example/runtime contract by creating the default `LabelLayer` with `addMarkerLayer` before adding or removing Home map markers. This addresses the physical-device `LabelManager or its layer is null` marker failure observed after the controller readiness fix.

- [x] Flutter mobile auth contract stabilization: Fixed mobile local signup/login to send the backend DTO field `email` instead of the obsolete `username`, aligned signup password validation with the backend 8-character minimum, restored Korean text in the login/signup and place registration flows, and kept backend auth validation messages Korean/UTF-8. Verified focused Dart analysis and `AuthServiceTest`.

- [x] Flutter mobile login profile parsing stabilization: Fixed the mobile `UserProfile` model to match the current backend `/api/users/me` response, which no longer includes the obsolete `username` field. The login flow now treats a successful token response plus profile fetch as authenticated instead of failing during profile parsing. Added user model coverage for profile and activity summary response field names, and verified signup-login-profile API flow locally.

- [x] Flutter My Page verification stabilization: Fixed the mobile phone verification response parsing to accept the backend `phoneVerified` field, removed the misleading fake `123456` code hint, restored Korean text in the My Page verification UI, and connected GPS-based region verification from My Page to `/api/regions/verify`. Verified backend region verification with the current Kakao REST configuration, ran focused Flutter analysis/tests, and rebuilt/installed the dev APK on the connected Android device with `adb reverse`.

- [x] Place registration failure fix: Reproduced the backend `POST /api/places` failure and traced it to `PlaceSearchDocumentService.syncPlace`, where a new `place_search_documents` row with an assigned `@MapsId` id was being saved through repository merge and raising `ObjectOptimisticLockingFailureException`. New search documents now use `EntityManager.persist`, existing documents still use repository save, and the focused backend regression test passes. Also restored Korean text in the Flutter place registration/service UI and clarified that road and jibun addresses do not both need full input; one address is enough.

- [x] Nearby refresh compatibility fix: Confirmed a newly registered place is saved as `APPROVED`/`VISIBLE`, but the installed pre-fix Android APK still calls nearby discovery with `radius=1000.0`, which Spring rejected because the controller expected an integer. Updated the backend nearby endpoint to accept decimal radius values and round them for the service layer, and changed the place-create success message to Korean.

- [x] Home map marker/card UX fix: Updated the Flutter home map so the bottom place card no longer opens automatically for every nearby result. The card now appears only after a Kakao map marker is selected, can be dismissed, and the floating action buttons move only when that card is visible. Restored Korean home-map UI text, changed category filtering to use backend category codes, and registered explicit Kakao marker styles with rank/text on a dedicated clickable marker layer so place and current-location markers are more visible. Cleared stale Flutter SDK lock files that were blocking local commands, then verified `flutter analyze`, rebuilt the dev APK, installed it on `R3CX306ZWGW`, captured the map screen showing visible current/place markers without the old bottom card covering the map, and confirmed on-device marker tap opens the selected-place card.

- [x] Place registration address coordinate fix: Reproduced the root cause that Flutter registration filled latitude/longitude from current GPS and the backend persisted those client coordinates without using the typed address. Added backend address coordinate resolution through Kakao Local address search on place create, then resolves the administrative dong from the resolved coordinate and uses that dong for registration-scope validation and persistence. Added regression coverage for address-resolved coordinates and address-resolved region usage. Verified `PlaceServiceTest` plus recommendation, visit, and comment service QA tests.

- [x] Place update address coordinate parity fix: Extended the same server-side Kakao Local address resolution rule to `PATCH /api/places/{placeId}` so edited road/jibun addresses update the stored marker coordinate and administrative dong from the address, then validate the resolved dong against the owner's registration scope. Added focused regression coverage for address-derived update coordinates and out-of-scope address rejection.

- [x] Flutter mobile owner place management wiring: Added a My Page entry for `내 등록 맛집 관리`, a mobile owner list for places returned by `/api/users/me/places`, and edit/delete actions backed by `PATCH /api/places/{placeId}` and `DELETE /api/places/{placeId}`. The edit action reuses the registration form in edit mode and loads the current backend detail before submission. Static diff checks pass; `flutter analyze` could not complete in that session because the Flutter SDK lock cleanup required an escalated operation that was rejected by the session usage limit.

- [x] Flutter dev APK verification unblock: Updated `scripts/dev-env.ps1` so local PowerShell sessions consistently resolve Windows system tools, Git, Flutter, and Dart, and inject `C:/flutter` as a process-scoped Git safe directory for the Codex sandbox user. Cleared stale Flutter SDK lock files after confirming no active Dart process should own them, restored the Android dev/prod app-name resource strings to valid Korean UTF-8, and verified `flutter analyze`, `flutter test`, and `flutter build apk --debug --flavor dev --dart-define=HONEY_DEV_API_BASE_URL=http://127.0.0.1:8080` all pass. The dev APK was produced at `mobile/build/app/outputs/flutter-apk/app-dev-debug.apk`. ADB reported no connected devices, so install and physical-device My Page owner-place edit/delete verification remain pending.
- [x] Local region verification unblock: Diagnosed the emulator/My Page neighborhood verification failure as missing local region seed data rather than a Kakao key, REST API, backend, or emulator ABI issue. Ran the existing region seed importer against the local MySQL database, expanding local region data to 18 cities, 256 districts, and 3630 dongs. Reverified `/api/regions/verify` with the current Android Studio mock coordinate `37.491013,126.720600`, which now succeeds as `인천광역시 부평구 부평1동`.
- [x] Flutter place registration eligibility fix: Reproduced the mobile place registration failure and confirmed the entered Bupyeong address succeeds through `POST /api/places` once the user is phone-verified, so the blocker was phone verification rather than address resolution. Added explicit Flutter registration eligibility logic for phone and region verification, updated the place registration screen to show Korean pre-submit guidance instead of a generic failure, and installed a rebuilt dev APK on the x86 emulator. Verified `flutter analyze`, full `flutter test`, and dev debug APK build.
- [x] Flutter place detail address display cleanup: Added a detail address formatter so the place detail screen hides the jibun address line when `addressJibun` is null, empty, or the literal `null`, preventing `[지번] null` from being shown. Also normalized the touched detail-screen Korean UI strings. Verified `dart format`, `flutter analyze`, full `flutter test`, dev debug APK build, emulator install, `adb reverse`, and app launch.

Next task:

Run a quick emulator UI pass for place detail after registering a road-address-only place, confirming the address section shows the road address without a `[지번] null` line.

- recommended reasoning level: medium

---

## 8. Known Risks

- GPS spoofing (visit validation risk)
- duplicate account creation (phone verification required)
- ranking manipulation attempts
- policy misconfiguration
- early-stage data imbalance

Mitigation strategy:
- strong validation
- admin monitoring tools
- policy control

---

## 9. Notes for Codex

- Follow agent.md strictly
- Never hardcode policy values
- Always validate duplication and cooldown
- Keep controllers thin
- Implement logic in services
- Use DB schema as defined
- Do not change API structure without reason

---

## 10. Update Rules

This file MUST be updated when:

- a feature is completed
- a major decision changes
- a new system is added
- priorities shift

---

## 11. Example Update Format

When updating progress:

### Example:

Authentication:
- [x] signup API implemented
- [x] login API implemented
- [ ] OAuth not implemented yet

Region System:
- [x] region tables created
- [x] region lookup API implemented
- [ ] GPS verification pending

---

## 12. Final Note

This document is critical for maintaining development flow.

Always keep it updated.

It ensures:
- consistent progress
- reduced confusion
- better Codex performance
