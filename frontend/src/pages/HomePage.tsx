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
    <div className="min-h-screen bg-[#FFFBEB] pb-[100px]">
      <SearchBar onSearch={onSearch} />
      <CategoryTabs />

      <div className="mt-4 px-6">
        <div className="flex h-[240px] items-center justify-center rounded-xl bg-[#d8b7ae] font-medium text-gray-600">
          지도 미리보기
        </div>
      </div>

      <div className="mt-8 px-6">
        <div className="mb-4 flex items-center justify-between gap-3">
          <h2 className="text-lg font-bold">오늘의 꿀스팟</h2>
          <Link
            to="/places/new"
            className="rounded-lg bg-yellow-400 px-3 py-2 text-sm font-bold text-black"
          >
            등록
          </Link>
        </div>

        {loading && (
          <div className="rounded-xl bg-white p-5 text-sm text-gray-500">
            장소를 불러오는 중입니다.
          </div>
        )}

        {!loading && errorMessage && (
          <div className="rounded-xl bg-white p-5 text-sm text-red-500">
            {errorMessage}
          </div>
        )}

        {!loading && !errorMessage && places.length === 0 && (
          <div className="rounded-xl bg-white p-5 text-sm text-gray-500">
            아직 등록된 장소가 없습니다.
          </div>
        )}

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
      </div>

      <BottomNav />
    </div>
  );
}
