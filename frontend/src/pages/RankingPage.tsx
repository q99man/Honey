import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  getCurrentSeason,
  getPlaceRanking,
  type CurrentSeason,
  type PlaceRanking,
  type PlaceRankingItem,
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
  type: RankingRegionType;
  id: number;
  label: string;
};

const REGION_LABELS: Record<RankingRegionType, string> = {
  dong: "동네",
  district: "구",
  city: "시",
};

const LOAD_ERROR = "랭킹 정보를 불러오지 못했어요.";

export default function RankingPage({
  places,
  placesLoading,
  placesErrorMessage,
}: Props) {
  const [selectedType, setSelectedType] = useState<RankingRegionType>("dong");
  const [season, setSeason] = useState<CurrentSeason | null>(null);
  const [ranking, setRanking] = useState<PlaceRanking | null>(null);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const regionOptions = useMemo(() => buildRegionOptions(places), [places]);
  const selectedRegion = regionOptions.find(
    (option) => option.type === selectedType,
  );
  const topItems = ranking?.items.slice(0, 3) ?? [];
  const remainingItems = ranking?.items.slice(3) ?? [];

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

  return (
    <div className="min-h-screen bg-neutral-100">
      <main className="mx-auto min-h-screen max-w-[430px] bg-[#fffaf0] px-4 pb-24 pt-6">
        <header className="text-left">
          <p className="inline-flex rounded-full bg-[#fff1bf] px-3 py-1 text-xs font-semibold text-[#5c3b13]">
            랭킹
          </p>
          <h1 className="mt-3 text-2xl font-bold leading-8 text-[#2b210f]">
            우리 동네 꿀맛집 랭킹
          </h1>
          <p className="mt-2 text-sm leading-5 text-gray-600">
            동네 사람들의 추천과 활동을 바탕으로 정리했어요.
          </p>
        </header>

        <section
          className="mt-5 flex gap-2 overflow-x-auto pb-1"
          aria-label="랭킹 지역 범위"
        >
          {(["dong", "district", "city"] as RankingRegionType[]).map(
            (type) => (
              <button
                key={type}
                type="button"
                onClick={() => setSelectedType(type)}
                className={`h-10 shrink-0 rounded-full px-4 text-sm font-semibold transition duration-200 active:scale-[0.98] ${
                  selectedType === type
                    ? "bg-[#f6b800] text-[#2b210f]"
                    : "border border-gray-200 bg-white text-gray-500"
                }`}
                aria-pressed={selectedType === type}
              >
                {REGION_LABELS[type]}
              </button>
            ),
          )}
        </section>

        {!selectedRegion && placesLoading && <RankingLoadingState />}

        {!selectedRegion && !placesLoading && placesErrorMessage && (
          <RankingMessageCard
            title="랭킹 지역 정보를 준비하지 못했어요."
            description={placesErrorMessage}
            tone="error"
          />
        )}

        {!selectedRegion && !placesLoading && !placesErrorMessage && (
          <RankingMessageCard
            title="아직 랭킹을 보여줄 지역 정보가 없어요."
            description="맛집 목록을 먼저 불러온 뒤 랭킹을 확인할 수 있어요."
          />
        )}

        {selectedRegion && (
          <>
            <section className="mt-5 rounded-3xl bg-white p-4 text-left shadow-sm">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold text-gray-500">
                    현재 범위
                  </p>
                  <p className="mt-1 text-base font-bold text-[#2b210f]">
                    {selectedRegion.label}
                  </p>
                </div>
                <div className="text-right">
                  <p className="text-xs font-semibold text-gray-500">시즌</p>
                  <p className="mt-1 max-w-[160px] truncate text-sm font-semibold text-[#5c3b13]">
                    {season ? season.seasonName : "현재 시즌"}
                  </p>
                </div>
              </div>
            </section>

            {loading && <RankingLoadingState />}

            {!loading && errorMessage && (
              <RankingMessageCard
                title="랭킹 정보를 불러오지 못했어요."
                description="잠시 후 다시 시도해주세요."
                tone="error"
              />
            )}

            {!loading && !errorMessage && ranking?.items.length === 0 && (
              <RankingMessageCard
                title="아직 랭킹에 올라온 맛집이 없어요."
                description="첫 번째 꿀맛집을 추천해보세요."
              />
            )}

            {!loading && !errorMessage && topItems.length > 0 && (
              <section className="mt-6">
                <div className="mb-3 flex items-center justify-between">
                  <h2 className="text-lg font-bold text-[#2b210f]">Top 3</h2>
                  <span className="text-xs font-semibold text-gray-500">
                    {ranking?.regionName ?? selectedRegion.label}
                  </span>
                </div>
                <div className="space-y-3">
                  {topItems.map((item) => (
                    <RankingCard key={item.placeId} item={item} featured />
                  ))}
                </div>
              </section>
            )}

            {!loading && !errorMessage && remainingItems.length > 0 && (
              <section className="mt-6">
                <h2 className="mb-3 text-lg font-bold text-[#2b210f]">
                  전체 순위
                </h2>
                <div className="space-y-3">
                  {remainingItems.map((item) => (
                    <RankingCard key={item.placeId} item={item} />
                  ))}
                </div>
              </section>
            )}
          </>
        )}
      </main>

      <BottomNav />
    </div>
  );
}

function RankingCard({
  item,
  featured = false,
}: {
  item: PlaceRankingItem;
  featured?: boolean;
}) {
  return (
    <Link to={`/places/${item.placeId}`} className="block">
      <article
        className={`rounded-3xl bg-white p-4 text-left shadow-sm transition duration-200 active:scale-[0.98] ${
          featured ? "border border-[#fff1bf]" : ""
        }`}
      >
        <div className="flex items-center gap-3">
          <span
            className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full text-sm font-bold ${
              featured
                ? "bg-[#f6b800] text-[#2b210f]"
                : "bg-[#fff1bf] text-[#5c3b13]"
            }`}
          >
            {item.rank}
          </span>
          <div className="min-w-0 flex-1">
            <h3 className="truncate text-base font-bold text-[#2b210f]">
              {item.name}
            </h3>
            <p className="mt-1 text-sm text-gray-500">
              별 {item.starLevel} · 총점 {formatScore(item.totalScore)}
            </p>
          </div>
          {featured && (
            <span className="shrink-0 rounded-full bg-[#fff1bf] px-3 py-1 text-xs font-semibold text-[#5c3b13]">
              상위권
            </span>
          )}
        </div>
        {item.audienceTags.length > 0 && (
          <div className="mt-3 flex flex-wrap gap-2">
            {item.audienceTags.slice(0, 3).map((tag) => (
              <span
                key={tag}
                className="rounded-full bg-gray-100 px-3 py-1 text-xs font-semibold text-gray-500"
              >
                {tag}
              </span>
            ))}
          </div>
        )}
      </article>
    </Link>
  );
}

function RankingLoadingState() {
  return (
    <section className="mt-6 space-y-3">
      <div className="rounded-3xl bg-white p-4 text-sm text-gray-500 shadow-sm">
        랭킹을 불러오는 중이에요...
      </div>
      {[0, 1, 2].map((item) => (
        <div
          key={item}
          className="flex animate-pulse items-center gap-3 rounded-3xl bg-white p-4 shadow-sm"
        >
          <div className="h-11 w-11 rounded-full bg-gray-100" />
          <div className="min-w-0 flex-1">
            <div className="h-4 w-2/3 rounded-full bg-gray-100" />
            <div className="mt-2 h-3 w-1/2 rounded-full bg-gray-100" />
          </div>
        </div>
      ))}
    </section>
  );
}

function RankingMessageCard({
  title,
  description,
  tone = "default",
}: {
  title: string;
  description: string;
  tone?: "default" | "error";
}) {
  return (
    <section className="mt-6 rounded-3xl bg-white p-5 text-left shadow-sm">
      <p
        className={`text-sm font-bold ${
          tone === "error" ? "text-red-500" : "text-[#2b210f]"
        }`}
      >
        {title}
      </p>
      <p className="mt-2 text-sm leading-5 text-gray-500">{description}</p>
    </section>
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
