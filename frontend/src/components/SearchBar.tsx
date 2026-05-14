type Props = {
  onSearch: (keyword: string) => void;
};

export default function SearchBar({ onSearch }: Props) {
  return (
    <form
      className="flex h-11 items-center gap-2.5 rounded-m3-full bg-m3-surface-container-high px-3.5 text-m3-on-surface shadow-m3-1 backdrop-blur"
      onSubmit={(event) => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        onSearch(String(form.get("keyword") ?? ""));
      }}
    >
      <span className="shrink-0 text-m3-label-md text-m3-primary">
        🍯
      </span>
      <input
        name="keyword"
        aria-label="맛집 검색"
        placeholder="검색어를 입력하세요"
        className="min-w-0 flex-1 bg-transparent text-m3-body-md text-m3-on-surface placeholder:text-m3-on-surface-variant focus:outline-none"
      />
    </form>
  );
}
