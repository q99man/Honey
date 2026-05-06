import { useState } from "react";

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
  const [failedImageUrl, setFailedImageUrl] = useState<string | null>(null);
  const showImage = Boolean(imageUrl && imageUrl !== failedImageUrl);
  const metaItems = [distance, `별 ${rating}`].filter(Boolean);

  return (
    <div className="relative rounded-3xl border border-gray-100 bg-white p-3 text-left shadow-sm transition duration-200 active:scale-[0.98]">
      <div className="flex gap-3">
        <div className="flex aspect-[4/3] w-28 max-w-[34%] shrink-0 items-center justify-center overflow-hidden rounded-2xl bg-[#fff1bf] text-center text-xs font-semibold leading-4 text-[#5c3b13]">
          {showImage ? (
            <img
              src={imageUrl}
              alt={`${title} 대표 이미지`}
              className="h-full w-full object-cover"
              loading="lazy"
              onError={() => setFailedImageUrl(imageUrl ?? null)}
            />
          ) : (
            <span>
              꿀맛집
              <br />
              이미지 준비 중
            </span>
          )}
        </div>

        <div
          className={`flex min-w-0 flex-1 flex-col gap-1 ${
            onToggleWish ? "pr-9" : ""
          }`}
        >
          <div className="line-clamp-1 text-base font-semibold text-[#2b210f]">
            {title}
          </div>
          <div className="line-clamp-2 text-sm leading-5 text-gray-600">
            {desc || "동네 사람들이 추천한 맛집이에요."}
          </div>

          <div className="mt-auto flex flex-wrap items-center gap-1.5 text-xs text-gray-500">
            {metaItems.map((item) => (
              <span key={item} className="max-w-full truncate">
                {item}
              </span>
            ))}
            {price && (
              <span className="max-w-full truncate rounded-full bg-[#fff1bf] px-2 py-0.5 font-semibold text-[#5c3b13]">
                {price}
              </span>
            )}
          </div>
        </div>
      </div>

      {onToggleWish && (
        <button
          type="button"
          aria-label={isWished ? "찜 해제" : "찜하기"}
          aria-pressed={isWished}
          onClick={(event) => {
            event.preventDefault();
            event.stopPropagation();
            onToggleWish();
          }}
          className={`absolute right-3 top-3 flex h-11 w-11 items-center justify-center rounded-full bg-white text-lg shadow-sm transition duration-200 active:scale-[0.96] ${
            isWished ? "text-[#d99a00]" : "text-gray-400"
          }`}
        >
          {isWished ? "♥" : "♡"}
        </button>
      )}
    </div>
  );
}
