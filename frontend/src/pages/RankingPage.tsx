import { Link } from "react-router-dom";
import BottomNav from "../components/BottomNav";
import SpaceCard from "../components/SpaceCard";
import type { Place } from "../types/place";

type Props = {
  places: Place[];
  onToggleWish: (id: number) => void;
};

export default function RankingPage({ places, onToggleWish }: Props) {
  const rankedPlaces = [...places].sort(
    (a, b) => b.recommendCount + b.visitCount - (a.recommendCount + a.visitCount),
  );
  const topPlace = rankedPlaces[0];

  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-[100px]">
      <main className="px-6 pt-8">
        <h1 className="text-2xl font-bold">랭킹</h1>
        <p className="mt-2 text-sm text-gray-500">
          지금 가장 많은 관심을 받은 꿀스팟을 확인해보세요.
        </p>

        {topPlace && (
          <section className="mt-6 rounded-2xl bg-yellow-300 p-5 text-left shadow-sm">
            <p className="text-sm font-semibold">오늘의 1위</p>
            <h2 className="mt-2 text-xl font-bold">{topPlace.title}</h2>
            <p className="mt-2 text-sm text-gray-700">
              추천 {topPlace.recommendCount} · 방문 {topPlace.visitCount}
            </p>
          </section>
        )}

        <section className="mt-6">
          <h2 className="mb-4 text-lg font-bold">전체 랭킹</h2>

          {rankedPlaces.length === 0 && (
            <div className="rounded-xl bg-white p-5 text-sm text-gray-500">
              아직 랭킹에 표시할 장소가 없습니다.
            </div>
          )}

          <div className="flex flex-col gap-4">
            {rankedPlaces.map((place, index) => (
              <Link key={place.id} to={`/places/${place.id}`}>
                <div className="flex items-center gap-3">
                  <span className="w-8 text-center text-lg font-bold">
                    {index + 1}
                  </span>
                  <div className="flex-1">
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
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </section>
      </main>

      <BottomNav />
    </div>
  );
}
