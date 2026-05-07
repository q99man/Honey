import { NavLink } from "react-router-dom";

const navItems = [
  { to: "/", label: "홈", shortLabel: "홈" },
  { to: "/ranking", label: "랭킹", shortLabel: "순" },
  { to: "/wishlist", label: "찜", shortLabel: "찜" },
  { to: "/my", label: "마이", shortLabel: "나" },
];

export default function BottomNav() {
  return (
    <nav
      aria-label="하단 내비게이션"
      className="fixed bottom-0 left-1/2 z-50 h-20 w-full max-w-[430px] -translate-x-1/2 border-t border-gray-200 bg-white px-4 shadow-[0_-4px_16px_rgba(0,0,0,0.04)]"
    >
      <div className="grid h-full grid-cols-4">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/"}
            className={({ isActive }) =>
              `flex flex-col items-center justify-center gap-1 text-xs font-semibold transition ${
                isActive ? "text-[#d99a00]" : "text-gray-400"
              }`
            }
          >
            <span className="flex h-7 w-7 items-center justify-center rounded-full bg-[#fff7dc] text-xs">
              {item.shortLabel}
            </span>
            <span>{item.label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
