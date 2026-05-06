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
- [x] Disposable operator release rehearsal completed on `main` with schema `honey_stage_rehearsal_20260506153715`
- [x] Seed/bootstrap startup completed with Flyway and schema validation, then local staging backend/frontend smoke started from the wrapper
- [x] Health, frontend HTTP 200, admin login, `/api/users/me`, `/api/users/me/status`, region lookup, GPS region verify, place registration policy, place create, recommendation, visit verification, ranking season creation, ranking recalculation, ranking read, admin dashboard, admin action logs, and admin user action logs were verified against disposable data
- [x] Wrapper stop cleanup completed for recorded backend/frontend/listener processes
- [x] Rollback readiness artifacts were present: backend boot jar and frontend dist output
- [x] Live SOLAPI send/verify completed against the disposable backend without recording raw phone, code, or provider credentials

Next task:

Prepare MVP release go/no-go handoff

- summarize release gates, CI, rehearsal results, remaining operational risks, and rollback readiness for the release operator
- do not include raw secrets, recipient phone, verification code, or provider credentials
- recommended reasoning level: low

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
