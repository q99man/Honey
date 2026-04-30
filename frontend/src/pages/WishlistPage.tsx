import { Link } from "react-router-dom";
import BottomNav from "../components/BottomNav";
import SpaceCard from "../components/SpaceCard";
import type { Place } from "../types/place";

type Props = {
  places: Place[];
  onToggleWish: (id: number) => void;
};

export default function WishlistPage({ places, onToggleWish }: Props) {
  const wishedPlaces = places.filter((place) => place.isWished);

  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-[100px]">
      <main className="px-6 pt-8">
        <h1 className="text-2xl font-bold">찜</h1>
        <p className="mt-2 text-sm text-gray-500">
          저장해 둔 꿀스팟을 모아볼 수 있어요.
        </p>

        <section className="mt-6 flex flex-col gap-4">
          {wishedPlaces.length === 0 && (
            <div className="rounded-xl bg-white p-5 text-sm text-gray-500">
              아직 찜한 장소가 없습니다.
            </div>
          )}

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
        </section>
      </main>

      <BottomNav />
    </div>
  );
}
