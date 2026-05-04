import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  getAdminActionLogs,
  getAdminUserActionLogs,
  type AdminActionLog,
  type AdminUserActionLog,
} from "../api/adminApi";
import { getApiErrorMessage } from "../api/http";

type AuditTab = "admin" | "user";
const FILTER_ALL = "ALL";
const LOAD_ERROR = "감사 로그를 불러오지 못했습니다.";

export default function AdminAuditLogsPage() {
  const [tab, setTab] = useState<AuditTab>("admin");
  const [adminLogs, setAdminLogs] = useState<AdminActionLog[]>([]);
  const [userLogs, setUserLogs] = useState<AdminUserActionLog[]>([]);
  const [selectedAdminLogId, setSelectedAdminLogId] = useState<number | null>(
    null,
  );
  const [selectedUserLogId, setSelectedUserLogId] = useState<number | null>(
    null,
  );
  const [actionFilter, setActionFilter] = useState(FILTER_ALL);
  const [targetFilter, setTargetFilter] = useState(FILTER_ALL);
  const [keyword, setKeyword] = useState("");
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    Promise.all([getAdminActionLogs(), getAdminUserActionLogs()])
      .then(([nextAdminLogs, nextUserLogs]) => {
        if (!mounted) {
          return;
        }
        setAdminLogs(nextAdminLogs);
        setUserLogs(nextUserLogs);
        setSelectedAdminLogId(nextAdminLogs[0]?.logId ?? null);
        setSelectedUserLogId(nextUserLogs[0]?.logId ?? null);
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

  const activeAdminLogs = useMemo(
    () =>
      filterAdminLogs(adminLogs, {
        actionFilter,
        targetFilter,
        keyword,
      }),
    [actionFilter, adminLogs, keyword, targetFilter],
  );

  const activeUserLogs = useMemo(
    () =>
      filterUserLogs(userLogs, {
        actionFilter,
        targetFilter,
        keyword,
      }),
    [actionFilter, keyword, targetFilter, userLogs],
  );

  const actionOptions = useMemo(() => {
    const values = tab === "admin" ? adminLogs : userLogs;
    return uniqueOptions(values.map((log) => log.actionType));
  }, [adminLogs, tab, userLogs]);

  const targetOptions = useMemo(() => {
    const values =
      tab === "admin"
        ? adminLogs.map((log) => log.targetType)
        : userLogs.map((log) => log.targetType);
    return uniqueOptions(values.filter((value): value is string => Boolean(value)));
  }, [adminLogs, tab, userLogs]);

  const selectedAdminLog = useMemo(
    () =>
      adminLogs.find((log) => log.logId === selectedAdminLogId) ??
      activeAdminLogs[0] ??
      null,
    [activeAdminLogs, adminLogs, selectedAdminLogId],
  );

  const selectedUserLog = useMemo(
    () =>
      userLogs.find((log) => log.logId === selectedUserLogId) ??
      activeUserLogs[0] ??
      null,
    [activeUserLogs, selectedUserLogId, userLogs],
  );

  const activeCount =
    tab === "admin" ? activeAdminLogs.length : activeUserLogs.length;

  const handleTabChange = (nextTab: AuditTab) => {
    setTab(nextTab);
    setActionFilter(FILTER_ALL);
    setTargetFilter(FILTER_ALL);
    setKeyword("");
  };

  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-10">
      <main className="mx-auto max-w-[1120px] px-5 py-8">
        <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-bold text-[#2f6f5f]">관리자</p>
            <h1 className="mt-1 text-2xl font-bold">감사 로그</h1>
            <p className="mt-2 text-sm text-gray-500">
              관리자 조치와 사용자 참여 기록을 읽기 전용으로 확인합니다.
            </p>
          </div>
          <nav className="flex flex-wrap gap-2">
            <AdminLink to="/admin" label="대시보드" />
            <AdminLink to="/admin/activities" label="활동 관리" />
            <AdminLink to="/admin/places" label="장소 관리" />
            <AdminLink to="/admin/users" label="사용자 관리" />
            <AdminLink to="/admin/reports" label="신고 관리" danger />
            <AdminLink to="/admin/policies" label="정책 관리" />
          </nav>
        </header>

        <section className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
          <SummaryCell label="관리자 조치" value={adminLogs.length} />
          <SummaryCell label="사용자 활동" value={userLogs.length} />
          <SummaryCell
            label="관리 대상"
            value={uniqueOptions(adminLogs.map((log) => log.targetType)).length}
          />
          <SummaryCell
            label="참여 사용자"
            value={new Set(userLogs.map((log) => log.userId)).size}
          />
        </section>

        <section className="mt-5 grid grid-cols-2 gap-2">
          <button
            type="button"
            onClick={() => handleTabChange("admin")}
            className={`h-10 rounded-lg text-sm font-bold ${
              tab === "admin"
                ? "bg-yellow-400 text-black"
                : "bg-white text-gray-500"
            }`}
          >
            관리자 조치 로그
          </button>
          <button
            type="button"
            onClick={() => handleTabChange("user")}
            className={`h-10 rounded-lg text-sm font-bold ${
              tab === "user"
                ? "bg-yellow-400 text-black"
                : "bg-white text-gray-500"
            }`}
          >
            사용자 활동 로그
          </button>
        </section>

        {message && (
          <p className="mt-4 rounded-lg bg-white px-4 py-3 text-sm font-semibold text-red-500 shadow-sm">
            {message}
          </p>
        )}

        {loading && (
          <section className="mt-6 rounded-lg bg-white p-5 text-sm text-gray-500 shadow-sm">
            감사 로그를 불러오는 중입니다.
          </section>
        )}

        {!loading && (
          <div className="mt-5 grid gap-5 lg:grid-cols-[380px_1fr]">
            <section className="rounded-lg bg-white p-4 shadow-sm">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-bold">
                  {tab === "admin" ? "조치 로그" : "활동 로그"}
                </h2>
                <span className="text-xs font-semibold text-gray-400">
                  {activeCount.toLocaleString("ko-KR")}건
                </span>
              </div>

              <label className="mt-4 block text-sm font-semibold">
                검색
                <input
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="닉네임, 조치, 대상, 메모, ID"
                  className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                />
              </label>

              <div className="mt-3 grid gap-2 sm:grid-cols-2">
                <label className="text-sm font-semibold">
                  조치 유형
                  <select
                    value={actionFilter}
                    onChange={(event) => setActionFilter(event.target.value)}
                    className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                  >
                    <option value={FILTER_ALL}>전체</option>
                    {actionOptions.map((value) => (
                      <option key={value} value={value}>
                        {actionTypeLabel(value)}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="text-sm font-semibold">
                  대상 유형
                  <select
                    value={targetFilter}
                    onChange={(event) => setTargetFilter(event.target.value)}
                    className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                  >
                    <option value={FILTER_ALL}>전체</option>
                    {targetOptions.map((value) => (
                      <option key={value} value={value}>
                        {targetTypeLabel(value)}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              {activeCount === 0 && (
                <p className="mt-5 rounded-lg bg-[#FFFBEB] p-4 text-sm text-gray-500">
                  조건에 맞는 감사 로그가 없습니다.
                </p>
              )}

              <div className="mt-4 flex max-h-[720px] flex-col gap-3 overflow-y-auto pr-1">
                {tab === "admin" &&
                  activeAdminLogs.map((log) => (
                    <AdminLogListItem
                      key={log.logId}
                      item={log}
                      selected={log.logId === selectedAdminLog?.logId}
                      onSelect={() => setSelectedAdminLogId(log.logId)}
                    />
                  ))}

                {tab === "user" &&
                  activeUserLogs.map((log) => (
                    <UserLogListItem
                      key={log.logId}
                      item={log}
                      selected={log.logId === selectedUserLog?.logId}
                      onSelect={() => setSelectedUserLogId(log.logId)}
                    />
                  ))}
              </div>
            </section>

            <section className="rounded-lg bg-white p-5 shadow-sm">
              {tab === "admin" && <AdminLogDetail item={selectedAdminLog} />}
              {tab === "user" && <UserLogDetail item={selectedUserLog} />}
            </section>
          </div>
        )}
      </main>
    </div>
  );
}

function AdminLogListItem({
  item,
  selected,
  onSelect,
}: {
  item: AdminActionLog;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      className={`rounded-lg border p-3 text-left text-sm ${
        selected ? "border-yellow-400 bg-yellow-50" : "border-yellow-100 bg-[#FFFBEB]"
      }`}
    >
      <ListHeader
        title={actionTypeLabel(item.actionType)}
        badge={targetTypeLabel(item.targetType)}
      />
      <p className="mt-1 text-xs text-gray-500">
        {item.adminNickname} · 대상 #{item.targetId}
      </p>
      <p className="mt-2 text-xs text-gray-400">{formatDate(item.createdAt)}</p>
    </button>
  );
}

function UserLogListItem({
  item,
  selected,
  onSelect,
}: {
  item: AdminUserActionLog;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      className={`rounded-lg border p-3 text-left text-sm ${
        selected ? "border-yellow-400 bg-yellow-50" : "border-yellow-100 bg-[#FFFBEB]"
      }`}
    >
      <ListHeader
        title={actionTypeLabel(item.actionType)}
        badge={targetTypeLabel(item.targetType)}
      />
      <p className="mt-1 text-xs text-gray-500">
        {item.nickname} · 대상 {item.targetId === null ? "없음" : `#${item.targetId}`}
      </p>
      <p className="mt-2 text-xs text-gray-400">{formatDate(item.createdAt)}</p>
    </button>
  );
}

function ListHeader({ title, badge }: { title: string; badge: string }) {
  return (
    <div className="flex items-start justify-between gap-2">
      <p className="font-bold">{title}</p>
      <span className="rounded-full bg-white px-2 py-1 text-xs font-bold text-[#2f6f5f]">
        {badge}
      </span>
    </div>
  );
}

function AdminLogDetail({ item }: { item: AdminActionLog | null }) {
  if (!item) {
    return <EmptyDetail label="관리자 조치 로그를 선택해 주세요." />;
  }

  return (
    <div>
      <DetailHeader
        title={`관리자 조치 #${item.logId}`}
        subtitle={`${item.adminNickname} · ${actionTypeLabel(item.actionType)}`}
        badge={targetTypeLabel(item.targetType)}
      />
      <dl className="mt-5 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3">
        <InfoItem label="관리자" value={`${item.adminNickname} (#${item.adminUserId})`} />
        <InfoItem label="조치 유형" value={item.actionType} />
        <InfoItem label="대상" value={`${item.targetType} #${item.targetId}`} />
        <InfoItem label="기록 시각" value={formatDate(item.createdAt)} />
        <InfoItem label="메모" value={item.memo ?? "기록 없음"} wide />
      </dl>
      <div className="mt-5 grid gap-4 lg:grid-cols-2">
        <JsonBlock title="변경 전" value={item.beforeValue} />
        <JsonBlock title="변경 후" value={item.afterValue} />
      </div>
    </div>
  );
}

function UserLogDetail({ item }: { item: AdminUserActionLog | null }) {
  if (!item) {
    return <EmptyDetail label="사용자 활동 로그를 선택해 주세요." />;
  }

  return (
    <div>
      <DetailHeader
        title={`사용자 활동 #${item.logId}`}
        subtitle={`${item.nickname} · ${actionTypeLabel(item.actionType)}`}
        badge={targetTypeLabel(item.targetType)}
      />
      <dl className="mt-5 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3">
        <InfoItem label="사용자" value={`${item.nickname} (#${item.userId})`} />
        <InfoItem label="활동 유형" value={item.actionType} />
        <InfoItem
          label="대상"
          value={
            item.targetType && item.targetId
              ? `${item.targetType} #${item.targetId}`
              : "기록 없음"
          }
        />
        <InfoItem label="IP" value={item.ipAddress ?? "기록 없음"} />
        <InfoItem label="기록 시각" value={formatDate(item.createdAt)} />
        <InfoItem label="User-Agent" value={item.userAgent ?? "기록 없음"} wide />
      </dl>
      <div className="mt-5">
        <JsonBlock title="메타데이터" value={item.metadataJson} />
      </div>
    </div>
  );
}

function DetailHeader({
  title,
  subtitle,
  badge,
}: {
  title: string;
  subtitle: string;
  badge: string;
}) {
  return (
    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
      <div>
        <h2 className="text-xl font-bold">{title}</h2>
        <p className="mt-2 text-sm text-gray-500">{subtitle}</p>
      </div>
      <span className="w-fit rounded-full bg-yellow-50 px-3 py-1 text-xs font-bold text-[#2f6f5f]">
        {badge}
      </span>
    </div>
  );
}

function JsonBlock({ title, value }: { title: string; value: string | null }) {
  return (
    <div className="rounded-lg bg-[#FFFBEB] p-4">
      <p className="text-sm font-bold">{title}</p>
      <pre className="mt-3 max-h-72 overflow-auto whitespace-pre-wrap break-words text-xs leading-5 text-gray-600">
        {formatMaybeJson(value)}
      </pre>
    </div>
  );
}

function EmptyDetail({ label }: { label: string }) {
  return <p className="text-sm text-gray-500">{label}</p>;
}

function SummaryCell({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg bg-white p-4 text-center shadow-sm">
      <p className="text-2xl font-bold">{value.toLocaleString("ko-KR")}</p>
      <p className="mt-1 text-xs font-semibold text-gray-500">{label}</p>
    </div>
  );
}

function InfoItem({
  label,
  value,
  wide = false,
}: {
  label: string;
  value: string;
  wide?: boolean;
}) {
  return (
    <div className={`rounded-lg bg-[#FFFBEB] p-3 ${wide ? "sm:col-span-2 lg:col-span-3" : ""}`}>
      <dt className="text-xs text-gray-500">{label}</dt>
      <dd className="mt-1 break-words font-semibold">{value}</dd>
    </div>
  );
}

function AdminLink({
  to,
  label,
  danger = false,
}: {
  to: string;
  label: string;
  danger?: boolean;
}) {
  return (
    <Link
      to={to}
      className={`h-10 rounded-lg border px-4 pt-2 text-center text-sm font-bold ${
        danger
          ? "border-red-100 text-red-500"
          : "border-emerald-100 text-[#2f6f5f]"
      }`}
    >
      {label}
    </Link>
  );
}

function filterAdminLogs(
  logs: AdminActionLog[],
  filters: {
    actionFilter: string;
    targetFilter: string;
    keyword: string;
  },
) {
  return logs.filter((log) => {
    return (
      matchesExact(filters.actionFilter, log.actionType) &&
      matchesExact(filters.targetFilter, log.targetType) &&
      matchesKeyword(
        filters.keyword,
        log.adminNickname,
        log.actionType,
        log.targetType,
        String(log.adminUserId),
        String(log.targetId),
        log.memo ?? "",
        log.beforeValue ?? "",
        log.afterValue ?? "",
      )
    );
  });
}

function filterUserLogs(
  logs: AdminUserActionLog[],
  filters: {
    actionFilter: string;
    targetFilter: string;
    keyword: string;
  },
) {
  return logs.filter((log) => {
    return (
      matchesExact(filters.actionFilter, log.actionType) &&
      matchesExact(filters.targetFilter, log.targetType) &&
      matchesKeyword(
        filters.keyword,
        log.nickname,
        log.actionType,
        log.targetType ?? "",
        String(log.userId),
        log.targetId === null ? "" : String(log.targetId),
        log.ipAddress ?? "",
        log.userAgent ?? "",
        log.metadataJson ?? "",
      )
    );
  });
}

function matchesExact(filter: string, value: string | null) {
  return filter === FILTER_ALL || value === filter;
}

function matchesKeyword(keyword: string, ...values: string[]) {
  const normalizedKeyword = keyword.trim().toLowerCase();
  if (!normalizedKeyword) {
    return true;
  }
  return values.join(" ").toLowerCase().includes(normalizedKeyword);
}

function uniqueOptions(values: string[]) {
  return Array.from(new Set(values)).sort((a, b) => a.localeCompare(b));
}

function actionTypeLabel(value: string) {
  const labels: Record<string, string> = {
    COMMENT_BLIND: "댓글 블라인드",
    COMMENT_CREATE: "댓글 작성",
    COMMENT_DELETE: "댓글 삭제",
    PLACE_APPROVAL_UPDATE: "장소 승인 변경",
    PLACE_CREATE: "장소 등록",
    PLACE_DELETE: "장소 삭제",
    PLACE_EXPOSURE_UPDATE: "장소 노출 변경",
    PLACE_FRANCHISE_REVIEW_UPDATE: "프랜차이즈 검토 변경",
    PLACE_RANKING_EXCLUSION_UPDATE: "랭킹 제외 변경",
    PLACE_SCORE_ADJUST: "장소 점수 조정",
    PLACE_UPDATE: "장소 수정",
    RANKING_HISTORY_FINALIZE: "랭킹 이력 확정",
    RANKING_RECALCULATE: "랭킹 재계산",
    RECOMMENDATION_CANCEL: "추천 취소",
    RECOMMENDATION_CREATE: "추천 등록",
    RECOMMENDATION_INVALIDATE: "추천 무효화",
    REPORT_CREATE: "신고 등록",
    REPORT_FOLLOW_UP: "신고 후속 조치",
    REPORT_PROCESS: "신고 처리",
    USER_RECOMMEND_WEIGHT_ADJUST: "추천 가중치 조정",
    USER_SANCTION: "사용자 제재",
    USER_TRUST_ADJUST: "신뢰도 조정",
    VISIT_INVALIDATE: "방문 무효화",
    VISIT_VERIFY: "방문 인증",
  };
  return labels[value] ?? value;
}

function targetTypeLabel(value: string | null) {
  if (!value) {
    return "대상 없음";
  }
  const labels: Record<string, string> = {
    COMMENT: "댓글",
    PLACE: "장소",
    POLICY: "정책",
    RANKING: "랭킹",
    RECOMMENDATION: "추천",
    REPORT: "신고",
    SEASON: "시즌",
    USER: "사용자",
    VISIT: "방문",
  };
  return labels[value] ?? value;
}

function formatMaybeJson(value: string | null) {
  if (!value) {
    return "기록 없음";
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}
