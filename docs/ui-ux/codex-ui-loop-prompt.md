# Honeytong Codex UI Loop Prompt

## 1. 문서 목적

이 문서는 Honeytong 프로젝트의 UI/UX 리팩토링을 Codex가 안전하게 반복 수행하도록 하기 위한 마스터 루프 프롬프트이다.

Honeytong은 벌과 꿀을 테마로 한 동네 맛집 탐색 웹앱이다.

현재 목표는 기능 추가가 아니라 UI/UX 완성도 향상이다.  
단, UI/UX를 개선하는 과정에서 기존 기능, 라우팅, API 연결, 찜 상태, 상세 이동, 하단 네비게이션 동작을 절대 깨뜨리면 안 된다.

Codex는 이 문서를 기준으로 작은 단위의 UI/UX 개선 작업을 반복한다.

---

## 2. 현재 프로젝트 상태

Honeytong 프로젝트의 현재 상태는 다음과 같다.

```txt
React + Vite + TypeScript + Tailwind 기반 프론트 구현 완료
Spring Boot 백엔드 기본 기능 구현 거의 완료
프론트 mock 제거 후 실제 API 연결 작업 진행/완료 단계
Home / Detail / Ranking / Wishlist / MyPage 라우팅 완료
카드 컴포넌트 구현 완료
하단 네비게이션 구현 완료
찜 상태 구현 완료
상세 이동 구현 완료
Figma 모바일 기준 393x852 와이어프레임 경험 있음
이제 기능 구현보다 UI/UX 완성도 향상이 목표
```

---

## 3. 반드시 먼저 읽을 문서

Codex는 작업을 시작하기 전에 반드시 아래 문서를 먼저 읽는다.

```txt
docs/AGENT.md
docs/PRD.md
docs/ARCHITECTURE.md
docs/TASKS.md
docs/PROGRESS.md
docs/ui-ux/design-system.md
docs/ui-ux/ux-rules.md
docs/ui-ux/ui-components.md
docs/ui-ux/screen-specs.md
docs/ui-ux/interaction-rules.md
docs/ui-ux/codex-ui-loop-prompt.md
```

문서를 읽은 뒤 현재 코드 구조와 비교하고, 이번 루프에서 진행할 가장 작은 UI/UX 개선 단위를 선택한다.

---

## 4. Honeytong 디자인 방향

Codex는 Honeytong UI를 다음 방향으로 개선한다.

```txt
벌/꿀 테마
노랑/검정 기반
카카오 느낌은 참고하되 너무 카카오처럼 보이지 않게
따뜻하고 귀여운 동네 맛집 탐색 앱 느낌
모바일 우선
반응형 웹앱
393x852 모바일 기준 우선
카드형 UI
둥근 모서리
과한 그림자 금지
귀여운 bee/honey 감성은 살리되 너무 유치하지 않게
한국어 UI 유지
```

---

## 5. 절대 지켜야 할 원칙

Codex는 UI/UX 리팩토링 중 다음 원칙을 반드시 지킨다.

```txt
1. 한 번에 전체 UI를 갈아엎지 않는다.
2. 작은 단위로 리팩토링한다.
3. 기존 라우팅을 깨지 않는다.
4. 기존 API 연결을 깨지 않는다.
5. mock 데이터를 새로 만들지 않는다.
6. 기존 기능을 삭제하지 않는다.
7. 찜 기능을 유지한다.
8. 상세 이동을 유지한다.
9. 하단 네비게이션 동작을 유지한다.
10. TypeScript 타입 안정성을 유지한다.
11. 한국어 UI 문구를 깨뜨리지 않는다.
12. 불필요한 라이브러리를 새로 추가하지 않는다.
13. Tailwind 기반으로 개선한다.
14. UI 변경 후 빌드 가능 상태를 유지한다.
15. 작업 결과를 docs/PROGRESS.md에 기록한다.
```

---

## 6. 금지 사항

Codex는 다음 작업을 하면 안 된다.

```txt
전체 페이지를 한 번에 전부 갈아엎기
기존 API 호출 제거
기존 API 응답 타입 임의 변경
기존 라우팅 경로 변경
mock 데이터 재도입
찜 기능 삭제
상세 이동 삭제
하단 네비게이션 삭제
props 이름을 이유 없이 변경
기존 props 삭제
TypeScript 오류를 any로 덮기
한국어 문구를 영어로 변경
사용하지 않는 UI 라이브러리 추가
검색/필터 미구현 기능을 실제처럼 꾸미기
카드 클릭과 찜 버튼 클릭 충돌 방치
과한 shadow 사용
너무 강한 노랑 배경 반복
카카오톡처럼 보이는 UI 만들기
docs/PROGRESS.md 기록 없이 종료
```

---

## 7. 리팩토링 순서

Codex는 다음 순서로 UI/UX를 개선한다.

```txt
1. 공통 Layout / AppShell 정리
2. PageContainer 정리
3. BottomNavigation 정리
4. PageHeader 정리
5. RestaurantCard 정리
6. WishButton 이벤트 충돌 점검
7. Home 화면 UI/UX 개선
8. Detail 화면 UI/UX 개선
9. Ranking 화면 UI/UX 개선
10. Wishlist 화면 UI/UX 개선
11. MyPage 화면 UI/UX 개선
12. loading / empty / error 상태 정리
13. 전체 반응형 점검
14. 한국어 UI 문구 깨짐 점검
```

단, 실제 작업은 항상 이 중 하나의 작은 단위만 선택한다.

---

## 8. 이번 루프에서 선택 가능한 작은 작업 단위

Codex는 한 루프에서 아래 중 하나 정도만 선택한다.

```txt
AppShell 추가 또는 정리
PageContainer 추가 또는 정리
BottomNavigation 스타일 개선
BottomNavigation 활성 상태 점검
RestaurantCard 스타일 개선
RestaurantCard 찜 버튼 이벤트 충돌 수정
WishButton 접근성 aria-label 추가
Home 상단 문구 개선
Home 검색창 스타일 개선
Home 카테고리 칩 개선
Home 카드 간격 개선
Home loading / empty / error 상태 개선
Detail 상단 이미지 영역 개선
Detail 뒤로가기 버튼 개선
Detail 찜 버튼 위치 개선
Detail 정보 섹션 구분
Ranking Top 3 카드 개선
Ranking 필터 UI 개선
Wishlist 빈 상태 개선
Wishlist 찜 해제 UX 개선
MyPage 프로필 카드 개선
MyPage 활동 요약 카드 개선
한국어 UI 문구 점검
모바일 393x852 기준 레이아웃 점검
```

작업 단위가 크다고 판단되면 더 작게 쪼갠다.

---

## 9. UI 기준 요약

Codex는 아래 UI 기준을 따른다.

### 9.1 색상

```txt
기본 배경: #fffaf0
핵심 꿀색: #f6b800
진한 텍스트: #2b210f
보조 갈색: #5c3b13
카드 배경: #ffffff
경계선: #e5e5e5
```

---

### 9.2 레이아웃

```txt
모바일 우선
기준 화면 393x852
앱 최대 폭 max-w-[430px]
데스크톱에서는 중앙 정렬
페이지 좌우 여백 px-4
하단 네비게이션 고려 pb-24
```

권장 구조:

```tsx
<div className="min-h-screen bg-neutral-100">
  <main className="mx-auto min-h-screen max-w-[430px] bg-[#fffaf0] pb-24">
    {/* page content */}
  </main>
</div>
```

---

### 9.3 카드

```txt
rounded-3xl
bg-white
p-3 또는 p-4
shadow-sm
border는 필요할 때만 border-gray-100 또는 border-gray-200
과한 shadow 금지
```

권장 예시:

```tsx
className="rounded-3xl bg-white p-4 shadow-sm"
```

---

### 9.4 이미지

맛집 카드 이미지:

```tsx
className="aspect-[4/3] w-full rounded-2xl object-cover"
```

상세 상단 이미지:

```tsx
className="aspect-[16/10] w-full object-cover"
```

또는:

```tsx
className="aspect-[16/10] w-full rounded-b-3xl object-cover"
```

---

### 9.5 버튼

Primary Button:

```tsx
className="rounded-full bg-[#f6b800] px-5 py-3 text-sm font-semibold text-[#2b210f] transition duration-200 active:scale-[0.98]"
```

Secondary Button:

```tsx
className="rounded-full border border-gray-200 bg-white px-4 py-2 text-sm font-semibold text-gray-700"
```

Icon Button:

```tsx
className="flex h-11 w-11 items-center justify-center rounded-full bg-white shadow-sm transition duration-200 active:scale-[0.96]"
```

---

### 9.6 배지

```tsx
className="inline-flex items-center rounded-full bg-[#fff1bf] px-3 py-1 text-xs font-semibold text-[#5c3b13]"
```

---

## 10. 인터랙션 기준 요약

### 10.1 카드 클릭

맛집 카드는 클릭 시 상세 화면으로 이동한다.  
기존 라우팅 경로를 유지한다.

```tsx
onClick={() => navigate(`/restaurants/${id}`)}
```

실제 프로젝트 경로가 다르다면 기존 경로를 따른다.

---

### 10.2 찜 버튼

카드 내부 찜 버튼은 이벤트 전파를 막는다.

```tsx
onClick={(event) => {
  event.stopPropagation();
  onToggleWish?.();
}}
```

아이콘만 있는 버튼에는 aria-label을 제공한다.

```tsx
aria-label={isWished ? "찜 해제" : "찜하기"}
```

---

### 10.3 하단 네비게이션

활성 메뉴:

```tsx
className="text-[#d99a00]"
```

비활성 메뉴:

```tsx
className="text-gray-400"
```

하단 네비게이션이 본문을 가리지 않도록 페이지 하단에 여백을 둔다.

```tsx
className="pb-24"
```

---

### 10.4 로딩 상태

권장 문구:

```txt
꿀맛집을 불러오는 중이에요...
맛집 정보를 불러오는 중이에요...
랭킹을 불러오는 중이에요...
찜한 맛집을 불러오는 중이에요...
내 정보를 불러오는 중이에요...
```

---

### 10.5 빈 상태

권장 문구:

```txt
아직 보여줄 맛집이 없어요.
아직 찜한 맛집이 없어요.
마음에 드는 맛집을 꿀단지에 담아보세요.
아직 랭킹에 올라온 맛집이 없어요.
```

---

### 10.6 에러 상태

권장 문구:

```txt
맛집 정보를 불러오지 못했어요.
랭킹 정보를 불러오지 못했어요.
찜 목록을 불러오지 못했어요.
내 정보를 불러오지 못했어요.
잠시 후 다시 시도해주세요.
```

버튼:

```txt
다시 불러오기
```

---

## 11. 한국어 UI 문구 규칙

Codex는 UI 문구를 기본적으로 한국어로 유지한다.

### 11.1 권장 문구

```txt
오늘은 어디서 꿀맛을 찾을까요?
우리 동네 꿀맛집을 모아봤어요.
동네 사람들이 자주 찾는 곳이에요.
마음에 드는 맛집을 꿀단지에 담아보세요.
아직 찜한 맛집이 없어요.
꿀맛집을 불러오는 중이에요...
맛집 정보를 불러오지 못했어요.
잠시 후 다시 시도해주세요.
다시 불러오기
```

---

### 11.2 피해야 할 문구

```txt
Home
Main Page
Submit
Cancel
Loading
Error
No Data
Restaurant Detail
Ranking List
User Page
```

---

## 12. 작업 방식

Codex는 매 루프마다 다음 순서로 작업한다.

```txt
1. 필수 문서를 읽는다.
2. 현재 코드 구조를 파악한다.
3. 현재 구현된 기능을 확인한다.
4. 이번에 개선할 가장 작은 UI/UX 단위를 선택한다.
5. 변경 전 문제점을 간단히 정리한다.
6. 변경 범위를 명확히 정한다.
7. 코드를 수정한다.
8. 기존 기능이 깨지지 않았는지 확인한다.
9. 가능한 경우 빌드 또는 타입 체크를 실행한다.
10. docs/PROGRESS.md에 변경 사항을 기록한다.
11. 다음 추천 작업을 제안한다.
```

---

## 13. 확인 명령어

프로젝트 구조에 맞게 가능한 명령어를 사용한다.

프론트엔드:

```bash
npm run build
```

또는:

```bash
npm run typecheck
```

또는:

```bash
npm run lint
```

백엔드까지 건드린 경우:

```bash
./gradlew clean build -x test
```

단, 이번 UI/UX 리팩토링에서는 백엔드 코드를 불필요하게 수정하지 않는다.

---

## 14. docs/PROGRESS.md 기록 규칙

Codex는 작업 후 `docs/PROGRESS.md`에 변경 사항을 기록한다.

기록 형식 예시:

```md
### YYYY-MM-DD UI/UX 리팩토링

#### 작업 범위
- Home 상단 영역 문구 및 여백 정리

#### 변경 파일
- src/pages/HomePage.tsx
- src/components/PageHeader.tsx

#### 변경 내용
- Honeytong 톤에 맞는 상단 문구 적용
- 모바일 기준 여백 정리
- 기존 API 호출과 라우팅 유지

#### 확인 결과
- npm run build 성공
- 상세 이동 유지 확인
- 찜 버튼 동작 유지 확인

#### 다음 작업
- RestaurantCard 스타일 정리
```

---

## 15. 첫 실행용 프롬프트

아래 프롬프트는 Codex에 처음 넣는 용도다.  
문서 생성/확인과 현재 UI 구조 점검까지만 진행하도록 한다.

```md
Honeytong 프로젝트 UI/UX 리팩토링 준비 작업을 시작합니다.

먼저 아래 경로에 UI/UX Harness 문서가 있는지 확인하세요.

docs/ui-ux/design-system.md
docs/ui-ux/ux-rules.md
docs/ui-ux/ui-components.md
docs/ui-ux/screen-specs.md
docs/ui-ux/interaction-rules.md
docs/ui-ux/codex-ui-loop-prompt.md

없으면 생성하고, 있으면 내용을 읽고 현재 프로젝트 상태에 맞게 보완하세요.

목표:
- 기능 구현이 아니라 UI/UX 리팩토링 기준 문서 세팅
- Honeytong 벌/꿀 테마 디자인 기준 정리
- 모바일 우선 393x852 기준 정리
- Home → Detail → Ranking → Wishlist → MyPage 순서 리팩토링 기준 정리
- 기존 API, 라우팅, 찜 상태, 상세 이동 기능은 절대 변경하지 않기

작업 방식:
1. docs/AGENT.md, PRD.md, ARCHITECTURE.md, TASKS.md, PROGRESS.md를 먼저 읽으세요.
2. docs/ui-ux 문서가 없으면 생성하세요.
3. 기존 화면과 컴포넌트 구조를 점검하세요.
4. 아직 실제 UI 코드는 크게 수정하지 마세요.
5. 마지막에 다음 UI 리팩토링 1순위를 추천하세요.
6. docs/PROGRESS.md에 이번 작업 내용을 기록하세요.

완료 후 다음 형식으로 보고하세요.

### UI/UX Harness 준비 결과

#### 생성/수정한 문서
- ...

#### 확인한 현재 UI 구조
- ...

#### 지켜야 할 핵심 기준
- ...

#### 다음 추천 작업
- ...

#### 주의할 점
- ...
```

---

## 16. Home UI/UX 리팩토링 1차 프롬프트

문서 준비가 끝난 뒤 Codex에 넣을 첫 번째 실제 UI 리팩토링 프롬프트다.  
범위는 Home 전체가 아니라 **공통 Layout + Home 상단 영역 + 간격 정리** 정도로 제한한다.

```md
Honeytong UI/UX 리팩토링 1차 작업을 진행합니다.

이번 작업 범위는 Home 화면의 전체 구조를 크게 갈아엎는 것이 아니라, 모바일 앱처럼 보이도록 레이아웃과 상단 영역을 정리하는 것입니다.

반드시 먼저 읽을 문서:
- docs/AGENT.md
- docs/PRD.md
- docs/ARCHITECTURE.md
- docs/TASKS.md
- docs/PROGRESS.md
- docs/ui-ux/design-system.md
- docs/ui-ux/ux-rules.md
- docs/ui-ux/ui-components.md
- docs/ui-ux/screen-specs.md
- docs/ui-ux/interaction-rules.md
- docs/ui-ux/codex-ui-loop-prompt.md

이번 작업 목표:
1. 앱 전체가 모바일 웹앱처럼 보이도록 max-w-[430px] 기준을 적용합니다.
2. Home 화면 상단 문구를 Honeytong 톤으로 정리합니다.
3. Home 화면의 section 간격, padding, 카드 여백을 정리합니다.
4. 기존 API 호출, 라우팅, 찜 상태, 상세 이동은 절대 깨뜨리지 않습니다.
5. mock 데이터를 새로 만들지 않습니다.
6. 하단 네비게이션과 본문이 겹치지 않도록 pb-24 등을 확인합니다.

디자인 기준:
- 기본 배경: #fffaf0
- 카드 배경: white
- 핵심 꿀색: #f6b800
- 텍스트: #2b210f
- rounded-3xl 중심
- shadow-sm 이하의 약한 그림자
- 과한 장식 금지
- 한국어 UI 유지

주의:
- 한 번에 Detail, Ranking, Wishlist, MyPage까지 수정하지 마세요.
- Home 화면과 공통 Layout에 필요한 최소 변경만 하세요.
- 기존 컴포넌트 props를 함부로 삭제하지 마세요.
- TypeScript 오류 없이 유지하세요.

가능하면 다음 중 필요한 파일만 수정하세요.
- App.tsx 또는 라우터 관련 파일
- 공통 Layout/AppShell 파일
- Home 페이지 파일
- Home에서 사용하는 카드/섹션 컴포넌트
- BottomNavigation이 겹치는 경우에만 최소 수정

완료 후 다음 형식으로 보고하세요.

### Home UI/UX 리팩토링 1차 결과

#### 1. 변경한 파일
- ...

#### 2. 변경 내용
- ...

#### 3. 유지한 기능
- API 연결:
- 상세 이동:
- 찜 상태:
- 하단 네비게이션:

#### 4. 확인 결과
- 빌드:
- 타입 체크:
- 모바일 화면 기준:

#### 5. 다음 추천 작업
- ...
```

---

## 17. RestaurantCard 리팩토링 프롬프트

Home 1차 작업 후 카드 완성도를 높일 때 사용한다.

```md
Honeytong UI/UX 리팩토링 작업을 이어서 진행합니다.

이번 작업 범위는 RestaurantCard 컴포넌트 정리입니다.

반드시 먼저 읽을 문서:
- docs/AGENT.md
- docs/PRD.md
- docs/ARCHITECTURE.md
- docs/TASKS.md
- docs/PROGRESS.md
- docs/ui-ux/design-system.md
- docs/ui-ux/ux-rules.md
- docs/ui-ux/ui-components.md
- docs/ui-ux/screen-specs.md
- docs/ui-ux/interaction-rules.md
- docs/ui-ux/codex-ui-loop-prompt.md

이번 작업 목표:
1. RestaurantCard를 Honeytong 디자인 시스템에 맞게 정리합니다.
2. 카드 스타일을 rounded-3xl, bg-white, shadow-sm 중심으로 정리합니다.
3. 대표 이미지는 aspect-[4/3], rounded-2xl, object-cover 기준으로 정리합니다.
4. 맛집 이름, 지역, 카테고리, 설명, 추천 수, 찜 상태가 보기 좋게 정리되도록 합니다.
5. 카드 클릭 시 기존 Detail 이동이 유지되어야 합니다.
6. 찜 버튼 클릭 시 Detail 이동이 발생하지 않아야 합니다.
7. 기존 props와 API 응답 구조를 함부로 변경하지 않습니다.
8. mock 데이터를 새로 만들지 않습니다.

주의:
- Home, Ranking, Wishlist 전체를 한 번에 수정하지 마세요.
- RestaurantCard와 직접 관련된 최소 파일만 수정하세요.
- 기존 라우팅 경로를 변경하지 마세요.
- TypeScript 오류 없이 유지하세요.
- 한국어 UI 문구를 유지하세요.

완료 후 다음 형식으로 보고하세요.

### RestaurantCard UI/UX 리팩토링 결과

#### 1. 변경한 파일
- ...

#### 2. 변경 내용
- ...

#### 3. 유지한 기능
- 카드 클릭 상세 이동:
- 찜 버튼 동작:
- API 데이터 표시:
- 기존 props:

#### 4. 확인 결과
- 빌드:
- 타입 체크:
- 모바일 화면 기준:

#### 5. 다음 추천 작업
- ...
```

---

## 18. BottomNavigation 리팩토링 프롬프트

하단 네비게이션을 정리할 때 사용한다.

```md
Honeytong UI/UX 리팩토링 작업을 이어서 진행합니다.

이번 작업 범위는 BottomNavigation 정리입니다.

반드시 먼저 읽을 문서:
- docs/AGENT.md
- docs/PRD.md
- docs/ARCHITECTURE.md
- docs/TASKS.md
- docs/PROGRESS.md
- docs/ui-ux/design-system.md
- docs/ui-ux/ux-rules.md
- docs/ui-ux/ui-components.md
- docs/ui-ux/screen-specs.md
- docs/ui-ux/interaction-rules.md
- docs/ui-ux/codex-ui-loop-prompt.md

이번 작업 목표:
1. 하단 네비게이션을 모바일 앱처럼 max-w-[430px] 기준 하단 고정으로 정리합니다.
2. 메뉴는 홈 / 랭킹 / 찜 / 마이를 유지합니다.
3. 현재 경로에 해당하는 메뉴를 꿀색 계열로 명확히 활성화합니다.
4. 비활성 메뉴는 gray 계열로 표시합니다.
5. 본문이 하단 네비게이션에 가려지지 않도록 페이지 하단 padding을 확인합니다.
6. 기존 라우팅 경로를 절대 변경하지 않습니다.
7. 기존 페이지 구조를 크게 변경하지 않습니다.

디자인 기준:
- bg-white
- border-t border-gray-200
- max-w-[430px]
- fixed bottom-0 left-1/2 -translate-x-1/2
- 활성 메뉴 text-[#d99a00]
- 비활성 메뉴 text-gray-400
- 아이콘과 텍스트 함께 표시
- 터치 영역 충분히 확보

주의:
- 메뉴 이름을 영어로 변경하지 마세요.
- 기존 라우팅 경로를 변경하지 마세요.
- 하단 네비게이션을 삭제하지 마세요.
- 전체 화면을 한 번에 수정하지 마세요.

완료 후 다음 형식으로 보고하세요.

### BottomNavigation UI/UX 리팩토링 결과

#### 1. 변경한 파일
- ...

#### 2. 변경 내용
- ...

#### 3. 유지한 기능
- 홈 이동:
- 랭킹 이동:
- 찜 이동:
- 마이 이동:
- 현재 경로 활성화:

#### 4. 확인 결과
- 빌드:
- 타입 체크:
- 모바일 화면 기준:
- 본문 겹침 여부:

#### 5. 다음 추천 작업
- ...
```

---

## 19. Detail 화면 리팩토링 1차 프롬프트

Home과 카드 정리 후 Detail 화면을 정리할 때 사용한다.

```md
Honeytong UI/UX 리팩토링 작업을 이어서 진행합니다.

이번 작업 범위는 Detail 화면 1차 정리입니다.

반드시 먼저 읽을 문서:
- docs/AGENT.md
- docs/PRD.md
- docs/ARCHITECTURE.md
- docs/TASKS.md
- docs/PROGRESS.md
- docs/ui-ux/design-system.md
- docs/ui-ux/ux-rules.md
- docs/ui-ux/ui-components.md
- docs/ui-ux/screen-specs.md
- docs/ui-ux/interaction-rules.md
- docs/ui-ux/codex-ui-loop-prompt.md

이번 작업 목표:
1. Detail 화면의 상단 대표 이미지 영역을 정리합니다.
2. 뒤로가기 버튼이 명확히 보이도록 정리합니다.
3. 찜 버튼 위치와 상태 표현을 정리합니다.
4. 맛집 이름, 지역, 카테고리, 거리, 추천 수가 보기 좋게 정리되도록 합니다.
5. 상세 정보 섹션을 카드 형태로 구분합니다.
6. 없는 데이터에 대한 fallback 문구를 추가하거나 정리합니다.
7. 기존 API 호출과 라우팅을 절대 변경하지 않습니다.
8. 기존 찜 기능을 깨뜨리지 않습니다.

디자인 기준:
- 기본 배경 #fffaf0
- 상단 이미지 aspect-[16/10]
- 정보 카드 rounded-3xl bg-white p-4 shadow-sm
- 버튼 rounded-full
- 한국어 UI 문구 유지
- 과한 shadow 금지

주의:
- Detail 화면 외 다른 화면을 크게 수정하지 마세요.
- 백엔드 API 구조를 변경하지 마세요.
- mock 데이터를 새로 만들지 마세요.
- 미구현 기능은 활성 버튼처럼 만들지 마세요.

완료 후 다음 형식으로 보고하세요.

### Detail UI/UX 리팩토링 1차 결과

#### 1. 변경한 파일
- ...

#### 2. 변경 내용
- ...

#### 3. 유지한 기능
- API 상세 조회:
- 뒤로가기:
- 찜 상태:
- 기존 라우팅:

#### 4. 확인 결과
- 빌드:
- 타입 체크:
- 모바일 화면 기준:
- 없는 데이터 fallback:

#### 5. 다음 추천 작업
- ...
```

---

## 20. Ranking 화면 리팩토링 1차 프롬프트

```md
Honeytong UI/UX 리팩토링 작업을 이어서 진행합니다.

이번 작업 범위는 Ranking 화면 1차 정리입니다.

반드시 먼저 읽을 문서:
- docs/AGENT.md
- docs/PRD.md
- docs/ARCHITECTURE.md
- docs/TASKS.md
- docs/PROGRESS.md
- docs/ui-ux/design-system.md
- docs/ui-ux/ux-rules.md
- docs/ui-ux/ui-components.md
- docs/ui-ux/screen-specs.md
- docs/ui-ux/interaction-rules.md
- docs/ui-ux/codex-ui-loop-prompt.md

이번 작업 목표:
1. Ranking 화면 상단 제목과 설명 문구를 Honeytong 톤으로 정리합니다.
2. 동네 / 구 / 시 필터 UI를 정리합니다.
3. 선택된 필터가 명확히 보이도록 합니다.
4. Top 3 영역이 있다면 적당히 강조하되 과한 게임 UI처럼 만들지 않습니다.
5. 랭킹 카드에서 순위, 맛집 이름, 지역, 추천 수가 잘 보이도록 합니다.
6. 카드 클릭 시 기존 Detail 이동이 유지되어야 합니다.
7. loading / empty / error 상태를 정리합니다.
8. 기존 API 연결을 변경하지 않습니다.

디자인 기준:
- 기본 배경 #fffaf0
- 카드 bg-white rounded-3xl shadow-sm
- 선택 필터 bg-[#f6b800] text-[#2b210f]
- 과한 왕관/게임 효과 금지
- 한국어 UI 유지

주의:
- Ranking 화면과 직접 관련 없는 파일은 수정하지 마세요.
- 라우팅 경로를 바꾸지 마세요.
- mock 데이터를 새로 만들지 마세요.
- 미구현 필터는 실제 기능처럼 보이게 만들지 마세요.

완료 후 다음 형식으로 보고하세요.

### Ranking UI/UX 리팩토링 1차 결과

#### 1. 변경한 파일
- ...

#### 2. 변경 내용
- ...

#### 3. 유지한 기능
- 랭킹 API:
- 필터 상태:
- 카드 상세 이동:
- 하단 네비게이션:

#### 4. 확인 결과
- 빌드:
- 타입 체크:
- 모바일 화면 기준:
- 상태 처리:

#### 5. 다음 추천 작업
- ...
```

---

## 21. Wishlist 화면 리팩토링 1차 프롬프트

```md
Honeytong UI/UX 리팩토링 작업을 이어서 진행합니다.

이번 작업 범위는 Wishlist 화면 1차 정리입니다.

반드시 먼저 읽을 문서:
- docs/AGENT.md
- docs/PRD.md
- docs/ARCHITECTURE.md
- docs/TASKS.md
- docs/PROGRESS.md
- docs/ui-ux/design-system.md
- docs/ui-ux/ux-rules.md
- docs/ui-ux/ui-components.md
- docs/ui-ux/screen-specs.md
- docs/ui-ux/interaction-rules.md
- docs/ui-ux/codex-ui-loop-prompt.md

이번 작업 목표:
1. Wishlist 화면 제목과 설명 문구를 Honeytong 톤으로 정리합니다.
2. 찜 개수 요약 영역을 정리하거나 추가합니다.
3. 찜한 맛집 리스트는 RestaurantCard를 최대한 재사용합니다.
4. 찜 해제 버튼 클릭 시 카드 상세 이동이 발생하지 않도록 확인합니다.
5. 찜 목록이 비었을 때 EmptyState를 친근하게 보여줍니다.
6. loading / error 상태를 정리합니다.
7. 기존 찜 API와 라우팅을 변경하지 않습니다.

디자인 기준:
- 제목 예시: 내 꿀단지
- 설명 예시: 마음에 담아둔 맛집을 다시 확인해보세요.
- Empty 문구: 아직 찜한 맛집이 없어요. 마음에 드는 맛집을 꿀단지에 담아보세요.
- 카드 bg-white rounded-3xl shadow-sm
- 기본 배경 #fffaf0
- 한국어 UI 유지

주의:
- 찜 API 구조를 변경하지 마세요.
- 찜 해제 기능을 삭제하지 마세요.
- mock 데이터를 새로 만들지 마세요.
- Home이나 Detail을 크게 수정하지 마세요.

완료 후 다음 형식으로 보고하세요.

### Wishlist UI/UX 리팩토링 1차 결과

#### 1. 변경한 파일
- ...

#### 2. 변경 내용
- ...

#### 3. 유지한 기능
- 찜 목록 조회:
- 찜 해제:
- 카드 상세 이동:
- 하단 네비게이션:

#### 4. 확인 결과
- 빌드:
- 타입 체크:
- 모바일 화면 기준:
- EmptyState:

#### 5. 다음 추천 작업
- ...
```

---

## 22. MyPage 화면 리팩토링 1차 프롬프트

```md
Honeytong UI/UX 리팩토링 작업을 이어서 진행합니다.

이번 작업 범위는 MyPage 화면 1차 정리입니다.

반드시 먼저 읽을 문서:
- docs/AGENT.md
- docs/PRD.md
- docs/ARCHITECTURE.md
- docs/TASKS.md
- docs/PROGRESS.md
- docs/ui-ux/design-system.md
- docs/ui-ux/ux-rules.md
- docs/ui-ux/ui-components.md
- docs/ui-ux/screen-specs.md
- docs/ui-ux/interaction-rules.md
- docs/ui-ux/codex-ui-loop-prompt.md

이번 작업 목표:
1. MyPage 화면 제목과 설명 문구를 Honeytong 톤으로 정리합니다.
2. 프로필 요약 카드를 정리합니다.
3. 닉네임, 동네 정보, 동네 인증 상태가 보기 좋게 표시되도록 합니다.
4. 내 활동 요약 영역을 카드 형태로 정리합니다.
5. 메뉴 리스트를 과하지 않게 정리합니다.
6. 없는 데이터에 대한 fallback 문구를 제공합니다.
7. loading / error / 비로그인 상태가 있다면 정리합니다.
8. 기존 API, 인증 흐름, 라우팅을 변경하지 않습니다.

디자인 기준:
- 제목: 마이페이지
- 설명 예시: 내 활동과 동네 인증 상태를 확인해보세요.
- 프로필 문구 예시: 꿀벌님, 우리 동네 꿀맛집을 찾는 중이에요.
- 카드 bg-white rounded-3xl shadow-sm
- 기본 배경 #fffaf0
- 과한 게임 UI 금지
- 한국어 UI 유지

주의:
- 인증 로직을 변경하지 마세요.
- 로그아웃 기능을 삭제하지 마세요.
- mock 데이터를 새로 만들지 마세요.
- 다른 화면을 크게 수정하지 마세요.

완료 후 다음 형식으로 보고하세요.

### MyPage UI/UX 리팩토링 1차 결과

#### 1. 변경한 파일
- ...

#### 2. 변경 내용
- ...

#### 3. 유지한 기능
- 내 정보 조회:
- 인증 상태:
- 로그아웃:
- 하단 네비게이션:

#### 4. 확인 결과
- 빌드:
- 타입 체크:
- 모바일 화면 기준:
- fallback 처리:

#### 5. 다음 추천 작업
- ...
```

---

## 23. 매 루프 완료 보고 형식

Codex는 모든 UI/UX 루프 완료 후 아래 형식으로 보고한다.

```md
### 이번 UI/UX 루프 결과

#### 1. 읽은 문서
- docs/AGENT.md
- docs/PRD.md
- docs/ARCHITECTURE.md
- docs/TASKS.md
- docs/PROGRESS.md
- docs/ui-ux/design-system.md
- docs/ui-ux/ux-rules.md
- docs/ui-ux/ui-components.md
- docs/ui-ux/screen-specs.md
- docs/ui-ux/interaction-rules.md
- docs/ui-ux/codex-ui-loop-prompt.md

#### 2. 선택한 작업 단위
- ...

#### 3. 변경한 파일
- ...

#### 4. 변경 내용
- ...

#### 5. 유지한 기능
- API 연결:
- 라우팅:
- 찜 동작:
- 상세 이동:
- 하단 네비게이션:

#### 6. 확인 결과
- 빌드:
- 타입 체크:
- 모바일 화면:
- 주요 동작:

#### 7. docs/PROGRESS.md 기록
- 기록 여부:
- 기록 내용 요약:

#### 8. 다음 추천 작업
- ...
```

---

## 24. 최종 운영 원칙

Honeytong UI/UX 리팩토링은 아래 흐름으로 운영한다.

```txt
문서 먼저 읽기
↓
현재 코드 확인
↓
작은 작업 단위 선택
↓
기존 기능 보존
↓
UI/UX만 개선
↓
빌드/타입 체크
↓
PROGRESS.md 기록
↓
다음 작은 작업 추천
```

절대 다음처럼 요청하지 않는다.

```txt
전체 UI 예쁘게 다 바꿔줘
```

대신 다음처럼 작게 반복한다.

```txt
이번에는 Home 상단 영역만 정리해줘.
이번에는 RestaurantCard만 정리해줘.
이번에는 BottomNavigation만 정리해줘.
이번에는 Detail 상단 이미지 영역만 정리해줘.
```

Honeytong UI/UX 리팩토링의 핵심은 **작게, 안전하게, 일관되게**다.