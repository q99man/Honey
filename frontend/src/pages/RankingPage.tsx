import { Link } from "react-router-dom";
import BottomNav from "../components/BottomNav";
import SpaceCard from "../components/SpaceCard";
import { mockSpaces } from "../data/mockSpaces";

export default function RankingPage() {
  const rankedSpaces = [...mockSpaces].sort((a, b) => b.rating - a.rating);

  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-[100px]">
      <main className="px-6 pt-8">
        <h1 className="text-2xl font-bold">랭킹</h1>
        <p className="mt-2 text-sm text-gray-500">
          지금 가장 인기 있는 꿀스팟을 확인해보세요.
        </p>

        <section className="mt-6 rounded-2xl bg-yellow-300 p-5 shadow-sm">
          <p className="text-sm font-semibold">오늘의 1위</p>
          <h2 className="mt-2 text-xl font-bold">{rankedSpaces[0].title}</h2>
          <p className="mt-2 text-sm text-gray-700">
            ⭐ {rankedSpaces[0].rating} · 리뷰 {rankedSpaces[0].reviewCount}개
          </p>
        </section>

        <section className="mt-6">
          <h2 className="mb-4 text-lg font-bold">전체 랭킹</h2>

          <div className="flex flex-col gap-4">
            {rankedSpaces.map((space, index) => (
              <Link key={space.id} to={`/spaces/${space.id}`}>
                <div className="flex items-center gap-3">
                  <span className="w-8 text-center text-lg font-bold">
                    {index + 1}
                  </span>
                  <div className="flex-1">
                    <SpaceCard
                      title={space.title}
                      desc={space.desc}
                      distance={space.distance}
                      rating={space.rating}
                      price={space.price}
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