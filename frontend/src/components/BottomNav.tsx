import { NavLink } from "react-router-dom";
import {
  HomeIcon,
  MessageIcon,
  StarIcon,
  TrophyIcon,
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
      className="fixed bottom-0 left-0 z-50 h-[calc(72px+env(safe-area-inset-bottom))] w-full border-t border-m3-outline-variant bg-m3-surface-container-low/95 px-2 pb-[env(safe-area-inset-bottom)] shadow-m3-2 backdrop-blur-md"
    >
      <div className="grid h-full grid-cols-5">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === "/"}
            className={({ isActive }) =>
              `group relative flex min-w-0 flex-col items-center justify-center gap-1 text-m3-label-md transition-all duration-200 ${
                isActive
                  ? "text-m3-on-secondary-container"
                  : "text-m3-on-surface-variant hover:text-m3-on-surface"
              }`
            }
          >
            {({ isActive }) => (
              <>
                <span
                  className={`flex h-8 min-w-14 items-center justify-center rounded-m3-full transition-colors duration-200 ${
                    isActive
                      ? "bg-m3-secondary-container"
                      : "group-hover:bg-m3-surface-container-high"
                  }`}
                  aria-hidden="true"
                >
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
