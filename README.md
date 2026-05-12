# Honeytong (허니통)

## 프로젝트 소개

Honeytong은 **모바일 퍼스트**, **위치 기반**의 로컬 맛집 탐험/발견 플랫폼입니다.  
사용자(꿀벌)가 동네 식당(꽃)을 탐험하고, **추천/방문 인증/댓글** 같은 신뢰 기반 활동을 통해 지역 커뮤니티 데이터가 쌓이며, **집계(aggregation) 기반 랭킹**으로 “동네 가이드”를 만들어갑니다.

핵심 원칙(중요):
- **비즈니스 정책 값 하드코딩 금지**: 제한/가중치/쿨다운 등은 반드시 DB 정책(`system_policies`)에서 읽습니다.
- **모든 검증은 서버에서**: 프론트는 UX 보조, 최종 판단은 서버가 합니다.
- **컨트롤러는 얇게 / 비즈니스 로직은 서비스에**
- **관리자 API 분리**: 사용자 `/api`, 관리자 `/api/admin`
- **랭킹은 집계 기반**: 실시간 계산이 아니라 집계 테이블을 읽습니다(`place_season_scores`, `place_stats` 등).

관련 문서: `docs/prd.md`, `docs/rules.md`, `docs/architecture.md`, `docs/decisions.md`

## 주요 기능 (현재 구현됨)

### 사용자(일반) 기능
- **회원가입/로그인**: 로컬 계정 + OAuth(카카오/네이버/구글)
- **JWT 인증**: access/refresh 토큰, refresh 회전/로그아웃(폐기)
- **전화번호 인증**: 인증코드 발송/검증/상태 조회 (로컬 dev sender + SOLAPI/Naver SENS 어댑터)
- **지역(동) 인증**: GPS 기반 지역 인증, 내 지역 조회/변경(정책 기반 쿨다운)
- **장소(꽃)**
  - 등록/수정/삭제(논리 삭제)
  - 상세/검색/지역별 목록/내 주변(nearby) 목록
  - 등록 가능 정책 조회(범위/제한)
- **참여(신뢰 기반 활동)**
  - 추천/추천 취소(일일 제한, 1인 1추천)
  - 방문 인증(GPS 반경, 쿨다운 정책)
  - 댓글 작성/수정/삭제(1인 1댓글, 삭제 후 재작성 복원)
- **랭킹**
  - 시즌/지역(dong/district/city) 기준 장소 랭킹 조회
  - 장소별 랭킹 히스토리(시즌 스냅샷) 조회
- **신고**
  - 장소/댓글/유저 신고 생성, 내 신고 목록 조회

### 관리자 기능
- **대시보드**: 오늘 지표/대기 신고 등 운영 지표 조회
- **정책 관리**: 시스템 정책 조회/수정(감사 로그 포함)
- **신고 처리**: 신고 검토(승인/반려) + 후속 조치(숨김/삭제/제재 등)
- **유저 관리**: 목록/상세, 제재 부여, 신뢰/추천 가중치 조정
- **장소 관리**: 노출/승인/프랜차이즈 심사 상태 변경, 점수 수동 조정, 랭킹 제외
- **활동 관리**: 추천 무효화, 방문 무효화, 댓글 블라인드/삭제
- **감사/행동 로그**: 관리자 액션 로그 및 사용자 행동 로그 조회

## 기술 스택

### Backend
- **Java 21**
- **Spring Boot 3.5.x**
  - Spring Web, Spring Validation, Spring Security, Spring AOP, Spring Actuator
  - Spring Data JPA
  - Spring Data Redis(옵트인 캐시/쿨다운/카운터)
- **MySQL**
- **Flyway** (프로덕션 마이그레이션)
- **JWT**: `jjwt` 기반 access/refresh 토큰

### Frontend
- **React 19**, **TypeScript**
- **Vite**
- **React Router**
- **Tailwind CSS**
- **Axios**

### Dev/Ops (로컬 실행 편의)
- Windows PowerShell 래퍼: `scripts/dev-env.ps1`, `scripts/run-backend-gradle.ps1`, `scripts/run-frontend-npm.ps1`

## 담당 역할

이 저장소 기준으로 구현/정리된 역할 범위는 다음과 같습니다.
- **백엔드**: 인증/정책/지역/장소/참여/랭킹/신고/관리자 API, 집계 기반 랭킹 구조, 감사/행동 로그
- **프론트**: 모바일 퍼스트 UI, 사용자 플로우(로그인/인증/탐색/상세/참여), 관리자 콘솔 화면
- **운영 도구/문서화**: Windows 개발환경 정규화 스크립트, 스모크/런북, PRD/스키마/API 문서 싱크

## 핵심 구현 (왜/어떻게)

- **정책 기반 운영(Policy-driven)**: `system_policies`에 저장된 값으로 일일 제한, 반경, 쿨다운, 가중치 등을 결정합니다. 운영자가 코드 변경 없이 튜닝할 수 있도록 설계했습니다.
- **신뢰(Trust) 가중치 반영**: 추천은 유저 신뢰도에 따른 가중치가 적용되며, 활동 데이터는 집계 테이블에 누적됩니다.
- **집계 기반 랭킹(Precomputed Ranking)**: 랭킹 API는 실시간으로 추천/방문/댓글을 계산하지 않고, 집계/시즌 테이블에서 빠르게 읽도록 구성했습니다.
- **관리자 분리 및 감사 로그**: 관리자 API는 별도 경로(`/api/admin`)로 분리하고, 정책/모더레이션/랭킹 같은 주요 조작은 `admin_action_logs`로 추적 가능하게 합니다.
- **Redis는 “옵트인”**: 캐시/쿨다운/카운터를 Redis로 가속할 수 있지만, DB가 항상 소스 오브 트루스가 되도록 경계(캐시 어댑터)로 분리했습니다.

자세한 설계 근거: `docs/architecture.md`, `docs/decisions.md`, `docs/db-schema.md`, `docs/api-spec.md`

## 트러블슈팅

### PowerShell에서 npm/실행 정책 오류가 날 때

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
. .\scripts\dev-env.ps1
```

프로젝트는 PowerShell 실행 정책 문제를 피하기 위해 `npm.cmd`를 우선 사용하도록 구성되어 있습니다(`scripts/dev-env.ps1` 참고).

### 환경변수(.env) 바꿨는데 프론트에 반영이 안 될 때
- Vite는 실행 시점 환경을 읽습니다. `.env` 변경 후 **`npm run dev` 재시작**이 필요합니다.

### 로컬에서 프론트 API 호출이 5173으로 가거나 CORS 문제가 날 때
- 기본 개발 흐름은 Vite 프록시(`/api` → `http://127.0.0.1:8080`)를 사용합니다. (`frontend/vite.config.ts` 참고)
- `VITE_API_BASE_URL`을 임의로 프론트 dev 서버 주소로 설정하면 API가 꼬일 수 있습니다.

### (참고) `pwsh`가 없는 환경
- 일부 Windows 환경에서는 `pwsh`(PowerShell 7)가 없을 수 있습니다. 이 저장소의 스크립트/명령은 기본 `powershell` 기준으로 제공합니다.

## 실행 방법

### 1) 개발 세션 정규화 (권장)

리포지토리 루트에서:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
. .\scripts\dev-env.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\check-dev-env.ps1
```

### 2) 환경변수 설정

- 루트의 `.env.example`을 참고해 `.env`를 생성합니다(커밋 금지).
- 로컬 기준 핵심 값:
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
  - `JWT_SECRET`
  - `KAKAO_REST_API_KEY`, `KAKAO_JAVASCRIPT_KEY`
  - `VITE_KAKAO_MAP_JAVASCRIPT_KEY`

### 3) 백엔드

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-backend-gradle.ps1 test
powershell -ExecutionPolicy Bypass -File .\scripts\run-backend-gradle.ps1 bootRun
```

- 헬스체크: `GET /actuator/health`

### 4) 프론트

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-frontend-npm.ps1 run lint
powershell -ExecutionPolicy Bypass -File .\scripts\run-frontend-npm.ps1 run build
```

개발 서버:

```powershell
cd frontend
npm run dev -- --host 127.0.0.1
```

### (선택) 로컬 스테이징 스모크

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-local-staging-smoke.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\run-local-staging-smoke.ps1 -Stop
```

절차/주의사항: `docs/mvp-release-runbook.md`

## 화면 이미지

현재 리포지토리에는 README에 바로 링크할 **스크린샷 이미지 파일이 포함되어 있지 않습니다.**  
아래 위치 중 하나에 이미지를 추가해두면, README에서 상대 경로로 바로 노출할 수 있습니다.

권장 폴더:
- `docs/images/` (또는 `docs/screenshots/`)

예시(이미지 추가 후 경로만 바꾸면 됩니다):

```md
![홈(지도 탐색)](docs/images/home-map.png)
![장소 상세(추천/방문/댓글)](docs/images/place-detail.png)
![랭킹](docs/images/ranking.png)
![관리자 대시보드](docs/images/admin-dashboard.png)
```

## 저장소 구조

```text
backend/   Spring Boot (API, 정책/신뢰/랭킹/관리자)
frontend/  Web (모바일 퍼스트 UI + 관리자 UI)
docs/      PRD/규칙/스키마/API/아키텍처/런북
scripts/   Windows PowerShell 개발/스모크 래퍼
```

