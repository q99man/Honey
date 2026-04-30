import { Link, useLocation } from "react-router-dom";

export default function BottomNav() {
  const location = useLocation();

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 h-[80px] border-t bg-white">
      <div className="mx-auto flex h-full max-w-[430px] items-center justify-around">
        <NavItem to="/" icon="홈" label="홈" active={location.pathname === "/"} />
        <NavItem
          to="/ranking"
          icon="순위"
          label="랭킹"
          active={location.pathname === "/ranking"}
        />
        <NavItem
          to="/wishlist"
          icon="찜"
          label="찜"
          active={location.pathname === "/wishlist"}
        />
        <NavItem
          to="/my"
          icon="나"
          label="마이"
          active={location.pathname === "/my"}
        />
      </div>
    </nav>
  );
}

type NavItemProps = {
  to: string;
  icon: string;
  label: string;
  active: boolean;
};

function NavItem({ to, icon, label, active }: NavItemProps) {
  return (
    <Link to={to} className="flex flex-col items-center gap-1">
      <span className={active ? "text-sm text-black" : "text-sm text-gray-400"}>
        {icon}
      </span>
      <span
        className={`text-xs ${
          active ? "font-semibold text-black" : "text-gray-400"
        }`}
      >
        {label}
      </span>
    </Link>
  );
}
