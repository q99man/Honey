import { useCallback, useEffect, useRef, useState } from "react";
import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import { getPlaces, searchPlaces } from "./api/placeApi";
import AdminActivitiesPage from "./pages/AdminActivitiesPage";
import AdminAuditLogsPage from "./pages/AdminAuditLogsPage";
import AdminDashboardPage from "./pages/AdminDashboardPage";
import AdminPlacesPage from "./pages/AdminPlacesPage";
import AdminPoliciesPage from "./pages/AdminPoliciesPage";
import AdminReportsPage from "./pages/AdminReportsPage";
import AdminUsersPage from "./pages/AdminUsersPage";
import CommunityPage from "./pages/CommunityPage";
import DetailPage from "./pages/DetailPage";
import HomePage from "./pages/HomePage";
import MyPage from "./pages/MyPage";
import PlaceRegisterPage from "./pages/PlaceRegisterPage";
import RankingPage from "./pages/RankingPage";
import WishlistPage from "./pages/WishlistPage";
import type { Place } from "./types/place";

const WISHLIST_STORAGE_KEY = "honeytong-wished-place-ids";
const LIST_LOAD_ERROR = "맛집 목록을 불러오지 못했어요.";
const SEARCH_LOAD_ERROR = "검색 결과를 불러오지 못했어요.";

function App() {
  const [places, setPlaces] = useState<Place[]>([]);
  const [wishedIds, setWishedIds] = useState<Set<number>>(() => {
    const saved = localStorage.getItem(WISHLIST_STORAGE_KEY);
    if (!saved) {
      return new Set();
    }
    try {
      return new Set(JSON.parse(saved) as number[]);
    } catch {
      localStorage.removeItem(WISHLIST_STORAGE_KEY);
      return new Set();
    }
  });
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [searchKeyword, setSearchKeyword] = useState("");
  const initialWishedIdsRef = useRef<Set<number> | null>(null);

  if (initialWishedIdsRef.current === null) {
    initialWishedIdsRef.current = new Set(wishedIds);
  }

  const applyWishlist = useCallback((items: Place[], ids: Set<number>) => {
    return items.map((place) => ({
      ...place,
      isWished: ids.has(place.id),
    }));
  }, []);

  const loadPlaces = useCallback(
    async (keyword: string | undefined, ids: Set<number>) => {
      const items = keyword?.trim()
        ? await searchPlaces(keyword.trim())
        : await getPlaces();
      setPlaces(applyWishlist(items, ids));
      setErrorMessage(null);
    },
    [applyWishlist],
  );

  useEffect(() => {
    let mounted = true;
    loadPlaces(undefined, initialWishedIdsRef.current!)
      .catch(() => {
        if (mounted) {
          setErrorMessage(LIST_LOAD_ERROR);
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
  }, [loadPlaces]);

  useEffect(() => {
    localStorage.setItem(
      WISHLIST_STORAGE_KEY,
      JSON.stringify(Array.from(wishedIds)),
    );
  }, [wishedIds]);

  const search = useCallback(
    async (keyword: string) => {
      const trimmedKeyword = keyword.trim();
      setLoading(true);
      try {
        await loadPlaces(trimmedKeyword, wishedIds);
        setSearchKeyword(trimmedKeyword);
      } catch {
        setErrorMessage(SEARCH_LOAD_ERROR);
      } finally {
        setLoading(false);
      }
    },
    [loadPlaces, wishedIds],
  );

  const toggleWish = useCallback(
    (id: number) => {
      const next = new Set(wishedIds);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      setWishedIds(next);
      setPlaces((prev) => applyWishlist(prev, next));
    },
    [applyWishlist, wishedIds],
  );

  const updatePlace = useCallback(
    (updatedPlace: Place) => {
      setPlaces((prev) =>
        prev.map((place) =>
          place.id === updatedPlace.id
            ? { ...updatedPlace, isWished: wishedIds.has(updatedPlace.id) }
            : place,
        ),
      );
    },
    [wishedIds],
  );

  const removePlace = useCallback((placeId: number) => {
    setPlaces((prev) => prev.filter((place) => place.id !== placeId));
    setWishedIds((prev) => {
      const next = new Set(prev);
      next.delete(placeId);
      return next;
    });
  }, []);

  const refreshPlaces = useCallback(async () => {
    await loadPlaces(undefined, wishedIds);
  }, [loadPlaces, wishedIds]);

  return (
    <BrowserRouter>
      <Routes>
        {/* User Routes */}
        <Route
          path="/"
          element={
            <HomePage
              places={places}
              loading={loading}
              errorMessage={errorMessage}
              searchKeyword={searchKeyword}
              searchActive={searchKeyword.length > 0}
              onToggleWish={toggleWish}
              onSearch={search}
            />
          }
        />
        <Route
          path="/places/:id"
          element={
            <DetailPage
              wishedIds={wishedIds}
              onToggleWish={toggleWish}
              onPlaceUpdated={updatePlace}
            />
          }
        />
        <Route
          path="/ranking"
          element={
            <RankingPage
              places={places}
              placesLoading={loading}
              placesErrorMessage={errorMessage}
            />
          }
        />
        <Route
          path="/places/new"
          element={<PlaceRegisterPage onPlaceCreated={refreshPlaces} />}
        />
        <Route
          path="/places/:id/edit"
          element={
            <PlaceRegisterPage
              mode="edit"
              onPlaceCreated={refreshPlaces}
              onPlaceUpdated={updatePlace}
            />
          }
        />
        <Route
          path="/wishlist"
          element={
            <WishlistPage
              places={places}
              loading={loading}
              errorMessage={errorMessage}
              onToggleWish={toggleWish}
            />
          }
        />
        <Route path="/my" element={<MyPage onPlaceDeleted={removePlace} />} />
        <Route path="/community" element={<CommunityPage />} />

        {/* Admin Routes */}
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
        <Route path="/admin/activities" element={<AdminActivitiesPage />} />
        <Route path="/admin/audit-logs" element={<AdminAuditLogsPage />} />
        <Route path="/admin/policies" element={<AdminPoliciesPage />} />
        <Route path="/admin/places" element={<AdminPlacesPage />} />
        <Route path="/admin/reports" element={<AdminReportsPage />} />
        <Route path="/admin/users" element={<AdminUsersPage />} />

        {/* Fallback route */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
