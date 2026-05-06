import { Link } from "react-router-dom";
import BottomNav from "../components/BottomNav";
import SpaceCard from "../components/SpaceCard";
import type { Place } from "../types/place";

type Props = {
  places: Place[];
  loading: boolean;
  errorMessage: string | null;
  onToggleWish: (id: number) => void;
};

export default function WishlistPage({
  places,
  loading,
  errorMessage,
  onToggleWish,
}: Props) {
  const wishedPlaces = places.filter((place) => place.isWished);
  const wishedCount = wishedPlaces.length;

  return (
    <div className="min-h-screen bg-neutral-100">
      <main className="mx-auto min-h-screen max-w-[430px] bg-[#fffaf0] px-4 pb-24 pt-6">
        <header className="text-left">
          <p className="inline-flex rounded-full bg-[#fff1bf] px-3 py-1 text-xs font-semibold text-[#5c3b13]">
            찜 목록
          </p>
          <h1 className="mt-3 text-2xl font-bold leading-8 text-[#2b210f]">
            내 꿀단지
          </h1>
          <p className="mt-2 text-sm leading-5 text-gray-600">
            마음에 담아둔 맛집을 다시 확인해보세요.
          </p>
        </header>

        <section className="mt-5 rounded-3xl bg-white p-4 text-left shadow-sm">
          <p className="text-xs font-semibold text-gray-500">담아둔 맛집</p>
          <p className="mt-2 text-2xl font-bold text-[#2b210f]">
            {wishedCount}개
          </p>
          <p className="mt-1 text-sm text-gray-500">
            {wishedCount > 0
              ? "오늘 다시 가보고 싶은 맛집을 골라보세요."
              : "마음에 드는 맛집을 꿀단지에 담아보세요."}
          </p>
        </section>

        <section className="mt-6">
          {loading && <WishlistLoadingState />}

          {!loading && errorMessage && (
            <WishlistMessageCard
              title="찜 목록을 불러오지 못했어요."
              description={errorMessage || "잠시 후 다시 시도해주세요."}
              tone="error"
            />
          )}

          {!loading && !errorMessage && wishedPlaces.length === 0 && (
            <WishlistMessageCard
              title="아직 찜한 맛집이 없어요."
              description="마음에 드는 맛집을 꿀단지에 담아보세요."
            />
          )}

          {!loading && !errorMessage && wishedPlaces.length > 0 && (
            <div className="flex flex-col gap-4">
              {wishedPlaces.map((place) => (
                <Link key={place.id} to={`/places/${place.id}`}>
                  <SpaceCard
                    title={place.title}
                    desc={place.desc}
                    distance={place.distance}
                    rating={place.rating}
                    price={place.price}
                    imageUrl={place.imageUrl}
                    isWished={place.isWished}
                    onToggleWish={() => onToggleWish(place.id)}
                  />
                </Link>
              ))}
            </div>
          )}
        </section>
      </main>

      <BottomNav />
    </div>
  );
}

function WishlistLoadingState() {
  return (
    <div className="space-y-3">
      <div className="rounded-3xl bg-white p-4 text-sm text-gray-500 shadow-sm">
        찜한 맛집을 불러오는 중이에요...
      </div>
      {[0, 1, 2].map((item) => (
        <div
          key={item}
          className="flex animate-pulse gap-3 rounded-3xl bg-white p-3 shadow-sm"
        >
          <div className="aspect-[4/3] w-28 max-w-[34%] rounded-2xl bg-gray-100" />
          <div className="min-w-0 flex-1 py-1">
            <div className="h-4 w-2/3 rounded-full bg-gray-100" />
            <div className="mt-3 h-3 w-full rounded-full bg-gray-100" />
            <div className="mt-2 h-3 w-1/2 rounded-full bg-gray-100" />
          </div>
        </div>
      ))}
    </div>
  );
}

function WishlistMessageCard({
  title,
  description,
  tone = "default",
}: {
  title: string;
  description: string;
  tone?: "default" | "error";
}) {
  return (
    <div className="rounded-3xl bg-white p-6 text-center shadow-sm">
      <p
        className={`text-sm font-bold ${
          tone === "error" ? "text-red-500" : "text-[#2b210f]"
        }`}
      >
        {title}
      </p>
      <p className="mt-2 text-sm leading-5 text-gray-500">{description}</p>
    </div>
  );
}
