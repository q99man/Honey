type Props = {
  onSearch: (keyword: string) => void;
};

export default function SearchBar({ onSearch }: Props) {
  return (
    <form
      className="flex h-12 items-center gap-3 rounded-full bg-white px-4 shadow-sm"
      onSubmit={(event) => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        onSearch(String(form.get("keyword") ?? ""));
      }}
    >
      <span className="shrink-0 text-sm font-semibold text-[#d99a00]">
        검색
      </span>
      <input
        name="keyword"
        aria-label="맛집 검색"
        placeholder="동네 맛집이나 메뉴를 검색해보세요"
        className="min-w-0 flex-1 bg-transparent text-sm text-[#2b210f] placeholder:text-gray-400 focus:outline-none"
      />
    </form>
  );
}
