# Mobile Smoke Checklist

Honeytong MVP 모바일 핵심 플로우를 반복 확인하기 위한 체크리스트다. UI, 라우팅, 상태 관리, seeded data, Kakao 지도 연동을 건드리는 변경 후에는 이 문서를 기준으로 smoke test를 다시 실행한다.

## 1. 테스트 환경

- [ ] Backend dev server가 실행 중이다.
- [ ] Frontend dev server가 실행 중이다.
- [ ] Frontend가 실제 Kakao Maps JavaScript SDK 키를 로드한다.
  - `VITE_KAKAO_MAP_JAVASCRIPT_KEY`를 우선 사용한다.
  - 기존 로컬 fallback인 `VITE_KAKAO_JAVASCRIPT_KEY`를 쓰는 경우에도 실제 JavaScript 키인지 확인한다.
  - Kakao Developers JavaScript SDK 도메인에 현재 frontend origin이 등록되어 있다.
  - `.env` 변경 후에는 Vite dev server를 재시작한다.
- [ ] Local UI smoke seeded data가 준비되어 있다.
  - 기준 seed: `scripts/dev-seed-honeytong-ui-smoke.sql`
  - production DB에는 실행하지 않는다.
- [ ] 모바일 폭을 각각 확인한다.
  - 360px
  - 393px
  - 430px
- [ ] 가능하면 Codex in-app browser에서도 Home 지도 영역과 샘플 맛집 링크 로드를 확인한다.

## 2. 핵심 사용자 플로우

### Home

- [ ] Home에 진입한다.
- [ ] Kakao 지도 타일이 로드된다.
- [ ] 지도 마커가 표시된다.
- [ ] 샘플 맛집 목록이 표시된다.
- [ ] 샘플 맛집 카드 또는 지도 마커에서 Detail로 이동할 수 있다.
- [ ] 검색어 입력 후 검색 결과가 표시된다.
- [ ] 결과가 없는 검색어에서 0건 상태가 자연스럽게 표시된다.
- [ ] 카페 카테고리 필터가 적용된다.

### Detail

- [ ] Home에서 Detail로 이동한다.
- [ ] Detail에서 뒤로가기로 이전 화면에 돌아간다.
- [ ] Detail 찜 토글이 동작한다.
- [ ] 긴 장소명, 긴 설명, 긴 주소가 모바일 폭에서 자연스럽게 줄바꿈된다.

### Wishlist

- [ ] Detail에서 찜한 장소가 Wishlist에 표시된다.
- [ ] Wishlist에서 찜 해제가 동작한다.
- [ ] 찜한 장소가 모두 해제되면 빈 상태가 표시된다.

### Ranking

- [ ] Ranking에 진입한다.
- [ ] Ranking 목록이 seeded ranking data 기준으로 표시된다.
- [ ] Ranking 지역 필터 가로 스크롤이 동작한다.
- [ ] Ranking 항목에서 Detail로 이동할 수 있다.

### MyPage

- [ ] 로그아웃 상태가 자연스럽게 표시된다.
- [ ] 임시 로그인 또는 테스트 로그인 상태가 자연스럽게 표시된다.
- [ ] 긴 닉네임이 모바일 폭에서 레이아웃을 깨지 않는다.
- [ ] 긴 지역명이 모바일 폭에서 레이아웃을 깨지 않는다.

## 3. 공통 모바일 확인 기준

- [ ] 앱 전체에서 의도하지 않은 가로 overflow가 없다.
- [ ] 하단 네비게이션이 콘텐츠와 주요 버튼을 가리지 않는다.
- [ ] 버튼 텍스트가 부자연스럽게 줄바꿈되거나 찌그러지지 않는다.
- [ ] 긴 텍스트가 말줄임, 줄바꿈, 영역 확장 중 해당 화면에 맞는 방식으로 자연스럽게 처리된다.
- [ ] 한국어 문구가 깨지지 않는다.
- [ ] 검색, 빈 상태, 로딩, 오류 상태의 한국어 문구가 화면 안에 들어온다.
- [ ] 의도된 카테고리 필터와 지역 필터의 가로 스크롤은 회귀로 판단하지 않는다.
- [ ] Kakao 지도 내부 장식 컨트롤 또는 지도 provider UI는 앱 UI 회귀로 판단하지 않는다.
- [ ] 자동 측정에서 작은 타깃이 잡히면 앱 버튼인지 Kakao 지도 내부 컨트롤인지 구분한다.

## 4. 검증 명령

Windows 로컬 작업에서는 먼저 도구 세션을 정규화한다.

```powershell
. .\scripts\dev-env.ps1
```

Frontend 검증:

```powershell
.\scripts\run-frontend-npm.ps1 lint
.\scripts\run-frontend-npm.ps1 build
```

Repository 검증:

```powershell
git diff --check
bash ./scripts/check-korean-encoding.sh
bash ./scripts/verify-ui-language.sh
bash ./scripts/verify-doc-sync.sh
```

`bash`가 현재 Windows 세션에서 잡히지 않으면 `scripts/dev-env.ps1` 적용 상태와 Git Bash 경로를 먼저 확인한다.

## 5. 기록 기준

- smoke test를 실행한 날짜, 대상 branch 또는 commit, backend/frontend URL, 사용한 DB 또는 seed 상태를 `docs/progress.md`에 기록한다.
- 실패 항목은 이 문서의 체크박스 이름을 기준으로 남긴다.
- UI 변경 후 회귀가 없으면 "이 체크리스트 기준으로 반복 smoke 통과"라고 기록한다.
- 앱 기능 코드, API, routing, backend, seed 구조 변경 없이 확인만 한 경우 그 사실도 함께 기록한다.
