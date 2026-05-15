import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import type {
  CSSProperties,
  FormEvent as ReactFormEvent,
  MouseEvent as ReactMouseEvent,
  PointerEvent as ReactPointerEvent,
} from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  clearAuthTokens,
  getApiErrorMessage,
  hasStoredAccessToken,
} from "../api/http";
import {
  createComment,
  getPlaceComments,
  type CommentItem,
} from "../api/participationApi";
import { login, signup } from "../api/authApi";
import {
  getMyRegion,
  getRegionChangePolicy,
  verifyRegion,
  type MyRegion,
  type RegionChangePolicy,
} from "../api/regionApi";
import {
  getMyActivitySummary,
  getMyProfile,
  getMyStatus,
  type MyActivitySummary,
  type MyProfile,
  type MyStatus,
} from "../api/userApi";
import AuthCard, { type AuthMode } from "../components/AuthCard";
import BottomNav from "../components/BottomNav";
import CategoryTabs from "../components/CategoryTabs";
import {
  HomeIcon,
  MessageIcon,
  StarIcon,
  TrophyIcon,
  UserIcon,
} from "../components/NavIcons";
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
  searchKeyword: string;
  searchActive: boolean;
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

type MobileSheetStage = "closed" | "half" | "full";
type DesktopPanelMode = "map" | "ranking" | "saved" | "community" | "my";
type DesktopPlaceTab = "home" | "menu" | "photos" | "comments" | "report";
type PlaceClusterSelection = {
  anchorPlaceId: number;
  placeIds: number[];
};

const DEFAULT_MY_ACTIVITY_SUMMARY: MyActivitySummary = {
  recommendedCount: 0,
  visitCount: 0,
  commentCount: 0,
  registeredPlaceCount: 0,
};

const CurrentLocationIcon = () => (
  <svg
    width="22"
    height="22"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    <circle cx="12" cy="12" r="8.5"></circle>
    <circle cx="12" cy="12" r="2" fill="currentColor" stroke="none"></circle>
    <path d="M12 3.5v3"></path>
    <path d="M12 20.5v-3"></path>
    <path d="M3.5 12h3"></path>
    <path d="M20.5 12h-3"></path>
  </svg>
);

const ZoomInIcon = () => (
  <svg
    width="22"
    height="22"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.5"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    <line x1="12" y1="5" x2="12" y2="19"></line>
    <line x1="5" y1="12" x2="19" y2="12"></line>
  </svg>
);

const ZoomOutIcon = () => (
  <svg
    width="22"
    height="22"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.5"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    <line x1="5" y1="12" x2="19" y2="12"></line>
  </svg>
);

const SearchIcon = () => (
  <svg
    width="21"
    height="21"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.1"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    <circle cx="10.8" cy="10.8" r="6.2"></circle>
    <path d="M15.4 15.4 20 20"></path>
  </svg>
);

const CloseIcon = () => (
  <svg
    width="21"
    height="21"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.4"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    <path d="M18 6 6 18"></path>
    <path d="m6 6 12 12"></path>
  </svg>
);

const SaveStarIcon = ({ active = false }: { active?: boolean }) => (
  <svg
    width="22"
    height="22"
    viewBox="0 0 24 24"
    fill={active ? "currentColor" : "none"}
    stroke="currentColor"
    strokeWidth="2.2"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
  </svg>
);

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
  searchKeyword,
  searchActive,
  onToggleWish,
  onSearch,
}: Props) {
  const navigate = useNavigate();
  const [selectedCategory, setSelectedCategory] = useState("ALL");
  const [selectedPlaceId, setSelectedPlaceId] = useState<number | null>(null);
  const [mobileSheetStage, setMobileSheetStage] =
    useState<MobileSheetStage>("closed");
  const [desktopPanelMode, setDesktopPanelMode] =
    useState<DesktopPanelMode>("map");
  const [desktopPanelCollapsed, setDesktopPanelCollapsed] = useState(false);
  const [desktopCategoryMenuOpen, setDesktopCategoryMenuOpen] = useState(false);
  const [userLocation, setUserLocation] = useState<UserLocation | null>(null);
  const [locationMessage, setLocationMessage] = useState<string | null>(null);
  const [mapActions, setMapActions] = useState<MapActions | null>(null);
  const [placeClusterSelection, setPlaceClusterSelection] =
    useState<PlaceClusterSelection | null>(null);

  const visiblePlaces = useMemo(() => {
    if (selectedCategory === "ALL") {
      return places;
    }
    return places.filter((place) => place.category === selectedCategory);
  }, [places, selectedCategory]);

  const selectedPlace = useMemo(() => {
    return visiblePlaces.find((place) => place.id === selectedPlaceId) ?? null;
  }, [selectedPlaceId, visiblePlaces]);

  const selectedClusterPlaces = useMemo(() => {
    if (!placeClusterSelection) {
      return [];
    }
    const placesById = new Map(visiblePlaces.map((place) => [place.id, place]));
    return placeClusterSelection.placeIds
      .map((placeId) => placesById.get(placeId))
      .filter((place): place is Place => place != null);
  }, [placeClusterSelection, visiblePlaces]);

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

  const wishedPlaces = useMemo(
    () => places.filter((place) => place.isWished),
    [places],
  );

  const handleSelectPlace = useCallback((placeId: number, nearbyPlaces?: Place[]) => {
    if (nearbyPlaces && nearbyPlaces.length > 1) {
      setSelectedPlaceId(null);
      setPlaceClusterSelection({
        anchorPlaceId: placeId,
        placeIds: nearbyPlaces.map((place) => place.id),
      });
    } else {
      setSelectedPlaceId(placeId);
      setPlaceClusterSelection(null);
    }
    setMobileSheetStage("half");
    setDesktopPanelMode("map");
    setDesktopPanelCollapsed(false);
  }, []);

  const handleSelectCategory = useCallback((value: string) => {
    setSelectedCategory(value);
    setSelectedPlaceId(null);
    setPlaceClusterSelection(null);
    setDesktopPanelMode("map");
    setDesktopPanelCollapsed(false);
    setMobileSheetStage("closed");
    setDesktopCategoryMenuOpen(false);
  }, []);

  const handleClearMapSelection = useCallback(() => {
    setSelectedCategory("ALL");
    setSelectedPlaceId(null);
    setPlaceClusterSelection(null);
    setDesktopCategoryMenuOpen(false);
    setMobileSheetStage("closed");
  }, []);

  const handleCloseMobileSheet = useCallback(() => {
    setSelectedPlaceId(null);
    setPlaceClusterSelection(null);
    setMobileSheetStage("closed");
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
      <main className="relative h-[100dvh] min-h-[640px] w-full overflow-hidden bg-m3-surface">
        <PlaceMap
          places={visiblePlaces}
          selectedPlaceId={
            selectedPlace?.id ?? placeClusterSelection?.anchorPlaceId ?? null
          }
          userLocation={userLocation}
          onSelectPlace={handleSelectPlace}
          onMapActionsReady={setMapActions}
        />

        <section
          data-map-ui-layer="mobile-overlay"
          className="pointer-events-none absolute inset-0 z-10 w-full lg:hidden"
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
            sheetOpen={mobileSheetStage !== "closed"}
            onUseCurrentLocation={handleUseCurrentLocation}
            onRegister={() => navigate("/places/new")}
          />

          <div
            className={`pointer-events-none absolute inset-x-0 bottom-0 z-30 ${
              mobileSheetStage === "full"
                ? "px-0 pb-0 md:px-0 md:pb-0"
                : mobileSheetStage === "half"
                  ? "px-0 pb-0 md:px-0 md:pb-0"
                  : "px-4 pb-[calc(96px+env(safe-area-inset-bottom))] md:px-6 md:pb-8"
            }`}
          >
            <MobileSelectedPlaceSheet
              places={places}
              visiblePlaces={visiblePlaces}
              selectedPlace={selectedPlace}
              clusterPlaces={selectedClusterPlaces}
              stage={mobileSheetStage}
              loading={loading}
              errorMessage={errorMessage}
              selectedCategory={selectedCategory}
              searchKeyword={searchKeyword}
              searchActive={searchActive}
              locationMessage={locationMessage}
              onToggleWish={onToggleWish}
              onSelectClusterPlace={handleSelectPlace}
              onRetry={() => onSearch("")}
              onStageChange={setMobileSheetStage}
              onClose={handleCloseMobileSheet}
            />
          </div>
        </section>

        <section
          data-map-ui-layer="desktop-overlay"
          className="hidden"
          aria-hidden="true"
        />

        <section
          data-map-ui-layer="desktop-map-layout"
          className="pointer-events-none absolute inset-0 z-10 hidden lg:block"
        >
          <DesktopSideNav
            activeMode={desktopPanelMode}
            onSelectMode={(mode) => {
              setDesktopPanelMode(mode);
              setDesktopPanelCollapsed(false);
            }}
          />

          <aside
            data-desktop-place-panel="root"
            className={
              "pointer-events-auto absolute inset-y-0 left-[84px] z-20 flex w-[348px] min-h-0 flex-col border-r border-m3-outline-variant bg-m3-surface-container-lowest p-5 text-m3-on-surface shadow-m3-2 transition-[transform,opacity] duration-300 ease-out xl:w-[388px] " +
              (desktopPanelCollapsed
                ? "-translate-x-[calc(100%+16px)] opacity-0"
                : "translate-x-0 opacity-100")
            }
            aria-hidden={desktopPanelCollapsed}
          >
            <DesktopPanelContent
              mode={desktopPanelMode}
              places={places}
              visiblePlaces={visiblePlaces}
              wishedPlaces={wishedPlaces}
              rankedPlaces={rankedPlaces}
              selectedPlace={selectedPlace}
              clusterPlaces={selectedClusterPlaces}
              loading={loading}
              errorMessage={errorMessage}
              selectedCategory={selectedCategory}
              searchKeyword={searchKeyword}
              searchActive={searchActive}
              locationMessage={locationMessage}
              onSearch={onSearch}
              onToggleWish={onToggleWish}
              onSelectClusterPlace={handleSelectPlace}
              onRetry={() => onSearch("")}
              onSelectPlace={handleSelectPlace}
            />
          </aside>

          <div
            className={`pointer-events-none absolute inset-y-0 z-10 w-px bg-m3-outline-variant transition-all ${
              desktopPanelCollapsed
                ? "left-[84px]"
                : "left-[432px] xl:left-[472px]"
            }`}
          />

          <button
            type="button"
            data-desktop-panel-toggle="root"
            onClick={() =>
              setDesktopPanelCollapsed((currentValue) => !currentValue)
            }
            aria-label={
              desktopPanelCollapsed ? "정보 패널 펼치기" : "정보 패널 접기"
            }
            className={`pointer-events-auto absolute top-1/2 z-30 flex h-16 w-9 -translate-y-1/2 items-center justify-center rounded-r-m3-lg border border-l-0 border-m3-outline-variant bg-m3-surface-container-lowest text-lg font-black text-m3-primary shadow-m3-2 transition-all active:scale-95 ${
              desktopPanelCollapsed
                ? "left-[84px]"
                : "left-[432px] xl:left-[472px]"
            }`}
          >
            {desktopPanelCollapsed ? ">" : "<"}
          </button>

          <DesktopMapCategoryBar
            selectedCategory={selectedCategory}
            open={desktopCategoryMenuOpen}
            panelCollapsed={desktopPanelCollapsed}
            onSelectCategory={handleSelectCategory}
            onToggleOpen={() =>
              setDesktopCategoryMenuOpen((currentOpen) => !currentOpen)
            }
            onClose={() => setDesktopCategoryMenuOpen(false)}
          />

          <DesktopMapRegionChip
            selectedCategory={selectedCategory}
            selectedPlace={selectedPlace}
            panelCollapsed={desktopPanelCollapsed}
            onUseCurrentLocation={handleUseCurrentLocation}
            onClearSelection={handleClearMapSelection}
          />

          <DesktopMapControls
            mapActions={mapActions}
            onUseCurrentLocation={handleUseCurrentLocation}
          />
        </section>
      </main>
      <div
        data-bottom-nav="mobile-bottom-nav"
        className={mobileSheetStage !== "closed" ? "hidden" : "lg:hidden"}
      >
        <BottomNav />
      </div>
    </div>
  );
}

function DesktopSideNav({
  activeMode,
  onSelectMode,
}: {
  activeMode: DesktopPanelMode;
  onSelectMode: (mode: DesktopPanelMode) => void;
}) {
  const navItems = [
    { mode: "map" as const, label: "\uC9C0\uB3C4", icon: <HomeIcon /> },
    { mode: "ranking" as const, label: "\uB7AD\uD0B9", icon: <TrophyIcon /> },
    { mode: "saved" as const, label: "\uC800\uC7A5", icon: <StarIcon /> },
    {
      mode: "community" as const,
      label: "\uCEE4\uBBA4\uB2C8\uD2F0",
      icon: <MessageIcon />,
    },
    { mode: "my" as const, label: "\uB9C8\uC774", icon: <UserIcon /> },
  ];

  return (
    <nav
      data-desktop-side-nav="root"
      className="pointer-events-auto absolute inset-y-0 left-0 z-30 flex w-[84px] flex-col items-center border-r border-m3-outline-variant bg-m3-surface-container-low py-6 shadow-m3-2"
    >
      <Link
        to="/"
        className="flex h-11 w-11 items-center justify-center rounded-m3-lg bg-m3-primary text-xl font-black text-m3-on-primary shadow-m3-1"
        aria-label="Honeytong \uD648"
      >
        H
      </Link>

      <div className="mt-8 flex w-full flex-1 flex-col items-center gap-2">
        {navItems.map((item) => {
          const className = `flex w-[68px] flex-col items-center justify-center gap-1 rounded-m3-xl px-2 py-3 text-m3-label-md transition ${
            activeMode === item.mode
              ? "bg-m3-secondary-container text-m3-on-secondary-container"
              : "text-m3-on-surface-variant hover:bg-m3-surface-container-high hover:text-m3-on-surface"
          }`;
          const content = (
            <>
              <span className="flex h-6 w-6 items-center justify-center" aria-hidden="true">
                {item.icon}
              </span>
              <span>{item.label}</span>
            </>
          );

          if (item.mode === "community") {
            return (
              <Link key={item.mode} to="/community" className={className}>
                {content}
              </Link>
            );
          }

          return (
            <button
              key={item.mode}
              type="button"
              onClick={() => onSelectMode(item.mode)}
              className={className}
            >
              {content}
            </button>
          );
        })}
      </div>

      <div className="h-10" aria-hidden="true" />
    </nav>
  );
}

function DesktopMapCategoryBar({
  selectedCategory,
  open,
  panelCollapsed,
  onSelectCategory,
  onToggleOpen,
  onClose,
}: {
  selectedCategory: string;
  open: boolean;
  panelCollapsed: boolean;
  onSelectCategory: (value: string) => void;
  onToggleOpen: () => void;
  onClose: () => void;
}) {
  const primaryCategories = FOOD_CATEGORIES.filter(
    (category) => category.value !== "ALL",
  ).slice(0, 6);
  const extraCategories = FOOD_CATEGORIES.filter(
    (category) =>
      category.value === "ALL" ||
      !primaryCategories.some(
        (primaryCategory) => primaryCategory.value === category.value,
      ),
  );

  return (
    <div
      data-desktop-category-bar="root"
      className={
        "pointer-events-none absolute top-5 z-40 transition-all " +
        (panelCollapsed ? "left-[108px]" : "left-[452px] xl:left-[492px]")
      }
    >
      <div className="pointer-events-auto flex w-fit max-w-[calc(100vw-560px)] items-start gap-2 xl:max-w-[760px]">
        <div className="flex min-w-0 max-w-[calc(100vw-632px)] items-center gap-2 overflow-x-auto rounded-m3-xl bg-transparent p-0.5 [-ms-overflow-style:none] [scrollbar-width:none] xl:max-w-[704px] [&::-webkit-scrollbar]:hidden">
          {primaryCategories.map((category) => (
            <button
              key={category.value}
              data-desktop-category-chip="primary"
              type="button"
              onClick={() => onSelectCategory(category.value)}
              className={
                "flex h-10 min-w-[92px] shrink-0 items-center justify-center gap-2 rounded-m3-full border px-3 text-m3-label-lg shadow-m3-1 transition active:scale-[0.98] " +
                (selectedCategory === category.value
                  ? "border-m3-primary bg-m3-secondary-container text-m3-on-secondary-container"
                  : "border-m3-outline-variant bg-m3-surface-container-lowest text-m3-on-surface hover:border-m3-primary")
              }
            >
              <span
                aria-hidden="true"
                className="flex h-6 w-6 shrink-0 items-center justify-center rounded-m3-full bg-m3-surface-container-high text-base"
              >
                {category.emoji}
              </span>
              <span className="truncate">{category.label}</span>
            </button>
          ))}
        </div>

        <div className="relative shrink-0 p-0.5">
          <button
            type="button"
            data-desktop-category-more="button"
            onClick={onToggleOpen}
            aria-expanded={open}
            aria-label="카테고리 더보기"
            className={
              "flex h-10 w-10 items-center justify-center rounded-m3-full border text-lg font-black shadow-m3-1 transition active:scale-95 " +
              (open
                ? "border-m3-primary bg-m3-secondary-container text-m3-on-secondary-container"
                : "border-m3-outline-variant bg-m3-surface-container-lowest text-m3-on-surface")
            }
          >
            ...
          </button>

          {open && (
            <div
              data-desktop-category-popover="root"
              className="absolute left-0 top-12 z-50 w-[300px] max-w-[calc(100vw-32px)] rounded-m3-xl border border-m3-outline-variant bg-m3-surface-container-lowest p-3 text-m3-on-surface shadow-m3-3"
            >
              <div className="grid grid-cols-3 gap-2">
                {extraCategories.map((category) => (
                  <button
                    key={category.value}
                    type="button"
                    onClick={() => {
                      onSelectCategory(category.value);
                      onClose();
                    }}
                    className={
                      "flex min-h-16 flex-col items-center justify-center gap-1 rounded-m3-lg border px-2 py-2 text-m3-label-md transition active:scale-[0.98] " +
                      (selectedCategory === category.value
                        ? "border-m3-primary bg-m3-secondary-container text-m3-on-secondary-container"
                        : "border-m3-outline-variant bg-m3-surface-container-lowest text-m3-on-surface hover:border-m3-primary")
                    }
                  >
                    <span
                      className="flex h-8 w-8 shrink-0 items-center justify-center rounded-m3-full bg-m3-surface-container-high text-lg"
                      aria-hidden="true"
                    >
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

function DesktopMapRegionChip({
  selectedCategory,
  selectedPlace,
  panelCollapsed,
  onUseCurrentLocation,
  onClearSelection,
}: {
  selectedCategory: string;
  selectedPlace: Place | null;
  panelCollapsed: boolean;
  onUseCurrentLocation: () => void;
  onClearSelection: () => void;
}) {
  const title =
    selectedPlace?.regionName || selectedPlace?.address || "현재 지도 중심 지역";
  const desc = selectedPlace
    ? getFoodCategory(selectedPlace.category).label
    : selectedCategory === "ALL"
      ? "내 주변 맛집을 탐색 중이에요."
      : `${getFoodCategoryLabel(selectedCategory)} 맛집을 보는 중이에요.`;
  const canClearSelection = selectedPlace != null || selectedCategory !== "ALL";

  return (
    <div
      data-desktop-map-region-chip="root"
      className={
        "pointer-events-auto absolute top-[70px] z-30 transition-all " +
        (panelCollapsed ? "left-[108px]" : "left-[452px] xl:left-[492px]")
      }
    >
      <div className="flex min-h-12 max-w-[330px] items-center gap-3 rounded-m3-lg border border-m3-outline-variant bg-m3-surface-container-lowest px-3 py-2 text-m3-on-surface shadow-m3-2">
        <button
          type="button"
          onClick={onUseCurrentLocation}
          aria-label="현재 위치 기준으로 보기"
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-m3-md bg-m3-primary-container text-m3-on-primary-container transition active:scale-95"
        >
          <CurrentLocationIcon />
        </button>
        <div className="min-w-0 flex-1">
          <p className="truncate text-m3-title-sm text-m3-on-surface">{title}</p>
          <p className="mt-0.5 truncate text-m3-body-sm text-m3-on-surface-variant">
            {desc}
          </p>
        </div>
        {canClearSelection && (
          <button
            type="button"
            onClick={onClearSelection}
            aria-label="지역 선택 초기화"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-m3-full border border-m3-outline-variant bg-m3-surface-container-lowest text-base font-black text-m3-on-surface-variant transition active:scale-95"
          >
            ×
          </button>
        )}
      </div>
    </div>
  );
}

function DesktopMapControls({
  mapActions,
  onUseCurrentLocation,
}: {
  mapActions: MapActions | null;
  onUseCurrentLocation: () => void;
}) {
  const controls = [
    {
      label: "내 위치",
      icon: <CurrentLocationIcon />,
      onClick: onUseCurrentLocation,
      disabled: false,
    },
    {
      label: "확대",
      icon: <ZoomInIcon />,
      onClick: mapActions?.zoomIn,
      disabled: !mapActions,
    },
    {
      label: "축소",
      icon: <ZoomOutIcon />,
      onClick: mapActions?.zoomOut,
      disabled: !mapActions,
    },
  ];

  return (
    <div
      data-desktop-map-controls="root"
      className="pointer-events-auto absolute right-6 top-[92px] z-20 flex flex-col overflow-hidden rounded-m3-lg border border-m3-outline-variant bg-m3-surface-container-lowest text-m3-on-surface shadow-m3-3"
    >
      {controls.map((control, index) => (
        <button
          key={control.label}
          type="button"
          aria-label={control.label}
          onClick={control.onClick}
          disabled={control.disabled}
          title={control.label}
          className={
            "flex h-12 w-12 items-center justify-center text-lg font-black transition hover:bg-m3-primary-container/60 active:scale-[0.98] disabled:opacity-50" +
            (index > 0 ? " border-t border-m3-outline-variant" : "")
          }
        >
          {control.icon}
        </button>
      ))}
    </div>
  );
}

function DesktopPanelSearch({
  onSearch,
}: {
  onSearch: (keyword: string) => void;
}) {
  return (
    <form
      className="flex h-12 items-center gap-3 rounded-m3-lg border border-m3-outline bg-m3-surface-container-lowest px-3 text-m3-on-surface shadow-m3-1 transition focus-within:border-m3-primary focus-within:ring-2 focus-within:ring-m3-primary/20"
      onSubmit={(event) => {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        onSearch(String(form.get("keyword") ?? ""));
      }}
    >
      <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-m3-md bg-m3-primary-container text-m3-on-primary-container">
        <SearchIcon />
      </span>
      <input
        name="keyword"
        aria-label="맛집 검색"
        placeholder="검색어를 입력하세요"
        className="min-w-0 flex-1 bg-transparent text-m3-body-md text-m3-on-surface placeholder:text-m3-on-surface-variant focus:outline-none"
      />
    </form>
  );
}

function DesktopPanelContent({
  mode,
  places,
  visiblePlaces,
  wishedPlaces,
  rankedPlaces,
  selectedPlace,
  clusterPlaces,
  loading,
  errorMessage,
  selectedCategory,
  searchKeyword,
  searchActive,
  locationMessage,
  onSearch,
  onToggleWish,
  onSelectClusterPlace,
  onRetry,
  onSelectPlace,
}: {
  mode: DesktopPanelMode;
  places: Place[];
  visiblePlaces: Place[];
  wishedPlaces: Place[];
  rankedPlaces: Place[];
  selectedPlace: Place | null;
  clusterPlaces: Place[];
  loading: boolean;
  errorMessage: string | null;
  selectedCategory: string;
  searchKeyword: string;
  searchActive: boolean;
  locationMessage: string | null;
  onSearch: (keyword: string) => void;
  onToggleWish: (id: number) => void;
  onSelectClusterPlace: (placeId: number) => void;
  onRetry: () => void;
  onSelectPlace: (placeId: number) => void;
}) {
  const modePlaces =
    mode === "ranking" ? rankedPlaces : mode === "saved" ? wishedPlaces : [];

  return (
    <>
      <div className="shrink-0">
        <DesktopPanelSearch onSearch={onSearch} />
      </div>

      {mode === "map" ? (
        <DesktopSelectedPlacePanel
          places={places}
          visiblePlaces={visiblePlaces}
          selectedPlace={selectedPlace}
          clusterPlaces={clusterPlaces}
          loading={loading}
          errorMessage={errorMessage}
          selectedCategory={selectedCategory}
          searchKeyword={searchKeyword}
          searchActive={searchActive}
          locationMessage={locationMessage}
          onToggleWish={onToggleWish}
          onSelectClusterPlace={onSelectClusterPlace}
          onRetry={onRetry}
          onSelectPlace={onSelectPlace}
        />
      ) : (
        <DesktopModePanel
          mode={mode}
          places={modePlaces}
          onSelectPlace={onSelectPlace}
        />
      )}
    </>
  );
}

function DesktopModePanel({
  mode,
  places,
  onSelectPlace,
}: {
  mode: Exclude<DesktopPanelMode, "map">;
  places: Place[];
  onSelectPlace: (placeId: number) => void;
}) {
  if (mode === "my") {
    return <DesktopMyPanel />;
  }

  const emptyMessage = {
    ranking: "아직 보여줄 랭킹 맛집이 없어요",
    saved: "저장한 맛집이 아직 없어요",
    community: "커뮤니티 소식은 준비 중이에요.",
  }[mode];

  const desc = {
    ranking: "지도는 유지한 채 반응 좋은 맛집을 빠르게 확인해요.",
    saved: "찜한 맛집을 지도 위에서 다시 살펴볼 수 있어요.",
    community: "동네 소식과 참여 기능은 다음 단계에서 연결할게요.",
  }[mode];

  return (
    <div className="desktop-compact-scrollbar mt-4 min-h-0 flex-1 overflow-y-auto pr-1">
      {places.length > 0 ? (
        <div className="space-y-3">
          {places.map((place, index) => (
            <button
              key={place.id}
              type="button"
              onClick={() => onSelectPlace(place.id)}
              className="block w-full rounded-m3-xl border border-m3-outline-variant bg-m3-surface-container-lowest p-4 text-left text-m3-on-surface shadow-m3-1 transition hover:border-m3-primary hover:bg-m3-surface-container-low active:scale-[0.99]"
            >
              <div className="flex items-start gap-3">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-m3-full bg-m3-primary-container text-m3-label-lg text-m3-on-primary-container">
                  {mode === "ranking" ? index + 1 : getFoodCategory(place.category).emoji}
                </span>
                <div className="min-w-0 flex-1">
                  <h3 className="truncate text-m3-title-sm text-m3-on-surface">
                    {place.title}
                  </h3>
                  <p className="mt-1 line-clamp-2 text-m3-body-sm text-m3-on-surface-variant">
                    {place.desc || "동네 사람들이 추천한 꿀맛집이에요."}
                  </p>
                  <p className="mt-2 text-m3-body-sm font-medium text-m3-tertiary">
                    추천 {place.recommendCount} · 방문 {place.visitCount}
                  </p>
                </div>
              </div>
            </button>
          ))}
        </div>
      ) : (
        <StateCard title={emptyMessage} desc={desc} />
      )}
    </div>
  );
}

function DesktopMyPanel() {
  const [authMode, setAuthMode] = useState<AuthMode>("login");
  const [authenticated, setAuthenticated] = useState(hasStoredAccessToken());
  const [profile, setProfile] = useState<MyProfile | null>(null);
  const [status, setStatus] = useState<MyStatus | null>(null);
  const [summary, setSummary] = useState<MyActivitySummary>(
    DEFAULT_MY_ACTIVITY_SUMMARY,
  );
  const [region, setRegion] = useState<MyRegion | null>(null);
  const [regionPolicy, setRegionPolicy] =
    useState<RegionChangePolicy | null>(null);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [loading, setLoading] = useState(hasStoredAccessToken());
  const [busy, setBusy] = useState(false);
  const [regionBusy, setRegionBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const loadDesktopMy = useCallback(async () => {
    if (!hasStoredAccessToken()) {
      setAuthenticated(false);
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const [
        nextProfile,
        nextStatus,
        nextSummary,
        nextRegion,
        nextRegionPolicy,
      ] =
        await Promise.all([
          getMyProfile(),
          getMyStatus(),
          getMyActivitySummary(),
          settle(getMyRegion()),
          settle(getRegionChangePolicy()),
        ]);
      setProfile(nextProfile);
      setStatus(nextStatus);
      setSummary(nextSummary);
      setRegion(nextRegion.ok ? nextRegion.value : null);
      setRegionPolicy(nextRegionPolicy.ok ? nextRegionPolicy.value : null);
      setAuthenticated(true);
    } catch (error) {
      clearAuthTokens();
      setAuthenticated(false);
      setProfile(null);
      setStatus(null);
      setSummary(DEFAULT_MY_ACTIVITY_SUMMARY);
      setRegion(null);
      setRegionPolicy(null);
      setMessage(
        getApiErrorMessage(
          error,
          "로그인 상태를 확인하지 못했어요. 다시 로그인해주세요.",
        ),
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!hasStoredAccessToken()) {
      return;
    }
    const timerId = window.setTimeout(() => {
      void loadDesktopMy();
    }, 0);
    return () => window.clearTimeout(timerId);
  }, [loadDesktopMy]);

  const handleSubmit = async () => {
    setBusy(true);
    setMessage(null);
    try {
      if (authMode === "signup") {
        await signup(email.trim(), password, nickname.trim());
      }
      await login(email.trim(), password);
      setMessage(
        authMode === "signup"
          ? "회원가입과 로그인을 완료했어요."
          : "로그인했어요.",
      );
      await loadDesktopMy();
    } catch (error) {
      setMessage(
        getApiErrorMessage(
          error,
          authMode === "signup"
            ? "회원가입 요청을 처리하지 못했어요."
            : "로그인 요청을 처리하지 못했어요.",
        ),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleVerifyRegion = async () => {
    setRegionBusy(true);
    setMessage(null);
    try {
      const position = await getCurrentPosition();
      const nextRegion = await verifyRegion(
        position.coords.latitude,
        position.coords.longitude,
      );
      setRegion(nextRegion);
      await loadDesktopMy();
      setMessage(`${formatDesktopRegionName(nextRegion)} 동네 인증이 완료됐어요.`);
    } catch (error) {
      setMessage(getApiErrorMessage(error, "동네 인증을 처리하지 못했어요."));
    } finally {
      setRegionBusy(false);
    }
  };

  if (loading) {
    return (
      <div
        data-desktop-my-panel="loading"
        className="desktop-compact-scrollbar mt-4 min-h-0 flex-1 overflow-y-auto pr-1"
      >
        <section className="rounded-m3-xl bg-m3-surface-container-lowest p-4 text-m3-on-surface shadow-m3-1">
          <p className="text-m3-title-sm">
            내 정보를 불러오는 중이에요.
          </p>
          <div className="mt-4 space-y-3">
            <div className="h-4 w-3/4 animate-pulse rounded-m3-full bg-m3-surface-container-high" />
            <div className="h-4 w-1/2 animate-pulse rounded-m3-full bg-m3-surface-container-high" />
            <div className="h-20 animate-pulse rounded-m3-lg bg-m3-surface-container" />
          </div>
        </section>
      </div>
    );
  }

  if (!authenticated || !profile) {
    return (
      <div
        data-desktop-my-auth-panel="root"
        className="desktop-compact-scrollbar mt-4 min-h-0 flex-1 overflow-y-auto pr-1"
      >
        <AuthCard
          className="mt-0"
          authMode={authMode}
          setAuthMode={setAuthMode}
          email={email}
          setEmail={setEmail}
          password={password}
          setPassword={setPassword}
          nickname={nickname}
          setNickname={setNickname}
          busy={busy}
          onSubmit={handleSubmit}
        />
        {message && (
          <p className="mt-4 rounded-m3-xl bg-m3-secondary-container px-4 py-3 text-m3-body-md text-m3-on-secondary-container shadow-m3-1">
            {message}
          </p>
        )}
      </div>
    );
  }

  const displayName = profile.nickname?.trim() || "꿀벌님";

  return (
    <div
      data-desktop-my-panel="root"
      className="desktop-compact-scrollbar mt-4 min-h-0 flex-1 overflow-y-auto pr-1"
    >
      <section className="rounded-m3-xl bg-m3-surface-container-lowest p-4 text-m3-on-surface shadow-m3-1">
        <div className="flex items-center gap-4">
          <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-m3-full bg-m3-primary text-m3-label-lg text-m3-on-primary shadow-m3-1">
            꿀벌
          </div>
          <div className="min-w-0 flex-1">
            <h2 className="truncate text-m3-title-lg text-m3-on-surface">
              {displayName}
            </h2>
            <p className="mt-1 text-m3-body-sm text-m3-on-surface-variant">
              {region ? formatDesktopRegionName(region) : "아직 인증한 동네가 없어요."}
            </p>
          </div>
        </div>

        <div className="mt-4 grid grid-cols-2 gap-2">
          <DesktopMyStatCard label="레벨" value={status ? String(status.level) : "-"} />
          <DesktopMyStatCard
            label="신뢰도"
            value={status?.trustGrade || "확인 중"}
          />
          <DesktopMyStatCard
            label="전화 인증"
            value={profile.phoneVerified ? "완료" : "필요"}
          />
          <DesktopMyStatCard
            label="동네 인증"
            value={status?.regionVerified ? "완료" : "필요"}
          />
        </div>
      </section>

      <section className="mt-4 rounded-m3-xl bg-m3-surface-container-lowest p-4 text-m3-on-surface shadow-m3-1">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h3 className="text-m3-title-md text-m3-on-surface">동네 인증</h3>
            <p className="mt-1 text-m3-body-sm text-m3-on-surface-variant">
              {status?.regionVerified
                ? "인증한 동네 기준으로 활동할 수 있어요."
                : "현재 위치로 내 동네를 인증해보세요."}
            </p>
          </div>
          <DesktopMyBadge verified={region?.verified === true}>
            {region?.verified ? "인증 완료" : "인증 필요"}
          </DesktopMyBadge>
        </div>

        <div className="mt-4 rounded-m3-lg bg-m3-surface-container-low p-4">
          <p className="text-m3-label-md text-m3-on-surface-variant">현재 인증 동네</p>
          <p className="mt-1 truncate text-m3-title-md text-m3-on-surface">
            {region ? formatDesktopRegionName(region) : "아직 인증한 동네가 없어요."}
          </p>
          <p className="mt-2 text-m3-body-sm text-m3-on-surface-variant">
            {formatDesktopRegionPolicy(regionPolicy)}
          </p>
        </div>

        <button
          type="button"
          onClick={handleVerifyRegion}
          disabled={regionBusy}
          className="mt-4 h-11 w-full rounded-m3-full bg-m3-primary text-m3-label-lg text-m3-on-primary shadow-m3-1 transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
        >
          {regionBusy ? "위치 확인 중..." : "현재 위치로 인증"}
        </button>
      </section>

      <section className="mt-4 rounded-m3-xl bg-m3-surface-container-lowest p-4 text-m3-on-surface shadow-m3-1">
        <h3 className="text-m3-title-md text-m3-on-surface">내 활동</h3>
        <div className="mt-3 grid grid-cols-2 gap-2">
          <DesktopMyStatCard
            label="추천한 맛집"
            value={String(summary.recommendedCount)}
          />
          <DesktopMyStatCard
            label="방문 인증"
            value={String(summary.visitCount)}
          />
          <DesktopMyStatCard label="댓글" value={String(summary.commentCount)} />
          <DesktopMyStatCard
            label="등록 맛집"
            value={String(summary.registeredPlaceCount)}
          />
        </div>
      </section>

      {message && (
        <p className="mt-4 rounded-m3-xl bg-m3-secondary-container px-4 py-3 text-m3-body-md text-m3-on-secondary-container shadow-m3-1">
          {message}
        </p>
      )}
    </div>
  );
}

function DesktopMyStatCard({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-m3-lg bg-m3-surface-container-low px-3 py-3">
      <p className="text-m3-label-md text-m3-on-surface-variant">{label}</p>
      <p className="mt-1 truncate text-m3-title-sm text-m3-on-surface">{value}</p>
    </div>
  );
}

function DesktopMyBadge({
  children,
  verified,
}: {
  children: string;
  verified: boolean;
}) {
  return (
    <span
      className={
        "inline-flex shrink-0 items-center rounded-m3-full px-3 py-1 text-m3-label-md " +
        (verified ? "bg-m3-primary text-m3-on-primary" : "bg-m3-secondary-container text-m3-on-secondary-container")
      }
    >
      {children}
    </span>
  );
}

function PlaceClusterList({
  places,
  onSelectPlace,
  className,
}: {
  places: Place[];
  onSelectPlace: (placeId: number) => void;
  className: string;
}) {
  return (
    <section className={className}>
      <div className="sticky top-0 z-10 bg-m3-surface-container-lowest pb-3 pt-1 text-m3-on-surface">
        <p className="text-m3-title-md">
          이 위치의 맛집 {places.length}곳
        </p>
        <p className="mt-1 text-m3-body-sm text-m3-on-surface-variant">
          마커가 겹쳐 보여요. 확인할 맛집을 선택해 주세요.
        </p>
      </div>
      <div className="divide-y divide-m3-outline-variant">
        {places.map((place) => {
          const category = getFoodCategory(place.category);
          return (
            <button
              key={place.id}
              type="button"
              onClick={() => onSelectPlace(place.id)}
              className="block w-full bg-m3-surface-container-lowest py-3 text-left text-m3-on-surface transition hover:bg-m3-surface-container-low active:scale-[0.995]"
            >
              <div className="flex items-start gap-3">
                <div className="h-14 w-14 shrink-0 overflow-hidden rounded-m3-md bg-m3-primary-container">
                  <PlaceImagePane place={place} className="h-full" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex min-w-0 items-center gap-2">
                    <h3 className="truncate text-m3-title-sm text-m3-on-surface">
                      {place.title}
                    </h3>
                    <span className="shrink-0 text-m3-body-sm text-m3-on-surface-variant">
                      {category.label}
                    </span>
                  </div>
                  <p className="mt-1 truncate text-m3-body-sm text-m3-on-surface-variant">
                    {place.address || place.regionName || place.distance}
                  </p>
                  <p className="mt-1 text-m3-body-sm font-medium text-m3-tertiary">
                    추천 {place.recommendCount} · 방문 {place.visitCount} · 댓글{" "}
                    {place.commentCount}
                  </p>
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </section>
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
      className="pointer-events-auto space-y-2 md:mx-auto md:w-full md:max-w-[680px]"
    >
      <form
        className="flex h-12 items-center gap-2 rounded-m3-lg border border-m3-outline-variant bg-m3-surface-container-lowest px-3 text-m3-on-surface shadow-m3-2 transition focus-within:border-m3-primary focus-within:ring-2 focus-within:ring-m3-primary/20"
        onSubmit={(event) => {
          event.preventDefault();
          const form = new FormData(event.currentTarget);
          onSearch(String(form.get("keyword") ?? ""));
        }}
      >
        <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-m3-md bg-m3-primary-container text-m3-on-primary-container">
          <SearchIcon />
        </span>
        <input
          name="keyword"
          aria-label="맛집 검색"
          placeholder="검색어를 입력하세요"
          className="min-w-0 flex-1 bg-transparent text-m3-body-md text-m3-on-surface placeholder:text-m3-on-surface-variant focus:outline-none"
        />
        <button
          type="submit"
          className="flex h-9 w-12 shrink-0 items-center justify-center rounded-m3-md bg-m3-primary text-m3-label-md text-m3-on-primary shadow-m3-1 active:scale-95"
        >
          검색
        </button>
      </form>

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
  sheetOpen,
  onUseCurrentLocation,
  onRegister,
}: {
  mapActions: MapActions | null;
  sheetOpen: boolean;
  onUseCurrentLocation: () => void;
  onRegister: () => void;
}) {
  const controls = [
    {
      label: "내 위치",
      icon: <CurrentLocationIcon />,
      onClick: onUseCurrentLocation,
      disabled: false,
    },
    {
      label: "확대",
      icon: <ZoomInIcon />,
      onClick: mapActions?.zoomIn,
      disabled: !mapActions,
    },
    {
      label: "축소",
      icon: <ZoomOutIcon />,
      onClick: mapActions?.zoomOut,
      disabled: !mapActions,
    },
  ];

  return (
    <>
      <div
        data-map-control="floating-actions"
        className="pointer-events-auto absolute right-4 top-[112px] z-20 flex flex-col overflow-hidden rounded-m3-lg border border-m3-outline-variant bg-m3-surface-container-lowest text-m3-on-surface shadow-m3-3 sm:right-5 sm:top-[120px] md:right-6"
      >
        {controls.map((control, index) => (
          <button
            key={control.label}
            type="button"
            onClick={control.onClick}
            disabled={control.disabled}
            aria-label={control.label}
            title={control.label}
            className={
              "flex h-12 w-12 items-center justify-center text-lg font-black transition hover:bg-m3-primary-container/60 active:scale-[0.98] disabled:opacity-50" +
              (index > 0 ? " border-t border-m3-outline-variant" : "")
            }
          >
            {control.icon}
          </button>
        ))}
      </div>
      {!sheetOpen && (
        <button
          type="button"
          onClick={onRegister}
          aria-label="맛집 등록"
          className="pointer-events-auto absolute bottom-[calc(92px+env(safe-area-inset-bottom))] right-4 z-20 flex h-[56px] w-[56px] items-center justify-center rounded-m3-lg bg-m3-primary text-2xl font-black text-m3-on-primary shadow-m3-3 transition active:scale-95"
        >
          +
        </button>
      )}
    </>
  );
}

function MobileSelectedPlaceSheet({
  places,
  visiblePlaces,
  selectedPlace,
  clusterPlaces,
  stage,
  loading,
  errorMessage,
  selectedCategory,
  searchKeyword,
  searchActive,
  locationMessage,
  onToggleWish,
  onSelectClusterPlace,
  onRetry,
  onStageChange,
  onClose,
}: {
  places: Place[];
  visiblePlaces: Place[];
  selectedPlace: Place | null;
  clusterPlaces: Place[];
  stage: MobileSheetStage;
  loading: boolean;
  errorMessage: string | null;
  selectedCategory: string;
  searchKeyword: string;
  searchActive: boolean;
  locationMessage: string | null;
  onToggleWish: (id: number) => void;
  onSelectClusterPlace: (placeId: number) => void;
  onRetry: () => void;
  onStageChange: (stage: MobileSheetStage) => void;
  onClose: () => void;
}) {
  const sheetRef = useRef<HTMLElement | null>(null);
  const startYRef = useRef<number | null>(null);
  const draggedRef = useRef(false);
  const suppressClickRef = useRef(false);
  const [dragOffset, setDragOffset] = useState(0);
  const open = stage !== "closed";
  const full = stage === "full";

  const resetDrag = (clearOffset = true) => {
    startYRef.current = null;
    draggedRef.current = false;
    if (clearOffset) {
      setDragOffset(0);
    }
  };

  const finishDrag = (clientY: number) => {
    const startY = startYRef.current;
    if (startY == null) {
      return;
    }

    const deltaY = clientY - startY;
    const dragged = draggedRef.current || Math.abs(deltaY) > 8;
    resetDrag();
    if (!dragged) {
      return;
    }

    suppressClickRef.current = true;
    window.setTimeout(() => {
      suppressClickRef.current = false;
    }, 180);

    if (deltaY > 72) {
      if (full) {
        onStageChange("half");
      } else {
        onClose();
      }
      return;
    }

    if (deltaY < -48) {
      onStageChange("full");
    }
  };

  const shouldStartSheetDrag = (target: EventTarget | null) => {
    if (!(target instanceof HTMLElement)) {
      return true;
    }
    if (target.closest("input, textarea, select")) {
      return false;
    }
    const scrollContainer = target.closest<HTMLElement>(
      "[data-mobile-sheet-scroll='content']",
    );
    return !(full && scrollContainer && scrollContainer.scrollTop > 0);
  };

  const markDragProgress = (clientY: number) => {
    const startY = startYRef.current;
    if (startY == null) {
      return;
    }
    const deltaY = clientY - startY;
    if (full && deltaY < -8) {
      resetDrag();
      return;
    }
    if (Math.abs(deltaY) > 8) {
      draggedRef.current = true;
    }
    if (draggedRef.current) {
      setDragOffset(Math.max(-88, Math.min(260, deltaY)));
    }
  };

  const handlePointerDown = (event: ReactPointerEvent<HTMLElement>) => {
    if (!shouldStartSheetDrag(event.target)) {
      resetDrag();
      return;
    }
    startYRef.current = event.clientY;
    draggedRef.current = false;
    setDragOffset(0);
    event.currentTarget.setPointerCapture?.(event.pointerId);
  };

  const handlePointerMove = (event: ReactPointerEvent<HTMLElement>) => {
    markDragProgress(event.clientY);
  };

  const handlePointerUp = (event: ReactPointerEvent<HTMLElement>) => {
    finishDrag(event.clientY);
  };

  const handlePointerCancel = () => {
    resetDrag();
  };

  const handleMouseDown = (event: ReactMouseEvent<HTMLElement>) => {
    if (!shouldStartSheetDrag(event.target)) {
      resetDrag();
      return;
    }
    startYRef.current = event.clientY;
    draggedRef.current = false;
    setDragOffset(0);
  };

  const handleMouseMove = (event: ReactMouseEvent<HTMLElement>) => {
    markDragProgress(event.clientY);
  };

  const handleMouseUp = (event: ReactMouseEvent<HTMLElement>) => {
    finishDrag(event.clientY);
  };

  const beginTouchDrag = (target: EventTarget | null, clientY: number | null) => {
    if (clientY == null || !shouldStartSheetDrag(target)) {
      resetDrag();
      return;
    }
    startYRef.current = clientY;
    draggedRef.current = false;
    setDragOffset(0);
  };

  const handleClickCapture = (event: ReactMouseEvent<HTMLElement>) => {
    if (!suppressClickRef.current) {
      return;
    }
    event.preventDefault();
    event.stopPropagation();
    suppressClickRef.current = false;
  };

  useEffect(() => {
    const sheet = sheetRef.current;
    if (!sheet) {
      return;
    }

    const handleNativeTouchStart = (event: TouchEvent) => {
      beginTouchDrag(event.target, event.touches[0]?.clientY ?? null);
    };

    const handleNativeTouchMove = (event: TouchEvent) => {
      const clientY = event.touches[0]?.clientY;
      if (clientY == null) {
        return;
      }
      markDragProgress(clientY);
      if (draggedRef.current) {
        event.preventDefault();
      }
    };

    const handleNativeTouchEnd = (event: TouchEvent) => {
      finishDrag(event.changedTouches[0]?.clientY ?? startYRef.current ?? 0);
    };

    const handleNativeTouchCancel = () => {
      resetDrag();
    };

    sheet.addEventListener("touchstart", handleNativeTouchStart, {
      passive: true,
    });
    sheet.addEventListener("touchmove", handleNativeTouchMove, {
      passive: false,
    });
    sheet.addEventListener("touchend", handleNativeTouchEnd);
    sheet.addEventListener("touchcancel", handleNativeTouchCancel);

    return () => {
      sheet.removeEventListener("touchstart", handleNativeTouchStart);
      sheet.removeEventListener("touchmove", handleNativeTouchMove);
      sheet.removeEventListener("touchend", handleNativeTouchEnd);
      sheet.removeEventListener("touchcancel", handleNativeTouchCancel);
    };
  });

  const sheetStyle: CSSProperties = {
    touchAction: full ? "pan-y" : "none",
    ...(dragOffset !== 0
      ? {
          transform: `translate3d(0, ${dragOffset}px, 0)`,
          transitionDuration: "0ms",
        }
      : {}),
  };

  return (
    <section
      ref={sheetRef}
      data-selected-place-sheet="mobile-sheet"
      style={sheetStyle}
      onClickCapture={handleClickCapture}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onPointerCancel={handlePointerCancel}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      className={`pointer-events-auto relative z-30 flex flex-col overflow-hidden rounded-t-m3-xl bg-m3-surface-container-lowest text-m3-on-surface shadow-m3-3 transition-all duration-300 ease-out md:mx-auto md:w-full md:max-w-[680px] ${
        open ? "translate-y-0" : "translate-y-[calc(100%+24px)]"
      } ${
        full
          ? "h-[calc(100dvh-118px)] max-h-[calc(100dvh-118px)] md:h-[calc(100dvh-132px)] md:max-h-[calc(100dvh-132px)]"
          : "h-[min(430px,70dvh)] max-h-[min(430px,70dvh)] pb-[calc(8px+env(safe-area-inset-bottom))]"
      }`}
    >
      <div className="relative h-6 shrink-0">
        <button
          type="button"
          data-mobile-sheet-drag-handle="true"
          aria-label={full ? "맛집 상세 접기" : "맛집 상세 펼치기"}
          onClick={() => (full ? onStageChange("half") : onStageChange("full"))}
          className="absolute left-1/2 top-0 flex h-6 w-28 -translate-x-1/2 items-center justify-center"
        >
          <span className="h-1 w-11 rounded-m3-full bg-m3-outline-variant" />
        </button>
      </div>
      <div className="min-h-0 flex-1 overflow-hidden">
        {open && (
          <PanelState
            places={places}
            visiblePlaces={visiblePlaces}
            selectedPlace={selectedPlace}
            clusterPlaces={clusterPlaces}
            loading={loading}
            errorMessage={errorMessage}
            selectedCategory={selectedCategory}
            searchKeyword={searchKeyword}
            searchActive={searchActive}
            locationMessage={locationMessage}
            onToggleWish={onToggleWish}
            onSelectClusterPlace={onSelectClusterPlace}
            onRetry={onRetry}
            full={full}
            onStageChange={onStageChange}
            onClose={onClose}
          />
        )}
      </div>
    </section>
  );
}

function DesktopSelectedPlacePanel({
  places,
  visiblePlaces,
  selectedPlace,
  clusterPlaces,
  loading,
  errorMessage,
  selectedCategory,
  searchKeyword,
  searchActive,
  locationMessage,
  onToggleWish,
  onSelectClusterPlace,
  onRetry,
  onSelectPlace,
}: {
  places: Place[];
  visiblePlaces: Place[];
  selectedPlace: Place | null;
  clusterPlaces: Place[];
  loading: boolean;
  errorMessage: string | null;
  selectedCategory: string;
  searchKeyword: string;
  searchActive: boolean;
  locationMessage: string | null;
  onToggleWish: (id: number) => void;
  onSelectClusterPlace: (placeId: number) => void;
  onRetry: () => void;
  onSelectPlace: (placeId: number) => void;
}) {
  if (clusterPlaces.length > 1) {
    return (
      <PlaceClusterList
        places={clusterPlaces}
        onSelectPlace={onSelectClusterPlace}
        className="desktop-compact-scrollbar mt-4 min-h-0 flex-1 overflow-y-auto pr-1"
      />
    );
  }

  if (selectedPlace) {
    return (
      <div
        data-desktop-selected-panel="root"
        className="desktop-compact-scrollbar mt-4 min-h-0 flex-1 overflow-y-auto pr-1"
      >
        <DesktopPlaceDetailCard
          key={selectedPlace.id}
          place={selectedPlace}
          locationMessage={locationMessage}
          onToggleWish={() => onToggleWish(selectedPlace.id)}
        />
      </div>
    );
  }

  if (selectedCategory !== "ALL") {
    return (
      <DesktopCategoryPlaceList
        places={visiblePlaces}
        selectedCategory={selectedCategory}
        onSelectPlace={onSelectPlace}
      />
    );
  }

  return (
    <div
      data-desktop-selected-panel="root"
      className="desktop-compact-scrollbar mt-4 min-h-0 flex-1 overflow-y-auto pr-1"
    >
      <PanelState
        places={places}
        visiblePlaces={visiblePlaces}
        selectedPlace={selectedPlace}
        clusterPlaces={clusterPlaces}
        loading={loading}
        errorMessage={errorMessage}
        selectedCategory={selectedCategory}
        searchKeyword={searchKeyword}
        searchActive={searchActive}
        locationMessage={locationMessage}
        onToggleWish={onToggleWish}
        onSelectClusterPlace={onSelectClusterPlace}
        onRetry={onRetry}
        full
      />
    </div>
  );
}

function DesktopCategoryPlaceList({
  places,
  selectedCategory,
  onSelectPlace,
}: {
  places: Place[];
  selectedCategory: string;
  onSelectPlace: (placeId: number) => void;
}) {
  if (places.length === 0) {
    return (
      <div className="desktop-compact-scrollbar mt-4 min-h-0 flex-1 overflow-y-auto pr-1">
        <StateCard
          title={getFoodCategoryLabel(selectedCategory) + " 맛집이 아직 없어요"}
          desc="다른 카테고리를 둘러보세요."
        />
      </div>
    );
  }

  return (
    <div className="desktop-compact-scrollbar mt-2 min-h-0 flex-1 overflow-y-auto pr-1">
      <div className="divide-y divide-m3-outline-variant">
        {places.map((place) => {
          const category = getFoodCategory(place.category);
          const scoreText =
            place.rating > 0 ? place.rating.toFixed(1) : "평점 집계 중";
          return (
            <button
              key={place.id}
              type="button"
              onClick={() => onSelectPlace(place.id)}
              className="block w-full bg-m3-surface-container-lowest px-0 py-4 text-left transition hover:bg-m3-surface-container active:scale-[0.995]"
            >
              <div className="flex items-start gap-3">
                <div className="h-16 w-16 shrink-0 overflow-hidden rounded-m3-md bg-m3-primary-container">
                  <PlaceImagePane place={place} className="h-full" />
                </div>
                <div className="min-w-0 flex-1 pt-0.5">
                  <div className="flex min-w-0 items-center gap-2">
                    <h3 className="truncate text-m3-title-sm text-m3-on-surface">
                      {place.title}
                    </h3>
                    <span className="shrink-0 text-m3-body-sm text-m3-on-surface-variant">
                      {category.label}
                    </span>
                  </div>
                  <p className="mt-1 flex flex-wrap items-center gap-x-1.5 gap-y-1 text-m3-body-sm text-m3-on-surface-variant">
                    <span className="text-m3-tertiary">★★★★★</span>
                    <span className="text-m3-on-surface">{scoreText}</span>
                    <span>추천 {place.recommendCount}</span>
                    <span>방문 {place.visitCount}</span>
                  </p>
                  <p className="mt-1 truncate text-m3-body-sm text-m3-on-surface-variant">
                    {place.address || place.regionName || place.distance}
                  </p>
                  <p className="mt-1 line-clamp-2 text-m3-body-sm text-m3-on-surface-variant">
                    {place.desc || "동네 사람들이 추천한 꿀맛집이에요."}
                  </p>
                </div>
              </div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function DesktopPlaceDetailCard({
  place,
  locationMessage,
  onToggleWish,
}: {
  place: Place;
  locationMessage: string | null;
  onToggleWish: () => void;
}) {
  const [activeTab, setActiveTab] = useState<DesktopPlaceTab>("home");
  const category = getFoodCategory(place.category);
  const scoreText = place.rating > 0 ? place.rating.toFixed(1) : "평점 집계 중";
  const reviewCount =
    place.reviewCount > 0
      ? place.reviewCount
      : place.commentCount + place.visitCount + place.recommendCount;
  const tabs: Array<{ value: DesktopPlaceTab; label: string }> = [
    { value: "home", label: "홈" },
    { value: "menu", label: "메뉴" },
    { value: "photos", label: "사진" },
    { value: "comments", label: "댓글" },
    { value: "report", label: "신고" },
  ];

  return (
    <article className="flex min-h-full flex-col overflow-hidden rounded-m3-xl bg-m3-surface-container-lowest text-m3-on-surface shadow-m3-1">
      <button type="button" onClick={() => setActiveTab("photos")} className="relative block w-full text-left" aria-label="사진 탭 보기">
        <div className="grid h-[178px] grid-cols-[1.2fr_0.9fr] gap-1 overflow-hidden bg-m3-primary-container">
          <PlaceImagePane place={place} className="h-full" />
          <div className="grid h-full grid-rows-2 gap-1">
            <PlaceImagePane place={place} className="h-full brightness-[0.98]" />
            <PlaceImagePane place={place} className="h-full" />
          </div>
        </div>
        <span className="absolute bottom-3 left-1/2 -translate-x-1/2 rounded-full bg-black/55 px-3 py-1 text-xs font-bold text-white">1/6</span>
      </button>

      <div className="px-4 py-3">
        <div className="flex items-start gap-3">
          <div className="min-w-0 flex-1">
            <div className="flex min-w-0 items-center gap-2">
              <h2 className="truncate text-m3-title-lg text-m3-on-surface">{place.title}</h2>
              <span className="shrink-0 text-m3-body-sm text-m3-on-surface-variant">{category.label}</span>
            </div>
            <div className="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 text-m3-body-md text-m3-on-surface-variant">
              <span className="text-m3-tertiary">★★★★★</span>
              <span className="text-m3-on-surface">{scoreText}</span>
              <span>({reviewCount})</span>
              <span className="text-m3-tertiary">영업정보 확인 중</span>
            </div>
          </div>
          <button
            type="button"
            aria-label={place.isWished ? "저장 취소" : "저장"}
            onClick={onToggleWish}
            className={
              "flex h-10 w-10 shrink-0 items-center justify-center rounded-m3-full border shadow-m3-1 transition active:scale-95 " +
              (place.isWished
                ? "border-m3-primary bg-m3-secondary-container text-m3-on-secondary-container"
                : "border-m3-outline-variant bg-m3-surface-container-lowest text-m3-on-surface-variant")
            }
          >
            <SaveStarIcon active={place.isWished} />
          </button>
        </div>
        <p className="mt-3 line-clamp-2 text-m3-body-md text-m3-on-surface-variant">{place.desc || "동네 사람들이 추천한 꿀맛집이에요."}</p>
      </div>

      <div className="flex h-12 items-center overflow-x-auto border-y border-m3-outline-variant px-2 text-m3-label-lg text-m3-on-surface-variant [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {tabs.map((tab) => (
          <button
            key={tab.value}
            type="button"
            onClick={() => setActiveTab(tab.value)}
            className={activeTab === tab.value ? "flex h-full shrink-0 items-center border-b-2 border-m3-primary px-4 text-m3-primary transition focus:outline-none focus-visible:ring-2 focus-visible:ring-m3-primary/20" : "flex h-full shrink-0 items-center border-b-2 border-transparent px-4 transition hover:text-m3-on-surface focus:outline-none focus-visible:ring-2 focus-visible:ring-m3-primary/20"}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div className="desktop-compact-scrollbar min-h-0 flex-1 overflow-y-auto">
        <DesktopPlaceTabContent tab={activeTab} place={place} locationMessage={locationMessage} />
      </div>
    </article>
  );
}

function DesktopPlaceTabContent({
  tab,
  place,
  locationMessage,
}: {
  tab: DesktopPlaceTab;
  place: Place;
  locationMessage: string | null;
}) {
  if (tab === "menu") {
    return <div className="space-y-3 p-4"><CompactInfoCard title="추천 메뉴" desc={place.desc || "등록된 추천 문구를 확인해 주세요."} /><CompactInfoCard title="가격대" desc={place.price} /></div>;
  }
  if (tab === "photos") {
    return <div className="grid grid-cols-2 gap-2 p-4">{Array.from({ length: 6 }).map((_, index) => <div key={index} className="aspect-square overflow-hidden rounded-m3-lg bg-m3-primary-container"><PlaceImagePane place={place} className="h-full" /></div>)}</div>;
  }
  if (tab === "comments") {
    return <PlaceCommentsPanel place={place} className="p-4" />;
  }
  if (tab === "report") {
    return <div className="space-y-3 p-4"><CompactInfoCard title="신고 안내" desc="잘못된 정보, 폐업, 부적절한 내용은 장소 상세 신고 기능으로 접수할 수 있어요." /><CompactInfoCard title="운영 기준" desc="신고 내용은 운영자가 확인한 뒤 필요한 조치를 진행해요." /></div>;
  }
  return (
    <div>
      <DetailInfoRow icon="시간" title="영업정보 준비 중" desc="방문 전 영업시간을 확인해 주세요" />
      <DetailInfoRow icon="위치" title={place.address || place.regionName || "주소 정보 준비 중"} desc={place.distance} />
      <DetailInfoRow icon="링크" title="https://www.honeytong.co.kr" />
      <DetailInfoRow icon="전화" title="전화 정보 준비 중" />
      <DetailInfoRow icon="주차" title="주차정보" desc="등록된 정보가 아직 없어요" />
      {locationMessage && <p className="px-4 py-3 text-m3-body-sm font-medium text-m3-tertiary">{locationMessage}</p>}
    </div>
  );
}

function CompactInfoCard({ title, desc }: { title: string; desc: string }) {
  return (
    <div className="border-b border-m3-outline-variant bg-m3-surface-container-lowest px-4 py-3">
      <p className="text-m3-title-sm text-m3-on-surface">{title}</p>
      <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">{desc}</p>
    </div>
  );
}

function PlaceCommentsPanel({
  place,
  className,
}: {
  place: Place;
  className?: string;
}) {
  const [comments, setComments] = useState<CommentItem[]>([]);
  const [commentText, setCommentText] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const loadComments = useCallback(async () => {
    setLoading(true);
    setMessage(null);
    try {
      setComments(await getPlaceComments(place.id));
    } catch (error) {
      setComments([]);
      setMessage(getApiErrorMessage(error, "댓글을 불러오지 못했어요."));
    } finally {
      setLoading(false);
    }
  }, [place.id]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadComments();
    }, 0);
    return () => window.clearTimeout(timer);
  }, [loadComments]);

  const handleSubmit = async (event: ReactFormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const content = commentText.trim();
    if (!content) {
      setMessage("댓글 내용을 입력해 주세요.");
      return;
    }

    setSubmitting(true);
    setMessage(null);
    try {
      await createComment(place.id, content);
      setCommentText("");
      setMessage("댓글을 남겼어요.");
      await loadComments();
    } catch (error) {
      setMessage(getApiErrorMessage(error, "댓글을 저장하지 못했어요."));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className={className}>
      <div className="space-y-3">
        <div>
          <p className="text-m3-title-md text-m3-on-surface">
            댓글 {comments.length || place.commentCount}개
          </p>
          <p className="mt-1 text-m3-body-sm text-m3-on-surface-variant">
            이 맛집에 대한 동네 이웃들의 한마디예요.
          </p>
        </div>

        {loading ? (
          <CompactInfoCard title="댓글을 불러오는 중이에요." desc="잠시만 기다려 주세요." />
        ) : comments.length === 0 ? (
          <CompactInfoCard title="아직 댓글이 없어요." desc="첫 번째 동네 한마디를 남겨보세요." />
        ) : (
          <div className="space-y-2">
            {comments.map((comment) => (
              <article
                key={comment.commentId}
                className="rounded-m3-lg border border-m3-outline-variant bg-m3-surface-container-lowest p-3 shadow-m3-1"
              >
                <div className="flex items-center justify-between gap-3">
                  <p className="truncate text-m3-title-sm text-m3-on-surface">
                    {comment.nickname || "꿀벌님"}
                  </p>
                  <time className="shrink-0 text-m3-body-sm text-m3-on-surface-variant">
                    {formatCommentTime(comment.createdAt)}
                  </time>
                </div>
                <p className="mt-2 whitespace-pre-wrap text-m3-body-md text-m3-on-surface-variant">
                  {comment.content}
                </p>
              </article>
            ))}
          </div>
        )}

        {message && (
          <p className="rounded-m3-md bg-m3-secondary-container px-3 py-2 text-m3-body-sm text-m3-on-secondary-container">
            {message}
          </p>
        )}
      </div>

      <form
        onSubmit={handleSubmit}
        className="sticky bottom-0 mt-4 flex gap-2 border-t border-m3-outline-variant bg-m3-surface-container-lowest pt-3"
      >
        <input
          value={commentText}
          onChange={(event) => setCommentText(event.target.value)}
          maxLength={500}
          placeholder={
            hasStoredAccessToken()
              ? "댓글을 입력해 주세요."
              : "로그인하면 댓글을 남길 수 있어요."
          }
          className="min-w-0 flex-1 rounded-m3-md border border-m3-outline bg-m3-surface-container-lowest px-3 py-3 text-m3-body-md text-m3-on-surface outline-none transition placeholder:text-m3-on-surface-variant focus:border-m3-primary focus:ring-2 focus:ring-m3-primary/20"
        />
        <button
          type="submit"
          disabled={submitting}
          className="flex h-12 w-16 shrink-0 items-center justify-center rounded-m3-md bg-m3-primary text-m3-label-lg text-m3-on-primary shadow-m3-1 active:scale-95 disabled:opacity-60"
        >
          {submitting ? "등록 중" : "등록"}
        </button>
      </form>
    </section>
  );
}

function formatCommentTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "";
  }
  return new Intl.DateTimeFormat("ko-KR", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function PanelState({
  places,
  visiblePlaces,
  selectedPlace,
  clusterPlaces,
  loading,
  errorMessage,
  selectedCategory,
  searchKeyword,
  searchActive,
  locationMessage,
  onToggleWish,
  onSelectClusterPlace,
  onRetry,
  full = false,
  onStageChange,
  onClose,
}: {
  places: Place[];
  visiblePlaces: Place[];
  selectedPlace: Place | null;
  clusterPlaces: Place[];
  loading: boolean;
  errorMessage: string | null;
  selectedCategory: string;
  searchKeyword: string;
  searchActive: boolean;
  locationMessage: string | null;
  onToggleWish: (id: number) => void;
  onSelectClusterPlace: (placeId: number) => void;
  onRetry: () => void;
  full?: boolean;
  onStageChange?: (stage: MobileSheetStage) => void;
  onClose?: () => void;
}) {
  if (loading) return <StateCard title="맛집을 불러오는 중이에요." />;
  if (errorMessage) return <StateCard title={errorMessage} desc="잠시 뒤 다시 확인해보세요." actionLabel="다시 불러오기" onAction={onRetry} />;
  if (searchActive && places.length === 0) return <StateCard title={`"${searchKeyword}" 검색 결과가 없어요.`} desc="다른 맛집 이름, 메뉴, 지역명으로 다시 검색해보세요." actionLabel="전체 맛집 보기" onAction={onRetry} />;
  if (places.length === 0) return <StateCard title="아직 보여줄 맛집이 없어요." desc="우리 동네 첫 꿀맛집을 등록해보세요." />;
  if (visiblePlaces.length === 0) return <StateCard title={getFoodCategoryLabel(selectedCategory) + " 맛집이 아직 없어요"} desc="다른 카테고리를 둘러보세요." />;
  if (clusterPlaces.length > 1) return <PlaceClusterList places={clusterPlaces} onSelectPlace={onSelectClusterPlace} className="h-full overflow-y-auto px-4 pb-4" />;
  if (!selectedPlace) return <StateCard title="지도에서 맛집 마커를 선택해 주세요." desc="선택한 맛집 정보가 여기에 표시돼요." />;
  return <SelectedPlaceCard key={selectedPlace.id} place={selectedPlace} locationMessage={locationMessage} onToggleWish={() => onToggleWish(selectedPlace.id)} full={full} onStageChange={onStageChange} onClose={onClose} />;
}

function SelectedPlaceCard({
  place,
  locationMessage,
  onToggleWish,
  full,
  onStageChange,
  onClose,
}: {
  place: Place;
  locationMessage: string | null;
  onToggleWish: () => void;
  full: boolean;
  onStageChange?: (stage: MobileSheetStage) => void;
  onClose?: () => void;
}) {
  const [activeTab, setActiveTab] = useState<DesktopPlaceTab>("home");
  const category = getFoodCategory(place.category);
  const scoreText = place.rating > 0 ? place.rating.toFixed(1) : "평점 집계 중";
  const reviewCount = place.reviewCount > 0 ? place.reviewCount : place.commentCount + place.visitCount + place.recommendCount;
  const tabs: Array<{ value: DesktopPlaceTab; label: string }> = [
    { value: "home", label: "홈" },
    { value: "menu", label: "메뉴" },
    { value: "photos", label: "사진" },
    { value: "comments", label: "댓글" },
    { value: "report", label: "신고" },
  ];
  const handleTabClick = (tab: DesktopPlaceTab) => {
    setActiveTab(tab);
    if (!full) {
      onStageChange?.("full");
    }
  };

  const handleImageClick = () => {
    handleTabClick("photos");
  };

  return (
    <article className="flex h-full min-h-0 flex-col bg-m3-surface-container-lowest text-m3-on-surface">
      <div className={full ? "relative block shrink-0 px-2 pb-3 text-left" : "relative block min-h-0 flex-1 basis-1/2 px-2 pb-0 text-left"}>
        <button
          type="button"
          onClick={handleImageClick}
          className="block h-full w-full text-left focus:outline-none focus-visible:ring-2 focus-visible:ring-[#f6b800]/45"
          aria-label="사진 탭 보기"
        >
          <div
            className={
              full
                ? "grid h-[202px] grid-cols-[1.05fr_1fr_0.82fr] gap-2 overflow-hidden rounded-m3-lg bg-m3-primary-container"
                : "grid h-full min-h-[150px] grid-cols-3 gap-px overflow-hidden rounded-m3-lg bg-m3-primary-container"
            }
          >
            <PlaceImagePane place={place} className="h-full" />
            <PlaceImagePane place={place} className="h-full brightness-[0.98]" />
            <PlaceImagePane place={place} className="h-full" />
          </div>
        </button>
        <span className="pointer-events-none absolute bottom-6 left-1/2 -translate-x-1/2 rounded-full bg-black/55 px-3 py-1 text-xs font-bold text-white">
          1/6
        </span>
        <button
          type="button"
          aria-label="맛집 상세 닫기"
          onClick={onClose}
          className="absolute right-4 top-3 flex h-9 w-9 items-center justify-center rounded-m3-full border border-m3-outline-variant bg-m3-surface-container-lowest/95 text-m3-on-surface shadow-m3-1 backdrop-blur-md transition active:scale-95"
        >
          <CloseIcon />
        </button>
      </div>

      <div className={full ? "shrink-0 px-4 pb-3" : "flex shrink-0 items-center px-4 py-3"}>
        <div className="grid w-full grid-cols-[minmax(0,1fr)_auto] items-start gap-2">
          <div className="min-w-0 flex-1">
            <div className="flex min-w-0 items-center gap-2">
              <h2 className="min-w-0 truncate text-m3-title-lg text-m3-on-surface">{place.title}</h2>
              <span className="shrink-0 text-m3-body-sm text-m3-on-surface-variant">{category.label}</span>
            </div>
            <div className="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 text-m3-body-md text-m3-on-surface-variant">
              <span className="text-m3-tertiary">★★★★★</span>
              <span className="text-m3-on-surface">{scoreText}</span>
              <span>({reviewCount})</span>
              <span className="text-m3-tertiary">영업정보 확인 중</span>
            </div>
          </div>
          <div className="flex shrink-0 items-center justify-end">
            <button
              type="button"
              aria-label={place.isWished ? "저장 취소" : "저장"}
              onClick={onToggleWish}
              className={
                "flex h-11 w-11 items-center justify-center rounded-m3-full border shadow-m3-1 transition active:scale-95 " +
                (place.isWished
                  ? "border-m3-primary bg-m3-secondary-container text-m3-on-secondary-container"
                  : "border-m3-outline-variant bg-m3-surface-container-lowest text-m3-on-surface-variant")
              }
            >
              <SaveStarIcon active={place.isWished} />
            </button>








          </div>
        </div>
      </div>

      <div className="flex h-12 shrink-0 items-center overflow-x-auto border-y border-m3-outline-variant px-2 text-m3-label-lg text-m3-on-surface-variant [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {tabs.map((tab) => (
          <button
            key={tab.value}
            type="button"
            onClick={() => handleTabClick(tab.value)}
            className={activeTab === tab.value ? "flex h-full shrink-0 items-center border-b-2 border-m3-primary px-4 text-m3-primary transition focus:outline-none focus-visible:ring-2 focus-visible:ring-m3-primary/20" : "flex h-full shrink-0 items-center border-b-2 border-transparent px-4 transition focus:outline-none focus-visible:ring-2 focus-visible:ring-m3-primary/20 active:text-m3-on-surface"}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <div
        data-mobile-sheet-scroll="content"
        className={
          full
            ? "min-h-0 flex-1 overflow-y-auto overscroll-contain pb-[calc(16px+env(safe-area-inset-bottom))] [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
            : "h-0 min-h-0 overflow-hidden"
        }
      >
        {full && <MobilePlaceTabContent tab={activeTab} place={place} locationMessage={locationMessage} />}
      </div>
    </article>
  );
}

function MobilePlaceTabContent({
  tab,
  place,
  locationMessage,
}: {
  tab: DesktopPlaceTab;
  place: Place;
  locationMessage: string | null;
}) {
  if (tab === "menu") {
    return (
      <div className="space-y-3 p-4">
        <CompactInfoCard title="추천 메뉴" desc={place.desc || "등록된 추천 메뉴 정보가 아직 없어요."} />
        <CompactInfoCard title="가격대" desc={place.price} />
      </div>
    );
  }

  if (tab === "photos") {
    return (
      <div className="grid grid-cols-3 gap-2 p-4">
        {Array.from({ length: 6 }).map((_, index) => (
          <div key={index} className="aspect-square overflow-hidden rounded-m3-md bg-m3-primary-container">
            <PlaceImagePane place={place} className="h-full" />
          </div>
        ))}
      </div>
    );
  }

  if (tab === "comments") {
    return <PlaceCommentsPanel place={place} className="p-4" />;
  }

  if (tab === "report") {
    return (
      <div className="space-y-3 p-4">
        <CompactInfoCard title="신고" desc="잘못된 정보나 부적절한 내용은 신고 기능으로 접수할 수 있어요." />
        <CompactInfoCard title="운영 확인" desc="신고 내용은 운영자가 확인한 뒤 필요한 조치를 진행해요." />
      </div>
    );
  }

  return (
    <div>
      <div className="border-b border-m3-outline-variant px-4 py-3">
        <p className="text-m3-label-md text-m3-on-surface-variant">소개</p>
        <p className="mt-1 text-m3-body-md text-m3-on-surface">{place.desc || "동네 사람들이 추천한 꿀맛집이에요."}</p>
      </div>
      <DetailInfoRow icon="시간" title="영업정보 준비 중" desc="방문 전 영업시간을 확인해 주세요" />
      <DetailInfoRow icon="위치" title={place.address || place.regionName || "주소 정보 준비 중"} desc="복사" />
      <DetailInfoRow icon="링크" title="https://www.honeytong.co.kr" />
      <DetailInfoRow icon="전화" title="전화 정보 준비 중" />
      <DetailInfoRow icon="주차" title="주차정보" desc="등록된 정보가 아직 없어요" />
      {locationMessage && <p className="px-4 py-3 text-m3-body-sm font-medium text-m3-tertiary">{locationMessage}</p>}
    </div>
  );
}

function PlaceImagePane({ place, className }: { place: Place; className: string }) {
  const category = getFoodCategory(place.category);
  if (place.imageUrl) {
    return <img src={place.imageUrl} alt={place.title + " 대표 이미지"} className={className + " w-full object-cover"} />;
  }
  return <div className={className + " flex w-full items-center justify-center bg-m3-primary-container px-3 text-center text-m3-label-md text-m3-on-primary-container"}>{category.label} 이미지 준비 중</div>;
}

function DetailInfoRow({ icon, title, desc }: { icon: string; title: string; desc?: string }) {
  return <div className="flex min-h-[52px] items-center gap-3 border-b border-m3-outline-variant px-4 py-3 text-m3-body-md"><span className="flex min-h-7 min-w-10 shrink-0 items-center justify-center rounded-m3-full bg-m3-surface-container-high px-2 text-m3-label-md text-m3-on-surface-variant">{icon}</span><div className="min-w-0 flex-1"><p className="truncate text-m3-title-sm text-m3-on-surface">{title}</p>{desc && <p className="mt-0.5 truncate text-m3-body-sm text-m3-on-surface-variant">{desc}</p>}</div><span className="text-m3-on-surface-variant">›</span></div>;
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
    <div className={`${flush ? "mt-5" : ""} rounded-m3-xl bg-m3-surface-container-low p-5 text-center text-m3-on-surface shadow-m3-1`}>
      <p className="text-m3-title-sm">{title}</p>
      {desc && <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">{desc}</p>}
      {actionLabel && onAction && (
        <button
          type="button"
          onClick={onAction}
          className="mt-4 h-10 rounded-m3-full bg-m3-primary px-5 text-m3-label-lg text-m3-on-primary shadow-m3-1"
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
  onSelectPlace: (placeId: number, nearbyPlaces?: Place[]) => void;
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
        const zoomWithAnimation = (nextLevel: number) => {
          if (nextLevel === map.getLevel()) {
            return;
          }
          map.setLevel(nextLevel, { animate: { duration: 250 } });
        };

        onMapActionsReady({
          zoomIn: () => zoomWithAnimation(Math.max(1, map.getLevel() - 1)),
          zoomOut: () => zoomWithAnimation(Math.min(14, map.getLevel() + 1)),
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
            onSelectPlace(
              place.id,
              getVisuallyOverlappingMarkerPlaces(place, validPlaces, map, kakao),
            );
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
      className="pointer-events-auto absolute inset-0 z-0 h-full w-full touch-none select-none overflow-hidden bg-m3-surface-container-low"
      style={mapGestureStyle}
    />
  );
}

function MapStatus({ title, desc }: { title: string; desc: string }) {
  return (
    <div className="absolute left-0 top-0 z-[1] flex h-full min-h-screen w-full items-center justify-center bg-m3-surface-container-low px-8 text-center">
      <div className="rounded-m3-xl bg-m3-surface-container-lowest p-5 text-m3-on-surface shadow-m3-1">
        <p className="text-m3-title-sm">{title}</p>
        <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">{desc}</p>
      </div>
    </div>
  );
}

function createPlaceOverlay(place: Place, active: boolean) {
  const category = getFoodCategory(place.category);
  const root = document.createElement("div");
  root.dataset.placeMarkerRoot = "true";
  root.className = "pointer-events-none flex h-16 w-16 items-center justify-center";
  root.style.pointerEvents = "none";
  root.style.width = "64px";
  root.style.height = "64px";

  const marker = document.createElement("button");
  marker.type = "button";
  marker.title = place.title;
  marker.dataset.placeMarkerHit = "true";
  marker.className = active
    ? "pointer-events-auto flex h-12 w-12 origin-bottom -rotate-45 items-center justify-center rounded-[50%_50%_50%_0] border-[3px] border-m3-surface-container-lowest bg-m3-primary shadow-m3-3 ring-4 ring-m3-primary/20 transition hover:-translate-y-0.5 active:scale-95"
    : "pointer-events-auto flex h-11 w-11 origin-bottom -rotate-45 items-center justify-center rounded-[50%_50%_50%_0] border-2 border-m3-surface-container-lowest bg-m3-tertiary shadow-m3-2 ring-2 ring-m3-outline-variant transition hover:-translate-y-0.5 hover:bg-m3-primary active:scale-95";
  marker.style.pointerEvents = "auto";

  const icon = document.createElement("span");
  icon.className = active
    ? "pointer-events-none flex rotate-45 items-center justify-center text-[22px] leading-none drop-shadow-[0_1px_0_rgba(255,255,255,0.75)]"
    : "pointer-events-none flex rotate-45 items-center justify-center text-[20px] leading-none drop-shadow-[0_1px_0_rgba(255,255,255,0.75)]";
  icon.textContent = category.emoji;
  marker.append(icon);

  root.append(marker);
  return root;
}

function createUserLocationOverlay() {
  const dot = document.createElement("div");
  dot.className =
    "pointer-events-none flex h-6 w-6 items-center justify-center rounded-m3-full bg-m3-primary shadow-[0_0_0_10px_rgba(103,80,164,0.18)]";
  dot.style.pointerEvents = "none";

  const core = document.createElement("div");
  core.className = "pointer-events-none h-3 w-3 rounded-m3-full border-2 border-m3-surface-container-lowest bg-m3-primary";
  core.style.pointerEvents = "none";
  dot.append(core);
  return dot;
}

function isPlaceWithCoordinates(place: Place) {
  return Number.isFinite(place.latitude) && Number.isFinite(place.longitude);
}

function getVisuallyOverlappingMarkerPlaces(
  anchorPlace: Place,
  places: Place[],
  map: KakaoMap,
  kakao: Awaited<ReturnType<typeof loadKakaoMapSdk>>,
) {
  const markerBodyOverlapPx = 40;
  const projection = map.getProjection();
  const anchorPoint = projection.containerPointFromCoords(
    new kakao.maps.LatLng(anchorPlace.latitude, anchorPlace.longitude),
  );

  return places
    .map((place) => {
      const point = projection.containerPointFromCoords(
        new kakao.maps.LatLng(place.latitude, place.longitude),
      );
      return {
        place,
        distancePx: Math.hypot(point.x - anchorPoint.x, point.y - anchorPoint.y),
      };
    })
    .filter((item) => item.distancePx <= markerBodyOverlapPx)
    .sort((a, b) => a.distancePx - b.distancePx)
    .map((item) => item.place);
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

function getCurrentPosition(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 10000,
    });
  });
}

async function settle<T>(promise: Promise<T>) {
  try {
    return { ok: true as const, value: await promise };
  } catch {
    return { ok: false as const };
  }
}

function formatDesktopRegionName(region: MyRegion) {
  return `${region.cityName} ${region.districtName} ${region.dongName}`;
}

function formatDesktopRegionPolicy(policy: RegionChangePolicy | null) {
  if (!policy) {
    return "동네 변경 가능 상태를 불러오지 못했어요.";
  }
  if (policy.changeAllowed) {
    return `동네 변경 가능, 기준 주기 ${policy.cooldownDays}일`;
  }
  if (policy.nextAvailableAt) {
    return `다음 변경 가능 시간: ${formatDateTime(policy.nextAvailableAt)}`;
  }
  return "현재는 동네를 변경할 수 없어요.";
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
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
