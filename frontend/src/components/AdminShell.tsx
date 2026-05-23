import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { useTranslation } from "../hooks/useTranslation";
import { type LocaleType } from "../context/LocaleContext";

type AdminNavItem = {
  to: string;
  labelKey: string;
  emphasis?: "primary" | "warning" | "danger";
};

const DEFAULT_ADMIN_NAV_ITEMS: AdminNavItem[] = [
  { to: "/admin", labelKey: "dashboard" },
  { to: "/admin/activities", labelKey: "activities" },
  { to: "/admin/places", labelKey: "places" },
  { to: "/admin/users", labelKey: "users" },
  { to: "/admin/audit-logs", labelKey: "auditLogs" },
  { to: "/admin/policies", labelKey: "policies", emphasis: "primary" },
  { to: "/admin/reports", labelKey: "reports", emphasis: "warning" },
];

const navItemTranslations: Record<LocaleType, Record<string, string>> = {
  ko: {
    dashboard: "대시보드",
    activities: "활동 관리",
    places: "장소 관리",
    users: "사용자 관리",
    auditLogs: "감사 로그",
    policies: "정책 관리",
    reports: "신고 관리",
    adminLabel: "관리자",
    home: "사용자 홈"
  },
  en: {
    dashboard: "Dashboard",
    activities: "Activities",
    places: "Places",
    users: "Users",
    auditLogs: "Audit Logs",
    policies: "Policies",
    reports: "Reports",
    adminLabel: "Admin",
    home: "User Home"
  },
  ja: {
    dashboard: "ダッシュボード",
    activities: "活動管理",
    places: "場所管理",
    users: "ユーザー管理",
    auditLogs: "監査ログ",
    policies: "ポリシー管理",
    reports: "報告管理",
    adminLabel: "管理者",
    home: "ユーザーホーム"
  }
};

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
  const { locale, changeLanguage } = useTranslation();
  const translations = navItemTranslations[locale] || navItemTranslations.ko;

  return (
    <header className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
      <div className="min-w-0">
        <div className="flex items-center gap-3">
          <p className="text-m3-label-md text-m3-primary">{translations.adminLabel}</p>
          <select
            value={locale}
            onChange={(e) => changeLanguage(e.target.value as LocaleType)}
            className="h-7 rounded-m3-full border border-m3-outline-variant bg-m3-surface-container-lowest px-2 py-0 text-m3-label-sm text-m3-on-surface-variant outline-none transition focus:border-m3-primary"
          >
            <option value="ko">한국어</option>
            <option value="en">English</option>
            <option value="ja">日本語</option>
          </select>
        </div>
        <h1 className="mt-1 text-m3-title-lg text-m3-on-surface">{title}</h1>
        <p className="mt-2 max-w-2xl text-m3-body-md text-m3-on-surface-variant">
          {description}
        </p>
      </div>
      <nav className="flex flex-wrap gap-2" aria-label="관리자 메뉴">
        {navItems.map((item) => (
          <AdminNavLink key={item.to} {...item} label={translations[item.labelKey] || item.labelKey} />
        ))}
      </nav>
    </header>
  );
}

function AdminNavLink({ to, label, emphasis }: AdminNavItem & { label: string }) {
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

