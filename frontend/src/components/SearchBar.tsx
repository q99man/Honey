import { useState } from "react";
import type { FormEvent } from "react";

type Props = {
  onSearch: (keyword: string) => void;
};

export default function SearchBar({ onSearch }: Props) {
  const [keyword, setKeyword] = useState("");

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    onSearch(keyword);
  };

  return (
    <form className="mt-5 px-4" onSubmit={submit}>
      <div className="flex h-12 items-center gap-2 rounded-2xl border border-gray-200 bg-white px-4 text-sm text-gray-700 shadow-sm transition duration-200 focus-within:border-[#f6b800]">
        <span aria-hidden="true" className="text-[#d99a00]">
          검색
        </span>
        <input
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          type="search"
          aria-label="장소 검색"
          placeholder="동네 맛집이나 메뉴를 검색해보세요"
          className="min-w-0 flex-1 bg-transparent text-sm text-[#2b210f] outline-none placeholder:text-gray-400"
        />
      </div>
    </form>
  );
}
