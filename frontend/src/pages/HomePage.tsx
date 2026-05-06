import { Link } from "react-router-dom";
import BottomNav from "../components/BottomNav";
import CategoryTabs from "../components/CategoryTabs";
import SearchBar from "../components/SearchBar";
import SpaceCard from "../components/SpaceCard";
import type { Place } from "../types/place";

type Props = {
  places: Place[];
  loading: boolean;
  errorMessage: string | null;
  onToggleWish: (id: number) => void;
  onSearch: (keyword: string) => void;
};

export default function HomePage({
  places,
  loading,
  errorMessage,
  onToggleWish,
  onSearch,
}: Props) {
  return (
    <div className="min-h-screen bg-neutral-100">
      <main className="mx-auto min-h-screen max-w-[430px] bg-[#fffaf0] pb-24">
        <div className="px-4 pt-5">
          <p className="text-sm font-semibold text-[#d99a00]">허니통</p>
          <h1 className="mt-2 text-2xl font-bold leading-8 text-[#2b210f]">
            오늘은 어디서 꿀맛을 찾을까요?
          </h1>
          <p className="mt-2 text-sm leading-5 text-gray-600">
            우리 동네 사람들이 추천한 맛집을 모아봤어요.
          </p>
        </div>

        <SearchBar onSearch={onSearch} />
        <CategoryTabs />

        <div className="mt-5 px-4">
          <div className="flex h-[180px] items-center justify-center rounded-3xl border border-[#f4dfb7] bg-[#fff1bf] text-sm font-semibold text-[#5c3b13] shadow-sm">
            동네 지도 미리보기
          </div>
        </div>

        <section className="mt-7 px-4">
          <div className="mb-4 flex items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-bold text-[#2b210f]">
                오늘의 꿀맛집
              </h2>
              <p className="mt-1 text-sm text-gray-500">
                마음에 드는 맛집은 찜해두고 다시 찾아보세요.
              </p>
            </div>
            <Link
              to="/places/new"
              className="shrink-0 rounded-full bg-[#f6b800] px-4 py-2 text-sm font-semibold text-[#2b210f] transition duration-200 active:scale-[0.98]"
            >
              등록
            </Link>
          </div>

          {loading && (
            <div className="space-y-3">
              <div className="rounded-3xl bg-white p-5 text-sm text-gray-500 shadow-sm">
                꿀맛집을 불러오는 중이에요...
              </div>
              {[1, 2, 3].map((item) => (
                <div
                  key={item}
                  className="animate-pulse rounded-3xl bg-white p-3 shadow-sm"
                >
                  <div className="flex gap-3">
                    <div className="aspect-[4/3] w-28 rounded-2xl bg-[#fff1bf]" />
                    <div className="flex flex-1 flex-col gap-2 py-1">
                      <div className="h-4 w-2/3 rounded-full bg-gray-100" />
                      <div className="h-3 w-full rounded-full bg-gray-100" />
                      <div className="h-3 w-1/2 rounded-full bg-gray-100" />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {!loading && errorMessage && (
            <div className="rounded-3xl bg-white p-5 shadow-sm">
              <p className="text-sm font-semibold text-[#2b210f]">
                맛집 정보를 불러오지 못했어요.
              </p>
              <p className="mt-1 text-sm text-gray-500">
                잠시 후 다시 시도해주세요.
              </p>
              <p className="mt-2 text-xs text-red-500">{errorMessage}</p>
              <button
                type="button"
                onClick={() => onSearch("")}
                className="mt-4 rounded-full bg-[#f6b800] px-4 py-2 text-sm font-semibold text-[#2b210f] transition duration-200 active:scale-[0.98]"
              >
                다시 불러오기
              </button>
            </div>
          )}

          {!loading && !errorMessage && places.length === 0 && (
            <div className="rounded-3xl bg-white p-6 text-center shadow-sm">
              <p className="text-sm font-semibold text-[#2b210f]">
                아직 보여줄 맛집이 없어요.
              </p>
              <p className="mt-1 text-sm text-gray-500">
                조금 후에 다시 확인해볼까요?
              </p>
            </div>
          )}

          {!loading && !errorMessage && places.length > 0 && (
            <div className="flex flex-col gap-4">
              {places.map((place) => (
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
