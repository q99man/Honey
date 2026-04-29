import { Link } from "react-router-dom";
import BottomNav from "../components/BottomNav";
import CategoryTabs from "../components/CategoryTabs";
import SearchBar from "../components/SearchBar";
import SpaceCard from "../components/SpaceCard";
import type { Place } from "../types/place";

type Props = {
  spaces: Place[];
  onToggleWish: (id: number) => void;
};

export default function HomePage({ spaces, onToggleWish }: Props) {
  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-[100px]">
      <SearchBar />
      <CategoryTabs />

      <div className="mt-4 px-6">
        <div className="flex h-[240px] items-center justify-center rounded-xl bg-[#d8b7ae] font-medium text-gray-600">
          지도 미리보기
        </div>
      </div>

      <div className="mt-8 px-6">
        <h2 className="mb-4 text-lg font-bold">오늘의 꿀스팟</h2>

        <div className="flex flex-col gap-4">
          {spaces.map((space) => (
            <Link key={space.id} to={`/spaces/${space.id}`}>
              <SpaceCard
                title={space.title}
                desc={space.desc}
                distance={space.distance}
                rating={space.rating}
                price={space.price}
                isWished={space.isWished}
                onToggleWish={() => onToggleWish(space.id)}
              />
            </Link>
          ))}
        </div>
      </div>

      <BottomNav />
    </div>
  );
}
