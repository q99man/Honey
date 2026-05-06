export default function CategoryTabs() {
  return (
    <div className="mt-4 flex gap-2 overflow-x-auto px-4 pb-1">
      <Tab active label="전체" />
      <Tab label="한식" />
      <Tab label="분식" />
      <Tab label="카페" />
      <Tab label="일식" />
    </div>
  );
}

type TabProps = {
  label: string;
  active?: boolean;
};

function Tab({ label, active = false }: TabProps) {
  return (
    <div
      className={`flex h-10 shrink-0 items-center rounded-full border px-4 ${
        active
          ? "border-[#f6b800] bg-[#f6b800] text-[#2b210f]"
          : "border-gray-200 bg-white text-gray-600"
      }`}
    >
      <span className="text-sm font-semibold">{label}</span>
    </div>
  );
}
