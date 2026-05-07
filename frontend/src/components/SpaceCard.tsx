import type { MouseEvent } from "react";

type Props = {
  title: string;
  desc: string;
  distance: string;
  rating: number;
  price: string;
  isWished?: boolean;
  imageUrl?: string;
  onToggleWish?: (event: MouseEvent<HTMLButtonElement>) => void;
};

export default function SpaceCard({
  title,
  desc,
  distance,
  rating,
  price,
  isWished = false,
  imageUrl,
  onToggleWish,
}: Props) {
  const metaItems = [
    distance,
    rating > 0 ? `별점 ${rating}` : "별점 준비 중",
    price,
  ].filter(Boolean);

  return (
    <article className="overflow-hidden rounded-3xl bg-white shadow-sm">
      <div className="relative h-36 bg-[#fff1bf]">
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={`${title} 대표 이미지`}
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center px-6 text-center text-sm font-semibold leading-6 text-[#8a6315]">
            꿀맛집 이미지 준비 중
          </div>
        )}

        {onToggleWish && (
          <button
            type="button"
            aria-label={isWished ? "찜 해제" : "찜하기"}
            onClick={(event) => {
              event.preventDefault();
              event.stopPropagation();
              onToggleWish(event);
            }}
            className="absolute right-3 top-3 flex h-9 w-9 items-center justify-center rounded-full bg-white/95 text-lg font-bold text-[#d99a00] shadow-sm transition active:scale-95"
          >
            {isWished ? "♥" : "♡"}
          </button>
        )}
      </div>

      <div className="p-4">
        <h3 className="line-clamp-1 text-base font-bold text-[#2b210f]">
          {title}
        </h3>
        <p className="mt-2 line-clamp-2 text-sm leading-6 text-gray-600">
          {desc || "동네 사람들이 추천한 꿀맛집이에요."}
        </p>
        <p className="mt-3 line-clamp-1 text-xs font-semibold text-gray-500">
          {metaItems.join(" · ")}
        </p>
      </div>
    </article>
  );
}
