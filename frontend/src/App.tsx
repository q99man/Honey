import { useCallback, useEffect, useRef, useState } from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { getPlaces, searchPlaces } from "./api/placeApi";
import HomePage from "./pages/HomePage";
import DetailPage from "./pages/DetailPage";
import RankingPage from "./pages/RankingPage";
import MyPage from "./pages/MyPage";
import AdminActivitiesPage from "./pages/AdminActivitiesPage";
import AdminDashboardPage from "./pages/AdminDashboardPage";
import AdminPoliciesPage from "./pages/AdminPoliciesPage";
import AdminPlacesPage from "./pages/AdminPlacesPage";
import AdminReportsPage from "./pages/AdminReportsPage";
import AdminUsersPage from "./pages/AdminUsersPage";
import PlaceRegisterPage from "./pages/PlaceRegisterPage";
import WishlistPage from "./pages/WishlistPage";
import type { Place } from "./types/place";

const WISHLIST_STORAGE_KEY = "honeytong-wished-place-ids";
const LIST_LOAD_ERROR = "\uc7a5\uc18c \ubaa9\ub85d\uc744 \ubd88\ub7ec\uc624\uc9c0 \ubabb\ud588\uc2b5\ub2c8\ub2e4.";
const SEARCH_LOAD_ERROR = "\uac80\uc0c9 \uacb0\uacfc\ub97c \ubd88\ub7ec\uc624\uc9c0 \ubabb\ud588\uc2b5\ub2c8\ub2e4.";

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

  const search = async (keyword: string) => {
    setLoading(true);
    try {
      await loadPlaces(keyword, wishedIds);
    } catch {
      setErrorMessage(SEARCH_LOAD_ERROR);
    } finally {
      setLoading(false);
    }
  };

  const toggleWish = (id: number) => {
    const next = new Set(wishedIds);
    if (next.has(id)) {
      next.delete(id);
    } else {
      next.add(id);
    }
    setWishedIds(next);
    setPlaces((prev) => applyWishlist(prev, next));
  };

  const updatePlace = (updatedPlace: Place) => {
    setPlaces((prev) =>
      prev.map((place) =>
        place.id === updatedPlace.id
          ? { ...updatedPlace, isWished: wishedIds.has(updatedPlace.id) }
          : place,
      ),
    );
  };

  const removePlace = (placeId: number) => {
    setPlaces((prev) => prev.filter((place) => place.id !== placeId));
    setWishedIds((prev) => {
      const next = new Set(prev);
      next.delete(placeId);
      return next;
    });
  };

  const refreshPlaces = useCallback(async () => {
    await loadPlaces(undefined, wishedIds);
  }, [loadPlaces, wishedIds]);

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={
            <HomePage
              places={places}
              loading={loading}
              errorMessage={errorMessage}
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
          element={<RankingPage places={places} />}
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
          element={<WishlistPage places={places} onToggleWish={toggleWish} />}
        />
        <Route path="/my" element={<MyPage onPlaceDeleted={removePlace} />} />
        <Route path="/admin" element={<AdminDashboardPage />} />
        <Route path="/admin/activities" element={<AdminActivitiesPage />} />
        <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
        <Route path="/admin/policies" element={<AdminPoliciesPage />} />
        <Route path="/admin/places" element={<AdminPlacesPage />} />
        <Route path="/admin/reports" element={<AdminReportsPage />} />
        <Route path="/admin/users" element={<AdminUsersPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
