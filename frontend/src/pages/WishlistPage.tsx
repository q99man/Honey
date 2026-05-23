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
    <div className="min-h-screen bg-m3-surface">
      <main className="mx-auto min-h-screen max-w-[430px] bg-m3-surface px-4 pb-24 pt-6 text-m3-on-surface">
        <header>
          <p className="text-m3-label-md text-m3-primary">찜 목록</p>
          <h1 className="mt-1 text-m3-title-lg text-m3-on-surface">
            다시 가고 싶은 맛집
          </h1>
          <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">
            마음에 들어 저장해 둔 동네 맛집을 모아봤어요.
          </p>
        </header>

        <section className="mt-5 rounded-m3-xl bg-m3-surface-container-lowest p-4 shadow-m3-1">
          <p className="text-m3-label-md text-m3-on-surface-variant">
            찜한 맛집
          </p>
          <p className="mt-1 text-[32px] font-medium leading-10 text-m3-on-surface">
            {wishedPlaces.length}개
          </p>
        </section>

        {loading && <StateCard title="찜 목록을 불러오는 중이에요." />}

        {!loading && errorMessage && (
          <StateCard
            title={errorMessage}
            desc="목록을 다시 불러오면 찜한 맛집을 확인할 수 있어요."
          />
        )}

        {!loading && !errorMessage && wishedPlaces.length === 0 && (
          <StateCard
            title="아직 찜한 맛집이 없어요."
            desc="마음에 드는 맛집의 하트를 눌러 이곳에 모아보세요."
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
    <section className="mt-5 rounded-m3-xl bg-m3-surface-container-lowest p-5 text-center text-m3-on-surface shadow-m3-1">
      <p className="text-m3-title-sm">{title}</p>
      {desc && <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">{desc}</p>}
    </section>
  );
}
