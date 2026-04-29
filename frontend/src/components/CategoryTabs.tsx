export default function CategoryTabs() {
  return (
    <div className="mt-4 flex gap-3 overflow-x-auto px-6">
      <Tab active label="전체" />
      <Tab label="한식" />
      <Tab label="카페" />
      <Tab label="혼밥" />
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
      className={`flex h-[36px] shrink-0 items-center rounded-full border px-4 ${
        active ? "border-yellow-400 bg-yellow-400" : "border-yellow-300"
      }`}
    >
      <span className="text-sm">{label}</span>
    </div>
  );
}
