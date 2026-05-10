import { NavLink } from "react-router-dom";

const navItems = [
  { to: "/", label: "홈", icon: "⌂" },
  { to: "/ranking", label: "랭킹", icon: "🏆" },
  { to: "/wishlist", label: "저장", icon: "▱" },
  { to: "/community", label: "커뮤니티", icon: "⋯" },
  { to: "/my", label: "마이", icon: "👤" },
];

export default function BottomNav() {
  return (
    <nav
      aria-label="하단 내비게이션"
      className="fixed bottom-0 left-0 z-50 h-[calc(76px+env(safe-area-inset-bottom))] w-full border-t border-gray-200 bg-white px-2 pb-[env(safe-area-inset-bottom)] shadow-[0_-4px_16px_rgba(0,0,0,0.04)]"
    >
      <div className="grid h-full grid-cols-5">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/"}
            className={({ isActive }) =>
              `flex min-w-0 flex-col items-center justify-center gap-1 text-[11px] font-semibold transition ${
                isActive ? "text-[#d99a00]" : "text-[#3b3222]"
              }`
            }
          >
            <span className="flex h-7 w-7 items-center justify-center rounded-full bg-[#fff7dc] text-base leading-none">
              {item.icon}
            </span>
            <span className="max-w-full truncate">{item.label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
