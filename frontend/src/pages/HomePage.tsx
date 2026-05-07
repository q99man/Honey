import { useMemo, useState } from "react";
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

const categoryLabels: Record<string, string> = {
  ALL: "전체",
  KOREAN: "한식",
  SNACK: "분식",
  CAFE: "카페",
  JAPANESE: "일식",
};

export default function HomePage({
  places,
  loading,
  errorMessage,
  onToggleWish,
  onSearch,
}: Props) {
  const [selectedCategory, setSelectedCategory] = useState("ALL");
  const visiblePlaces = useMemo(() => {
    if (selectedCategory === "ALL") {
      return places;
    }
    return places.filter((place) => place.category === selectedCategory);
  }, [places, selectedCategory]);

  const rankedPlaces = useMemo(() => {
    return [...visiblePlaces]
      .sort(
        (a, b) =>
          b.recommendCount +
          b.visitCount +
          b.commentCount -
          (a.recommendCount + a.visitCount + a.commentCount),
      )
      .slice(0, 5);
  }, [visiblePlaces]);

  return (
    <div className="min-h-screen bg-neutral-100">
      <main className="relative mx-auto min-h-screen max-w-[430px] overflow-hidden bg-[#eaf2e4] lg:max-w-none">
        <MapCanvas places={visiblePlaces} />

        <section className="relative z-10 mx-auto flex min-h-screen max-w-[430px] flex-col px-4 pb-24 pt-5 lg:hidden">
          <MobileFloatingHeader
            selectedCategory={selectedCategory}
            onSelectCategory={setSelectedCategory}
            onSearch={onSearch}
          />

          <div className="flex-1" />

          <MobilePlaceSheet
            places={places}
            visiblePlaces={visiblePlaces}
            loading={loading}
            errorMessage={errorMessage}
            selectedCategory={selectedCategory}
            onToggleWish={onToggleWish}
            onRetry={() => onSearch("")}
          />
        </section>

        <section className="relative z-10 hidden min-h-screen grid-cols-[360px_minmax(420px,1fr)_340px] gap-5 px-6 py-6 lg:grid">
          <aside className="flex min-h-0 flex-col rounded-3xl bg-white/95 p-5 shadow-sm backdrop-blur">
            <p className="text-xs font-semibold text-[#d99a00]">Honeytong</p>
            <h1 className="mt-1 text-2xl font-bold text-[#2b210f]">
              오늘은 어디서 꿀맛을 찾을까요?
            </h1>
            <p className="mt-2 text-sm leading-6 text-gray-600">
              동네 사람들이 추천한 맛집을 지도에서 바로 살펴보세요.
            </p>

            <div className="mt-5">
              <SearchBar onSearch={onSearch} />
            </div>

            <div className="mt-4">
              <CategoryTabs
                selectedCategory={selectedCategory}
                onSelectCategory={setSelectedCategory}
              />
            </div>

            <div className="mt-5 flex items-center justify-between gap-3">
              <div>
                <h2 className="text-base font-bold text-[#2b210f]">
                  맛집 목록
                </h2>
                <p className="mt-1 text-xs text-gray-500">
                  {categoryLabels[selectedCategory] ?? "선택한"} 기준으로 보고 있어요.
                </p>
              </div>
              <Link
                to="/places/new"
                className="shrink-0 rounded-full bg-[#f6b800] px-4 py-2 text-sm font-semibold text-[#2b210f]"
              >
                등록
              </Link>
            </div>

            <PlaceList
              places={places}
              visiblePlaces={visiblePlaces}
              loading={loading}
              errorMessage={errorMessage}
              selectedCategory={selectedCategory}
              onToggleWish={onToggleWish}
              onRetry={() => onSearch("")}
              compact
            />
          </aside>

          <section className="relative min-h-0 rounded-[32px] border border-white/70 bg-white/10 shadow-sm">
            <div className="absolute left-5 top-5 rounded-full bg-white/95 px-4 py-3 text-sm font-semibold text-[#2b210f] shadow-sm backdrop-blur">
              {visiblePlaces.length > 0
                ? `${visiblePlaces.length}개의 꿀맛집이 보이는 중이에요`
                : "지도에서 동네 맛집을 찾는 중이에요"}
            </div>
            <div className="absolute bottom-5 left-1/2 w-[min(560px,calc(100%-40px))] -translate-x-1/2 rounded-3xl bg-white/95 p-4 shadow-sm backdrop-blur">
              <p className="text-xs font-semibold text-[#d99a00]">
                지도 탐색
              </p>
              <p className="mt-1 text-lg font-bold text-[#2b210f]">
                가운데 지도에서 위치를 보고, 양쪽 패널에서 정보를 확인하세요.
              </p>
            </div>
          </section>

          <aside className="flex min-h-0 flex-col rounded-3xl bg-white/95 p-5 shadow-sm backdrop-blur">
            <div>
              <p className="text-xs font-semibold text-[#d99a00]">랭킹</p>
              <h2 className="mt-1 text-xl font-bold text-[#2b210f]">
                지금 반응 좋은 맛집
              </h2>
              <p className="mt-2 text-sm leading-6 text-gray-500">
                추천, 방문, 댓글 수를 기준으로 가볍게 정리했어요.
              </p>
            </div>

            <div className="mt-5 space-y-3 overflow-y-auto pr-1">
              {rankedPlaces.length > 0 ? (
                rankedPlaces.map((place, index) => (
                  <Link
                    key={place.id}
                    to={`/places/${place.id}`}
                    className="block rounded-3xl border border-gray-100 bg-white p-4 shadow-sm transition active:scale-[0.99]"
                  >
                    <div className="flex items-start gap-3">
                      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[#fff1bf] text-sm font-bold text-[#8a6315]">
                        {index + 1}
                      </span>
                      <div className="min-w-0 flex-1">
                        <h3 className="truncate text-sm font-bold text-[#2b210f]">
                          {place.title}
                        </h3>
                        <p className="mt-1 line-clamp-2 text-xs leading-5 text-gray-500">
                          {place.desc || "동네 사람들이 추천한 꿀맛집이에요."}
                        </p>
                        <p className="mt-2 text-xs font-semibold text-[#8a6315]">
                          추천 {place.recommendCount} · 방문 {place.visitCount}
                        </p>
                      </div>
                    </div>
                  </Link>
                ))
              ) : (
                <StateCard
                  title="아직 랭킹으로 볼 맛집이 없어요."
                  desc="seed 데이터나 추천 활동이 쌓이면 여기에 보여드릴게요."
                  flush
                />
              )}
            </div>
          </aside>
        </section>
      </main>
      <div className="lg:hidden">
        <BottomNav />
      </div>
    </div>
  );
}

function MobileFloatingHeader({
  selectedCategory,
  onSelectCategory,
  onSearch,
}: {
  selectedCategory: string;
  onSelectCategory: (value: string) => void;
  onSearch: (keyword: string) => void;
}) {
  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between gap-3">
        <div className="rounded-full bg-white/95 px-4 py-2 shadow-sm backdrop-blur">
          <p className="text-sm font-bold text-[#2b210f]">우리 동네 꿀맛 지도</p>
        </div>
        <div className="rounded-full bg-white/95 px-3 py-2 text-xs font-semibold text-[#8a6315] shadow-sm backdrop-blur">
          현재 위치 기준
        </div>
      </div>

      <SearchBar onSearch={onSearch} />

      <div className="-mx-4 overflow-hidden px-4">
        <CategoryTabs
          selectedCategory={selectedCategory}
          onSelectCategory={onSelectCategory}
        />
      </div>
    </div>
  );
}

function MobilePlaceSheet({
  places,
  visiblePlaces,
  loading,
  errorMessage,
  selectedCategory,
  onToggleWish,
  onRetry,
}: {
  places: Place[];
  visiblePlaces: Place[];
  loading: boolean;
  errorMessage: string | null;
  selectedCategory: string;
  onToggleWish: (id: number) => void;
  onRetry: () => void;
}) {
  return (
    <section className="max-h-[38vh] overflow-hidden rounded-t-[32px] bg-white/95 p-4 shadow-[0_-8px_24px_rgba(0,0,0,0.08)] backdrop-blur">
      <div className="mx-auto mb-3 h-1 w-10 rounded-full bg-gray-300" />
      <div className="flex items-end justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-[#2b210f]">맛집 정보</h2>
          <p className="mt-1 text-sm text-gray-500">
            {categoryLabels[selectedCategory] ?? "선택한"} 맛집을 모아봤어요.
          </p>
        </div>
        <Link
          to="/places/new"
          className="shrink-0 rounded-full bg-[#f6b800] px-4 py-2 text-sm font-semibold text-[#2b210f]"
        >
          등록
        </Link>
      </div>

      <PlaceList
        places={places}
        visiblePlaces={visiblePlaces}
        loading={loading}
        errorMessage={errorMessage}
        selectedCategory={selectedCategory}
        onToggleWish={onToggleWish}
        onRetry={onRetry}
      />
    </section>
  );
}

function PlaceList({
  places,
  visiblePlaces,
  loading,
  errorMessage,
  selectedCategory,
  onToggleWish,
  onRetry,
  compact = false,
}: {
  places: Place[];
  visiblePlaces: Place[];
  loading: boolean;
  errorMessage: string | null;
  selectedCategory: string;
  onToggleWish: (id: number) => void;
  onRetry: () => void;
  compact?: boolean;
}) {
  if (loading) {
    return <StateCard title="맛집을 불러오는 중이에요." flush={compact} />;
  }

  if (errorMessage) {
    return (
      <StateCard
        title={errorMessage}
        desc="잠시 뒤 다시 확인해보세요."
        actionLabel="다시 불러오기"
        onAction={onRetry}
        flush={compact}
      />
    );
  }

  if (places.length === 0) {
    return (
      <StateCard
        title="아직 보여줄 맛집이 없어요."
        desc="우리 동네 첫 꿀맛집을 등록해보세요."
        flush={compact}
      />
    );
  }

  if (visiblePlaces.length === 0) {
    return (
      <StateCard
        title={`${categoryLabels[selectedCategory] ?? "선택한"} 맛집이 아직 없어요.`}
        desc="다른 카테고리도 둘러보세요."
        flush={compact}
      />
    );
  }

  return (
    <div className="mt-4 max-h-[22vh] space-y-3 overflow-y-auto pr-1 lg:max-h-none lg:flex-1">
      {visiblePlaces.map((place) => (
        <Link key={place.id} to={`/places/${place.id}`} className="block">
          <SpaceCard
            title={place.title}
            desc={place.desc}
            distance={place.distance || place.regionName}
            rating={place.rating}
            price={place.price}
            imageUrl={place.imageUrl}
            isWished={place.isWished}
            onToggleWish={() => onToggleWish(place.id)}
          />
        </Link>
      ))}
    </div>
  );
}

function StateCard({
  title,
  desc,
  actionLabel,
  onAction,
  flush = false,
}: {
  title: string;
  desc?: string;
  actionLabel?: string;
  onAction?: () => void;
  flush?: boolean;
}) {
  return (
    <div className={`${flush ? "mt-5" : "mt-4"} rounded-3xl bg-white p-5 text-center shadow-sm`}>
      <p className="text-sm font-semibold text-[#2b210f]">{title}</p>
      {desc && <p className="mt-2 text-sm leading-6 text-gray-500">{desc}</p>}
      {actionLabel && onAction && (
        <button
          type="button"
          onClick={onAction}
          className="mt-4 h-10 rounded-full bg-[#f6b800] px-5 text-sm font-semibold text-[#2b210f]"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
}

function MapCanvas({ places }: { places: Place[] }) {
  const markers = places.slice(0, 8);

  return (
    <div
      aria-hidden="true"
      className="absolute left-0 top-0 z-[1] h-full min-h-screen w-full overflow-hidden bg-[#eaf2e4]"
    >
      <svg
        className="absolute inset-0 h-full w-full"
        viewBox="0 0 430 860"
        preserveAspectRatio="none"
      >
        <rect width="430" height="860" fill="#eaf2e4" />
        <path d="M-20 100 H450 M-20 230 H450 M-20 390 H450 M-20 555 H450 M-20 720 H450" stroke="#ffffff" strokeWidth="22" />
        <path d="M64 -40 V900 M168 -40 V900 M285 -40 V900 M388 -40 V900" stroke="#ffffff" strokeWidth="20" />
        <path d="M-30 610 C120 560 250 650 460 590" stroke="#6d95d8" strokeWidth="14" fill="none" opacity="0.75" />
        <path d="M-20 310 C105 260 250 315 455 250" stroke="#ffffff" strokeWidth="28" fill="none" />
        <path d="M120 -20 C170 160 155 340 220 520 C260 640 245 760 290 900" stroke="#ffffff" strokeWidth="24" fill="none" />
        <rect x="28" y="155" width="112" height="82" rx="18" fill="#d8e8cf" />
        <rect x="254" y="170" width="122" height="92" rx="20" fill="#efd8cf" />
        <rect x="46" y="655" width="144" height="105" rx="22" fill="#d7e3f0" />
        <rect x="265" y="610" width="120" height="88" rx="20" fill="#ece5c9" />
        <text x="34" y="292" fill="#8f9a8d" fontSize="12" fontWeight="700">서교동</text>
        <text x="305" y="334" fill="#8f9a8d" fontSize="12" fontWeight="700">연남동</text>
        <text x="72" y="635" fill="#8f9a8d" fontSize="12" fontWeight="700">망원시장</text>
      </svg>

      {markers.map((place, index) => {
        const positions = [
          ["left-[26%]", "top-[42%]"],
          ["left-[54%]", "top-[34%]"],
          ["left-[68%]", "top-[52%]"],
          ["left-[39%]", "top-[61%]"],
          ["left-[75%]", "top-[28%]"],
          ["left-[18%]", "top-[66%]"],
          ["left-[46%]", "top-[24%]"],
          ["left-[60%]", "top-[70%]"],
        ];
        const [left, top] = positions[index % positions.length];

        return (
          <div
            key={place.id}
            className={`absolute ${left} ${top} flex h-10 w-10 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full bg-[#f6b800] text-sm font-bold text-[#2b210f] shadow-sm ring-4 ring-white/80`}
          >
            {index + 1}
          </div>
        );
      })}

      <div className="absolute left-1/2 top-[58%] flex h-10 w-10 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full bg-[#1473e6] shadow-[0_0_0_8px_rgba(20,115,230,0.18)] ring-4 ring-white">
        <span className="h-3 w-3 rounded-full bg-white" />
      </div>
    </div>
  );
}
