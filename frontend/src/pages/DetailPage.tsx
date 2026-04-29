import { useNavigate, useParams } from "react-router-dom";
import BottomNav from "../components/BottomNav";
import type { Place } from "../types/place";

type Props = {
  spaces: Place[];
};

export default function DetailPage({ spaces }: Props) {
  const { id } = useParams();
  const navigate = useNavigate();

  const space = spaces.find((item) => item.id === Number(id));

  if (!space) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        존재하지 않는 장소입니다.
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-[100px]">
      <div className="relative h-[320px] bg-[#d8b7ae] flex items-center justify-center">
        <button
          onClick={() => navigate(-1)}
          className="absolute left-5 top-5 h-10 w-10 rounded-full bg-white shadow-sm"
        >
          ←
        </button>

        <span className="text-gray-600 font-medium">
          이미지 영역 #{space.id}
        </span>
      </div>

      <main className="px-6 pt-6">
        <section className="bg-white rounded-2xl p-5 shadow-sm">
          <h1 className="text-2xl font-bold">{space.title}</h1>

          <p className="mt-2 text-sm text-gray-500">
            {space.distance} · ⭐ {space.rating} · 리뷰 {space.reviewCount}개
          </p>

          <p className="mt-4 text-xl font-bold">
            {space.price.toLocaleString()}원
          </p>
        </section>

        <section className="mt-5 bg-white rounded-2xl p-5 shadow-sm">
          <h2 className="text-lg font-bold">소개</h2>
          <p className="mt-3 text-sm text-gray-600">
            {space.desc}
          </p>
        </section>
      </main>

      <BottomNav />
    </div>
  );
}
