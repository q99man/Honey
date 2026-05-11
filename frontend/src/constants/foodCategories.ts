export type FoodCategory = {
  value: string;
  label: string;
  emoji: string;
  iconSvg: string;
  keywords: string[];
};

export const FOOD_CATEGORIES: FoodCategory[] = [
  {
    value: "ALL",
    label: "전체",
    emoji: "🧭",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><circle cx="12" cy="12" r="10"/><polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/></svg>',
    keywords: ["전체"],
  },
  {
    value: "KOREAN",
    label: "한식",
    emoji: "🍚",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><path d="M4 14a8 8 0 0 0 16 0H4z"/><path d="M12 4v4"/><path d="M8 5v2"/><path d="M16 5v2"/></svg>',
    keywords: ["한식", "백반", "국밥", "찌개", "고기", "불고기"],
  },
  {
    value: "CHINESE",
    label: "중식",
    emoji: "🥢",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><path d="M4 16a8 8 0 0 0 16 0H4z"/><path d="M2 10l20-4"/><path d="M2 14l20-4"/></svg>',
    keywords: ["중식", "짜장면", "짬뽕", "탕수육", "중국집", "jajang"],
  },
  {
    value: "JAPANESE",
    label: "일식",
    emoji: "🍣",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><path d="M2 14a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V10a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v4z"/><path d="M6 8V5a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v3"/></svg>',
    keywords: ["일식", "초밥", "라멘", "돈카츠", "우동"],
  },
  {
    value: "WESTERN",
    label: "양식",
    emoji: "🍝",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><path d="M12 2v20"/><path d="M8 2v6a4 4 0 0 0 8 0V2"/></svg>',
    keywords: ["양식", "파스타", "스테이크", "피자", "브런치"],
  },
  {
    value: "ASIAN",
    label: "아시안",
    emoji: "🍜",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><path d="M2 14a10 10 0 0 0 20 0H2z"/><path d="M22 14l2-2"/></svg>',
    keywords: ["아시안", "쌀국수", "커리", "태국", "베트남"],
  },
  {
    value: "CAFE",
    label: "카페",
    emoji: "☕",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><path d="M17 8h1a4 4 0 1 1 0 8h-1"/><path d="M3 8h14v9a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V8z"/><line x1="6" y1="2" x2="6" y2="4"/><line x1="10" y1="2" x2="10" y2="4"/><line x1="14" y1="2" x2="14" y2="4"/></svg>',
    keywords: ["카페", "커피", "디저트", "음료"],
  },
  {
    value: "DESSERT",
    label: "디저트",
    emoji: "🍰",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><path d="M20 21v-8a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8"/><path d="M4 16s.5-1 2-1 2.5 2 4 2 2.5-2 4-2 2.5 2 4 2 2-1 2-1"/><path d="M2 21h20"/><path d="M12 3v4"/><path d="M12 3a2 2 0 0 0 0 4z"/></svg>',
    keywords: ["디저트", "케이크", "마카롱", "빙수", "아이스크림"],
  },
  {
    value: "BAKERY",
    label: "빵집",
    emoji: "🥐",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><path d="M6 8a6 6 0 0 1 12 0v10a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V8z"/><path d="M6 14h12"/></svg>',
    keywords: ["빵집", "베이커리", "빵", "크루아상"],
  },
  {
    value: "STREET_FOOD",
    label: "노점",
    emoji: "🌭",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><path d="M2 7L4 3h16l2 4M2 7v14a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V7M2 7h20M7 7v16M17 7v16"/></svg>',
    keywords: ["노점", "분식", "붕어빵", "계란빵", "군고구마", "타코야끼"],
  },
  {
    value: "SNACK",
    label: "분식",
    emoji: "🍢",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><circle cx="12" cy="7" r="4"/><circle cx="12" cy="15" r="4"/><line x1="12" y1="22" x2="12" y2="19"/></svg>',
    keywords: ["분식", "떡볶이", "순대", "어묵", "김밥", "튀김"],
  },
  {
    value: "CHICKEN",
    label: "치킨",
    emoji: "🍗",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><path d="M15 13a4 4 0 0 0-4-4 4 4 0 0 0-4 4 4 4 0 0 0 4 4 4 4 0 0 0 4-4z"/><path d="M11 17l-4 4a2 2 0 1 0 3 3l4-4"/></svg>',
    keywords: ["치킨", "닭강정", "후라이드", "양념치킨"],
  },
  {
    value: "MEAT",
    label: "고기",
    emoji: "🥩",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><rect x="4" y="6" width="16" height="12" rx="4"/><line x1="8" y1="6" x2="8" y2="18"/><line x1="16" y1="6" x2="16" y2="18"/></svg>',
    keywords: ["고기", "삼겹살", "소고기", "갈비", "구이"],
  },
  {
    value: "SEAFOOD",
    label: "해산물",
    emoji: "🦐",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><path d="M2 12c0 0 4-6 10-6s10 6 10 6-4 6-10 6-10-6-10-6z"/><path d="M10 12h.01"/></svg>',
    keywords: ["해산물", "회", "조개", "새우", "생선"],
  },
  {
    value: "BAR",
    label: "술집",
    emoji: "🍻",
    iconSvg:
      '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" class="w-full h-full"><path d="M8 22h8"/><path d="M10 22V10"/><path d="M14 22V10"/><rect x="6" y="6" width="12" height="16" rx="2"/><path d="M6 6v-2a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v2"/></svg>',
    keywords: ["술집", "맥주", "이자카야", "포차", "와인"],
  },
];

export function getFoodCategory(value: string) {
  return (
    FOOD_CATEGORIES.find((category) => category.value === value) ??
    FOOD_CATEGORIES[0]
  );
}

export function getFoodCategoryLabel(value: string) {
  return getFoodCategory(value).label;
}
