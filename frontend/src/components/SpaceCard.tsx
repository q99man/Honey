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
  compact?: boolean;
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
  compact = false,
}: Props) {
  const metaItems = [
    distance,
    rating > 0 ? `별점 ${rating}` : "별점 준비 중",
    price,
  ].filter(Boolean);

  return (
    <article className="overflow-hidden rounded-m3-xl bg-m3-surface-container-lowest text-m3-on-surface shadow-m3-1">
      <div className={`relative bg-m3-primary-container ${compact ? "h-28" : "h-36"}`}>
        {imageUrl ? (
          <img
            src={imageUrl}
            alt={`${title} 대표 이미지`}
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center px-6 text-center text-m3-label-lg text-m3-on-primary-container">
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
            className="absolute right-3 top-3 flex h-9 w-9 items-center justify-center rounded-m3-full border border-m3-outline-variant bg-m3-secondary-container text-lg font-bold text-m3-on-secondary-container shadow-m3-1 transition active:scale-95"
          >
            {isWished ? "★" : "☆"}
          </button>
        )}
      </div>

      <div className={compact ? "p-3" : "p-4"}>
        <h3 className="line-clamp-1 text-m3-title-md text-m3-on-surface">
          {title}
        </h3>
        <p
          className={`${compact ? "mt-1" : "mt-2"} line-clamp-2 text-m3-body-md text-m3-on-surface-variant`}
        >
          {desc || "동네 사람들이 추천한 꿀맛집이에요."}
        </p>
        <p
          className={`${compact ? "mt-2" : "mt-3"} line-clamp-1 text-m3-body-sm text-m3-on-surface-variant`}
        >
          {metaItems.join(" · ")}
        </p>
      </div>
    </article>
  );
}
