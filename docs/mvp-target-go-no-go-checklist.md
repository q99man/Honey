# MVP Target Environment Go/No-Go Checklist

작성일: 2026-05-06 KST
기준 문서: `docs/mvp-release-go-no-go-handoff.md`
기준 커밋: `ca6c610 docs: add MVP release go no-go handoff`

이 문서는 release operator가 승인된 target environment에서 최종 go/no-go 판단을 기록하기 위한 실행 체크리스트다.
민감값은 적지 않는다. 전화번호, 인증 코드, provider credential, token, DB password, JWT secret, raw log excerpt는 기록하지 않는다.

---

## 1. 현재 저장소 기준 확인

Codex 확인 완료:

- [x] `main` is synchronized with `origin/main`
- [x] Latest handoff document exists: `docs/mvp-release-go-no-go-handoff.md`
- [x] Latest target checklist exists: `docs/mvp-target-go-no-go-checklist.md`
- [x] Release runbook exists: `docs/mvp-release-runbook.md`
- [x] Live phone smoke runbook exists: `docs/phone-verification-live-smoke.md`

Operator 확인 필요:

- [ ] target release commit is approved
- [ ] target release window is approved
- [ ] release operator and rollback owner are assigned

---

## 2. Target 환경 사전 확인

기록 방식:
- 값 자체를 적지 않는다.
- 확인 여부와 보관 위치 또는 담당자만 적는다.

Required backend secret/config:

- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `DB_URL`
- [ ] `DB_USERNAME`
- [ ] `DB_PASSWORD`
- [ ] `JWT_SECRET`
- [ ] `CORS_ALLOWED_ORIGINS`
- [ ] `KAKAO_REST_API_KEY`
- [ ] `KAKAO_JAVASCRIPT_KEY`
- [ ] `PHONE_VERIFICATION_SENDER_PROVIDER=solapi`
- [ ] `SOLAPI_API_KEY`
- [ ] `SOLAPI_API_SECRET`
- [ ] `SOLAPI_FROM`

Required frontend config:

- [ ] `VITE_API_BASE_URL`
- [ ] `VITE_KAKAO_MAP_JAVASCRIPT_KEY`

Safety defaults:

- [ ] `FLYWAY_ENABLED=true`
- [ ] `JPA_DDL_AUTO=validate`
- [ ] `REGION_SEED_ENABLED=false`
- [ ] `POLICY_SEED_ENABLED=false`
- [ ] `ADMIN_BOOTSTRAP_ENABLED=false`
- [ ] `RANKING_SCHEDULER_ENABLED=false`
- [ ] `LOG_LEVEL_SQL_BIND=OFF`

---

## 3. Backup and rollback readiness

Target DB:

- [ ] DB backup completed before deployment
- [ ] backup restore owner assigned
- [ ] restore path tested or approved
- [ ] Flyway baseline/history state reviewed

Artifacts:

- [ ] previous backend artifact or container image location confirmed
- [ ] previous frontend artifact location confirmed
- [ ] new backend artifact or container image location confirmed
- [ ] new frontend artifact location confirmed

Rollback trigger owner:

- [ ] write-traffic pause or maintenance-mode owner assigned
- [ ] rollback decision owner assigned
- [ ] incident communication owner assigned

---

## 4. Target deployment smoke

Backend:

- [ ] backend starts with production profile
- [ ] Flyway migration succeeds
- [ ] Hibernate schema validation succeeds
- [ ] `/actuator/health` returns `UP`
- [ ] normal logs do not expose raw token, password, phone, verification code, provider secret, or SQL bind values

Frontend:

- [ ] frontend serves built assets
- [ ] frontend points to target backend
- [ ] Korean UI text renders correctly
- [ ] browser console has no release-blocking error

Core user smoke:

- [ ] signup/login
- [ ] `/api/users/me`
- [ ] SOLAPI phone send/verify
- [ ] Kakao GPS region verify
- [ ] place create
- [ ] recommendation
- [ ] visit verification
- [ ] comment create/update/delete
- [ ] report create

Admin smoke:

- [ ] admin login
- [ ] admin dashboard
- [ ] admin policy read
- [ ] admin policy update with safe test value or no-op value
- [ ] admin report process and follow-up action in disposable test data
- [ ] admin action log read
- [ ] admin user action log read

Ranking smoke:

- [ ] create or activate target test season
- [ ] ranking recalculation
- [ ] public dong ranking read
- [ ] public district ranking read
- [ ] public city ranking read
- [ ] ranking result reviewed for obvious abnormality

---

## 5. No-go criteria

Mark NO-GO if any item below is true:

- [ ] DB backup is missing or restore owner is not assigned
- [ ] backend health is not `UP`
- [ ] Flyway migration fails
- [ ] Hibernate validation fails
- [ ] SOLAPI send/verify fails
- [ ] Kakao GPS region verify fails
- [ ] admin authorization or policy update behaves unexpectedly
- [ ] ranking recalculation fails or produces clearly invalid output
- [ ] raw secret, raw token, full phone number, or verification code appears in normal logs
- [ ] rollback artifact is missing

---

## 6. Final decision

Decision:
- [ ] GO
- [ ] NO-GO

Decision owner:
- name or role:
- decision time:

Notes:
- Do not paste secrets, phone numbers, verification codes, tokens, or raw provider responses.
- Link to private incident/release tracker only if it is access-controlled.

---

## 7. After decision

If GO:

- [ ] notify release stakeholders
- [ ] keep rollback owner available during the observation window
- [ ] monitor health, logs, admin dashboard, reports, and ranking reads
- [ ] record release result in `docs/progress.md`

If NO-GO:

- [ ] stop deployment or pause write traffic
- [ ] rollback or restore previous artifact as needed
- [ ] record failed checklist item without secrets
- [ ] open follow-up task with exact failing stage and owner
