export default function SearchBar() {
  return (
    <div className="mt-4 px-6">
      <div className="flex h-[48px] items-center gap-2 rounded-full border border-yellow-400 bg-white px-4">
        <span aria-hidden="true">⌕</span>
        <input
          placeholder="어디로 갈까요?"
          className="flex-1 bg-transparent text-sm outline-none"
        />
      </div>
    </div>
  );
}
