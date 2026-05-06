# MVP Release Runbook

이 문서는 Honeytong MVP 릴리스 후보를 만들기 전, 로컬 또는 스테이징에 가까운 환경에서 반드시 같은 순서로 실행할 절차를 정의한다.

목표:
- 환경 확인부터 빌드, 스모크, 브라우저 확인, 종료 정리까지 실행 순서를 고정한다.
- 포트 충돌, 스키마 누락, 외부 provider credential 문제의 재시도 기준을 명확히 한다.
- `.env`, SMS provider key, 실전화번호, 인증 코드, JWT secret 같은 민감값이 문서나 로그에 남지 않게 한다.

관련 문서:
- `docs/mvp-release-candidate-checklist.md`
- `docs/dev-environment.md`
- `docs/phone-verification-live-smoke.md`

---

## 1. 원칙

- 이 런북은 운영 절차 문서이며 business policy 값을 코드에 추가하지 않는다.
- 실제 운영 DB에는 릴리스 승인 전 절대 rehearsal 명령을 직접 실행하지 않는다.
- production profile은 Flyway migration과 `JPA_DDL_AUTO=validate`를 기준으로 검증한다.
- seed와 admin bootstrap은 일회성 setup 도구다. 실행 후 반드시 비활성화하고 재기동한다.
- 브라우저 스모크는 한국어 UI, 로그인, 마이페이지, 장소 상세 참여 영역, admin dashboard, admin policy navigation을 확인한다.
- 스모크 데이터는 추적 파일에 남기지 않는다.

---

## 2. 사전 조건

릴리스 담당자는 다음을 준비한다.

- Windows PowerShell
- MySQL client
- Java 21
- Node/npm
- Git
- 로컬 또는 스테이징 rehearsal 전용 MySQL schema
- 테스트용 Kakao key
- 테스트용 SOLAPI credential과 수신 가능한 휴대폰
- admin bootstrap용 비공개 계정 정보

민감값은 `.env`, 배포 secret manager, 현재 PowerShell process environment 중 하나에만 둔다.
문서, 커밋 메시지, 이슈, PR 본문, 일반 로그에 값을 붙여 넣지 않는다.

---

## 3. 실행 순서

### 3.1 도구 세션 정규화

저장소 루트에서 실행한다.

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
. .\scripts\dev-env.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\check-dev-env.ps1
git status --short
git check-ignore -v .env
```

통과 기준:
- Git, Node/npm, Java, Gradle 경로가 프로젝트 wrapper 기준으로 잡힌다.
- `.env`가 ignore 처리되어 있다.
- `git status --short`에서 의도하지 않은 secret, log, build output 변경이 없다.

### 3.2 릴리스 게이트 실행

다음 순서로 실행한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-backend-gradle.ps1 test
powershell -ExecutionPolicy Bypass -File .\scripts\run-backend-gradle.ps1 bootJar
powershell -ExecutionPolicy Bypass -File .\scripts\run-frontend-npm.ps1 run build
powershell -ExecutionPolicy Bypass -File .\scripts\run-frontend-npm.ps1 run lint
git diff --check
bash scripts/verify-ui-language.sh
bash scripts/check-korean-encoding.sh
bash scripts/verify-doc-sync.sh
```

통과 기준:
- backend test와 `bootJar`가 성공한다.
- frontend build와 lint가 성공한다.
- diff whitespace, UI language, Korean encoding, doc sync check가 모두 통과한다.

### 3.3 민감 스모크 데이터 스캔

실제 스모크에 사용한 이메일, 전화번호, 인증 코드 일부 패턴을 직접 대입해 검색한다.

```powershell
rg -n "REAL_PHONE|REAL_CODE|REAL_SMOKE_EMAIL" --glob '!**/.git/**' --glob '!**/node_modules/**' --glob '!**/build/**' --glob '!**/dist/**' --glob '!**/.gradle-user-home/**' --glob '!**/.env'
```

통과 기준:
- 명령이 tracked file에서 아무 결과도 반환하지 않는다.
- `.env` 자체는 검색 대상에서 제외하지만, `.env`가 Git에 포함되지 않는지는 3.1에서 별도로 확인한다.

### 3.4 스테이징 rehearsal schema 확인

`scripts/run-local-staging-smoke.ps1`는 기본적으로 최신 `honey_stage_rehearsal_*` schema를 찾는다.
schema를 명시하려면 다음처럼 실행한다.

```powershell
$env:HONEY_STAGING_SMOKE_SCHEMA = "honey_stage_rehearsal_YYYYMMDDHHMMSS"
```

스키마가 없으면 먼저 disposable schema에서 seed/bootstrap rehearsal을 수행한다.
이때만 다음 값을 일시적으로 활성화한다.

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:FLYWAY_ENABLED = "true"
$env:JPA_DDL_AUTO = "validate"
$env:REGION_SEED_ENABLED = "true"
$env:POLICY_SEED_ENABLED = "true"
$env:ADMIN_BOOTSTRAP_ENABLED = "true"
```

초기 setup boot가 끝나면 seed/bootstrap 값을 모두 `false`로 되돌리고 backend를 재시작해 `/actuator/health`가 `UP`인지 확인한다.

### 3.5 로컬 스테이징 smoke 시작

backend jar가 있어야 하므로 3.2의 `bootJar` 이후 실행한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-local-staging-smoke.ps1
```

다른 포트를 사용해야 하면 다음처럼 지정한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-local-staging-smoke.ps1 -BackendPort 18084 -FrontendPort 5175
```

통과 기준:
- backend health URL이 `UP`이다.
- frontend URL이 응답한다.
- `.tmp\local-staging-smoke\state.json`에 schema, URL, log path, PID가 기록된다.

### 3.6 API smoke

backend URL은 wrapper 출력값을 따른다. 기본값은 `http://127.0.0.1:18083`이다.

최소 확인:
- signup/login
- `/api/users/me`
- SOLAPI phone send/verify
- GPS region verify
- place create
- recommendation
- visit verification
- comment create/update/delete
- report create
- admin report process and follow-up action
- season create or activate
- ranking recalculation
- public dong/district/city ranking read
- admin action log read
- admin user action log read

전화 인증 스모크는 다음 문서를 따른다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-phone-verification.ps1 -BaseUrl "http://127.0.0.1:18083" -Phone "01012345678"
```

전화번호와 수신 인증 코드는 콘솔에 원문으로 남기지 않는다.

### 3.7 브라우저 smoke

wrapper가 출력한 frontend URL을 브라우저에서 연다. 기본값은 `http://127.0.0.1:5174`이다.

확인 범위:
- 홈 화면 한국어 문구가 깨지지 않는다.
- 로그인과 마이페이지 상태 조회가 동작한다.
- 전화 인증과 지역 인증 상태가 표시된다.
- 장소 상세에서 추천, 방문, 댓글 영역이 보인다.
- admin dashboard가 로드된다.
- admin policy 화면으로 이동할 수 있다.
- 브라우저 console에 error나 warning이 없다.

### 3.8 종료 정리

스모크 확인이 끝나면 wrapper가 기록한 process만 종료한다.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-local-staging-smoke.ps1 -Stop
```

통과 기준:
- state file에 기록된 backend/frontend/listener PID가 종료된다.
- 기존 8080 backend나 unrelated process를 종료하지 않는다.

---

## 4. 실패와 재실행

### 4.1 포트 충돌

증상:
- `Port 18083 is already in use`
- `Port 5174 is already in use`

대응:
- 기존 process가 현재 smoke wrapper가 띄운 것이라면 `-Stop`을 먼저 실행한다.
- unrelated process라면 종료하지 말고 `-BackendPort`, `-FrontendPort`로 다른 포트를 지정한다.
- 포트를 바꿨다면 `CORS_ALLOWED_ORIGINS`와 `VITE_API_BASE_URL`은 wrapper가 새 값에 맞춰 설정하는지 출력 URL로 확인한다.

### 4.2 staging schema 누락

증상:
- `No staging rehearsal schema found`
- `Schema does not exist`

대응:
- `-Schema`로 승인된 schema를 명시한다.
- schema가 실제로 없다면 disposable schema를 새로 만들고 seed/bootstrap rehearsal을 먼저 수행한다.
- production DB를 임시 rehearsal schema처럼 사용하지 않는다.

### 4.3 backend jar 누락

증상:
- `Backend jar not found`

대응:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-backend-gradle.ps1 bootJar
```

이후 smoke wrapper를 다시 실행한다.

### 4.4 provider credential 누락 또는 오류

증상:
- SMS send가 `EXTERNAL_SERVICE_ERROR`로 실패한다.
- Kakao GPS region verify가 provider 오류로 실패한다.

대응:
- `.env` 또는 process environment에 provider key가 있는지 확인한다.
- SOLAPI sender number 등록 상태를 확인한다.
- key, secret, sender number를 문서나 채팅에 붙여 넣지 않는다.
- credential 수정 후 backend를 재시작하고 smoke를 재실행한다.
- SMS 실패 시 latest verification-code cache가 남지 않아야 한다.

### 4.5 로그에 민감값이 보이는 경우

대응:
- 릴리스 후보를 중단한다.
- 해당 로그 파일을 공유하거나 커밋하지 않는다.
- 로그 설정을 `LOG_LEVEL_SQL_BIND=OFF`, SQL log `WARN` 기준으로 되돌린다.
- raw code, token, secret, full phone number가 출력된 원인을 수정한 뒤 3.2부터 다시 실행한다.

---

## 5. Rollback 준비

릴리스 전 준비:
- 이전 backend artifact 또는 container image를 보관한다.
- 이전 frontend artifact를 보관한다.
- Flyway 적용 전 DB backup을 만든다.
- 적용 migration의 backward compatibility를 기록한다.
- `RANKING_SCHEDULER_ENABLED=false`를 기본값으로 유지한다.

Rollback 실행 기준:
- 새 backend가 health check를 통과하지 못한다.
- 핵심 API smoke가 실패한다.
- 전화 인증 provider가 정상 전송을 하지 못한다.
- admin 권한 또는 policy update가 의도와 다르게 동작한다.
- ranking recalculation 결과가 비정상이고 수동 제외 또는 재계산으로 해결되지 않는다.

Rollback 절차:
1. write traffic을 중단하거나 점검 모드로 전환한다.
2. ranking scheduler가 켜져 있다면 즉시 끈다.
3. 이전 backend artifact로 되돌린다.
4. 이전 frontend artifact로 되돌린다.
5. migration rollback이 필요한 경우 backup restore 승인 후 DB를 복원한다.
6. `/actuator/health`, login, `/api/users/me`, admin dashboard를 확인한다.
7. 장애 원인, 영향 범위, 복구 시각을 기록한다.

---

## 6. 완료 기준

릴리스 후보 준비는 다음이 모두 만족될 때 완료된다.

- 3.1부터 3.8까지 순서대로 통과했다.
- 스모크 데이터와 secret scan이 깨끗하다.
- 한국어 UI가 브라우저에서 깨지지 않는다.
- admin audit log와 user action log에서 smoke 이벤트를 추적할 수 있다.
- seed/bootstrap은 비활성화된 상태로 재기동 확인이 끝났다.
- rollback artifact와 DB backup 위치가 확인되어 있다.

---

## 7. 릴리스 담당자 메모

릴리스 노트 초안은 `docs/mvp-release-candidate-checklist.md`의 "Release Notes Draft"를 기준으로 작성한다.

문제가 생기면 실패한 단계 번호, 실행 명령, backend/frontend URL, log path, schema 이름만 공유한다.
secret, 인증 코드, 전체 전화번호, access token, refresh token은 공유하지 않는다.
