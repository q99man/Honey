import { useEffect, useState } from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import HomePage from "./pages/HomePage";
import DetailPage from "./pages/DetailPage";
import RankingPage from "./pages/RankingPage";
import MyPage from "./pages/MyPage";
import WishlistPage from "./pages/WishlistPage";
import { mockSpaces } from "./data/mockSpaces";
import type { Place } from "./types/place";

function App() {
  const [spaces, setSpaces] = useState<Place[]>(() => {
    const saved = localStorage.getItem("honeytong-spaces");

    if (saved) {
      return JSON.parse(saved) as Place[];
    }

    return mockSpaces;
  });

  useEffect(() => {
    localStorage.setItem("honeytong-spaces", JSON.stringify(spaces));
  }, [spaces]);

  const toggleWish = (id: number) => {
    setSpaces((prev) =>
      prev.map((space) =>
        space.id === id
          ? { ...space, isWished: !space.isWished }
          : space
      )
    );
  };

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={<HomePage spaces={spaces} onToggleWish={toggleWish} />}
        />
        <Route path="/spaces/:id" element={<DetailPage spaces={spaces} />} />
        <Route path="/ranking" element={<RankingPage />} />
        <Route path="/wishlist" element={<WishlistPage spaces={spaces} />} />
        <Route path="/my" element={<MyPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
