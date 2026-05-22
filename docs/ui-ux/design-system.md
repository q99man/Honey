# Honeytong Design System

## 1. 문서 목적

이 문서는 Honeytong 프로젝트의 UI 디자인 기준을 정의한다.

Honeytong은 벌과 꿀을 테마로 한 동네 맛집 탐색 웹앱이다.  
이 문서의 목적은 Codex가 UI/UX 리팩토링을 진행할 때 색상, 간격, 카드, 버튼, 타이포그래피, 레이아웃을 일관되게 유지하도록 만드는 것이다.

Codex는 UI를 수정할 때 반드시 이 문서를 기준으로 삼아야 한다.

---

## 2. Honeytong 디자인 방향

Honeytong의 UI는 다음 방향을 따른다.

- 벌/꿀 테마
- 노랑/검정 기반
- 따뜻하고 귀여운 동네 맛집 탐색 앱 느낌
- 모바일 우선 웹앱
- 카드형 UI
- 둥근 모서리
- 부드러운 여백
- 과하지 않은 그림자
- 친근한 한국어 문구
- 귀엽지만 너무 유치하지 않은 분위기

Honeytong은 카카오의 노랑/검정 조합을 참고할 수는 있지만, 카카오톡처럼 보이면 안 된다.

---

## 3. 디자인 키워드

Honeytong UI를 만들 때 항상 아래 키워드를 기준으로 판단한다.

```txt
따뜻함
동네 친화적
꿀맛집
귀여움
신뢰감
모바일 앱 같은 사용성
가벼움
부드러움
```

---

## 4. 피해야 할 디자인

다음과 같은 디자인은 피한다.

- 카카오톡처럼 보이는 강한 노랑 배경
- 검정과 노랑만 반복되는 단조로운 UI
- 너무 유치한 캐릭터 중심 UI
- 게임처럼 과도하게 화려한 랭킹 UI
- 과한 그림자
- 너무 작은 글씨
- 너무 좁은 터치 영역
- 의미 없는 장식이 많은 화면
- 영어 UI 문구 남발
- 기존 기능을 해치는 디자인 변경

---

## 5. 기본 화면 기준

Honeytong은 모바일 우선 웹앱이다.

기준 모바일 화면은 다음을 우선으로 한다.

```txt
393px x 852px
```

앱의 최대 폭은 다음을 기준으로 한다.

```txt
max-width: 430px
```

데스크톱에서도 화면이 너무 넓어지지 않고, 모바일 앱처럼 중앙에 배치되어야 한다.

권장 레이아웃 예시:

```tsx
<div className="min-h-screen bg-neutral-100">
  <main className="mx-auto min-h-screen max-w-[430px] bg-[#fffaf0] pb-24">
    {/* page content */}
  </main>
</div>
```

---

## 6. 컬러 시스템

### 6.1 Primary Color

Honeytong의 핵심 컬러는 꿀빛 노랑이다.

```css
--color-honey-50: #fff9e6;
--color-honey-100: #fff1bf;
--color-honey-200: #ffe680;
--color-honey-300: #ffd84d;
--color-honey-400: #ffc928;
--color-honey-500: #f6b800;
--color-honey-600: #d99a00;
--color-honey-700: #a66f00;
```

대표 컬러:

```txt
#f6b800
```

사용 위치:

- 주요 버튼
- 선택된 카테고리
- 활성화된 하단 네비게이션
- 강조 배지
- 추천 맛집 표시
- 랭킹 강조
- 찜 활성 상태 일부

---

### 6.2 Background Color

앱 전체 배경은 순백색보다 따뜻한 크림색을 사용한다.

```txt
#fffaf0
```

Tailwind 예시:

```tsx
bg-[#fffaf0]
```

바깥 데스크톱 배경은 연한 회색을 사용할 수 있다.

```tsx
bg-neutral-100
```

---

### 6.3 Text Color

주요 텍스트는 진한 꿀갈색 또는 거의 검정에 가까운 색을 사용한다.

```txt
#2b210f
```

Tailwind 예시:

```tsx
text-[#2b210f]
```

보조 텍스트는 gray 계열을 사용한다.

```tsx
text-gray-500
text-gray-600
text-gray-700
```

---

### 6.4 Card Color

카드는 기본적으로 흰색을 사용한다.

```tsx
bg-white
```

카드 경계가 필요한 경우 매우 연한 회색 border를 사용한다.

```tsx
border border-gray-100
```

또는:

```tsx
border border-gray-200
```

---

### 6.5 Secondary Colors

Honeytong의 보조 컬러는 다음을 사용한다.

```css
--color-cream: #fffaf0;
--color-bee-brown: #5c3b13;
--color-honey-dark: #2b210f;
--color-flower: #ff8fab;
--color-leaf: #7ac74f;
```

사용 위치:

- 작은 배지
- 상태 표시
- 장식 요소
- 카테고리 아이콘 배경
- 랭킹 보조 강조

주의:

보조 컬러는 화면 전체를 지배하면 안 된다.  
한 화면에서 1~2개 정도의 포인트로만 사용한다.

---

## 7. 타이포그래피

Honeytong은 한국어 UI가 중심이다.  
글자는 모바일에서 읽기 쉬워야 하며, 너무 작은 글씨를 피한다.

### 7.1 기본 규칙

- 제목은 명확하고 짧게 작성한다.
- 설명은 1~2줄을 넘기지 않는다.
- 카드 제목은 한 줄 또는 최대 두 줄까지만 허용한다.
- 보조 설명은 너무 작게 만들지 않는다.
- 한국어 문구를 기본으로 사용한다.

---

### 7.2 권장 크기

| 역할 | Tailwind 예시 |
|---|---|
| 큰 제목 | `text-2xl font-bold` |
| 화면 제목 | `text-xl font-bold` |
| 섹션 제목 | `text-lg font-bold` |
| 카드 제목 | `text-base font-semibold` |
| 본문 | `text-sm text-gray-700` |
| 보조 설명 | `text-xs text-gray-500` |
| 버튼 | `text-sm font-semibold` |

---

### 7.3 텍스트 색상 기준

주요 제목:

```tsx
text-[#2b210f]
```

본문:

```tsx
text-gray-700
```

보조 설명:

```tsx
text-gray-500
```

비활성 텍스트:

```tsx
text-gray-400
```

---

## 8. Layout Spacing

모바일 화면에서는 여백이 매우 중요하다.  
Honeytong은 답답하지 않게 넉넉한 여백을 사용한다.

### 8.1 페이지 좌우 여백

기본 페이지 좌우 여백:

```tsx
px-4
```

큰 모바일 또는 태블릿 대응:

```tsx
px-4 sm:px-6
```

---

### 8.2 섹션 간격

섹션 사이 간격:

```tsx
space-y-6
```

또는:

```tsx
mt-6
```

---

### 8.3 카드 내부 여백

일반 카드:

```tsx
p-4
```

이미지 중심 카드:

```tsx
p-3
```

---

### 8.4 하단 네비게이션 대응 여백

하단 네비게이션이 본문을 가리지 않도록 페이지 하단에는 충분한 padding을 둔다.

```tsx
pb-24
```

---

## 9. Border Radius

Honeytong은 둥근 UI를 기본으로 한다.

| 요소 | 권장 Radius |
|---|---|
| 큰 카드 | `rounded-3xl` |
| 일반 카드 | `rounded-2xl` 또는 `rounded-3xl` |
| 이미지 | `rounded-2xl` |
| 버튼 | `rounded-full` |
| 배지 | `rounded-full` |
| 입력창 | `rounded-2xl` |

기본 카드에는 다음을 우선 사용한다.

```tsx
rounded-3xl
```

---

## 10. Shadow

Honeytong은 과한 그림자를 사용하지 않는다.  
카드는 가볍게 떠 있는 정도만 표현한다.

권장:

```tsx
shadow-sm
```

또는 아주 약한 커스텀 shadow:

```tsx
shadow-[0_8px_24px_rgba(0,0,0,0.06)]
```

피해야 할 것:

```tsx
shadow-xl
shadow-2xl
drop-shadow-2xl
```

강한 그림자는 특별한 히어로 영역 외에는 사용하지 않는다.

---

## 11. Card Design

Honeytong은 카드형 UI를 중심으로 한다.

### 11.1 기본 카드 스타일

```tsx
className="rounded-3xl bg-white p-4 shadow-sm"
```

border가 필요한 경우:

```tsx
className="rounded-3xl border border-gray-100 bg-white p-4 shadow-sm"
```

---

### 11.2 맛집 카드 스타일

맛집 카드는 다음 요소를 포함한다.

- 대표 이미지
- 맛집 이름
- 지역
- 카테고리
- 거리
- 한 줄 설명
- 꿀 점수 또는 추천 수
- 찜 버튼

권장 이미지 스타일:

```tsx
className="aspect-[4/3] w-full rounded-2xl object-cover"
```

카드 전체:

```tsx
className="rounded-3xl bg-white p-3 shadow-sm transition duration-200 active:scale-[0.98]"
```

---

## 12. Button Design

### 12.1 Primary Button

주요 행동 버튼이다.

사용 위치:

- 추천하기
- 방문 인증하기
- 저장하기
- 등록하기
- 맛집 둘러보기

스타일:

```tsx
className="rounded-full bg-[#f6b800] px-5 py-3 text-sm font-semibold text-[#2b210f] transition duration-200 active:scale-[0.98]"
```

---

### 12.2 Secondary Button

보조 행동 버튼이다.

사용 위치:

- 필터
- 정렬
- 취소
- 보조 이동

스타일:

```tsx
className="rounded-full border border-gray-200 bg-white px-4 py-2 text-sm font-semibold text-gray-700"
```

---

### 12.3 Icon Button

아이콘 버튼은 실제 터치 영역이 충분히 커야 한다.

```tsx
className="flex h-11 w-11 items-center justify-center rounded-full bg-white shadow-sm"
```

아이콘만 있는 버튼에는 `aria-label`을 제공한다.

```tsx
<button aria-label="찜하기">
  ...
</button>
```

---

## 13. Badge Design

배지는 작고 부드럽게 사용한다.

사용 위치:

- 동네 인증
- 인기 상승
- 신규 맛집
- 추천 많은 곳
- 1꿀 / 2꿀 / 3꿀
- 지역 정보
- 카테고리

기본 스타일:

```tsx
className="inline-flex items-center rounded-full bg-[#fff1bf] px-3 py-1 text-xs font-semibold text-[#5c3b13]"
```

배지는 한 카드에 너무 많이 넣지 않는다.  
맛집 카드 하나에는 1~3개 정도만 권장한다.

---

## 14. Image Design

맛집 이미지는 Honeytong에서 매우 중요한 요소다.

### 14.1 기본 규칙

- 이미지 비율을 유지한다.
- 깨진 이미지에 대한 fallback을 준비한다.
- 이미지 위에 텍스트를 너무 많이 올리지 않는다.
- 강한 오버레이를 피한다.
- 이미지 모서리는 둥글게 처리한다.

---

### 14.2 목록 카드 이미지

```tsx
className="aspect-[4/3] w-full rounded-2xl object-cover"
```

---

### 14.3 상세 화면 상단 이미지

```tsx
className="aspect-[16/10] w-full object-cover"
```

상세 화면 이미지 하단을 둥글게 처리할 수 있다.

```tsx
className="aspect-[16/10] w-full rounded-b-3xl object-cover"
```

---

## 15. Bottom Navigation

하단 네비게이션은 모바일 앱처럼 고정한다.

### 15.1 메뉴

기본 메뉴는 다음과 같다.

```txt
홈
랭킹
찜
마이
```

---

### 15.2 스타일

```tsx
className="fixed bottom-0 left-1/2 z-50 w-full max-w-[430px] -translate-x-1/2 border-t border-gray-200 bg-white px-4 py-2"
```

---

### 15.3 활성 상태

활성 메뉴:

```tsx
text-[#d99a00]
```

비활성 메뉴:

```tsx
text-gray-400
```

---

### 15.4 주의사항

본문이 하단 네비게이션에 가려지지 않도록 페이지에 다음 padding을 준다.

```tsx
pb-24
```

---

## 16. Icon Style

아이콘은 기본적으로 `lucide-react` 사용을 권장한다.

권장 크기:

```tsx
size={18}
size={20}
size={22}
```

규칙:

- 아이콘만으로 의미를 전달하지 않는다.
- 주요 기능은 텍스트와 함께 사용한다.
- 아이콘 버튼은 충분한 터치 영역을 가진다.
- 장식용 아이콘은 과하게 반복하지 않는다.

---

## 17. Honeytong 시각 요소

Honeytong만의 감성을 위해 다음 요소를 사용할 수 있다.

- 작은 벌 아이콘
- 꿀방울
- 벌집 패턴
- 꽃 아이콘
- 꿀단지
- 동네 지도 핀
- 꿀 등급 배지

단, 장식 요소는 화면당 1~2개 정도로 제한한다.  
기능보다 장식이 더 눈에 띄면 안 된다.

---

## 18. 한국어 UI 문구 기준

Honeytong UI 문구는 기본적으로 한국어를 사용한다.

권장 문구:

```txt
오늘은 어디서 꿀맛을 찾을까요?
우리 동네 꿀맛집을 모아봤어요.
동네 사람들이 자주 찾는 곳이에요.
아직 찜한 맛집이 없어요.
마음에 드는 맛집을 꿀단지에 담아보세요.
꿀맛집을 불러오는 중이에요...
맛집 정보를 불러오지 못했어요.
다시 불러오기
```

피해야 할 문구:

```txt
Loading
Error
No Data
Submit
Cancel
Restaurant Detail
Ranking List
User Page
```

---

## 19. Tailwind 사용 원칙

Honeytong은 Tailwind CSS 기반으로 UI를 정리한다.

### 19.1 권장

- Tailwind class 우선 사용
- 반복되는 스타일은 컴포넌트화
- 모바일 기준 class 먼저 작성
- 필요할 때만 반응형 class 추가

예시:

```tsx
className="rounded-3xl bg-white p-4 shadow-sm"
```

---

### 19.2 피해야 할 것

- 의미 없는 inline style 남발
- 한 컴포넌트에 너무 긴 className 반복
- 같은 카드 스타일을 여러 페이지에 중복 작성
- 사용하지 않는 CSS 파일 추가
- 불필요한 UI 라이브러리 추가

---

## 20. 접근성 기본 기준

Codex는 UI 수정 시 기본 접근성을 지켜야 한다.

- 버튼에는 명확한 텍스트 또는 aria-label 제공
- 이미지에는 alt 제공
- 아이콘만 있는 버튼은 aria-label 제공
- 텍스트 대비가 너무 낮지 않게 유지
- 클릭 가능한 요소는 충분한 크기 확보
- 키보드 포커스를 완전히 제거하지 않기

---

## 21. 전체 금지 사항

Codex는 UI 리팩토링 중 다음을 하면 안 된다.

- 기존 기능 삭제
- 기존 라우팅 변경
- 기존 API 호출 구조 임의 변경
- mock 데이터 재도입
- 페이지 전체를 한 번에 갈아엎기
- props를 이유 없이 삭제
- 한국어 UI 문구를 영어로 변경
- 강한 노랑 배경 반복
- 카카오톡처럼 보이는 UI 구성
- 과한 shadow 사용
- 너무 작은 텍스트 사용
- 너무 작은 버튼 사용
- 하단 네비게이션이 본문을 가리는 상태 방치
- TypeScript 오류가 있는 상태로 작업 종료

---

## 22. Codex 작업 완료 기준

Codex가 디자인 시스템을 기준으로 UI를 수정했다면 다음을 확인해야 한다.

- 모바일 393x852 기준에서 보기 좋은가?
- 최대 폭 430px 기준이 유지되는가?
- 색상이 Honeytong 톤에 맞는가?
- 카카오처럼 보이지 않는가?
- 카드, 버튼, 배지 스타일이 일관적인가?
- 하단 네비게이션이 본문을 가리지 않는가?
- 찜 버튼과 카드 클릭이 충돌하지 않는가?
- 한국어 문구가 깨지지 않았는가?
- 기존 API 연결이 유지되는가?
- 기존 라우팅이 유지되는가?
- TypeScript 오류가 없는가?