import { NavLink } from "react-router-dom";

const navItems = [
  { to: "/", label: "발견", icon: "📍" },
  { to: "/ranking", label: "랭킹", icon: "🏆" },
  { to: "/places/new", label: "등록", icon: "➕" },
  { to: "/wishlist", label: "저장", icon: "⭐" },
  { to: "/my", label: "마이", icon: "👤" },
];

export default function BottomNav() {
  return (
    <nav
      aria-label="하단 내비게이션"
      className="fixed bottom-0 left-1/2 z-50 h-[76px] w-full max-w-[430px] -translate-x-1/2 border-t border-gray-200 bg-white px-2 shadow-[0_-4px_16px_rgba(0,0,0,0.04)]"
    >
      <div className="grid h-full grid-cols-5">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/"}
            className={({ isActive }) =>
              `flex flex-col items-center justify-center gap-1 text-[11px] font-semibold transition ${
                isActive ? "text-[#d99a00]" : "text-gray-400"
              }`
            }
          >
            <span className="flex h-7 w-7 items-center justify-center rounded-full bg-[#fff7dc] text-base leading-none">
              {item.icon}
            </span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
