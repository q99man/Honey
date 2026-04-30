import { useCallback, useEffect, useRef, useState } from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { getPlaces, searchPlaces } from "./api/placeApi";
import HomePage from "./pages/HomePage";
import DetailPage from "./pages/DetailPage";
import RankingPage from "./pages/RankingPage";
import MyPage from "./pages/MyPage";
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
    setLoading(true);
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
    setPlaces((prev) => applyWishlist(prev, wishedIds));
  }, [applyWishlist, wishedIds]);

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
    setWishedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

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
          element={<DetailPage wishedIds={wishedIds} onToggleWish={toggleWish} />}
        />
        <Route
          path="/ranking"
          element={<RankingPage places={places} onToggleWish={toggleWish} />}
        />
        <Route
          path="/wishlist"
          element={<WishlistPage places={places} onToggleWish={toggleWish} />}
        />
        <Route path="/my" element={<MyPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
