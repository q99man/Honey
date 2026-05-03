import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  getCurrentSeason,
  getPlaceRanking,
  type CurrentSeason,
  type PlaceRanking,
  type RankingRegionType,
} from "../api/rankingApi";
import BottomNav from "../components/BottomNav";
import type { Place } from "../types/place";

type Props = {
  places: Place[];
};

type RegionOption = {
  type: RankingRegionType;
  id: number;
  label: string;
};

const REGION_LABELS: Record<RankingRegionType, string> = {
  dong: "동 랭킹",
  district: "구 랭킹",
  city: "시 랭킹",
};

const LOAD_ERROR = "랭킹 정보를 불러오지 못했습니다.";

export default function RankingPage({ places }: Props) {
  const [selectedType, setSelectedType] = useState<RankingRegionType>("dong");
  const [season, setSeason] = useState<CurrentSeason | null>(null);
  const [ranking, setRanking] = useState<PlaceRanking | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const regionOptions = useMemo(() => buildRegionOptions(places), [places]);
  const selectedRegion = regionOptions.find(
    (option) => option.type === selectedType,
  );

  useEffect(() => {
    if (!selectedRegion) {
      return;
    }

    let mounted = true;
    Promise.resolve()
      .then(() => {
        setLoading(true);
        return Promise.all([
          getCurrentSeason(),
          getPlaceRanking(selectedRegion.type, selectedRegion.id),
        ]);
      })
      .then(([currentSeason, placeRanking]) => {
        if (!mounted) {
          return;
        }
        setSeason(currentSeason);
        setRanking(placeRanking);
        setErrorMessage(null);
      })
      .catch(() => {
        if (mounted) {
          setErrorMessage(LOAD_ERROR);
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
  }, [selectedRegion]);

  const topItem = ranking?.items[0];

  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-[100px]">
      <main className="px-6 pt-8">
        <header>
          <h1 className="text-2xl font-bold">지역 랭킹</h1>
          <p className="mt-2 text-sm text-gray-500">
            시즌별 지역 순위와 별점 변화를 확인해 보세요.
          </p>
        </header>

        <section className="mt-5 grid grid-cols-3 gap-2">
          {(["dong", "district", "city"] as RankingRegionType[]).map((type) => (
            <button
              key={type}
              type="button"
              onClick={() => setSelectedType(type)}
              className={`h-10 rounded-full border text-sm font-semibold ${
                selectedType === type
                  ? "border-yellow-400 bg-yellow-400 text-black"
                  : "border-yellow-200 bg-white text-gray-500"
              }`}
            >
              {REGION_LABELS[type]}
            </button>
          ))}
        </section>

        {!selectedRegion && (
          <section className="mt-6 rounded-xl bg-white p-5 text-sm text-gray-500">
            랭킹을 조회할 지역 정보가 아직 없습니다. 장소 목록을 먼저 불러와 주세요.
          </section>
        )}

        {selectedRegion && (
          <>
            <section className="mt-5 rounded-xl bg-white p-4 text-sm text-gray-600 shadow-sm">
              <div className="flex items-center justify-between gap-3">
                <span>{selectedRegion.label}</span>
                <span>{season ? season.seasonName : "현재 시즌"}</span>
              </div>
            </section>

            {loading && (
              <section className="mt-6 rounded-xl bg-white p-5 text-sm text-gray-500">
                랭킹을 불러오는 중입니다.
              </section>
            )}

            {!loading && errorMessage && (
              <section className="mt-6 rounded-xl bg-white p-5 text-sm text-red-500">
                {errorMessage}
              </section>
            )}

            {!loading && !errorMessage && topItem && (
              <section className="mt-6 rounded-xl bg-yellow-300 p-5 text-left shadow-sm">
                <p className="text-sm font-semibold">오늘의 1위</p>
                <h2 className="mt-2 text-xl font-bold">{topItem.name}</h2>
                <p className="mt-2 text-sm text-gray-700">
                  별 {topItem.starLevel} · 총점 {formatScore(topItem.totalScore)}
                </p>
              </section>
            )}

            <section className="mt-6">
              <h2 className="mb-4 text-lg font-bold">전체 순위</h2>

              {!loading && !errorMessage && ranking?.items.length === 0 && (
                <div className="rounded-xl bg-white p-5 text-sm text-gray-500">
                  아직 이 지역에 표시할 랭킹이 없습니다.
                </div>
              )}

              <div className="flex flex-col gap-3">
                {ranking?.items.map((item) => (
                  <Link key={item.placeId} to={`/places/${item.placeId}`}>
                    <article className="flex items-center gap-3 rounded-xl bg-white p-4 shadow-sm">
                      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-yellow-100 text-sm font-bold">
                        {item.rank}
                      </span>
                      <div className="min-w-0 flex-1">
                        <h3 className="truncate text-base font-bold">
                          {item.name}
                        </h3>
                        <p className="mt-1 text-sm text-gray-500">
                          별 {item.starLevel} · 총점 {formatScore(item.totalScore)}
                        </p>
                      </div>
                    </article>
                  </Link>
                ))}
              </div>
            </section>
          </>
        )}
      </main>

      <BottomNav />
    </div>
  );
}

function buildRegionOptions(places: Place[]): RegionOption[] {
  const firstPlace = places[0];
  if (!firstPlace) {
    return [];
  }
  return [
    { type: "dong", id: firstPlace.dongId, label: firstPlace.regionName },
    { type: "district", id: firstPlace.districtId, label: "현재 구" },
    { type: "city", id: firstPlace.cityId, label: "현재 시" },
  ];
}

function formatScore(score: number) {
  return Number(score).toLocaleString("ko-KR", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  });
}
