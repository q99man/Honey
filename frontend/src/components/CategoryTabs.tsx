type CategoryOption = {
  label: string;
  value: string;
};

const categoryOptions: CategoryOption[] = [
  { label: "전체", value: "ALL" },
  { label: "한식", value: "KOREAN" },
  { label: "분식", value: "SNACK" },
  { label: "카페", value: "CAFE" },
  { label: "일식", value: "JAPANESE" },
];

type Props = {
  selectedCategory: string;
  onSelectCategory: (value: string) => void;
};

export default function CategoryTabs({
  selectedCategory,
  onSelectCategory,
}: Props) {
  return (
    <div className="flex gap-2 overflow-x-auto pb-1">
      {categoryOptions.map((category) => {
        const active = selectedCategory === category.value;

        return (
          <button
            key={category.value}
            type="button"
            aria-pressed={active}
            onClick={() => onSelectCategory(category.value)}
            className={`shrink-0 rounded-full px-4 py-2 text-sm font-semibold transition ${
              active
                ? "bg-[#f6b800] text-[#2b210f]"
                : "bg-white text-gray-500"
            }`}
          >
            {category.label}
          </button>
        );
      })}
    </div>
  );
}
