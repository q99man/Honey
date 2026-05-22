# Honeytong UI Components

## 1. 문서 목적

이 문서는 Honeytong 프로젝트에서 반복적으로 사용하는 UI 컴포넌트의 기준을 정의한다.

Codex는 UI/UX 리팩토링을 진행할 때 이 문서를 기준으로 기존 컴포넌트를 정리하고, 필요한 경우 새 컴포넌트를 작게 추가한다.

중요한 원칙은 다음과 같다.

- 기존 기능을 깨지 않는다.
- 기존 API 연결을 깨지 않는다.
- 기존 라우팅을 깨지 않는다.
- mock 데이터를 새로 만들지 않는다.
- 같은 UI를 여러 곳에 중복 구현하지 않는다.
- 작은 단위로 컴포넌트를 정리한다.

---

## 2. 컴포넌트 공통 원칙

### 2.1 모바일 우선

모든 컴포넌트는 모바일 화면을 먼저 기준으로 만든다.

기준 화면:

```txt
393px x 852px
```

앱 최대 폭:

```txt
430px
```

컴포넌트는 좁은 화면에서 먼저 보기 좋아야 한다.

---

### 2.2 재사용 우선

같은 역할의 UI는 가능한 한 같은 컴포넌트를 사용한다.

예시:

- Home 맛집 카드
- Ranking 맛집 카드
- Wishlist 맛집 카드

이 3개가 완전히 다른 구조가 되면 안 된다.  
가능하면 `RestaurantCard`를 공통으로 사용하고, 필요한 부분만 props로 제어한다.

---

### 2.3 Props 안정성 유지

Codex는 기존 props를 함부로 삭제하면 안 된다.

가능한 방식:

- 기존 props 유지
- 필요한 optional props 추가
- 타입 명확히 작성
- 사용하지 않는 props는 바로 삭제하지 말고 영향 범위 확인

피해야 할 방식:

- props 이름을 갑자기 변경
- API 응답 타입과 맞지 않게 변경
- 기존 페이지에서 쓰는 props 삭제
- `any`로 대충 처리

---

### 2.4 한국어 UI 유지

컴포넌트 내부 문구는 기본적으로 한국어를 사용한다.

권장:

```txt
맛집 둘러보기
다시 불러오기
찜한 맛집
우리 동네 꿀맛집
```

피해야 할 문구:

```txt
Submit
Cancel
Loading
Error
No Data
```

---

### 2.5 Tailwind 기준

Honeytong은 Tailwind CSS 기반으로 UI를 정리한다.

권장:

```tsx
className="rounded-3xl bg-white p-4 shadow-sm"
```

피해야 할 것:

- 불필요한 CSS 파일 추가
- inline style 남발
- 같은 className 반복 복붙
- 사용하지 않는 UI 라이브러리 추가

---

## 3. Layout Components

### 3.1 AppShell

#### 목적

앱 전체를 모바일 웹앱처럼 중앙에 배치하는 공통 레이아웃이다.

#### 사용 위치

모든 주요 페이지에서 사용할 수 있다.

- Home
- Detail
- Ranking
- Wishlist
- MyPage

#### 권장 코드

```tsx
type AppShellProps = {
  children: React.ReactNode;
};

export function AppShell({ children }: AppShellProps) {
  return (
    <div className="min-h-screen bg-neutral-100">
      <main className="mx-auto min-h-screen max-w-[430px] bg-[#fffaf0] pb-24">
        {children}
      </main>
    </div>
  );
}
```

#### 규칙

- 페이지 전체를 감싸는 역할만 한다.
- API 호출이나 비즈니스 로직을 넣지 않는다.
- 데스크톱에서도 앱이 중앙에 배치되게 한다.
- 하단 네비게이션에 가리지 않도록 `pb-24`를 기본으로 고려한다.

---

### 3.2 PageContainer

#### 목적

각 페이지의 내부 여백과 섹션 간격을 정리한다.

#### 권장 코드

```tsx
type PageContainerProps = {
  children: React.ReactNode;
  className?: string;
};

export function PageContainer({ children, className = "" }: PageContainerProps) {
  return (
    <div className={`px-4 pt-5 ${className}`}>
      {children}
    </div>
  );
}
```

#### 권장 사용

```tsx
<PageContainer className="space-y-6">
  ...
</PageContainer>
```

#### 규칙

- 페이지 좌우 여백은 기본 `px-4`를 사용한다.
- 섹션 간격은 `space-y-6` 정도를 기본으로 한다.
- 페이지마다 여백이 제각각이 되지 않게 한다.

---

### 3.3 BottomNavigation

#### 목적

주요 페이지 간 이동을 담당하는 하단 네비게이션이다.

#### 메뉴

```txt
홈
랭킹
찜
마이
```

#### 권장 구조

```tsx
type NavItem = {
  label: string;
  path: string;
  icon: React.ReactNode;
};

type BottomNavigationProps = {
  items: NavItem[];
  currentPath: string;
  onNavigate: (path: string) => void;
};
```

#### 권장 스타일

```tsx
className="fixed bottom-0 left-1/2 z-50 w-full max-w-[430px] -translate-x-1/2 border-t border-gray-200 bg-white px-4 py-2"
```

#### 활성 상태

활성 메뉴:

```tsx
text-[#d99a00]
```

비활성 메뉴:

```tsx
text-gray-400
```

#### 규칙

- 현재 경로와 일치하는 메뉴를 활성화한다.
- 아이콘과 텍스트를 함께 표시한다.
- 터치 영역은 충분히 크게 한다.
- 본문이 하단 네비게이션에 가려지지 않도록 한다.
- 라우팅 경로를 임의로 변경하지 않는다.

---

## 4. Header Components

### 4.1 PageHeader

#### 목적

화면 상단에서 현재 페이지의 목적을 알려준다.

#### 사용 위치

- Home
- Ranking
- Wishlist
- MyPage
- Detail 일부 영역

#### Props

```tsx
type PageHeaderProps = {
  title: string;
  description?: string;
  rightSlot?: React.ReactNode;
};
```

#### 권장 코드

```tsx
export function PageHeader({ title, description, rightSlot }: PageHeaderProps) {
  return (
    <header className="flex items-start justify-between gap-4">
      <div>
        <h1 className="text-xl font-bold text-[#2b210f]">{title}</h1>
        {description && (
          <p className="mt-1 text-sm leading-5 text-gray-600">
            {description}
          </p>
        )}
      </div>
      {rightSlot && <div className="shrink-0">{rightSlot}</div>}
    </header>
  );
}
```

#### 문구 예시

Home:

```txt
오늘은 어디서 꿀맛을 찾을까요?
우리 동네 사람들이 추천한 맛집을 모아봤어요.
```

Ranking:

```txt
우리 동네 꿀맛집 랭킹
동네 사람들의 활동을 바탕으로 정리했어요.
```

Wishlist:

```txt
내 꿀단지
마음에 담아둔 맛집을 다시 확인해보세요.
```

MyPage:

```txt
마이페이지
내 활동과 동네 인증 상태를 확인해보세요.
```

---

### 4.2 DetailHeader

#### 목적

상세 화면의 상단 이미지, 뒤로가기 버튼, 찜 버튼을 관리한다.

#### 포함 요소

- 대표 이미지
- 뒤로가기 버튼
- 찜 버튼
- 이미지 fallback

#### 권장 구조

```tsx
type DetailHeaderProps = {
  imageUrl?: string;
  title: string;
  isWished?: boolean;
  onBack: () => void;
  onToggleWish?: () => void;
};
```

#### 규칙

- 뒤로가기 버튼은 항상 있어야 한다.
- 찜 버튼 클릭 시 뒤로가기나 상세 이동과 충돌하면 안 된다.
- 이미지가 없을 때 fallback UI를 제공한다.
- 이미지 위의 버튼은 충분히 잘 보여야 한다.

---

## 5. Search / Filter Components

### 5.1 SearchBar

#### 목적

맛집, 메뉴, 지역 검색 진입점이다.

#### Props

```tsx
type SearchBarProps = {
  value?: string;
  placeholder?: string;
  disabled?: boolean;
  onChange?: (value: string) => void;
  onSubmit?: () => void;
  onClick?: () => void;
};
```

#### 기본 placeholder

```txt
동네 맛집이나 메뉴를 검색해보세요
```

#### 권장 스타일

```tsx
className="flex h-12 items-center gap-2 rounded-2xl border border-gray-200 bg-white px-4 text-sm text-gray-700 shadow-sm"
```

#### 규칙

- 검색 기능이 없으면 활성 검색처럼 보이게 만들지 않는다.
- 미구현 상태라면 클릭 시 안내 문구를 보여준다.
- 입력창 높이는 최소 48px 정도를 유지한다.

---

### 5.2 CategoryChips

#### 목적

카테고리 또는 필터를 빠르게 선택하는 컴포넌트이다.

#### Props

```tsx
type CategoryChip = {
  label: string;
  value: string;
};

type CategoryChipsProps = {
  items: CategoryChip[];
  selectedValue: string;
  onSelect: (value: string) => void;
};
```

#### 카테고리 예시

```txt
전체
한식
분식
카페
디저트
중식
일식
양식
혼밥
가성비
```

#### 권장 스타일

선택 전:

```tsx
className="rounded-full border border-gray-200 bg-white px-4 py-2 text-sm font-medium text-gray-600"
```

선택 후:

```tsx
className="rounded-full bg-[#f6b800] px-4 py-2 text-sm font-semibold text-[#2b210f]"
```

#### 규칙

- 가로 스크롤을 허용한다.
- 선택된 칩은 명확히 보여야 한다.
- 터치 영역은 충분히 커야 한다.
- 선택 시 결과가 없으면 EmptyState를 보여준다.

---

### 5.3 FilterTabs

#### 목적

랭킹 화면 등에서 지역 단위나 정렬 기준을 선택한다.

#### 예시

```txt
동네
구
시
```

#### Props

```tsx
type FilterTab = {
  label: string;
  value: string;
};

type FilterTabsProps = {
  items: FilterTab[];
  selectedValue: string;
  onSelect: (value: string) => void;
};
```

#### 규칙

- 선택된 탭은 꿀색 또는 진한 텍스트로 강조한다.
- 탭 전환 시 결과가 즉시 반영되어야 한다.
- 미구현 필터는 활성화하지 않는다.

---

## 6. Card Components

### 6.1 RestaurantCard

#### 목적

맛집 목록에서 가장 많이 사용하는 핵심 카드이다.

#### 사용 위치

- Home
- Ranking 일부
- Wishlist
- 검색 결과
- 추천 맛집 섹션

#### Props

```tsx
type RestaurantCardProps = {
  id: number | string;
  name: string;
  description?: string;
  imageUrl?: string;
  area?: string;
  distance?: string;
  category?: string;
  honeyScore?: number;
  recommendCount?: number;
  visitCount?: number;
  isWished?: boolean;
  onClick?: () => void;
  onToggleWish?: () => void;
};
```

#### 카드에서 보여줄 정보

- 대표 이미지
- 맛집 이름
- 지역
- 거리
- 카테고리
- 한 줄 설명
- 추천 수 또는 꿀 점수
- 찜 버튼

#### 권장 스타일

카드 전체:

```tsx
className="rounded-3xl bg-white p-3 shadow-sm transition duration-200 active:scale-[0.98]"
```

이미지:

```tsx
className="aspect-[4/3] w-full rounded-2xl object-cover"
```

제목:

```tsx
className="line-clamp-1 text-base font-semibold text-[#2b210f]"
```

설명:

```tsx
className="line-clamp-2 text-sm leading-5 text-gray-600"
```

#### 이벤트 규칙

카드 전체 클릭:

```tsx
onClick={onClick}
```

찜 버튼 클릭:

```tsx
onClick={(event) => {
  event.stopPropagation();
  onToggleWish?.();
}}
```

찜 버튼 클릭 시 카드 상세 이동이 발생하면 안 된다.

#### fallback 규칙

이미지가 없을 때:

```txt
꿀맛집 이미지 준비 중
```

설명이 없을 때:

```txt
동네 사람들이 추천한 맛집이에요.
```

거리 정보가 없을 때는 무리하게 표시하지 않는다.

---

### 6.2 RankingCard

#### 목적

랭킹 화면에서 순위 정보를 강조해서 보여주는 카드이다.

#### Props

```tsx
type RankingCardProps = {
  rank: number;
  id: number | string;
  name: string;
  imageUrl?: string;
  area?: string;
  category?: string;
  recommendCount?: number;
  visitCount?: number;
  reason?: string;
  onClick?: () => void;
};
```

#### 포함 정보

- 순위
- 맛집 이미지
- 맛집 이름
- 지역
- 추천 수
- 방문 수
- 짧은 추천 이유

#### Top 3 표현

1~3위는 조금 더 강조할 수 있다.

예시 문구:

```txt
1위
2위
3위
```

또는:

```txt
오늘의 꿀랭킹
```

#### 규칙

- 순위는 명확하게 보여야 한다.
- 너무 게임처럼 과장하지 않는다.
- 맛집 정보가 순위 숫자보다 덜 보이면 안 된다.
- 카드 클릭 시 상세로 이동한다.
- 찜 버튼이 필요하다면 RestaurantCard와 같은 이벤트 규칙을 따른다.

---

### 6.3 SummaryCard

#### 목적

마이페이지나 홈 상단에서 요약 정보를 보여준다.

#### 사용 위치

- MyPage 활동 요약
- Home 오늘의 요약
- Ranking 요약

#### Props

```tsx
type SummaryCardProps = {
  label: string;
  value: string | number;
  description?: string;
  icon?: React.ReactNode;
};
```

#### 예시

```txt
찜한 맛집 12
추천한 맛집 5
방문 인증 3
```

#### 규칙

- 숫자는 크게 보여준다.
- 설명은 작게 보여준다.
- 카드 하나에 너무 많은 정보를 넣지 않는다.

---

### 6.4 ProfileSummaryCard

#### 목적

마이페이지 상단에서 사용자 정보를 보여준다.

#### Props

```tsx
type ProfileSummaryCardProps = {
  nickname?: string;
  areaName?: string;
  isVerified?: boolean;
  avatarUrl?: string;
};
```

#### 문구 예시

```txt
꿀벌 성오님
우리 동네 꿀맛집을 찾는 중이에요.
```

동네 인증 상태:

```txt
동네 인증 완료
동네 인증 필요
```

#### 규칙

- 프로필 정보가 없을 때 fallback을 제공한다.
- 사용자 이름이 없으면 “꿀벌님”으로 표시할 수 있다.
- 인증 상태는 명확히 표시한다.
- 너무 게임 캐릭터처럼 과하게 만들지 않는다.

---

## 7. State Components

### 7.1 EmptyState

#### 목적

데이터가 없을 때 친근한 안내와 다음 행동을 제공한다.

#### Props

```tsx
type EmptyStateProps = {
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
};
```

#### 기본 스타일

```tsx
className="rounded-3xl bg-white p-6 text-center shadow-sm"
```

#### 문구 예시

Wishlist:

```txt
아직 찜한 맛집이 없어요.
마음에 드는 맛집을 꿀단지에 담아보세요.
```

Home:

```txt
아직 보여줄 맛집이 없어요.
조금 뒤에 다시 확인해보세요.
```

Ranking:

```txt
아직 랭킹에 올라온 맛집이 없어요.
첫 번째 꿀맛집을 추천해보세요.
```

#### 규칙

- 빈 상태를 흰 화면으로 방치하지 않는다.
- 가능하면 다음 행동 버튼을 제공한다.
- 문구는 한국어로 작성한다.

---

### 7.2 LoadingState

#### 목적

API 호출 중 화면이 비어 보이지 않게 한다.

#### Props

```tsx
type LoadingStateProps = {
  message?: string;
};
```

#### 문구 예시

```txt
꿀맛집을 불러오는 중이에요...
```

#### 권장 방식

- Skeleton 카드
- 짧은 문구
- 버튼 내부 loading 상태

#### 피해야 할 것

```txt
Loading...
```

spinner만 화면 중앙에 크게 보여주는 방식은 최소화한다.

---

### 7.3 ErrorState

#### 목적

API 실패 시 사용자에게 친근하게 안내하고 복구 행동을 제공한다.

#### Props

```tsx
type ErrorStateProps = {
  title?: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
};
```

#### 문구 예시

```txt
맛집 정보를 불러오지 못했어요.
잠시 후 다시 시도해주세요.
```

버튼:

```txt
다시 불러오기
```

#### 규칙

- 에러를 사용자 탓처럼 표현하지 않는다.
- 가능하면 다시 시도 버튼을 제공한다.
- 영어 에러 문구를 그대로 노출하지 않는다.

---

### 7.4 SkeletonCard

#### 목적

카드 목록 로딩 중 자연스러운 대기 상태를 보여준다.

#### 권장 스타일

```tsx
className="animate-pulse rounded-3xl bg-white p-3 shadow-sm"
```

#### 구조

- 이미지 영역 skeleton
- 제목 영역 skeleton
- 설명 영역 skeleton

#### 규칙

- 너무 많은 skeleton을 표시하지 않는다.
- 보통 3~5개 정도면 충분하다.

---

## 8. Action Components

### 8.1 PrimaryButton

#### 목적

주요 행동을 수행하는 버튼이다.

#### Props

```tsx
type PrimaryButtonProps = {
  children: React.ReactNode;
  disabled?: boolean;
  loading?: boolean;
  onClick?: () => void;
};
```

#### 권장 스타일

```tsx
className="rounded-full bg-[#f6b800] px-5 py-3 text-sm font-semibold text-[#2b210f] transition duration-200 active:scale-[0.98] disabled:opacity-50"
```

#### 사용 위치

- 맛집 둘러보기
- 추천하기
- 방문 인증하기
- 저장하기
- 등록하기

---

### 8.2 SecondaryButton

#### 목적

보조 행동을 수행하는 버튼이다.

#### 권장 스타일

```tsx
className="rounded-full border border-gray-200 bg-white px-4 py-2 text-sm font-semibold text-gray-700"
```

#### 사용 위치

- 필터
- 정렬
- 취소
- 뒤로가기성 보조 행동

---

### 8.3 IconButton

#### 목적

아이콘 기반 액션 버튼이다.

#### Props

```tsx
type IconButtonProps = {
  label: string;
  icon: React.ReactNode;
  onClick?: () => void;
};
```

#### 권장 스타일

```tsx
className="flex h-11 w-11 items-center justify-center rounded-full bg-white shadow-sm transition duration-200 active:scale-[0.96]"
```

#### 규칙

- `aria-label`을 반드시 제공한다.
- 터치 영역은 최소 44px에 가깝게 유지한다.

---

### 8.4 WishButton

#### 목적

맛집 찜 상태를 토글한다.

#### Props

```tsx
type WishButtonProps = {
  isWished: boolean;
  onToggle: () => void;
};
```

#### 상태

찜 안 됨:

```txt
빈 하트 또는 회색 하트
```

찜 됨:

```txt
채워진 하트 또는 꿀색 강조
```

#### 이벤트 규칙

카드 내부에서 사용할 경우 반드시 이벤트 전파를 막는다.

```tsx
onClick={(event) => {
  event.stopPropagation();
  onToggle();
}}
```

#### 규칙

- 클릭 즉시 시각 상태가 바뀌어야 한다.
- API 실패 시 원래 상태 복구 또는 안내가 필요하다.
- 찜 버튼 클릭 시 상세 이동이 발생하면 안 된다.

---

## 9. Section Components

### 9.1 SectionTitle

#### 목적

화면 안의 섹션을 구분한다.

#### Props

```tsx
type SectionTitleProps = {
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
};
```

#### 예시 제목

```txt
오늘의 꿀맛집
우리 동네 인기 맛집
최근 뜨는 맛집
내 꿀단지
지역별 랭킹
```

#### 권장 스타일

제목:

```tsx
className="text-lg font-bold text-[#2b210f]"
```

설명:

```tsx
className="mt-1 text-sm text-gray-500"
```

##### 규칙

- 제목은 짧고 명확하게 작성한다.
- actionLabel이 있으면 오른쪽에 작게 배치한다.
- 한 화면에 섹션이 너무 많아지지 않게 한다.

---

### 9.2 InfoSection

#### 목적

상세 화면에서 정보를 섹션별로 구분한다.

#### 사용 위치

- 상세 설명
- 운영 정보
- 위치 정보
- 추천 이유
- 방문 인증 정보

#### Props

```tsx
type InfoSectionProps = {
  title: string;
  children: React.ReactNode;
};
```

#### 권장 스타일

```tsx
className="rounded-3xl bg-white p-4 shadow-sm"
```

#### 규칙

- 긴 정보를 한 카드에 몰아넣지 않는다.
- 섹션 제목을 명확히 표시한다.
- 없는 데이터는 fallback 문구를 제공한다.

---

## 10. Component Refactoring Order

Codex는 컴포넌트 리팩토링을 다음 순서로 진행한다.

1. AppShell
2. PageContainer
3. BottomNavigation
4. PageHeader
5. RestaurantCard
6. WishButton
7. CategoryChips
8. EmptyState
9. LoadingState
10. ErrorState
11. RankingCard
12. ProfileSummaryCard
13. InfoSection

한 번에 여러 컴포넌트를 크게 바꾸지 않는다.

---

## 11. Component Completion Checklist

컴포넌트를 수정하거나 새로 만들었다면 다음을 확인한다.

- [ ] 기존 기능이 유지되는가?
- [ ] 기존 라우팅이 유지되는가?
- [ ] 기존 API 연결이 유지되는가?
- [ ] TypeScript 타입 오류가 없는가?
- [ ] props 이름이 명확한가?
- [ ] 한국어 UI 문구가 유지되는가?
- [ ] 모바일 393x852 기준에서 보기 좋은가?
- [ ] 터치 영역이 충분한가?
- [ ] Tailwind class가 지나치게 중복되지 않는가?
- [ ] 같은 UI를 여러 곳에 중복 구현하지 않았는가?
- [ ] loading / empty / error 상태가 필요한 곳에 적용되어 있는가?
- [ ] 찜 버튼과 카드 클릭 이벤트가 충돌하지 않는가?

---

## 12. 금지 사항

Codex는 컴포넌트 리팩토링 중 다음을 하면 안 된다.

- 기존 API 응답 구조 임의 변경
- 기존 라우팅 경로 임의 변경
- 기존 props 삭제
- mock 데이터 재도입
- 전체 페이지를 한 번에 갈아엎기
- 컴포넌트 이름을 이유 없이 변경
- 한국어 문구를 영어로 변경
- 사용하지 않는 UI 라이브러리 추가
- 강한 shadow 사용
- 너무 작은 버튼 사용
- 카드 클릭과 찜 클릭이 충돌하는 코드 작성
- TypeScript 오류가 있는 상태로 종료