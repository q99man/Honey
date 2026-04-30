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
    <form className="mt-4 px-6" onSubmit={submit}>
      <div className="flex h-[48px] items-center gap-2 rounded-full border border-yellow-400 bg-white px-4">
        <span aria-hidden="true">검색</span>
        <input
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="어디로 갈까요?"
          className="flex-1 bg-transparent text-sm outline-none"
        />
      </div>
    </form>
  );
}
