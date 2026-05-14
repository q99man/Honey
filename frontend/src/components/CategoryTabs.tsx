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
            className={`flex h-9 shrink-0 items-center gap-1.5 rounded-m3-full border px-3 text-m3-label-md shadow-m3-1 transition active:scale-[0.98] ${
              active
                ? "border-m3-primary bg-m3-secondary-container text-m3-on-secondary-container"
                : "border-m3-outline-variant bg-m3-surface-container-lowest text-m3-on-surface-variant hover:border-m3-primary"
            }`}
          >
            <span
              aria-hidden="true"
              className="flex h-[22px] w-[22px] shrink-0 items-center justify-center rounded-m3-full bg-m3-surface-container-high text-base"
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
