import { NavLink } from "react-router-dom";
import {
  HomeIcon,
  TrophyIcon,
  StarIcon,
  MessageIcon,
  UserIcon,
} from "./NavIcons";

const navItems = [
  { to: "/", label: "홈", icon: <HomeIcon /> },
  { to: "/ranking", label: "랭킹", icon: <TrophyIcon /> },
  { to: "/wishlist", label: "저장", icon: <StarIcon /> },
  { to: "/community", label: "커뮤니티", icon: <MessageIcon /> },
  { to: "/my", label: "마이", icon: <UserIcon /> },
];

export default function BottomNav() {
  return (
    <nav
      aria-label="하단 내비게이션"
      className="fixed bottom-0 left-0 z-50 h-[calc(70px+env(safe-area-inset-bottom))] w-full border-t border-gray-100 bg-white/95 px-2 pb-[env(safe-area-inset-bottom)] backdrop-blur-md shadow-[0_-8px_24px_rgba(43,33,15,0.04)]"
    >
      <div className="grid h-full grid-cols-5">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/"}
            className={({ isActive }) =>
              `group relative flex min-w-0 flex-col items-center justify-center gap-1.5 text-[10px] font-bold transition-all duration-300 ${
                isActive ? "text-[#f6b800]" : "text-gray-400 hover:text-gray-600"
              }`
            }
          >
            {({ isActive }) => (
              <>
                {isActive && (
                  <span className="absolute -top-px left-1/2 h-1 w-8 -translate-x-1/2 rounded-b-full bg-[#f6b800] transition-all" />
                )}
                <span className={`transition-transform duration-300 ${isActive ? "-translate-y-0.5 scale-110" : "scale-100"}`}>
                  {item.icon}
                </span>
                <span className="max-w-full truncate">{item.label}</span>
              </>
            )}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
