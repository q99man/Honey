import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getPlace } from "../api/placeApi";
import BottomNav from "../components/BottomNav";
import type { Place } from "../types/place";

type Props = {
  wishedIds: Set<number>;
  onToggleWish: (id: number) => void;
};

export default function DetailPage({ wishedIds, onToggleWish }: Props) {
  const { id } = useParams();
  const navigate = useNavigate();
  const [place, setPlace] = useState<Place | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const placeId = Number(id);
    if (!placeId) {
      setErrorMessage("장소를 찾을 수 없습니다.");
      setLoading(false);
      return;
    }

    let mounted = true;
    setLoading(true);
    getPlace(placeId)
      .then((item) => {
        if (!mounted) {
          return;
        }
        setPlace({ ...item, isWished: wishedIds.has(item.id) });
        setErrorMessage(null);
      })
      .catch(() => {
        if (mounted) {
          setErrorMessage("장소 정보를 불러오지 못했습니다.");
        }
      })
      .finally(() => {
        if (mounted) {
          setLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, [id, wishedIds]);

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#FFFBEB]">
        장소를 불러오는 중입니다.
      </div>
    );
  }

  if (!place || errorMessage) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#FFFBEB] px-6 text-center">
        {errorMessage ?? "장소를 찾을 수 없습니다."}
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-[100px]">
      <div className="relative flex h-[320px] items-center justify-center bg-[#d8b7ae]">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="absolute left-5 top-5 h-10 w-10 rounded-full bg-white shadow-sm"
          aria-label="뒤로"
        >
          ←
        </button>

        <button
          type="button"
          onClick={() => onToggleWish(place.id)}
          className="absolute right-5 top-5 h-10 w-10 rounded-full bg-white text-lg shadow-sm"
          aria-label={place.isWished ? "찜 해제" : "찜하기"}
        >
          {place.isWished ? "♥" : "♡"}
        </button>

        {place.imageUrl ? (
          <img
            src={place.imageUrl}
            alt=""
            className="h-full w-full object-cover"
          />
        ) : (
          <span className="font-medium text-gray-600">이미지 영역 #{place.id}</span>
        )}
      </div>

      <main className="px-6 pt-6">
        <section className="rounded-2xl bg-white p-5 text-left shadow-sm">
          <h1 className="text-2xl font-bold">{place.title}</h1>

          <p className="mt-2 text-sm text-gray-500">
            {place.distance} · 별 {place.rating} · 댓글 {place.reviewCount}개
          </p>

          <p className="mt-4 text-xl font-bold">{place.price}</p>
          <p className="mt-2 text-sm text-gray-500">{place.address}</p>
        </section>

        <section className="mt-5 rounded-2xl bg-white p-5 text-left shadow-sm">
          <h2 className="text-lg font-bold">소개</h2>
          <p className="mt-3 text-sm text-gray-600">{place.desc}</p>
        </section>

        <section className="mt-5 rounded-2xl bg-white p-5 text-left shadow-sm">
          <h2 className="text-lg font-bold">활동</h2>
          <div className="mt-4 flex justify-between text-center">
            <div>
              <p className="text-xl font-bold">{place.recommendCount}</p>
              <p className="text-xs text-gray-500">추천</p>
            </div>
            <div>
              <p className="text-xl font-bold">{place.visitCount}</p>
              <p className="text-xs text-gray-500">방문</p>
            </div>
            <div>
              <p className="text-xl font-bold">{place.reviewCount}</p>
              <p className="text-xs text-gray-500">댓글</p>
            </div>
          </div>
        </section>
      </main>

      <BottomNav />
    </div>
  );
}
