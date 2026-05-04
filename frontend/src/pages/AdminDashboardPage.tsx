import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getAdminDashboard, type AdminDashboard } from "../api/adminApi";
import { getApiErrorMessage } from "../api/http";

const LOAD_ERROR = "관리자 대시보드를 불러오지 못했습니다.";

export default function AdminDashboardPage() {
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    Promise.resolve()
      .then(() => getAdminDashboard())
      .then((nextDashboard) => {
        if (!mounted) {
          return;
        }
        setDashboard(nextDashboard);
        setMessage(null);
      })
      .catch((error) => {
        if (mounted) {
          setMessage(getApiErrorMessage(error, LOAD_ERROR));
        }
      })
      .finally(() => {
        if (mounted) {
          setLoading(false);
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  const cards = useMemo(() => {
    if (!dashboard) {
      return [];
    }
    return [
      {
        label: "오늘 신규 회원",
        value: dashboard.todayNewUsers,
        tone: "bg-yellow-100 text-[#3b2f2f]",
      },
      {
        label: "오늘 추천",
        value: dashboard.todayRecommendations,
        tone: "bg-emerald-50 text-[#2f6f5f]",
      },
      {
        label: "오늘 방문 인증",
        value: dashboard.todayVisits,
        tone: "bg-sky-50 text-sky-700",
      },
      {
        label: "대기 신고",
        value: dashboard.pendingReports,
        tone: "bg-red-50 text-red-600",
      },
      {
        label: "오늘 신규 장소",
        value: dashboard.newPlaces,
        tone: "bg-orange-50 text-orange-700",
      },
    ];
  }, [dashboard]);

  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-10">
      <main className="mx-auto max-w-[960px] px-5 py-8">
        <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-bold text-[#2f6f5f]">관리자</p>
            <h1 className="mt-1 text-2xl font-bold">운영 대시보드</h1>
            <p className="mt-2 text-sm text-gray-500">
              오늘의 참여 흐름과 운영 대기 항목을 확인합니다.
            </p>
          </div>
          <nav className="flex flex-wrap gap-2">
            <AdminLink to="/admin/activities" label="활동 관리" />
            <AdminLink to="/admin/places" label="장소 관리" />
            <AdminLink to="/admin/users" label="사용자 관리" />
            <AdminLink to="/admin/audit-logs" label="감사 로그" />
            <AdminLink to="/admin/policies" label="정책 관리" primary />
            <AdminLink to="/admin/reports" label="신고 관리" warning />
            <AdminLink to="/" label="사용자 홈" />
          </nav>
        </header>

        {loading && (
          <section className="mt-6 rounded-lg bg-white p-5 text-sm text-gray-500 shadow-sm">
            대시보드를 불러오는 중입니다.
          </section>
        )}

        {!loading && message && (
          <section className="mt-6 rounded-lg bg-white p-5 text-sm font-semibold text-red-500 shadow-sm">
            {message}
          </section>
        )}

        {!loading && dashboard && (
          <>
            <section className="mt-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
              {cards.map((card) => (
                <article
                  key={card.label}
                  className={`rounded-lg p-4 shadow-sm ${card.tone}`}
                >
                  <p className="text-sm font-semibold">{card.label}</p>
                  <p className="mt-3 text-3xl font-bold">
                    {card.value.toLocaleString("ko-KR")}
                  </p>
                </article>
              ))}
            </section>

            <section className="mt-5 rounded-lg bg-white p-5 shadow-sm">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <h2 className="text-lg font-bold">운영 우선순위</h2>
                  <p className="mt-1 text-sm text-gray-500">
                    신고 대기 건수와 오늘 참여량을 기준으로 점검 대상을 고릅니다.
                  </p>
                </div>
                <Link
                  to="/admin/audit-logs"
                  className="h-10 rounded-lg bg-[#2f6f5f] px-4 pt-2 text-center text-sm font-bold text-white"
                >
                  감사 로그 보기
                </Link>
              </div>

              <div className="mt-5 grid gap-3 sm:grid-cols-3">
                <PriorityItem
                  label="신고 검토"
                  value={
                    dashboard.pendingReports > 0
                      ? `${dashboard.pendingReports}건 대기`
                      : "대기 없음"
                  }
                  active={dashboard.pendingReports > 0}
                />
                <PriorityItem
                  label="신규 장소 확인"
                  value={`${dashboard.newPlaces}건 등록`}
                  active={dashboard.newPlaces > 0}
                />
                <PriorityItem
                  label="참여 흐름"
                  value={`${dashboard.todayRecommendations + dashboard.todayVisits}건`}
                  active={
                    dashboard.todayRecommendations + dashboard.todayVisits > 0
                  }
                />
              </div>
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <Link
                  to="/admin/reports"
                  className="h-10 rounded-lg bg-yellow-400 px-4 pt-2 text-center text-sm font-bold text-black"
                >
                  신고 관리 열기
                </Link>
                <Link
                  to="/admin/activities"
                  className="h-10 rounded-lg border border-emerald-100 px-4 pt-2 text-center text-sm font-bold text-[#2f6f5f]"
                >
                  활동 관리 열기
                </Link>
              </div>
            </section>
          </>
        )}
      </main>
    </div>
  );
}

function AdminLink({
  to,
  label,
  primary = false,
  warning = false,
}: {
  to: string;
  label: string;
  primary?: boolean;
  warning?: boolean;
}) {
  let className =
    "h-10 rounded-lg border px-4 pt-2 text-center text-sm font-bold";
  if (primary) {
    className += " border-[#2f6f5f] bg-[#2f6f5f] text-white";
  } else if (warning) {
    className += " border-yellow-300 bg-yellow-400 text-black";
  } else {
    className += " border-emerald-100 bg-white text-[#2f6f5f]";
  }

  return (
    <Link to={to} className={className}>
      {label}
    </Link>
  );
}

function PriorityItem({
  label,
  value,
  active,
}: {
  label: string;
  value: string;
  active: boolean;
}) {
  return (
    <div className="rounded-lg bg-[#FFFBEB] p-4">
      <p className="text-sm font-bold">{label}</p>
      <p
        className={`mt-2 text-sm font-semibold ${
          active ? "text-[#2f6f5f]" : "text-gray-400"
        }`}
      >
        {value}
      </p>
    </div>
  );
}
