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
  placesLoading: boolean;
  placesErrorMessage: string | null;
};

type RegionOption = {
  label: string;
  regionType: RankingRegionType;
  regionId: number;
};

const regionTypeLabels: Record<RankingRegionType, string> = {
  dong: "동네",
  district: "구",
  city: "시",
};

export default function RankingPage({
  places,
  placesLoading,
  placesErrorMessage,
}: Props) {
  const [season, setSeason] = useState<CurrentSeason | null>(null);
  const [ranking, setRanking] = useState<PlaceRanking | null>(null);
  const [selectedRegion, setSelectedRegion] = useState<RegionOption | null>(
    null,
  );
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const regionOptions = useMemo(() => buildRegionOptions(places), [places]);
  const activeRegion = selectedRegion ?? regionOptions[0] ?? null;

  useEffect(() => {
    let mounted = true;
    getCurrentSeason()
      .then((nextSeason) => {
        if (mounted) {
          setSeason(nextSeason);
        }
      })
      .catch(() => {
        if (mounted) {
          setErrorMessage("현재 랭킹 시즌을 불러오지 못했어요.");
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    if (!activeRegion) {
      return;
    }

    let mounted = true;
    Promise.resolve()
      .then(async () => {
        if (mounted) {
          setLoading(true);
          setErrorMessage(null);
        }
        const nextRanking = await getPlaceRanking(
          activeRegion.regionType,
          activeRegion.regionId,
          season?.seasonCode,
        );
        if (mounted) {
          setRanking(nextRanking);
        }
      })
      .catch(() => {
        if (mounted) {
          setRanking(null);
          setErrorMessage("랭킹 정보를 불러오지 못했어요.");
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
  }, [activeRegion, season?.seasonCode]);

  const isBusy = placesLoading || loading;
  const visibleError = placesErrorMessage ?? errorMessage;

  return (
    <div className="min-h-screen bg-neutral-100">
      <main className="mx-auto min-h-screen max-w-[430px] bg-[#fffaf0] px-4 pb-24 pt-6">
        <header>
          <p className="text-xs font-semibold text-[#d99a00]">랭킹</p>
          <h1 className="mt-1 text-2xl font-bold text-[#2b210f]">
            우리 동네 꿀맛집 랭킹
          </h1>
          <p className="mt-2 text-sm leading-6 text-gray-600">
            추천과 방문 인증이 모여 만든 믿을 수 있는 순위예요.
          </p>
        </header>

        <section className="mt-5 rounded-3xl bg-white p-4 shadow-sm">
          <p className="text-xs font-semibold text-gray-500">현재 시즌</p>
          <p className="mt-1 text-lg font-bold text-[#2b210f]">
            {season?.seasonName ?? "시즌 정보를 확인하는 중이에요"}
          </p>
          <p className="mt-2 text-sm text-gray-500">
            {activeRegion
              ? `${regionTypeLabels[activeRegion.regionType]} 기준`
              : "랭킹 지역을 준비하고 있어요"}
          </p>
        </section>

        {regionOptions.length > 0 && (
          <div className="mt-5 flex gap-2 overflow-x-auto pb-1">
            {regionOptions.map((option) => {
              const active =
                activeRegion?.regionType === option.regionType &&
                activeRegion?.regionId === option.regionId;

              return (
                <button
                  key={`${option.regionType}-${option.regionId}`}
                  type="button"
                  aria-pressed={active}
                  onClick={() => setSelectedRegion(option)}
                  className={`shrink-0 rounded-full px-4 py-2 text-sm font-semibold transition ${
                    active
                      ? "bg-[#f6b800] text-[#2b210f]"
                      : "bg-white text-gray-500"
                  }`}
                >
                  {option.label}
                </button>
              );
            })}
          </div>
        )}

        {isBusy && <StateCard title="랭킹을 불러오는 중이에요." />}

        {!isBusy && visibleError && (
          <StateCard title={visibleError} desc="잠시 후 다시 확인해보세요." />
        )}

        {!isBusy && !visibleError && regionOptions.length === 0 && (
          <StateCard
            title="랭킹을 만들 지역 정보가 아직 없어요."
            desc="맛집 목록을 먼저 불러오면 지역별 랭킹을 볼 수 있어요."
          />
        )}

        {!isBusy && !visibleError && ranking?.items.length === 0 && (
          <StateCard
            title="아직 랭킹에 오른 맛집이 없어요."
            desc="추천과 방문 인증이 쌓이면 순위가 만들어져요."
          />
        )}

        {!isBusy && !visibleError && ranking && ranking.items.length > 0 && (
          <section className="mt-5 space-y-4">
            <div>
              <h2 className="text-lg font-bold text-[#2b210f]">상위 3곳</h2>
              <p className="mt-1 text-sm text-gray-500">
                지금 가장 반응이 좋은 맛집이에요.
              </p>
            </div>

            <div className="space-y-3">
              {ranking.items.slice(0, 3).map((item) => (
                <RankingItem key={item.placeId} item={item} highlight />
              ))}
            </div>

            {ranking.items.length > 3 && (
              <div className="pt-2">
                <h2 className="text-lg font-bold text-[#2b210f]">전체 순위</h2>
                <div className="mt-3 space-y-3">
                  {ranking.items.slice(3).map((item) => (
                    <RankingItem key={item.placeId} item={item} />
                  ))}
                </div>
              </div>
            )}
          </section>
        )}
      </main>
      <BottomNav />
    </div>
  );
}

function RankingItem({
  item,
  highlight = false,
}: {
  item: PlaceRanking["items"][number];
  highlight?: boolean;
}) {
  return (
    <Link
      to={`/places/${item.placeId}`}
      className={`block rounded-3xl bg-white p-4 shadow-sm ${
        highlight ? "border-2 border-[#f6b800]" : ""
      }`}
    >
      <div className="flex items-start gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[#f6b800] text-sm font-bold text-[#2b210f]">
          {item.rank}
        </div>
        <div className="min-w-0 flex-1">
          <h3 className="truncate text-base font-bold text-[#2b210f]">
            {item.name}
          </h3>
          <p className="mt-1 text-sm text-gray-500">
            별 {item.starLevel} · 총점 {item.totalScore}
          </p>
          {item.audienceTags.length > 0 && (
            <div className="mt-3 flex flex-wrap gap-2">
              {item.audienceTags.slice(0, 3).map((tag) => (
                <span
                  key={tag}
                  className="rounded-full bg-[#fff7dc] px-3 py-1 text-xs font-semibold text-[#8a6315]"
                >
                  {tag}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>
    </Link>
  );
}

function StateCard({ title, desc }: { title: string; desc?: string }) {
  return (
    <section className="mt-5 rounded-3xl bg-white p-5 text-center shadow-sm">
      <p className="text-sm font-semibold text-[#2b210f]">{title}</p>
      {desc && <p className="mt-2 text-sm leading-6 text-gray-500">{desc}</p>}
    </section>
  );
}

function buildRegionOptions(places: Place[]) {
  const firstByDong = new Map<number, Place>();
  const firstByDistrict = new Map<number, Place>();
  const firstByCity = new Map<number, Place>();

  places.forEach((place) => {
    if (!firstByDong.has(place.dongId)) {
      firstByDong.set(place.dongId, place);
    }
    if (!firstByDistrict.has(place.districtId)) {
      firstByDistrict.set(place.districtId, place);
    }
    if (!firstByCity.has(place.cityId)) {
      firstByCity.set(place.cityId, place);
    }
  });

  const options: RegionOption[] = [];

  firstByDong.forEach((place) => {
    options.push({
      label: place.regionName || "현재 동네",
      regionType: "dong",
      regionId: place.dongId,
    });
  });

  firstByDistrict.forEach((place) => {
    options.push({
      label: "현재 구",
      regionType: "district",
      regionId: place.districtId,
    });
  });

  firstByCity.forEach((place) => {
    options.push({
      label: "현재 시",
      regionType: "city",
      regionId: place.cityId,
    });
  });

  return options;
}
