# MVP Release Candidate Checklist

This checklist prepares Honeytong MVP changes for a release candidate without committing secrets or mutating a production-like database accidentally.

## 1. Release Gate

The release candidate can be prepared only after these checks pass:

- `.\scripts\run-backend-gradle.ps1 test`
- `.\scripts\run-frontend-npm.ps1 run build`
- `.\scripts\run-frontend-npm.ps1 run lint`
- `git diff --check`
- `bash scripts/verify-ui-language.sh`
- `bash scripts/check-korean-encoding.sh`
- `bash scripts/verify-doc-sync.sh`
- `git check-ignore -v .env`

Sensitive smoke data must not appear in tracked files:

```powershell
rg -n "REAL_PHONE|REAL_CODE|REAL_SMOKE_EMAIL" --glob '!**/.git/**' --glob '!**/node_modules/**' --glob '!**/build/**' --glob '!**/dist/**' --glob '!**/.gradle-user-home/**' --glob '!**/.env'
```

Replace the sample patterns with the actual test values used during smoke testing. The command should return no matches.

## 2. Worktree Packaging

Before committing or opening a PR:

- Review `git status --short`.
- Group the accumulated work into one release-candidate change set unless the reviewer wants smaller commits.
- Do not include `.env`, local logs, build output, or ad hoc smoke credentials.
- Confirm new files are intentional, especially:
  - SOLAPI/Naver SENS sender classes and tests
  - admin audit log frontend page
  - phone verification live smoke script and runbook
  - this release checklist
- Run the release gate again after any conflict resolution or final formatting.

## 3. Backend Environment

Set these values in the deployment environment, not in Git:

Required production values:

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `KAKAO_REST_API_KEY`
- `KAKAO_JAVASCRIPT_KEY`
- `PHONE_VERIFICATION_SENDER_PROVIDER=solapi`
- `SOLAPI_API_KEY`
- `SOLAPI_API_SECRET`
- `SOLAPI_FROM`

Recommended explicit production values:

- `FLYWAY_ENABLED=true`
- `FLYWAY_VALIDATE_ON_MIGRATE=true`
- `JPA_DDL_AUTO=validate`
- `REGION_SEED_ENABLED=false`
- `POLICY_SEED_ENABLED=false`
- `ADMIN_BOOTSTRAP_ENABLED=false`
- `CACHE_TYPE=none`
- `APP_REDIS_ENABLED=false`
- `REDIS_HEALTH_ENABLED=false`
- `LOG_LEVEL_ROOT=INFO`
- `LOG_LEVEL_APP=INFO`
- `LOG_LEVEL_SQL=WARN`
- `LOG_LEVEL_SQL_BIND=OFF`

Optional values when Redis is intentionally enabled:

- `CACHE_TYPE=redis`
- `APP_REDIS_ENABLED=true`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `REDIS_DATABASE`
- `REDIS_TIMEOUT`
- `POLICY_CACHE_TTL`
- `RANKING_CACHE_TTL`
- `REDIS_HEALTH_ENABLED=true` only when Redis is required for runtime readiness.

Optional ranking scheduler values:

- `RANKING_SCHEDULER_ENABLED=false` for manual MVP operation.
- Set `RANKING_SCHEDULER_ENABLED=true` only after operations agree on automatic recalculation.
- `RANKING_SCHEDULER_CRON`
- `RANKING_SCHEDULER_ZONE=Asia/Seoul`
- `RANKING_SCHEDULER_SEASON_CODE`

## 4. Frontend Environment

Set these values for the frontend build/deployment environment:

- `VITE_API_BASE_URL`
- `VITE_KAKAO_JAVASCRIPT_KEY`

Confirm the frontend build is produced after environment values are set:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\scripts\run-frontend-npm.ps1 run build
```

Deploy `frontend/dist` through the selected static hosting or web server pipeline.

## 5. Database And Migration

Use Flyway for production schema changes.

Preflight:

- Back up the target database.
- Confirm the database is disposable, staging, or intentionally approved for release-candidate startup.
- Confirm whether the database already has `flyway_schema_history`.
- For existing non-empty databases without Flyway history, baseline intentionally only after backup and review.
- Keep `JPA_DDL_AUTO=validate`; do not let Hibernate mutate the production schema.

Startup check against a disposable or staging database:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:mysql://..."
$env:DB_USERNAME = "..."
$env:DB_PASSWORD = "..."
$env:JWT_SECRET = "..."
$env:CORS_ALLOWED_ORIGINS = "http://localhost:5173"
$env:PHONE_VERIFICATION_SENDER_PROVIDER = "solapi"
$env:SOLAPI_API_KEY = "..."
$env:SOLAPI_API_SECRET = "..."
$env:SOLAPI_FROM = "..."
.\scripts\run-backend-gradle.ps1 bootRun
```

Do not run this against a production database until the release candidate is approved.

## 6. Seed And Admin Bootstrap Policy

Region seed:

- Local development can use `REGION_SEED_ENABLED=true` with `REGION_SEED_LOCATION=classpath:region/region-seed.csv`.
- Production defaults must keep `REGION_SEED_ENABLED=false`.
- If a new environment has no region data, import once through an approved setup run, then disable it.

Policy seed:

- Local development can use `POLICY_SEED_ENABLED=true` to fill missing default policies.
- Production defaults must keep `POLICY_SEED_ENABLED=false`.
- If a new environment has no policies, import once through an approved setup run, then manage changes through admin policy APIs.
- Confirm required policies exist before opening user traffic:
  - `recommend.daily_limit`
  - `visit.radius_meter`
  - `visit.cooldown_hour`
  - `region.change_cooldown_day`
  - `region.registration_scope`
  - `place.registration_limit`
  - `growth.visit_exp`
  - `growth.level_exp_thresholds`
  - `trust.valid_visit_score`
  - `trust.grade_thresholds`
  - `trust.recommend_weight_by_grade`
  - `ranking.recommend_weight`
  - `ranking.visit_weight`
  - `ranking.comment_weight`

Admin bootstrap:

- Keep `ADMIN_BOOTSTRAP_ENABLED=false` for production.
- For a staging-only first admin setup, enable bootstrap with private credentials, start once, verify the admin account, then disable bootstrap and restart.
- Do not reuse local sample admin passwords.

## 7. Backend Health And Smoke

After backend startup:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/actuator/health -Method GET
```

Expected result:

```json
{
  "status": "UP"
}
```

Minimum smoke sequence:

- Signup/login with a test account.
- Read `/api/users/me`.
- Send and verify a phone code through SOLAPI using `scripts/smoke-phone-verification.ps1`.
- Verify GPS region against the intended Kakao-enabled environment.
- Create a test place.
- Recommend, visit, and comment with a phone-verified account.
- Create a report and process it from the admin UI.
- Create or activate a ranking season, recalculate, and read regional rankings.
- Confirm admin action logs and user action logs show the expected events.

## 8. Security And Logging

Before opening the environment:

- Confirm `.env` and deployment secret files are ignored and not committed.
- Confirm normal logs do not include raw verification codes, access tokens, refresh tokens, SOLAPI secrets, or SQL bind values.
- Keep `LOG_LEVEL_SQL_BIND=OFF`.
- Expose only `health` through actuator unless another endpoint is explicitly reviewed.
- Keep admin APIs under `/api/admin/**` and verify role restrictions with a non-admin account.

## 9. Rollback

Prepare rollback before release:

- Keep the previous backend artifact or container image available.
- Keep the previous frontend artifact available.
- Back up the database before Flyway migration.
- Record whether Flyway migrations are backward-compatible.
- If rollback needs DB restoration, pause write traffic first.
- Disable `RANKING_SCHEDULER_ENABLED` before rollback if scheduler behavior is suspected.

## 10. Release Notes Draft

Suggested release-candidate summary:

- MVP local auth, phone verification, region verification, place participation, ranking, reporting, and admin operation flows are implemented.
- SOLAPI live SMS delivery has been smoke tested.
- Ranking reads use aggregate season score rows.
- Core business limits remain server-side and policy-driven.
- Admin policy, report, place, user, activity, ranking, and audit-log screens are available.
- Production profile uses Flyway and `JPA_DDL_AUTO=validate` by default.

## 11. Known Follow-Up Work

Not required to ship the MVP release candidate, but should be tracked:

- Production-profile startup check against an approved disposable or staging database.
- Release branch or PR creation after reviewing the current dirty worktree.
- Operator rehearsal for first admin setup, policy import, region import, SOLAPI smoke, and rollback.
- Heavier query/index review after real traffic appears.
- Optional Redis enablement only after operational readiness is clear.
