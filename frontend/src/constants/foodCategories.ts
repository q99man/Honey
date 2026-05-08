export type FoodCategory = {
  value: string;
  label: string;
  emoji: string;
  keywords: string[];
};

export const FOOD_CATEGORIES: FoodCategory[] = [
  {
    value: "ALL",
    label: "전체",
    emoji: "🍯",
    keywords: ["전체"],
  },
  {
    value: "KOREAN",
    label: "한식",
    emoji: "🍚",
    keywords: ["한식", "백반", "국밥", "찌개", "고기", "불고기"],
  },
  {
    value: "CHINESE",
    label: "중식",
    emoji: "🍜",
    keywords: ["중식", "짜장면", "짬뽕", "탕수육", "중국집", "jajang"],
  },
  {
    value: "JAPANESE",
    label: "일식",
    emoji: "🍣",
    keywords: ["일식", "초밥", "라멘", "돈카츠", "우동"],
  },
  {
    value: "WESTERN",
    label: "양식",
    emoji: "🍝",
    keywords: ["양식", "파스타", "스테이크", "피자", "브런치"],
  },
  {
    value: "ASIAN",
    label: "아시안",
    emoji: "🍛",
    keywords: ["아시안", "쌀국수", "커리", "태국", "베트남"],
  },
  {
    value: "CAFE",
    label: "카페",
    emoji: "☕",
    keywords: ["카페", "커피", "라떼", "음료"],
  },
  {
    value: "DESSERT",
    label: "디저트",
    emoji: "🍰",
    keywords: ["디저트", "케이크", "마카롱", "빙수", "아이스크림"],
  },
  {
    value: "BAKERY",
    label: "빵집",
    emoji: "🥐",
    keywords: ["빵집", "베이커리", "빵", "크루아상"],
  },
  {
    value: "STREET_FOOD",
    label: "노점",
    emoji: "🥞",
    keywords: ["노점", "호떡", "붕어빵", "계란빵", "군고구마", "타코야끼"],
  },
  {
    value: "SNACK",
    label: "분식",
    emoji: "🍢",
    keywords: ["분식", "떡볶이", "순대", "어묵", "김밥", "튀김"],
  },
  {
    value: "CHICKEN",
    label: "치킨",
    emoji: "🍗",
    keywords: ["치킨", "닭강정", "후라이드", "양념치킨"],
  },
  {
    value: "MEAT",
    label: "고기",
    emoji: "🥩",
    keywords: ["고기", "삼겹살", "소고기", "갈비", "구이"],
  },
  {
    value: "SEAFOOD",
    label: "해산물",
    emoji: "🦐",
    keywords: ["해산물", "회", "조개", "새우", "생선"],
  },
  {
    value: "BAR",
    label: "술집",
    emoji: "🍺",
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
