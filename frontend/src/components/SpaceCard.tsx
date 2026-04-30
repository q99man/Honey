type Props = {
  title: string;
  desc: string;
  distance: string;
  rating: number;
  price: string;
  imageUrl?: string;
  isWished?: boolean;
  onToggleWish?: () => void;
};

export default function SpaceCard({
  title,
  desc,
  distance,
  rating,
  price,
  imageUrl,
  isWished = false,
  onToggleWish,
}: Props) {
  return (
    <div className="relative flex gap-3 rounded-xl bg-white p-3 text-left shadow-sm">
      <div className="h-[72px] w-[72px] shrink-0 overflow-hidden rounded-lg bg-[#f0d5a5]">
        {imageUrl && (
          <img
            src={imageUrl}
            alt=""
            className="h-full w-full object-cover"
            loading="lazy"
          />
        )}
      </div>

      <div className="flex flex-1 flex-col gap-1 pr-8">
        <div className="text-[16px] font-bold">{title}</div>
        <div className="line-clamp-2 text-[13px] text-gray-500">{desc}</div>

        <div className="text-[12px] text-gray-400">
          {distance} · 별 {rating} ·{" "}
          <span className="font-bold text-black">{price}</span>
        </div>
      </div>

      <button
        type="button"
        aria-label={isWished ? "찜 해제" : "찜하기"}
        onClick={(e) => {
          e.preventDefault();
          onToggleWish?.();
        }}
        className="absolute right-3 top-3 text-lg"
      >
        {isWished ? "♥" : "♡"}
      </button>
    </div>
  );
}
