import { Link } from "react-router-dom";
import SpaceCard from "../components/SpaceCard";
import BottomNav from "../components/BottomNav";
import SearchBar from "../components/SearchBar";
import CategoryTabs from "../components/CategoryTabs";
import type { Place } from "../types/place";

type Props = {
  spaces: Place[];
  onToggleWish: (id: number) => void;
};

export default function HomePage({ spaces, onToggleWish }: Props) {
  return (
    <div className="bg-[#FFFBEB] min-h-screen pb-[100px]">
      <SearchBar />
      <CategoryTabs />

      <div className="px-6 mt-4">
        <div className="h-[240px] bg-[#d8b7ae] rounded-xl flex items-center justify-center text-gray-600 font-medium">
          지도 미리보기
        </div>
      </div>

      <div className="px-6 mt-8">
        <h2 className="text-lg font-bold mb-4">오늘의 핫스팟</h2>

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
