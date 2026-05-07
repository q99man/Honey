import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
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
  const navigate = useNavigate();
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
      <main className="relative mx-auto min-h-screen max-w-[430px] overflow-hidden bg-[#eaf2e4] md:max-w-none">
        <PlaceMap
          places={visiblePlaces}
          onSelectPlace={(placeId) => navigate(`/places/${placeId}`)}
        />

        <section className="relative z-10 mx-auto flex min-h-screen max-w-[430px] flex-col px-4 pb-24 pt-4 sm:pt-5 md:max-w-[760px] md:px-6 md:pb-8 lg:hidden">
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

        <section className="relative z-10 hidden min-h-screen grid-cols-[320px_minmax(0,1fr)_300px] gap-4 px-5 py-5 lg:grid xl:grid-cols-[360px_minmax(0,1fr)_340px] xl:gap-5 xl:px-6 xl:py-6">
          <aside className="flex min-h-0 flex-col rounded-3xl bg-white/95 p-4 shadow-sm backdrop-blur xl:p-5">
            <p className="text-xs font-semibold text-[#d99a00]">Honeytong</p>
            <h1 className="mt-1 text-xl font-bold text-[#2b210f] xl:text-2xl">
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

          <section className="relative min-h-0 overflow-hidden rounded-[32px] border border-white/70 bg-white/10 shadow-sm">
            <div className="absolute left-5 top-5 rounded-full bg-white/95 px-4 py-3 text-sm font-semibold text-[#2b210f] shadow-sm backdrop-blur">
              {visiblePlaces.length > 0
                ? `${visiblePlaces.length}개의 꿀맛집이 보이는 중이에요`
                : "지도에서 동네 맛집을 찾는 중이에요"}
            </div>
            <div className="absolute bottom-5 left-1/2 w-[min(560px,calc(100%-40px))] -translate-x-1/2 rounded-3xl bg-white/95 p-4 shadow-sm backdrop-blur">
              <p className="text-xs font-semibold text-[#d99a00]">
                지도 탐색
              </p>
              <p className="mt-1 text-base font-bold leading-6 text-[#2b210f] xl:text-lg">
                가운데 지도에서 위치를 보고, 양쪽 패널에서 정보를 확인하세요.
              </p>
            </div>
          </section>

          <aside className="flex min-h-0 flex-col rounded-3xl bg-white/95 p-4 shadow-sm backdrop-blur xl:p-5">
            <div>
              <p className="text-xs font-semibold text-[#d99a00]">랭킹</p>
              <h2 className="mt-1 text-lg font-bold text-[#2b210f] xl:text-xl">
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
    <div className="space-y-2 sm:space-y-3 md:mx-auto md:w-full md:max-w-[680px]">
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
    <section className="max-h-[38dvh] overflow-hidden rounded-t-[32px] bg-white/95 p-4 shadow-[0_-8px_24px_rgba(0,0,0,0.08)] backdrop-blur sm:max-h-[36dvh] md:mx-auto md:w-full md:max-w-[680px] md:max-h-[34dvh]">
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
    <div className="mt-4 max-h-[21dvh] space-y-3 overflow-y-auto pr-1 sm:max-h-[20dvh] md:max-h-[18dvh] lg:max-h-none lg:flex-1">
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

function PlaceMap({
  places,
  onSelectPlace,
}: {
  places: Place[];
  onSelectPlace: (placeId: number) => void;
}) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const markersRef = useRef<KakaoMarker[]>([]);
  const appKey = import.meta.env.VITE_KAKAO_JAVASCRIPT_KEY as
    | string
    | undefined;

  useEffect(() => {
    if (!appKey || !containerRef.current) {
      return;
    }

    let canceled = false;

    loadKakaoMapSdk(appKey)
      .then((kakao) => {
        if (canceled || !containerRef.current) {
          return;
        }

        const validPlaces = places.filter(isPlaceWithCoordinates);
        const center = getMapCenter(validPlaces);
        const map =
          mapRef.current ??
          new kakao.maps.Map(containerRef.current, {
            center: new kakao.maps.LatLng(center.latitude, center.longitude),
            level: validPlaces.length > 1 ? 5 : 4,
          });

        mapRef.current = map;
        markersRef.current.forEach((marker) => marker.setMap(null));
        markersRef.current = [];

        if (validPlaces.length === 0) {
          map.setCenter(new kakao.maps.LatLng(center.latitude, center.longitude));
          return;
        }

        const bounds = new kakao.maps.LatLngBounds();
        validPlaces.forEach((place) => {
          const position = new kakao.maps.LatLng(place.latitude, place.longitude);
          bounds.extend(position);

          const marker = new kakao.maps.Marker({
            map,
            position,
            title: place.title,
          });

          kakao.maps.event.addListener(marker, "click", () => {
            onSelectPlace(place.id);
          });

          markersRef.current.push(marker);
        });

        if (validPlaces.length > 1) {
          map.setBounds(bounds);
        } else {
          map.setCenter(
            new kakao.maps.LatLng(
              validPlaces[0].latitude,
              validPlaces[0].longitude,
            ),
          );
        }
      })
      .catch(() => {
        mapRef.current = null;
      });

    return () => {
      canceled = true;
    };
  }, [appKey, places, onSelectPlace]);

  if (!appKey) {
    return (
      <MapStatus
        title="지도 키가 필요해요."
        desc="VITE_KAKAO_JAVASCRIPT_KEY를 설정하면 실제 Kakao 지도로 맛집 위치를 보여드릴게요."
      />
    );
  }

  return (
    <div
      ref={containerRef}
      aria-label="맛집 지도"
      className="absolute left-0 top-0 z-[1] h-full min-h-screen w-full bg-[#eaf2e4]"
    />
  );
}

function MapStatus({ title, desc }: { title: string; desc: string }) {
  return (
    <div className="absolute left-0 top-0 z-[1] flex h-full min-h-screen w-full items-center justify-center bg-[#eaf2e4] px-8 text-center">
      <div className="rounded-3xl bg-white/95 p-5 shadow-sm">
        <p className="text-sm font-bold text-[#2b210f]">{title}</p>
        <p className="mt-2 text-sm leading-6 text-gray-500">{desc}</p>
      </div>
    </div>
  );
}

function isPlaceWithCoordinates(place: Place) {
  return Number.isFinite(place.latitude) && Number.isFinite(place.longitude);
}

function getMapCenter(places: Place[]) {
  if (places.length === 0) {
    return { latitude: 37.5563, longitude: 126.9236 };
  }

  return {
    latitude:
      places.reduce((sum, place) => sum + place.latitude, 0) / places.length,
    longitude:
      places.reduce((sum, place) => sum + place.longitude, 0) / places.length,
  };
}

function loadKakaoMapSdk(appKey: string) {
  if (window.kakao?.maps) {
    return Promise.resolve(window.kakao);
  }

  return new Promise<KakaoWindow>((resolve, reject) => {
    const existingScript = document.getElementById(KAKAO_MAP_SCRIPT_ID);
    if (existingScript) {
      existingScript.addEventListener("load", () => {
        window.kakao?.maps.load(() => resolve(window.kakao!));
      });
      existingScript.addEventListener("error", reject);
      return;
    }

    const script = document.createElement("script");
    script.id = KAKAO_MAP_SCRIPT_ID;
    script.async = true;
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(
      appKey,
    )}&autoload=false`;
    script.onload = () => {
      window.kakao?.maps.load(() => resolve(window.kakao!));
    };
    script.onerror = reject;
    document.head.appendChild(script);
  });
}

const KAKAO_MAP_SCRIPT_ID = "kakao-map-sdk";

type KakaoWindow = {
  maps: {
    load: (callback: () => void) => void;
    LatLng: new (latitude: number, longitude: number) => KakaoLatLng;
    LatLngBounds: new () => KakaoLatLngBounds;
    Map: new (
      container: HTMLElement,
      options: { center: KakaoLatLng; level: number },
    ) => KakaoMap;
    Marker: new (options: {
      map: KakaoMap;
      position: KakaoLatLng;
      title: string;
    }) => KakaoMarker;
    event: {
      addListener: (
        target: KakaoMarker,
        type: "click",
        handler: () => void,
      ) => void;
    };
  };
};

type KakaoLatLng = object;

type KakaoLatLngBounds = {
  extend: (latLng: KakaoLatLng) => void;
};

type KakaoMap = {
  setBounds: (bounds: KakaoLatLngBounds) => void;
  setCenter: (latLng: KakaoLatLng) => void;
};

type KakaoMarker = {
  setMap: (map: KakaoMap | null) => void;
};

declare global {
  interface Window {
    kakao?: KakaoWindow;
  }
}
