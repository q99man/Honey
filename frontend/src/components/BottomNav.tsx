import { Link, useLocation } from "react-router-dom";

export default function BottomNav() {
  const location = useLocation();

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-50 h-[80px] border-t bg-white">
      <div className="mx-auto flex h-full max-w-[430px] items-center justify-around">
        <NavItem to="/" icon="⌂" label="홈" active={location.pathname === "/"} />
        <NavItem
          to="/ranking"
          icon="★"
          label="랭킹"
          active={location.pathname === "/ranking"}
        />
        <NavItem
          to="/wishlist"
          icon="♡"
          label="찜"
          active={location.pathname === "/wishlist"}
        />
        <NavItem
          to="/my"
          icon="☰"
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
      <span className={active ? "text-lg text-black" : "text-lg text-gray-400"}>
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
