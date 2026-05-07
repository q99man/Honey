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

  return (
    <div className="min-h-screen bg-neutral-100">
      <main className="mx-auto min-h-screen max-w-[430px] bg-[#fffaf0] px-4 pb-24 pt-6">
        <header>
          <p className="text-xs font-semibold text-[#d99a00]">찜 목록</p>
          <h1 className="mt-1 text-2xl font-bold text-[#2b210f]">
            내 꿀단지
          </h1>
          <p className="mt-2 text-sm leading-6 text-gray-600">
            다시 가보고 싶은 동네 맛집을 모아두었어요.
          </p>
        </header>

        <section className="mt-5 rounded-3xl bg-white p-4 shadow-sm">
          <p className="text-sm font-semibold text-gray-500">찜한 맛집</p>
          <p className="mt-1 text-3xl font-bold text-[#2b210f]">
            {wishedPlaces.length}개
          </p>
        </section>

        {loading && <StateCard title="찜 목록을 불러오는 중이에요." />}

        {!loading && errorMessage && (
          <StateCard
            title={errorMessage}
            desc="목록을 다시 불러오면 찜한 맛집도 함께 확인할 수 있어요."
          />
        )}

        {!loading && !errorMessage && wishedPlaces.length === 0 && (
          <StateCard
            title="아직 찜한 맛집이 없어요."
            desc="마음에 드는 맛집의 하트를 눌러 꿀단지에 담아보세요."
          />
        )}

        {!loading && !errorMessage && wishedPlaces.length > 0 && (
          <section className="mt-5 flex flex-col gap-4">
            {wishedPlaces.map((place) => (
              <Link key={place.id} to={`/places/${place.id}`}>
                <SpaceCard
                  title={place.title}
                  desc={place.desc}
                  distance={place.distance || place.regionName}
                  rating={place.rating}
                  price={place.price}
                  imageUrl={place.imageUrl}
                  isWished={place.isWished}
                  onToggleWish={() => onToggleWish(place.id)}
                />
              </Link>
            ))}
          </section>
        )}
      </main>
      <BottomNav />
    </div>
  );
}

function StateCard({ title, desc }: { title: string; desc?: string }) {
  return (
    <section className="mt-5 rounded-3xl bg-white p-5 text-center shadow-sm">
      <p className="text-sm font-semibold text-[#2b210f]">{title}</p>
      {desc && <p className="mt-2 text-sm leading-6 text-gray-500">{desc}</p>}
    </section>
  );
}
