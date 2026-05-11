import { FOOD_CATEGORIES } from "../constants/foodCategories";

type Props = {
  selectedCategory: string;
  onSelectCategory: (value: string) => void;
};

export default function CategoryTabs({
  selectedCategory,
  onSelectCategory,
}: Props) {
  return (
    <div className="flex gap-2 overflow-x-auto pb-1 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
      {FOOD_CATEGORIES.map((category) => {
        const active = selectedCategory === category.value;

        return (
          <button
            key={category.value}
            type="button"
            aria-pressed={active}
            onClick={() => onSelectCategory(category.value)}
            className={`flex h-9 shrink-0 items-center gap-1.5 rounded-full border px-3 text-xs font-bold shadow-sm transition active:scale-[0.98] ${
              active
                ? "border-[#f6b800] bg-[#fff1bf] text-[#2b210f]"
                : "border-white/80 bg-white/95 text-gray-600"
            }`}
          >
            <span
              aria-hidden="true"
              className="flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-full bg-[#fff7d7] text-base shadow-inner"
            >
              {category.emoji}
            </span>
            <span>{category.label}</span>
          </button>
        );
      })}
    </div>
  );
}
