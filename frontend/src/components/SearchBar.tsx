type Props = {
  onSearch: (keyword: string) => void;
};

export default function SearchBar({ onSearch }: Props) {
  return (
    <form
      className="flex h-11 items-center gap-2.5 rounded-full bg-white/95 px-3.5 shadow-sm backdrop-blur"
      onSubmit={(event) => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        onSearch(String(form.get("keyword") ?? ""));
      }}
    >
      <span className="shrink-0 text-xs font-bold text-[#d99a00]">
        🍯
      </span>
      <input
        name="keyword"
        aria-label="맛집 검색"
        placeholder="검색어를 입력하세요"
        className="min-w-0 flex-1 bg-transparent text-[13px] text-[#2b210f] placeholder:text-gray-400 focus:outline-none"
      />
    </form>
  );
}
