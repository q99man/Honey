# 📈 Development Progress

This document tracks the current development state of Honeytong.

It is used to:
- maintain development continuity
- inform Codex of current progress
- prevent duplicate or conflicting implementations
- guide next steps

---

## 1. Current Phase

Phase: BACKEND MVP DEVELOPMENT
Status: Core auth, user, region, policy, admin policy, and place foundation in progress

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
- [x] Frontend lint/build checks pass
- [x] CI updated for current frontend-first state and future backend Gradle project
- [x] Agent harness now requires next recommended task and matching reasoning level after work
- [x] Agent workflow skill added to adapt local andrej-skills guidance under Honeytong rules
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

### Place System
- [x] `places`, `place_images`, and `place_stats` entities/repositories implemented
- [x] Place creation API implemented at `POST /api/places`
- [x] Place creation requires phone verification via `@RequirePhoneVerified`
- [x] Place creation validates primary region and registration scope policy
- [x] Place registration limit reads `system_policies.place.registration_limit`
- [x] Place registration scope reads `system_policies.region.registration_scope`
- [x] Place detail API implemented at `GET /api/places/{placeId}`
- [x] Region-filtered place list API implemented at `GET /api/places`
- [x] Nearby place list API implemented at `GET /api/places/nearby`
- [x] Place search API implemented at `GET /api/places/search`
- [x] Place registration policy API implemented at `GET /api/places/registration-policy`
- [x] Frontend mock place list removed and connected to real `/api/places`
- [x] Frontend place registration flow connected from Home to `POST /api/places`
- [x] My registered places API implemented at `GET /api/users/me/places`
- [x] Frontend My Page registered places read flow connected
- [x] Frontend owner edit/delete flow connected from My Page to existing place update/delete APIs
- [x] Place update API implemented at `PATCH /api/places/{placeId}`
- [x] Place logical delete API implemented at `DELETE /api/places/{placeId}`
- [x] Place update/delete require owner or admin permission
- [x] Normal owner updates require phone verification and no active blocking sanction
- [x] Place region changes validate `system_policies.region.registration_scope`
- [x] Admin place update/delete actions through the user endpoint are logged to `admin_action_logs`

### Recommendation System
- [x] `recommendations` entity and repository implemented
- [x] Recommend API implemented at `POST /api/places/{placeId}/recommend`
- [x] Cancel recommendation API implemented at `DELETE /api/places/{placeId}/recommend`
- [x] Recommendation policy API implemented at `GET /api/places/{placeId}/recommend-policy`
- [x] My recommendations API implemented at `GET /api/users/me/recommendations`
- [x] Recommendation requires phone verification via `@RequirePhoneVerified`
- [x] One active recommendation per user/place is enforced
- [x] Daily recommendation limit reads `system_policies.recommend.daily_limit`
- [x] Daily recommendation count checks can use Redis when `APP_REDIS_ENABLED=true` and fall back to recommendation rows when disabled
- [x] User trust recommendation weight is applied
- [x] `place_stats.recommend_count`, `score_total`, and `trust_weighted_score` update on recommend/cancel
- [x] Place detail frontend recommendation action connected to backend recommend/cancel APIs

### Visit System
- [x] `visits` entity/repository implemented
- [x] Visit verification API implemented at `POST /api/places/{placeId}/visits`
- [x] Visit policy API implemented at `GET /api/places/{placeId}/visit-policy`
- [x] My visits API implemented at `GET /api/users/me/visits`
- [x] Place visit summary API implemented at `GET /api/places/{placeId}/visits/summary`
- [x] Visit verification requires phone verification via `@RequirePhoneVerified`
- [x] GPS distance validation reads `system_policies.visit.radius_meter`
- [x] Visit cooldown reads `system_policies.visit.cooldown_hour`
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
- [x] Existing admin screens expose a Korean `감사 로그` navigation label for audit review

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
- [x] Mobile bottom navigation was realigned to the five basic app destinations from the reference (`홈`, `랭킹`, `저장`, `커뮤니티`, `마이`), with place registration remaining as a floating map action and a lightweight Korean community placeholder route added until the community domain is implemented
- [x] Verification for the mobile app UI second refactor passed frontend build, frontend lint, UI language check, and Korean encoding check; browser screenshot QA was not completed because the in-app browser tool was unavailable in this session and the local Vite server could not be kept reachable from the tool environment
- [x] Home mobile layout QA follow-up made minimal overlap fixes after static responsive review: right floating map actions now hide while the place sheet is open, the mobile sheet has a 44px-class close target, full-sheet max height leaves room for the top chrome and bottom navigation, and bottom navigation/sheet offsets account for mobile safe-area insets; API, routing, data contracts, and Kakao map stabilization logic were left unchanged
- [x] Home mobile browser QA was rerun with Chrome CDP mobile emulation at 360px, 393px, and 430px; the basic map screen kept `documentElement.scrollWidth` equal to the viewport width, the app header/search/category row/bottom navigation did not clip, and the right floating map controls stayed clear of the category row and bottom navigation. The local static QA server did not load real Kakao map markers, so marker-click half sheet and expanded sheet visual QA still need a real Kakao SDK/key browser pass.
- [x] Home web browser QA was rerun with Chrome CDP at 768px, 1024px, 1280px, 1440px, 1536px, and 1920px; desktop widths kept `documentElement.scrollWidth` equal to the viewport width, the left rail/panel/map/category/control structure stayed separated, the category more popover remained within the viewport, and mobile 360px/393px/430px rechecks still used the app mobile overlay. Minimal desktop fixes made the top category chips horizontally scrollable at 1024px, hid the map status chip while the more popover is open, and aligned the desktop rail labels with the web navigation scope.
- [x] Home web 2.5 detail refactor follow-up kept the desktop map fixed while converting the left rail to Home-local panel modes, added a compact left information-panel collapse/expand toggle, kept desktop map categories limited to existing Honeytong food categories, removed the lower weather/status chip, and verified 360px, 1024px, 1280px, and 1440px Chrome CDP widths without horizontal overflow.
- [x] Home web selected-place information panel detail pass added a desktop-only detail card with a fuller image area, title-side save button, compact place summary, and the confirmed tabs `홈`, `메뉴`, `사진`, `댓글`, `신고`; the mobile place sheet remains on the existing shared card path. Frontend lint/build passed, while an attempted Chrome CDP selected-panel screenshot timed out in the local tool session after the temporary server and browser were cleaned up.
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

Next task:

2nd UI polish round

- rerun the selected-place desktop information panel screenshot QA in a normal browser session with real Kakao map markers visible, then tune only spacing or tab content density if the visual pass shows overlap
- then polish Detail/Ranking/Wishlist/MyPage spacing only where a seeded browser QA pass shows real overlap or density issues
- connect the community route to real community features when that domain enters scope
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
