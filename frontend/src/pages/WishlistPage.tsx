import { Link } from "react-router-dom";
import BottomNav from "../components/BottomNav";
import SpaceCard from "../components/SpaceCard";
import type { Place } from "../types/place";

type Props = {
  spaces: Place[];
};

export default function WishlistPage({ spaces }: Props) {
  const wishedSpaces = spaces.filter((space) => space.isWished);

  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-[100px]">
      <main className="px-6 pt-8">
        <h1 className="text-2xl font-bold">찜</h1>
        <p className="mt-2 text-sm text-gray-500">
          저장해둔 꿀스팟을 모아봤어요.
        </p>

        <section className="mt-6 flex flex-col gap-4">
          {wishedSpaces.map((space) => (
            <Link key={space.id} to={`/spaces/${space.id}`}>
              <SpaceCard
                title={space.title}
                desc={space.desc}
                distance={space.distance}
                rating={space.rating}
                price={space.price}
                isWished={space.isWished}
              />
            </Link>
          ))}
        </section>
      </main>

      <BottomNav />
    </div>
  );
}
