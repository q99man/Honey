# Honeytong (허니통)

## 프로젝트 소개

Honeytong은 모바일 우선, 위치 기반의 로컬 맛집 탐험 플랫폼입니다.
사용자는 지역을 인증하고 주변 맛집을 발견하며, 추천, 방문 인증, 댓글 같은 신뢰 기반 활동으로 지역 랭킹에 기여합니다.

이 서비스의 핵심 방향은 다음과 같습니다.

- 지도 중심 탐색 경험
- 전화 인증과 지역 인증 기반의 신뢰 참여
- 추천, 방문, 댓글을 집계한 지역 랭킹
- 운영자가 조정할 수 있는 정책 기반 시스템
- 모바일 앱 감각의 Home 지도 UI와 보조 웹/관리자 화면

## 현재 상태

MVP 핵심 기능 범위는 구현 및 로컬 검증이 완료된 상태입니다.
사용자 흐름, 관리자 흐름, 기술 게이트 검증 상태는 `docs/mvp-flow-checklist.md`에서 관리합니다.

현재 다음 작업은 코드 기능 추가보다 문서 기준 정리, 시드 데이터 기반 브라우저 QA, Home/My 패널 실제 API 상태 확인, 운영 go/no-go 준비입니다.

## 구현된 주요 범위

- 로컬 로그인/회원가입, OAuth 로그인, JWT access/refresh token
- 전화 인증, SOLAPI/Naver SENS 발송 어댑터, 핵심 액션 인증 가드
- GPS 기반 지역 인증과 지역 변경 정책
- 장소 등록, 목록, 검색, 상세, 수정, 삭제
- 추천, 방문 인증, 댓글, 신고
- 지역별 랭킹, 시즌, 랭킹 재계산, 랭킹 히스토리
- 관리자 대시보드, 정책, 사용자, 장소, 신고, 활동, 감사 로그
- 커뮤니티 자유게시판 CRUD 1차 범위
- Home 지도 UI, 모바일/웹 반응형 탐색 UI, My/Ranking/Wishlist/Admin 화면

## 문서 기준

작업 전에는 로컬 하네스 문서를 기준으로 확인합니다.

- `AGENTS.md`
- `docs/progress.md`
- `docs/tasks.md`
- `docs/mvp-flow-checklist.md`
- `docs/api-spec.md`
- `docs/db-schema.md`
- `docs/decisions.md`

문서와 코드가 어긋나면 문서를 먼저 최신 상태로 맞춘 뒤 다음 구현에 들어갑니다.

## 검증 기준

일반 변경 후 기본 검증은 다음을 사용합니다.

```powershell
cd backend
.\gradlew.bat test

cd ..\frontend
npm.cmd run build
```

프론트 UI 변경은 모바일 360px, 393px, 430px 기준 브라우저 QA를 우선 확인합니다.
운영 준비 단계에서는 `.env.example`, `backend/src/main/resources/application-prod.yml`, Flyway, `/actuator/health`, Kakao/SOLAPI 설정, 민감정보 로그 노출 여부를 함께 확인합니다.
