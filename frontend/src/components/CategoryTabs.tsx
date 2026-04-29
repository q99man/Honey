export default function CategoryTabs() {
  return (
    <div className="flex gap-3 px-6 mt-4">
      <Tab active label="전체" />
      <Tab label="카페" />
      <Tab label="공부" />
      <Tab label="모임" />
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
      className={`px-4 h-[36px] flex items-center rounded-full border ${
        active
          ? "bg-yellow-400 border-yellow-400"
          : "border-yellow-300"
      }`}
    >
      <span className="text-sm">{label}</span>
    </div>
  );
}
