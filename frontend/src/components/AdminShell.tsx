import type { ReactNode } from "react";
import { Link } from "react-router-dom";

type AdminNavItem = {
  to: string;
  label: string;
  emphasis?: "primary" | "warning" | "danger";
};

const DEFAULT_ADMIN_NAV_ITEMS: AdminNavItem[] = [
  { to: "/admin", label: "대시보드" },
  { to: "/admin/activities", label: "활동 관리" },
  { to: "/admin/places", label: "장소 관리" },
  { to: "/admin/users", label: "사용자 관리" },
  { to: "/admin/audit-logs", label: "감사 로그" },
  { to: "/admin/policies", label: "정책 관리", emphasis: "primary" },
  { to: "/admin/reports", label: "신고 관리", emphasis: "warning" },
];

export function AdminPageShell({
  title,
  description,
  maxWidth = "max-w-[1120px]",
  children,
  navItems = DEFAULT_ADMIN_NAV_ITEMS,
}: {
  title: string;
  description: string;
  maxWidth?: string;
  children: ReactNode;
  navItems?: AdminNavItem[];
}) {
  return (
    <div className="min-h-screen bg-m3-surface pb-10 text-m3-on-surface">
      <main className={`mx-auto ${maxWidth} px-4 py-6 sm:px-5 sm:py-8`}>
        <AdminHeader
          title={title}
          description={description}
          navItems={navItems}
        />
        {children}
      </main>
    </div>
  );
}

function AdminHeader({
  title,
  description,
  navItems,
}: {
  title: string;
  description: string;
  navItems: AdminNavItem[];
}) {
  return (
    <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
      <div className="min-w-0">
        <p className="text-m3-label-md text-m3-primary">관리자</p>
        <h1 className="mt-1 text-m3-title-lg text-m3-on-surface">{title}</h1>
        <p className="mt-2 max-w-2xl text-m3-body-md text-m3-on-surface-variant">
          {description}
        </p>
      </div>
      <nav className="flex flex-wrap gap-2" aria-label="관리자 메뉴">
        {navItems.map((item) => (
          <AdminNavLink key={item.to} {...item} />
        ))}
      </nav>
    </header>
  );
}

function AdminNavLink({ to, label, emphasis }: AdminNavItem) {
  const className =
    "inline-flex h-10 items-center justify-center rounded-m3-full border px-4 text-m3-label-lg transition active:scale-[0.98] " +
    (emphasis === "primary"
      ? "border-m3-primary bg-m3-primary text-m3-on-primary shadow-m3-1"
      : emphasis === "warning"
        ? "border-transparent bg-[#f6b800] text-[#2b210f] shadow-m3-1"
        : emphasis === "danger"
          ? "border-red-100 bg-red-50 text-red-600"
          : "border-m3-outline-variant bg-m3-surface-container-lowest text-m3-on-surface-variant");

  return (
    <Link to={to} className={className}>
      {label}
    </Link>
  );
}
