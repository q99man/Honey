export default function SearchBar() {
  return (
    <div className="px-6 mt-4">
      <div className="flex items-center gap-2 h-[48px] rounded-full border border-yellow-400 px-4 bg-white">
        <span>🔍</span>
        <input
          placeholder="어디로 갈까요?"
          className="flex-1 outline-none text-sm bg-transparent"
        />
      </div>
    </div>
  );
}