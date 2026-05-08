import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type {
  CSSProperties,
  PointerEvent as ReactPointerEvent,
} from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import BottomNav from "../components/BottomNav";
import CategoryTabs from "../components/CategoryTabs";
import SearchBar from "../components/SearchBar";
import {
  FOOD_CATEGORIES,
  getFoodCategory,
  getFoodCategoryLabel,
} from "../constants/foodCategories";
import {
  getKakaoMapJavaScriptKey,
  loadKakaoMapSdk,
  type KakaoMap,
  type KakaoOverlay,
} from "../lib/loadKakaoMapSdk";
import type { Place } from "../types/place";

type Props = {
  places: Place[];
  loading: boolean;
  errorMessage: string | null;
  onToggleWish: (id: number) => void;
  onSearch: (keyword: string) => void;
};

type UserLocation = {
  latitude: number;
  longitude: number;
};

type MapActions = {
  zoomIn: () => void;
  zoomOut: () => void;
};

const mapGestureStyle: CSSProperties & { WebkitUserDrag?: string } = {
  overscrollBehavior: "contain",
  touchAction: "none",
  userSelect: "none",
  WebkitUserDrag: "none",
  WebkitUserSelect: "none",
};

const DEBUG_MAP_LOGS = false;
const DEBUG_MAP_EVENTS = false;
const DEBUG_MAP_OVERLAY_SCAN = false;
// Local-only isolation switch for checking CustomOverlay drag interference.
const DEBUG_MAP_DISABLE_CUSTOM_OVERLAYS = false;

export default function HomePage({
  places,
  loading,
  errorMessage,
  onToggleWish,
  onSearch,
}: Props) {
  const navigate = useNavigate();
  const [selectedCategory, setSelectedCategory] = useState("ALL");
  const [selectedPlaceId, setSelectedPlaceId] = useState<number | null>(null);
  const [mobileSheetOpen, setMobileSheetOpen] = useState(false);
  const [desktopCategoryMenuOpen, setDesktopCategoryMenuOpen] = useState(false);
  const [userLocation, setUserLocation] = useState<UserLocation | null>(null);
  const [locationMessage, setLocationMessage] = useState<string | null>(null);
  const [mapActions, setMapActions] = useState<MapActions | null>(null);

  const visiblePlaces = useMemo(() => {
    if (selectedCategory === "ALL") {
      return places;
    }
    return places.filter((place) => place.category === selectedCategory);
  }, [places, selectedCategory]);

  const selectedPlace = useMemo(() => {
    return visiblePlaces.find((place) => place.id === selectedPlaceId) ?? null;
  }, [selectedPlaceId, visiblePlaces]);

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

  const handleSelectPlace = useCallback((placeId: number) => {
    setSelectedPlaceId(placeId);
    setMobileSheetOpen(true);
  }, []);

  const handleSelectCategory = useCallback((value: string) => {
    setSelectedCategory(value);
    setSelectedPlaceId(null);
    setMobileSheetOpen(false);
    setDesktopCategoryMenuOpen(false);
  }, []);

  const handleUseCurrentLocation = useCallback(async () => {
    if (!navigator.geolocation) {
      setLocationMessage("현재 위치를 확인할 수 없어요.");
      return;
    }

    setLocationMessage(null);
    try {
      const position = await getCurrentPosition();
      setUserLocation({
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
      });
      setLocationMessage("현재 위치를 지도에 표시했어요.");
    } catch {
      setLocationMessage("현재 위치 권한을 확인해 주세요.");
    }
  }, []);

  return (
    <div className="min-h-screen bg-neutral-100">
      <main className="relative mx-auto h-[100dvh] min-h-[640px] max-w-[430px] overflow-hidden bg-[#eaf2e4] md:max-w-none">
        <PlaceMap
          places={visiblePlaces}
          selectedPlaceId={selectedPlace?.id ?? null}
          userLocation={userLocation}
          onSelectPlace={handleSelectPlace}
          onMapActionsReady={setMapActions}
        />

        <section
          data-map-ui-layer="mobile-overlay"
          className="pointer-events-none absolute inset-0 z-10 mx-auto max-w-[430px] md:max-w-[760px] lg:hidden"
        >
          <div className="pointer-events-none absolute inset-x-0 top-0 z-20 px-4 pt-4 sm:pt-5 md:px-6">
            <MobileFloatingHeader
              selectedCategory={selectedCategory}
              onSelectCategory={handleSelectCategory}
              onSearch={onSearch}
            />
          </div>

          <FloatingMapActions
            mapActions={mapActions}
            onUseCurrentLocation={handleUseCurrentLocation}
            onRegister={() => navigate("/places/new")}
          />

          <div className="pointer-events-none absolute inset-x-0 bottom-0 z-30 px-4 pb-24 md:px-6 md:pb-8">
            <MobileSelectedPlaceSheet
              places={places}
              visiblePlaces={visiblePlaces}
              selectedPlace={selectedPlace}
              open={mobileSheetOpen}
              loading={loading}
              errorMessage={errorMessage}
              selectedCategory={selectedCategory}
              locationMessage={locationMessage}
              onToggleWish={onToggleWish}
              onRetry={() => onSearch("")}
              onClose={() => setMobileSheetOpen(false)}
            />
          </div>
        </section>

        <section
          data-map-ui-layer="desktop-overlay"
          className="hidden"
        >
          <aside className="pointer-events-auto flex min-h-0 flex-col rounded-[28px] bg-white/95 p-4 shadow-sm backdrop-blur xl:p-5">
            <p className="text-xs font-semibold text-[#d99a00]">Honeytong</p>
            <h1 className="mt-1 text-xl font-bold text-[#2b210f] xl:text-2xl">
              지도로 찾는 우리 동네 꿀맛집
            </h1>
            <p className="mt-2 text-sm leading-6 text-gray-600">
              카테고리를 고르고 마커를 눌러 한 곳씩 탐색해보세요.
            </p>

            <div className="mt-5">
              <SearchBar onSearch={onSearch} />
            </div>

            <div className="mt-4">
              <CategoryTabs
                selectedCategory={selectedCategory}
                onSelectCategory={handleSelectCategory}
              />
            </div>

            <div className="mt-5 grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={handleUseCurrentLocation}
                className="h-11 rounded-full border border-[#f6d365] bg-white text-sm font-bold text-[#2b210f]"
              >
                내 위치
              </button>
              <Link
                to="/places/new"
                className="flex h-11 items-center justify-center rounded-full bg-[#f6b800] text-sm font-bold text-[#2b210f]"
              >
                맛집 등록
              </Link>
            </div>

            <DesktopSelectedPlacePanel
              places={places}
              visiblePlaces={visiblePlaces}
              selectedPlace={selectedPlace}
              loading={loading}
              errorMessage={errorMessage}
              selectedCategory={selectedCategory}
              locationMessage={locationMessage}
              onToggleWish={onToggleWish}
              onRetry={() => onSearch("")}
            />
          </aside>

          <section className="pointer-events-none relative min-h-0 overflow-hidden rounded-[32px] border border-white/70 bg-white/10 shadow-sm">
            <div className="absolute left-5 top-5 rounded-full bg-white/95 px-4 py-3 text-sm font-semibold text-[#2b210f] shadow-sm backdrop-blur">
              {visiblePlaces.length > 0
                ? `${visiblePlaces.length}개의 꿀맛 마커가 보이는 중이에요`
                : "지도에서 동네 맛집을 찾는 중이에요"}
            </div>
            <div className="absolute bottom-5 left-1/2 w-[min(560px,calc(100%-40px))] -translate-x-1/2 rounded-[28px] bg-white/95 p-4 shadow-sm backdrop-blur">
              <p className="text-xs font-semibold text-[#d99a00]">
                지도 탐색
              </p>
              <p className="mt-1 text-base font-bold leading-6 text-[#2b210f] xl:text-lg">
                마커를 누르면 선택한 맛집 카드가 왼쪽에 고정돼요.
              </p>
            </div>
          </section>

          <aside className="pointer-events-auto flex min-h-0 flex-col rounded-[28px] bg-white/95 p-4 shadow-sm backdrop-blur xl:p-5">
            <div>
              <p className="text-xs font-semibold text-[#d99a00]">랭킹</p>
              <h2 className="mt-1 text-lg font-bold text-[#2b210f] xl:text-xl">
                지금 반응 좋은 맛집
              </h2>
              <p className="mt-2 text-sm leading-6 text-gray-500">
                추천, 방문, 댓글 수를 기준으로 정리했어요.
              </p>
            </div>

            <div className="mt-5 space-y-3 overflow-y-auto pr-1">
              {rankedPlaces.length > 0 ? (
                rankedPlaces.map((place, index) => (
                  <button
                    key={place.id}
                    type="button"
                    onClick={() => {
                      setSelectedPlaceId(place.id);
                      setMobileSheetOpen(true);
                    }}
                    className="block w-full rounded-[24px] border border-gray-100 bg-white p-4 text-left shadow-sm transition active:scale-[0.99]"
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
                  </button>
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

        <section
          data-map-ui-layer="desktop-map-layout"
          className="pointer-events-none absolute inset-0 z-10 hidden lg:block"
        >
          <DesktopSideNav />

          <aside className="pointer-events-auto absolute inset-y-5 left-[104px] z-20 flex w-[330px] min-h-0 flex-col rounded-[28px] border border-white/80 bg-white/95 p-4 shadow-[0_12px_36px_rgba(43,33,15,0.12)] backdrop-blur xl:left-[112px] xl:w-[360px] xl:p-5">
            <div>
              <SearchBar onSearch={onSearch} />
            </div>

            <div className="mt-5 flex items-start justify-between gap-3">
              <div className="min-w-0">
                <button
                  type="button"
                  onClick={handleUseCurrentLocation}
                  className="flex max-w-full items-center gap-2 text-left text-xl font-black text-[#2b210f] transition active:scale-[0.99]"
                >
                  <span className="truncate">부개3동</span>
                  <span className="text-sm text-gray-400">⌄</span>
                </button>
                <p className="mt-1 text-xs font-semibold text-gray-400">
                  현재 지도 중심 지역
                </p>
              </div>
              <span className="shrink-0 rounded-full bg-[#fff7d8] px-3 py-1.5 text-xs font-bold text-[#8a6315]">
                인천 부평구
              </span>
            </div>

            <DesktopSelectedPlacePanel
              places={places}
              visiblePlaces={visiblePlaces}
              selectedPlace={selectedPlace}
              loading={loading}
              errorMessage={errorMessage}
              selectedCategory={selectedCategory}
              locationMessage={locationMessage}
              onToggleWish={onToggleWish}
              onRetry={() => onSearch("")}
            />

            <div className="mt-4 border-t border-gray-100 pt-4">
              <div className="flex items-center justify-between gap-3">
                <p className="text-sm font-black text-[#2b210f]">둘러보기</p>
                <Link
                  to="/ranking"
                  className="text-xs font-bold text-[#8a6315] underline-offset-4 hover:underline"
                >
                  랭킹 보기
                </Link>
              </div>
              <DesktopExploreChips
                selectedCategory={selectedCategory}
                onSelectCategory={handleSelectCategory}
              />
            </div>
          </aside>

          <div className="pointer-events-none absolute inset-y-0 left-[452px] z-10 w-px bg-gray-200/80 xl:left-[492px]" />

          <DesktopMapCategoryBar
            selectedCategory={selectedCategory}
            open={desktopCategoryMenuOpen}
            onSelectCategory={handleSelectCategory}
            onToggleOpen={() =>
              setDesktopCategoryMenuOpen((currentOpen) => !currentOpen)
            }
            onClose={() => setDesktopCategoryMenuOpen(false)}
          />

          <div className="pointer-events-none absolute left-[472px] top-28 z-10 rounded-full bg-white/95 px-4 py-3 text-sm font-semibold text-[#2b210f] shadow-sm backdrop-blur xl:left-[512px]">
            {visiblePlaces.length > 0
              ? `${visiblePlaces.length}개의 꿀맛 마커가 보이는 중이에요`
              : "지도에서 동네 맛집을 찾는 중이에요"}
          </div>

          <div className="pointer-events-none absolute bottom-6 left-[472px] z-10 rounded-[22px] border border-white/80 bg-white/95 px-4 py-3 shadow-sm backdrop-blur xl:left-[512px]">
            <p className="text-xs font-bold text-[#8a6315]">☀ 21°</p>
            <p className="mt-1 text-xs font-semibold text-gray-500">
              미세먼지 보통
            </p>
          </div>

          <DesktopMapControls
            mapActions={mapActions}
            onUseCurrentLocation={handleUseCurrentLocation}
            onRegister={() => navigate("/places/new")}
          />
        </section>
      </main>
      <div data-bottom-nav="mobile-bottom-nav" className="lg:hidden">
        <BottomNav />
      </div>
    </div>
  );
}

function DesktopSideNav() {
  const navItems = [
    { to: "/", label: "\uD648", icon: "H", end: true },
    { to: "/ranking", label: "\uB7AD\uD0B9", icon: "#", end: false },
    { to: "/wishlist", label: "\uC990\uACA8\uCC3E\uAE30", icon: "*", end: false },
    { to: "/places/new", label: "\uB4F1\uB85D", icon: "+", end: false },
    { to: "/my", label: "\uB0B4 \uC815\uBCF4", icon: "i", end: false },
  ];

  return (
    <nav className="pointer-events-auto absolute inset-y-0 left-0 z-30 flex w-[84px] flex-col items-center border-r border-gray-100/80 bg-white/95 py-6 shadow-[8px_0_30px_rgba(43,33,15,0.08)] backdrop-blur">
      <Link
        to="/"
        className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#f6b800] text-xl font-black text-white shadow-sm"
        aria-label="Honeytong \uD648"
      >
        H
      </Link>

      <div className="mt-8 flex w-full flex-1 flex-col items-center gap-2">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            className={({ isActive }) =>
              `flex w-[68px] flex-col items-center justify-center gap-1 rounded-2xl px-2 py-3 text-[11px] font-bold transition ${
                isActive
                  ? "bg-[#fff3c4] text-[#8a6315]"
                  : "text-gray-500 hover:bg-gray-50 hover:text-[#2b210f]"
              }`
            }
          >
            <span className="text-xl leading-none">{item.icon}</span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </div>

      <Link
        to="/my"
        className="flex w-[68px] flex-col items-center justify-center gap-2 rounded-2xl px-2 py-3 text-[11px] font-bold text-gray-500 hover:bg-gray-50"
      >
        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-200 text-xs text-gray-600">
          {"\uB098"}
        </span>
        {"\uB0B4 \uC815\uBCF4"}
      </Link>
    </nav>
  );
}

function DesktopMapCategoryBar({
  selectedCategory,
  open,
  onSelectCategory,
  onToggleOpen,
  onClose,
}: {
  selectedCategory: string;
  open: boolean;
  onSelectCategory: (value: string) => void;
  onToggleOpen: () => void;
  onClose: () => void;
}) {
  const primaryCategories = FOOD_CATEGORIES.filter(
    (category) => category.value !== "ALL",
  ).slice(0, 7);
  const extraCategories = FOOD_CATEGORIES.filter(
    (category) =>
      category.value === "ALL" ||
      !primaryCategories.some(
        (primaryCategory) => primaryCategory.value === category.value,
      ),
  );

  return (
    <div className="pointer-events-none absolute left-[472px] right-[120px] top-5 z-20 xl:left-[512px] xl:right-[136px]">
      <div className="pointer-events-auto flex items-center gap-2">
        <div className="flex min-w-0 flex-1 gap-2 overflow-hidden rounded-[22px] border border-gray-100 bg-white/95 p-2 shadow-[0_10px_28px_rgba(43,33,15,0.12)] backdrop-blur">
          {primaryCategories.map((category) => (
            <button
              key={category.value}
              type="button"
              onClick={() => onSelectCategory(category.value)}
              className={`flex h-10 min-w-[92px] items-center justify-center gap-2 rounded-[16px] border px-3 text-sm font-bold transition active:scale-[0.98] ${
                selectedCategory === category.value
                  ? "border-[#f6d365] bg-[#fff3c4] text-[#8a6315]"
                  : "border-gray-100 bg-white text-[#2b210f] hover:border-[#f6d365]"
              }`}
            >
              <span aria-hidden="true">{category.emoji}</span>
              <span className="truncate">{category.label}</span>
            </button>
          ))}
        </div>

        <div className="relative">
          <button
            type="button"
            onClick={onToggleOpen}
            aria-expanded={open}
            className={`flex h-12 w-12 items-center justify-center rounded-full border text-xl font-black shadow-[0_10px_28px_rgba(43,33,15,0.12)] transition active:scale-95 ${
              open
                ? "border-[#f6d365] bg-[#fff3c4] text-[#8a6315]"
                : "border-gray-100 bg-white/95 text-[#2b210f]"
            }`}
          >
            ...
          </button>

          {open && (
            <div className="absolute right-0 top-14 w-[300px] rounded-[24px] border border-gray-100 bg-white/95 p-3 shadow-[0_18px_48px_rgba(43,33,15,0.18)] backdrop-blur">
              <div className="grid grid-cols-3 gap-2">
                {extraCategories.map((category) => (
                  <button
                    key={category.value}
                    type="button"
                    onClick={() => {
                      onSelectCategory(category.value);
                      onClose();
                    }}
                    className={`flex min-h-16 flex-col items-center justify-center gap-1 rounded-[18px] border px-2 py-2 text-xs font-bold transition active:scale-[0.98] ${
                      selectedCategory === category.value
                        ? "border-[#f6d365] bg-[#fff3c4] text-[#8a6315]"
                        : "border-gray-100 bg-white text-[#2b210f] hover:border-[#f6d365]"
                    }`}
                  >
                    <span className="text-lg" aria-hidden="true">
                      {category.emoji}
                    </span>
                    <span className="line-clamp-1">{category.label}</span>
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function DesktopExploreChips({
  selectedCategory,
  onSelectCategory,
}: {
  selectedCategory: string;
  onSelectCategory: (value: string) => void;
}) {
  const categories = FOOD_CATEGORIES.filter(
    (category) => category.value !== "ALL",
  ).slice(0, 5);

  return (
    <div className="mt-3 flex gap-2 overflow-hidden">
      {categories.map((category) => (
        <button
          key={category.value}
          type="button"
          onClick={() => onSelectCategory(category.value)}
          className={`flex h-9 min-w-0 shrink-0 items-center gap-1.5 rounded-full border px-3 text-xs font-bold transition active:scale-[0.98] ${
            selectedCategory === category.value
              ? "border-[#f6d365] bg-[#fff3c4] text-[#8a6315]"
              : "border-gray-100 bg-white text-gray-600"
          }`}
        >
          <span aria-hidden="true">{category.emoji}</span>
          <span>{category.label}</span>
        </button>
      ))}
    </div>
  );
}

function DesktopMapControls({
  mapActions,
  onUseCurrentLocation,
  onRegister,
}: {
  mapActions: MapActions | null;
  onUseCurrentLocation: () => void;
  onRegister: () => void;
}) {
  const controls = [
    {
      label: "\uD604\uC7AC\uC704\uCE58",
      icon: "o",
      onClick: onUseCurrentLocation,
      disabled: false,
    },
    {
      label: "\uB9DB\uC9D1 \uB4F1\uB85D",
      icon: "+",
      onClick: onRegister,
      disabled: false,
    },
    {
      label: "\uD655\uB300",
      icon: "+",
      onClick: mapActions?.zoomIn,
      disabled: !mapActions,
    },
    {
      label: "\uCD95\uC18C",
      icon: "-",
      onClick: mapActions?.zoomOut,
      disabled: !mapActions,
    },
  ];

  return (
    <div className="pointer-events-auto absolute right-6 top-1/2 z-20 flex -translate-y-1/2 flex-col overflow-hidden rounded-[24px] border border-gray-100 bg-white/95 shadow-[0_14px_38px_rgba(43,33,15,0.14)] backdrop-blur">
      {controls.map((control, index) => (
        <button
          key={control.label}
          type="button"
          onClick={control.onClick}
          disabled={control.disabled}
          className={`flex h-[74px] w-[70px] flex-col items-center justify-center gap-1 text-xs font-bold text-[#2b210f] transition hover:bg-[#fff8df] active:scale-[0.98] disabled:opacity-50 ${
            index > 0 ? "border-t border-gray-100" : ""
          }`}
        >
          <span className="text-xl leading-none" aria-hidden="true">
            {control.icon}
          </span>
          <span>{control.label}</span>
        </button>
      ))}
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
    <div
      data-map-control="mobile-header"
      className="pointer-events-auto space-y-2 sm:space-y-3 md:mx-auto md:w-full md:max-w-[680px]"
    >
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

function FloatingMapActions({
  mapActions,
  onUseCurrentLocation,
  onRegister,
}: {
  mapActions: MapActions | null;
  onUseCurrentLocation: () => void;
  onRegister: () => void;
}) {
  return (
    <div
      data-map-control="floating-actions"
      className="pointer-events-auto absolute right-3 top-[126px] z-20 flex flex-col gap-1.5 sm:top-[132px]"
    >
      <button
        type="button"
        onClick={mapActions?.zoomIn}
        disabled={!mapActions}
        aria-label="지도 확대"
        className="flex h-9 w-9 items-center justify-center rounded-full border border-[#f6d365] bg-white/95 text-lg font-black text-[#2b210f] shadow-md backdrop-blur active:scale-95 disabled:opacity-50"
      >
        +
      </button>
      <button
        type="button"
        onClick={mapActions?.zoomOut}
        disabled={!mapActions}
        aria-label="지도 축소"
        className="flex h-9 w-9 items-center justify-center rounded-full border border-[#f6d365] bg-white/95 text-lg font-black text-[#2b210f] shadow-md backdrop-blur active:scale-95 disabled:opacity-50"
      >
        −
      </button>
      <button
        type="button"
        onClick={onUseCurrentLocation}
        aria-label="현재 위치로 이동"
        className="flex h-9 w-9 items-center justify-center rounded-full border border-[#bfe8d3] bg-white/95 text-base font-bold text-[#2f6f5f] shadow-md backdrop-blur active:scale-95"
      >
        ◎
      </button>
      <button
        type="button"
        onClick={onRegister}
        aria-label="맛집 등록"
        className="flex h-9 w-9 items-center justify-center rounded-full bg-[#f6b800] text-base font-bold text-[#2b210f] shadow-md active:scale-95"
      >
        ＋
      </button>
    </div>
  );
}

function MobileSelectedPlaceSheet({
  places,
  visiblePlaces,
  selectedPlace,
  open,
  loading,
  errorMessage,
  selectedCategory,
  locationMessage,
  onToggleWish,
  onRetry,
  onClose,
}: {
  places: Place[];
  visiblePlaces: Place[];
  selectedPlace: Place | null;
  open: boolean;
  loading: boolean;
  errorMessage: string | null;
  selectedCategory: string;
  locationMessage: string | null;
  onToggleWish: (id: number) => void;
  onRetry: () => void;
  onClose: () => void;
}) {
  const startYRef = useRef<number | null>(null);

  const handlePointerDown = (event: ReactPointerEvent<HTMLButtonElement>) => {
    startYRef.current = event.clientY;
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handlePointerUp = (event: ReactPointerEvent<HTMLButtonElement>) => {
    const startY = startYRef.current;
    startYRef.current = null;
    if (startY != null && event.clientY - startY > 28) {
      onClose();
    }
  };

  return (
    <section
      data-selected-place-sheet="mobile-sheet"
      className={`pointer-events-auto relative z-30 rounded-t-[28px] bg-white/95 p-4 shadow-[0_-8px_24px_rgba(0,0,0,0.10)] backdrop-blur transition-transform duration-300 ease-out md:mx-auto md:w-full md:max-w-[680px] ${
        open ? "translate-y-0" : "translate-y-[calc(100%-20px)]"
      }`}
    >
      <button
        type="button"
        aria-label="맛집 상세 카드 내리기"
        onClick={onClose}
        onPointerDown={handlePointerDown}
        onPointerUp={handlePointerUp}
        className="mx-auto mb-3 flex h-5 w-20 items-center justify-center"
      >
        <span className="h-1 w-10 rounded-full bg-gray-300" />
      </button>
      {open && (
        <PanelState
          places={places}
          visiblePlaces={visiblePlaces}
          selectedPlace={selectedPlace}
          loading={loading}
          errorMessage={errorMessage}
          selectedCategory={selectedCategory}
          locationMessage={locationMessage}
          onToggleWish={onToggleWish}
          onRetry={onRetry}
        />
      )}
    </section>
  );
}

function DesktopSelectedPlacePanel({
  places,
  visiblePlaces,
  selectedPlace,
  loading,
  errorMessage,
  selectedCategory,
  locationMessage,
  onToggleWish,
  onRetry,
}: {
  places: Place[];
  visiblePlaces: Place[];
  selectedPlace: Place | null;
  loading: boolean;
  errorMessage: string | null;
  selectedCategory: string;
  locationMessage: string | null;
  onToggleWish: (id: number) => void;
  onRetry: () => void;
}) {
  return (
    <div className="mt-5 min-h-0 flex-1 overflow-y-auto pr-1">
      <PanelState
        places={places}
        visiblePlaces={visiblePlaces}
        selectedPlace={selectedPlace}
        loading={loading}
        errorMessage={errorMessage}
        selectedCategory={selectedCategory}
        locationMessage={locationMessage}
        onToggleWish={onToggleWish}
        onRetry={onRetry}
      />
    </div>
  );
}

function PanelState({
  places,
  visiblePlaces,
  selectedPlace,
  loading,
  errorMessage,
  selectedCategory,
  locationMessage,
  onToggleWish,
  onRetry,
}: {
  places: Place[];
  visiblePlaces: Place[];
  selectedPlace: Place | null;
  loading: boolean;
  errorMessage: string | null;
  selectedCategory: string;
  locationMessage: string | null;
  onToggleWish: (id: number) => void;
  onRetry: () => void;
}) {
  if (loading) {
    return <StateCard title="맛집을 불러오는 중이에요." />;
  }

  if (errorMessage) {
    return (
      <StateCard
        title={errorMessage}
        desc="잠시 뒤 다시 확인해보세요."
        actionLabel="다시 불러오기"
        onAction={onRetry}
      />
    );
  }

  if (places.length === 0) {
    return (
      <StateCard
        title="아직 보여줄 맛집이 없어요."
        desc="우리 동네 첫 꿀맛집을 등록해보세요."
      />
    );
  }

  if (visiblePlaces.length === 0) {
    return (
      <StateCard
        title={`${getFoodCategoryLabel(selectedCategory)} 맛집이 아직 없어요.`}
        desc="다른 카테고리도 둘러보세요."
      />
    );
  }

  if (!selectedPlace) {
    return (
      <StateCard
        title="지도에서 맛집 마커를 선택해 주세요."
        desc="선택한 맛집 정보가 여기에 표시돼요."
      />
    );
  }

  return (
    <SelectedPlaceCard
      place={selectedPlace}
      locationMessage={locationMessage}
      onToggleWish={() => onToggleWish(selectedPlace.id)}
    />
  );
}

function SelectedPlaceCard({
  place,
  locationMessage,
  onToggleWish,
}: {
  place: Place;
  locationMessage: string | null;
  onToggleWish: () => void;
}) {
  const category = getFoodCategory(place.category);

  return (
    <article className="overflow-hidden rounded-[26px] bg-white shadow-sm">
      <div className="relative h-36 bg-[#fff1bf] sm:h-40 lg:h-44">
        {place.imageUrl ? (
          <img
            src={place.imageUrl}
            alt={`${place.title} 대표 이미지`}
            className="h-full w-full object-cover"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center px-6 text-center text-sm font-bold leading-6 text-[#8a6315]">
            {category.emoji} 꿀맛집 이미지 준비 중
          </div>
        )}
        <button
          type="button"
          aria-label={place.isWished ? "찜 해제" : "찜하기"}
          onClick={onToggleWish}
          className="absolute right-3 top-3 flex h-10 w-10 items-center justify-center rounded-full bg-white/95 text-xl font-bold text-[#d99a00] shadow-sm active:scale-95"
        >
          {place.isWished ? "♥" : "♡"}
        </button>
      </div>

      <div className="p-4">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="text-xs font-bold text-[#d99a00]">
              {category.emoji} {category.label}
            </p>
            <h2 className="mt-1 line-clamp-2 text-lg font-bold leading-6 text-[#2b210f]">
              {place.title}
            </h2>
          </div>
          <span className="shrink-0 rounded-full bg-[#eaf7ef] px-3 py-1 text-xs font-bold text-[#2f6f5f]">
            {place.regionName || "동네"}
          </span>
        </div>

        <p className="mt-2 line-clamp-2 text-sm leading-6 text-gray-600">
          {place.desc || "동네 사람들이 추천한 꿀맛집이에요."}
        </p>

        <div className="mt-3 grid grid-cols-3 gap-2 text-center text-xs font-bold text-[#8a6315]">
          <InfoPill label="추천" value={String(place.recommendCount)} />
          <InfoPill label="방문" value={String(place.visitCount)} />
          <InfoPill label="댓글" value={String(place.commentCount)} />
        </div>

        <div className="mt-3 space-y-1 text-xs leading-5 text-gray-500">
          <p className="line-clamp-1">위치: {place.address || place.regionName}</p>
          <p className="line-clamp-1">가격: {place.price}</p>
          {locationMessage && (
            <p className="font-semibold text-[#2f6f5f]">{locationMessage}</p>
          )}
        </div>

        <div className="mt-4 grid grid-cols-[1fr_1fr] gap-2">
          <Link
            to={`/places/${place.id}`}
            className="flex h-11 items-center justify-center rounded-full bg-[#f6b800] text-sm font-bold text-[#2b210f]"
          >
            상세보기
          </Link>
          <a
            href={getKakaoMapUrl(place)}
            target="_blank"
            rel="noreferrer"
            className="flex h-11 items-center justify-center rounded-full border border-[#f6d365] bg-white text-sm font-bold text-[#2b210f]"
          >
            카카오 지도
          </a>
        </div>
      </div>
    </article>
  );
}

function InfoPill({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl bg-[#fff8db] px-2 py-2">
      <p>{value}</p>
      <p className="mt-0.5 text-[11px] font-semibold text-gray-500">{label}</p>
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
    <div className={`${flush ? "mt-5" : ""} rounded-[24px] bg-white p-5 text-center shadow-sm`}>
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
  selectedPlaceId,
  userLocation,
  onSelectPlace,
  onMapActionsReady,
}: {
  places: Place[];
  selectedPlaceId: number | null;
  userLocation: UserLocation | null;
  onSelectPlace: (placeId: number) => void;
  onMapActionsReady: (actions: MapActions | null) => void;
}) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const overlaysRef = useRef<KakaoOverlay[]>([]);
  const userOverlayRef = useRef<KakaoOverlay | null>(null);
  const debugListenersAddedRef = useRef(false);
  const fittedPlacesKeyRef = useRef<string | null>(null);
  const selectedPlaceIdRef = useRef<number | null>(null);
  const userLocationKeyRef = useRef<string | null>(null);
  const lastLayoutKeyRef = useRef<string | null>(null);
  const [containerReady, setContainerReady] = useState(false);
  const [mapReady, setMapReady] = useState(false);
  const [mapLayoutVersion, setMapLayoutVersion] = useState(0);
  const appKey = getKakaoMapJavaScriptKey();

  if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
    console.log("[Honeytong Map Debug] render PlaceMap");
  }

  const clearMapOverlays = useCallback(() => {
    try {
      overlaysRef.current.forEach((overlay) => overlay.setMap(null));
      overlaysRef.current = [];
      userOverlayRef.current?.setMap(null);
      userOverlayRef.current = null;
    } catch (error) {
      console.error("[Honeytong Map Debug] cleanup overlay failed", error);
    }
  }, []);

  useEffect(() => {
    if (!import.meta.env.DEV || !DEBUG_MAP_EVENTS) {
      return;
    }

    const handleMapGestureEvent = (
      event:
        | globalThis.MouseEvent
        | globalThis.PointerEvent
        | globalThis.TouchEvent,
    ) => {
      const point =
        "touches" in event && event.touches.length > 0
          ? event.touches[0]
          : "changedTouches" in event && event.changedTouches.length > 0
            ? event.changedTouches[0]
            : event;
      const clientX = "clientX" in point ? point.clientX : 0;
      const clientY = "clientY" in point ? point.clientY : 0;
      const target = document.elementFromPoint(clientX, clientY);
      const mapRoot = containerRef.current;
      const targetElement = target instanceof Element ? target : null;
      const blockingLayer = targetElement?.closest(
        "[data-map-ui-layer], [data-map-control], [data-selected-place-sheet], [data-bottom-nav]",
      );

      console.log("[Honeytong Map Debug] map gesture event", {
        cancelable: event.cancelable,
        defaultPrevented: event.defaultPrevented,
        pointerType: "pointerType" in event ? event.pointerType : null,
        touchCount: "touches" in event ? event.touches.length : null,
        type: event.type,
        x: clientX,
        y: clientY,
        target,
        targetTag: targetElement?.tagName ?? null,
        targetClass:
          targetElement instanceof HTMLElement ||
          targetElement instanceof SVGElement
            ? targetElement.className
            : null,
        targetId: targetElement?.id ?? null,
        isInsideMap: !!mapRoot?.contains(target),
        mapRect: mapRoot?.getBoundingClientRect().toJSON() ?? null,
        targetRect: targetElement?.getBoundingClientRect().toJSON() ?? null,
        blockingLayer:
          blockingLayer instanceof HTMLElement
            ? {
                tag: blockingLayer.tagName,
                className: blockingLayer.className,
                marker:
                  blockingLayer.dataset.mapUiLayer ??
                  blockingLayer.dataset.mapControl ??
                  blockingLayer.dataset.selectedPlaceSheet ??
                  blockingLayer.dataset.bottomNav ??
                  null,
              }
            : null,
        pointerEvents:
          targetElement instanceof Element
            ? window.getComputedStyle(targetElement).pointerEvents
            : null,
      });
    };

    const eventNames = [
      "mousedown",
      "mousemove",
      "mouseup",
      "pointerdown",
      "pointermove",
      "pointerup",
      "touchstart",
      "touchmove",
      "touchend",
    ] as const;

    eventNames.forEach((eventName) => {
      window.addEventListener(eventName, handleMapGestureEvent, true);
    });

    return () => {
      eventNames.forEach((eventName) => {
        window.removeEventListener(eventName, handleMapGestureEvent, true);
      });
    };
  }, []);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return;
    }

    let frame: number | null = null;
    let timer: number | null = null;

    const syncContainerReady = () => {
      const rect = container.getBoundingClientRect();
      const ready = rect.width > 0 && rect.height > 0;

      if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
        console.log("[Honeytong Map Debug] container ready check", {
          ready,
          rect: rect.toJSON(),
        });
      }

      if (ready) {
        setContainerReady(true);
      }
    };

    syncContainerReady();
    frame = window.requestAnimationFrame(syncContainerReady);
    timer = window.setTimeout(syncContainerReady, 100);

    const observer =
      typeof ResizeObserver === "undefined"
        ? null
        : new ResizeObserver(syncContainerReady);
    observer?.observe(container);

    return () => {
      if (frame != null) {
        window.cancelAnimationFrame(frame);
      }
      if (timer != null) {
        window.clearTimeout(timer);
      }
      observer?.disconnect();
    };
  }, []);

  useEffect(() => {
    if (!mapReady || !containerRef.current || !mapRef.current) {
      return;
    }

    const container = containerRef.current;
    const map = mapRef.current;
    let frame: number | null = null;
    let timer: number | null = null;

    const runRelayout = (reason: "resize" | "viewport change") => {
      frame = null;
      const rect = container.getBoundingClientRect();

      if (!isUsableMapContainerRect(container, rect)) {
        if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
          console.log(`[Honeytong Map Debug] map relayout skipped after ${reason}`, {
            height: rect.height,
            width: rect.width,
          });
          console.log("[Honeytong Map Debug] map container rect unstable, postpone reinit", {
            reason,
          });
        }
        scheduleRelayout(reason, 140);
        return;
      }

      const nextLayoutKey = `${Math.round(rect.width)}x${Math.round(
        rect.height,
      )}@${window.devicePixelRatio}`;
      if (lastLayoutKeyRef.current === nextLayoutKey) {
        return;
      }

      lastLayoutKeyRef.current = nextLayoutKey;
      map.relayout();
      map.setDraggable(true);
      map.setZoomable(true);

      if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
        console.log(`[Honeytong Map Debug] map relayout after ${reason}`, {
          devicePixelRatio: window.devicePixelRatio,
          height: rect.height,
          width: rect.width,
        });
      }

      setMapLayoutVersion((version) => version + 1);
    };

    const scheduleRelayout = (
      reason: "resize" | "viewport change",
      delay = 120,
    ) => {
      if (timer != null) {
        window.clearTimeout(timer);
      }

      timer = window.setTimeout(() => {
        timer = null;
        if (frame != null) {
          window.cancelAnimationFrame(frame);
        }
        frame = window.requestAnimationFrame(() => runRelayout(reason));
      }, delay);
    };

    const resizeObserver =
      typeof ResizeObserver === "undefined"
        ? null
        : new ResizeObserver(() => scheduleRelayout("resize", 80));
    resizeObserver?.observe(container);

    const handleViewportChange = () => {
      scheduleRelayout("viewport change", 140);
    };

    window.addEventListener("resize", handleViewportChange);
    window.addEventListener("orientationchange", handleViewportChange);
    window.visualViewport?.addEventListener("resize", handleViewportChange);
    scheduleRelayout("resize", 0);

    return () => {
      resizeObserver?.disconnect();
      window.removeEventListener("resize", handleViewportChange);
      window.removeEventListener("orientationchange", handleViewportChange);
      window.visualViewport?.removeEventListener("resize", handleViewportChange);
      if (frame != null) {
        window.cancelAnimationFrame(frame);
      }
      if (timer != null) {
        window.clearTimeout(timer);
      }
    };
  }, [mapReady]);

  useEffect(() => {
    if (!appKey || !containerReady || !containerRef.current) {
      return;
    }

    let canceled = false;
    let relayoutFrame: number | null = null;
    let relayoutTimer: number | null = null;
    let secondRelayoutTimer: number | null = null;
    let markerRetryTimer: number | null = null;
    const container = containerRef.current;
    const initialRect = container.getBoundingClientRect();

    if (!isUsableMapContainerRect(container, initialRect)) {
        if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
          console.log("[Honeytong Map Debug] map container rect unstable, postpone reinit", {
          height: initialRect.height,
          phase: "map-effect-start",
          width: initialRect.width,
        });
      }
      markerRetryTimer = window.setTimeout(() => {
        setMapLayoutVersion((version) => version + 1);
      }, 140);
      return () => {
        if (markerRetryTimer != null) {
          window.clearTimeout(markerRetryTimer);
        }
      };
    }

    if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
      console.log("[Honeytong Map Debug] map container", container);
      console.log(
        "[Honeytong Map Debug] map rect",
        container.getBoundingClientRect(),
      );
    }

    loadKakaoMapSdk(appKey)
      .then((kakao) => {
        if (canceled || !containerRef.current) {
          return;
        }

        applyMapGestureCss(containerRef.current);
        const activeContainer = containerRef.current;
        const activeRect = activeContainer.getBoundingClientRect();
        if (!isUsableMapContainerRect(activeContainer, activeRect)) {
          if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
            console.log("[Honeytong Map Debug] map container rect unstable, postpone reinit", {
              height: activeRect.height,
              phase: "before-map-init",
              width: activeRect.width,
            });
          }
          markerRetryTimer = window.setTimeout(() => {
            setMapLayoutVersion((version) => version + 1);
          }, 140);
          return;
        }

        const validPlaces = places.filter(isPlaceWithCoordinates);
        const center = userLocation ?? getMapCenter(validPlaces);
        const placesKey = validPlaces.map((place) => place.id).join(",");
        const userLocationKey = userLocation
          ? `${userLocation.latitude},${userLocation.longitude}`
          : null;
        const shouldFitPlaces = fittedPlacesKeyRef.current !== placesKey;
        if (selectedPlaceId == null) {
          selectedPlaceIdRef.current = null;
        }
        const shouldCenterSelected =
          selectedPlaceId != null &&
          selectedPlaceIdRef.current !== selectedPlaceId;
        const shouldCenterUserLocation =
          userLocationKey != null && userLocationKeyRef.current !== userLocationKey;
        let map = mapRef.current;
        const shouldStabilizeMap =
          !mapReady ||
          shouldFitPlaces ||
          shouldCenterSelected ||
          shouldCenterUserLocation;

        if (!map) {
          try {
            const initRect = containerRef.current.getBoundingClientRect();
            if (!isUsableMapContainerRect(containerRef.current, initRect)) {
              if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
                console.log("[Honeytong Map Debug] map container rect unstable, postpone reinit", {
                  height: initRect.height,
                  phase: "map-init-start",
                  width: initRect.width,
                });
              }
              markerRetryTimer = window.setTimeout(() => {
                setMapLayoutVersion((version) => version + 1);
              }, 140);
              return;
            }

            const initialCenter = new kakao.maps.LatLng(
              center.latitude,
              center.longitude,
            );
            const initialLevel = validPlaces.length > 1 ? 5 : 4;
            if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
              console.log("[Honeytong Map Debug] map init start", {
                center: initialCenter.toString(),
                level: initialLevel,
              });
            }
            map = new kakao.maps.Map(containerRef.current, {
              center: initialCenter,
              level: initialLevel,
            });
            applyMapGestureCss(containerRef.current);
            if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
              console.log("[Honeytong Map Debug] map init done", map);
            }
          } catch (error) {
            console.error("[Honeytong Map Debug] map create failed", error);
            return;
          }
        }

        mapRef.current = map;
        setMapReady(true);
        map.setDraggable(true);
        map.setZoomable(true);
        const syncMapLayout = () => {
          if (!containerRef.current) {
            return;
          }
          const rect = containerRef.current.getBoundingClientRect();
          if (!isUsableMapContainerRect(containerRef.current, rect)) {
            if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
              console.log("[Honeytong Map Debug] map container rect unstable, postpone reinit", {
                height: rect.height,
                phase: "stabilize",
                width: rect.width,
              });
            }
            markerRetryTimer = window.setTimeout(() => {
              setMapLayoutVersion((version) => version + 1);
            }, 140);
            return;
          }

          if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
            console.log("[Honeytong Map Debug] map stabilize start");
          }
          applyMapGestureCss(containerRef.current);
          map.relayout();
          map.setDraggable(true);
          map.setZoomable(true);
          if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
            console.log("[Honeytong Map Debug] map draggable status after stabilize", {
              mapRect: containerRef.current?.getBoundingClientRect().toJSON(),
            });
          }
        };

        if (shouldStabilizeMap) {
          relayoutFrame = window.requestAnimationFrame(syncMapLayout);
          relayoutTimer = window.setTimeout(syncMapLayout, 160);
          secondRelayoutTimer = window.setTimeout(syncMapLayout, 400);
        }

        if (import.meta.env.DEV && DEBUG_MAP_LOGS && !debugListenersAddedRef.current) {
          console.log("[Honeytong Map Debug] draggable enabled");
          kakao.maps.event.addListener(map, "dragstart", () => {
            console.log("[Honeytong Map Debug] kakao dragstart");
          });
          kakao.maps.event.addListener(map, "dragend", () => {
            console.log("[Honeytong Map Debug] kakao dragend");
          });
          kakao.maps.event.addListener(map, "center_changed", () => {
            console.log(
              "[Honeytong Map Debug] center_changed",
              map.getCenter().toString(),
            );
          });
          debugListenersAddedRef.current = true;
        }
        onMapActionsReady({
          zoomIn: () => map.setLevel(Math.max(1, map.getLevel() - 1)),
          zoomOut: () => map.setLevel(Math.min(14, map.getLevel() + 1)),
        });
        const markerRect = containerRef.current.getBoundingClientRect();
        if (!isUsableMapContainerRect(containerRef.current, markerRect)) {
          if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
            console.log("[Honeytong Map Debug] map container rect unstable, postpone reinit", {
              height: markerRect.height,
              phase: "before-marker-render",
              width: markerRect.width,
            });
          }
          markerRetryTimer = window.setTimeout(() => {
            setMapLayoutVersion((version) => version + 1);
          }, 140);
          return;
        }

        clearMapOverlays();

        if (userLocation && shouldCenterUserLocation) {
          const position = new kakao.maps.LatLng(
            userLocation.latitude,
            userLocation.longitude,
          );
          map.setCenter(position);
          userLocationKeyRef.current = userLocationKey;
          userOverlayRef.current = new kakao.maps.CustomOverlay({
            map,
            position,
            content: createUserLocationOverlay(),
            yAnchor: 0.5,
            zIndex: 20,
          });
        }

        if (validPlaces.length === 0) {
          if (shouldFitPlaces) {
            map.setCenter(new kakao.maps.LatLng(center.latitude, center.longitude));
            fittedPlacesKeyRef.current = placesKey;
          }
          return;
        }

        const bounds = new kakao.maps.LatLngBounds();
        validPlaces.forEach((place) => {
          const position = new kakao.maps.LatLng(place.latitude, place.longitude);
          const active = place.id === selectedPlaceId;
          bounds.extend(position);

          if (DEBUG_MAP_DISABLE_CUSTOM_OVERLAYS) {
            return;
          }

          const content = createPlaceOverlay(place, active);
          const hitTarget = content.querySelector<HTMLElement>(
            "[data-place-marker-hit='true']",
          );
          hitTarget?.addEventListener("click", () => {
            onSelectPlace(place.id);
          });

          const overlay = new kakao.maps.CustomOverlay({
            map,
            position,
            content,
            yAnchor: 1,
            zIndex: active ? 12 : 8,
          });
          overlaysRef.current.push(overlay);
        });

        if (import.meta.env.DEV && DEBUG_MAP_LOGS) {
          console.log("[Honeytong Map Debug] markers rendered after remount", {
            customOverlaysDisabled: DEBUG_MAP_DISABLE_CUSTOM_OVERLAYS,
            count: overlaysRef.current.length,
            mapLayoutVersion,
          });
        }
        applyMapGestureCss(containerRef.current);
        if (DEBUG_MAP_OVERLAY_SCAN) {
          logSuspiciousPointerAutoOverlays(containerRef.current);
        }

        if (shouldCenterSelected) {
          const selectedPlace = validPlaces.find(
            (place) => place.id === selectedPlaceId,
          );
          if (selectedPlace) {
            map.setCenter(
              new kakao.maps.LatLng(
                selectedPlace.latitude,
                selectedPlace.longitude,
              ),
            );
            selectedPlaceIdRef.current = selectedPlaceId;
            return;
          }
        }

        if (!userLocation && shouldFitPlaces && validPlaces.length > 1) {
          map.setBounds(bounds);
          fittedPlacesKeyRef.current = placesKey;
        } else if (!userLocation && shouldFitPlaces) {
          map.setCenter(
            new kakao.maps.LatLng(
              validPlaces[0].latitude,
              validPlaces[0].longitude,
            ),
          );
          fittedPlacesKeyRef.current = placesKey;
        }
      })
      .catch((error) => {
        console.error("[Honeytong Map Debug] Kakao SDK load failed", error);
        mapRef.current = null;
      });

    return () => {
      canceled = true;
      if (relayoutFrame != null) {
        window.cancelAnimationFrame(relayoutFrame);
      }
      if (relayoutTimer != null) {
        window.clearTimeout(relayoutTimer);
      }
      if (secondRelayoutTimer != null) {
        window.clearTimeout(secondRelayoutTimer);
      }
      if (markerRetryTimer != null) {
        window.clearTimeout(markerRetryTimer);
      }
    };
  }, [
    appKey,
    containerReady,
    mapReady,
    mapLayoutVersion,
    places,
    selectedPlaceId,
    userLocation,
    onSelectPlace,
    onMapActionsReady,
    clearMapOverlays,
  ]);

  if (!appKey) {
    return (
      <MapStatus
        title="지도 키가 필요해요."
        desc="VITE_KAKAO_MAP_JAVASCRIPT_KEY를 설정하면 실제 Kakao 지도로 맛집 위치를 보여드릴게요."
      />
    );
  }

  return (
    <div
      ref={containerRef}
      aria-label="맛집 지도"
      className="pointer-events-auto absolute inset-0 z-0 h-full w-full touch-none select-none overflow-hidden bg-[#eaf2e4]"
      style={mapGestureStyle}
    />
  );
}

function MapStatus({ title, desc }: { title: string; desc: string }) {
  return (
    <div className="absolute left-0 top-0 z-[1] flex h-full min-h-screen w-full items-center justify-center bg-[#eaf2e4] px-8 text-center">
      <div className="rounded-[24px] bg-white/95 p-5 shadow-sm">
        <p className="text-sm font-bold text-[#2b210f]">{title}</p>
        <p className="mt-2 text-sm leading-6 text-gray-500">{desc}</p>
      </div>
    </div>
  );
}

function createPlaceOverlay(place: Place, active: boolean) {
  const category = getFoodCategory(place.category);
  const root = document.createElement("div");
  root.dataset.placeMarkerRoot = "true";
  root.className = "pointer-events-none flex h-12 w-fit items-center justify-center";
  root.style.pointerEvents = "none";
  root.style.width = "fit-content";
  root.style.height = "48px";
  root.style.maxWidth = "160px";

  const marker = document.createElement("button");
  marker.type = "button";
  marker.title = place.title;
  marker.dataset.placeMarkerHit = "true";
  marker.className = `pointer-events-auto flex h-11 max-w-[148px] origin-bottom items-center justify-center gap-1.5 rounded-[24px_24px_24px_8px] border bg-[#fff8db] px-3 text-sm font-bold text-[#2b210f] shadow-md transition ${
    active
      ? "scale-110 border-[#f6b800] shadow-[0_10px_24px_rgba(217,154,0,0.35)] ring-4 ring-[#f6b800]/30"
      : "border-white/90 hover:border-[#f6b800]"
  }`;
  marker.style.pointerEvents = "auto";
  marker.style.maxWidth = "148px";
  marker.style.height = "44px";

  const emoji = document.createElement("span");
  emoji.textContent = category.emoji;
  emoji.className = "pointer-events-none text-lg leading-none";
  emoji.style.pointerEvents = "none";

  const label = document.createElement("span");
  label.textContent = active ? place.title : category.label;
  label.className = "pointer-events-none max-w-[108px] truncate";
  label.style.pointerEvents = "none";

  marker.append(emoji, label);
  root.append(marker);
  return root;
}

function createUserLocationOverlay() {
  const dot = document.createElement("div");
  dot.className =
    "pointer-events-none flex h-6 w-6 items-center justify-center rounded-full bg-[#2563eb] shadow-[0_0_0_10px_rgba(37,99,235,0.18)]";
  dot.style.pointerEvents = "none";

  const core = document.createElement("div");
  core.className = "pointer-events-none h-3 w-3 rounded-full border-2 border-white bg-[#2563eb]";
  core.style.pointerEvents = "none";
  dot.append(core);
  return dot;
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

function getKakaoMapUrl(place: Place) {
  return `https://map.kakao.com/link/map/${encodeURIComponent(
    place.title,
  )},${place.latitude},${place.longitude}`;
}

function getCurrentPosition(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 10000,
    });
  });
}

function applyMapGestureCss(container: HTMLDivElement | null) {
  if (!container) {
    return;
  }

  container.style.touchAction = "none";
  container.style.userSelect = "none";
  container.style.setProperty("-webkit-user-select", "none");
  container.style.setProperty("-webkit-user-drag", "none");
  container.style.overscrollBehavior = "contain";
}

function logSuspiciousPointerAutoOverlays(container: HTMLDivElement | null) {
  if (!import.meta.env.DEV || !container) {
    return;
  }

  container.querySelectorAll<HTMLElement | SVGElement>("*").forEach((element) => {
    const style = window.getComputedStyle(element);
    if (style.pointerEvents !== "auto") {
      return;
    }

    const rect = element.getBoundingClientRect();
    if (rect.width <= 100 && rect.height <= 100) {
      return;
    }

    console.warn("[Honeytong Map Debug] suspicious pointer-events auto overlay", {
      className:
        element instanceof HTMLElement || element instanceof SVGElement
          ? element.className
          : null,
      pointerEvents: style.pointerEvents,
      rect: rect.toJSON(),
      tagName: element.tagName,
      zIndex: style.zIndex,
    });
  });
}

function isUsableMapContainerRect(element: HTMLElement, rect: DOMRect) {
  return (
    rect.width > 0 &&
    rect.height > 0 &&
    element.offsetWidth > 0 &&
    element.offsetHeight > 0
  );
}
