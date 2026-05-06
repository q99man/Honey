# MVP Release Go/No-Go Handoff

작성일: 2026-05-06 KST
대상 커밋: `85d33b8 docs: record live SOLAPI smoke`
브랜치: `main`

이 문서는 Honeytong MVP release operator가 최종 go/no-go 판단을 내릴 때 필요한 릴리스 게이트, 리허설 결과, 남은 운영 리스크, 롤백 준비 상태를 요약한다.
전화번호, 인증 코드, provider credential, access token, refresh token, DB password, JWT secret은 포함하지 않는다.

---

## 1. 권고

권고 상태: GO for MVP release candidate handoff

조건:
- 운영 또는 승인된 staging 배포 직전 target DB backup 위치를 release operator가 확인한다.
- 이전 backend artifact 또는 container image와 이전 frontend artifact 위치를 release operator가 확인한다.
- target environment의 secret manager 또는 private environment에 필요한 값이 들어 있는지 확인한다.
- 운영 DB에는 승인 전 seed/bootstrap rehearsal 명령을 실행하지 않는다.

No-go 전환 기준:
- target backend가 `/actuator/health`를 통과하지 못한다.
- Flyway migration 또는 Hibernate schema validation이 실패한다.
- SOLAPI phone send/verify가 target environment에서 실패한다.
- Kakao GPS region verify가 target environment에서 실패한다.
- admin 권한, policy update, ranking recalculation, rollback artifact 중 하나라도 확인되지 않는다.
- 일반 로그에 raw phone, verification code, token, provider secret, SQL bind value가 노출된다.

---

## 2. 기준 상태

- latest `main`: `85d33b8`
- latest GitHub Actions CI on `main`: success
- release PR: `https://github.com/q99man/Honey/pull/1`
- release PR merge commit: `7759ca6 docs: prepare MVP release runbook`
- post-merge readiness commits:
  - `b0e268b docs: record post-merge release readiness`
  - `cd79fa3 docs: record operator release rehearsal`
  - `85d33b8 docs: record live SOLAPI smoke`

---

## 3. 통과한 릴리스 게이트

Post-merge release readiness on `main` passed:

- `.\scripts\run-backend-gradle.ps1 test`
- `.\scripts\run-backend-gradle.ps1 bootJar`
- `.\scripts\run-frontend-npm.ps1 run build`
- `.\scripts\run-frontend-npm.ps1 run lint`
- `git diff --check`
- `scripts/verify-ui-language.sh`
- `scripts/check-korean-encoding.sh`
- `scripts/verify-doc-sync.sh`
- release runbook presence check
- PR draft presence check
- tracked-file secret and smoke-data pattern scan

Documentation-only follow-up commits also passed:

- GitHub Actions CI on `cd79fa3`: success
- GitHub Actions CI on `85d33b8`: success

---

## 4. 리허설 결과

Disposable operator release rehearsal:

- Disposable schema: `honey_stage_rehearsal_20260506153715`
- Production profile boot reached `/actuator/health=UP`
- Flyway migration validation passed
- Hibernate schema validation passed with `JPA_DDL_AUTO=validate`
- seed/bootstrap startup completed once, then staging smoke ran with seed/bootstrap disabled
- local staging backend and frontend smoke started on isolated ports
- wrapper stop cleanup completed for recorded backend/frontend/listener processes

API smoke passed:

- admin login
- `/api/users/me`
- `/api/users/me/status`
- region lookup
- GPS region verify
- place registration policy
- place create
- recommendation
- visit verification
- ranking season creation
- ranking recalculation
- ranking read
- admin dashboard
- admin action logs
- admin user action logs

SOLAPI live smoke passed:

- signup/login for a disposable smoke account
- `POST /api/auth/phone/send-code` returned `sent=true`
- received code was verified through `POST /api/auth/phone/verify-code`
- `/api/auth/phone/status` returned `phoneVerified=true`
- raw phone, verification code, and provider credentials were not recorded in tracked docs
- disposable backend process was stopped after verification

---

## 5. 롤백 준비 상태

Verified locally:

- backend boot jar exists from release readiness build
- frontend `dist` output exists from release readiness build
- ranking scheduler default remains disabled unless explicitly enabled
- runbook rollback procedure is documented in `docs/mvp-release-runbook.md`

Operator must confirm before release:

- previous backend artifact or container image location
- previous frontend artifact location
- target DB backup location and restore owner
- Flyway migration compatibility and restore approval path
- who can pause write traffic or enter maintenance mode

Rollback trigger:

- backend health failure
- core API smoke failure
- SOLAPI or Kakao provider failure
- admin policy or authorization failure
- ranking recalculation abnormality that cannot be resolved by manual exclusion or rerun
- secret or raw verification data exposed in normal logs

---

## 6. 운영 환경 확인 항목

Required backend environment:

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

Recommended explicit backend environment:

- `FLYWAY_ENABLED=true`
- `JPA_DDL_AUTO=validate`
- `REGION_SEED_ENABLED=false`
- `POLICY_SEED_ENABLED=false`
- `ADMIN_BOOTSTRAP_ENABLED=false`
- `CACHE_TYPE=none`
- `APP_REDIS_ENABLED=false`
- `REDIS_HEALTH_ENABLED=false`
- `LOG_LEVEL_SQL=WARN`
- `LOG_LEVEL_SQL_BIND=OFF`
- `RANKING_SCHEDULER_ENABLED=false`

Required frontend environment:

- `VITE_API_BASE_URL`
- `VITE_KAKAO_JAVASCRIPT_KEY`

---

## 7. 남은 운영 리스크

- GPS spoofing: MVP는 radius/cooldown/server validation으로 기본 방어하고, 고급 탐지는 후속 작업이다.
- Duplicate accounts: phone verification으로 기본 제한하지만, 다중 번호 악용은 운영 모니터링이 필요하다.
- Ranking manipulation: admin activity moderation, ranking exclusion, ranking recalculation으로 수동 대응한다.
- Policy misconfiguration: SUPER_ADMIN policy update와 admin action log로 통제한다.
- Early data imbalance: MVP 초기에는 operator가 ranking season, manual adjustment, exclusion을 직접 관리한다.
- Redis: MVP release에서는 disabled-by-default로 두고, 운영 준비가 끝난 뒤 opt-in한다.

---

## 8. 최종 체크박스

Release operator 확인:

- [ ] target DB backup 완료
- [ ] previous backend artifact/container image 위치 확인
- [ ] previous frontend artifact 위치 확인
- [ ] target environment secret 주입 확인
- [ ] seed/bootstrap disabled 확인
- [ ] ranking scheduler disabled 확인
- [ ] backend `/actuator/health` 확인
- [ ] signup/login smoke 확인
- [ ] SOLAPI phone send/verify 확인
- [ ] Kakao GPS region verify 확인
- [ ] place create/recommend/visit/comment smoke 확인
- [ ] admin dashboard/policy/report/audit path 확인
- [ ] ranking season/recalculation/read smoke 확인
- [ ] normal logs에 raw secret, token, phone, code 없음 확인
- [ ] rollback owner와 restore 절차 확인

최종 판단:

- GO: 위 체크박스가 모두 충족되고 no-go 기준이 없다.
- NO-GO: 하나라도 실패하거나 secret/log 노출이 발견된다.

---

## 9. 다음 작업

Release operator가 이 handoff를 기준으로 target environment의 go/no-go 체크박스를 채운다.
릴리스 승인 후에는 `docs/mvp-release-runbook.md` 순서로 target environment smoke를 진행하고, 결과를 `docs/progress.md`에 남긴다.
