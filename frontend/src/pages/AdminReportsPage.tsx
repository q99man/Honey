import { useEffect, useMemo, useState } from "react";
import { getApiErrorMessage } from "../api/http";
import {
  applyAdminReportFollowUpAction,
  getAdminReport,
  getAdminReports,
  processAdminReport,
  type AdminReport,
  type AdminReportFollowUpActionType,
  type ReportStatus,
  type ReportTargetType,
  type UserSanctionType,
} from "../api/reportApi";
import { AdminPageShell } from "../components/AdminShell";

type StatusFilter = ReportStatus | "ALL";

const STATUS_FILTERS: { value: StatusFilter; label: string }[] = [
  { value: "PENDING", label: "대기" },
  { value: "APPROVED", label: "승인" },
  { value: "REJECTED", label: "반려" },
  { value: "ALL", label: "전체" },
];

const SANCTION_TYPES: { value: UserSanctionType; label: string }[] = [
  { value: "WARNING", label: "경고" },
  { value: "TEMPORARY_RESTRICTION", label: "일시 제한" },
  { value: "PERMANENT_RESTRICTION", label: "영구 제한" },
];

const LOAD_ERROR = "신고 목록을 불러오지 못했습니다.";

export default function AdminReportsPage() {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("PENDING");
  const [reports, setReports] = useState<AdminReport[]>([]);
  const [selectedReportId, setSelectedReportId] = useState<number | null>(null);
  const [selectedReport, setSelectedReport] = useState<AdminReport | null>(null);
  const [reviewStatus, setReviewStatus] =
    useState<Exclude<ReportStatus, "PENDING">>("APPROVED");
  const [reviewNote, setReviewNote] = useState("");
  const [followUpAction, setFollowUpAction] =
    useState<AdminReportFollowUpActionType | null>(null);
  const [sanctionType, setSanctionType] =
    useState<UserSanctionType>("WARNING");
  const [sanctionReason, setSanctionReason] = useState("");
  const [startAt, setStartAt] = useState("");
  const [endAt, setEndAt] = useState("");
  const [memo, setMemo] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    Promise.resolve()
      .then(async () => {
        setLoading(true);
        return getAdminReports(statusFilter);
      })
      .then((nextReports) => {
        if (!mounted) {
          return;
        }
        setReports(nextReports);
        setSelectedReportId(
          (current) => current ?? nextReports[0]?.reportId ?? null,
        );
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
  }, [statusFilter]);

  useEffect(() => {
    if (!selectedReportId) {
      return;
    }

    let mounted = true;
    Promise.resolve()
      .then(() => getAdminReport(selectedReportId))
      .then((report) => {
        if (!mounted) {
          return;
        }
        setSelectedReport(report);
        setReviewNote(report.reviewNote ?? "");
        setReviewStatus(report.status === "REJECTED" ? "REJECTED" : "APPROVED");
        const actions = followUpActionsFor(report.targetType);
        setFollowUpAction(actions[0]?.value ?? null);
        setMemo("");
        setSanctionReason("");
        setStartAt("");
        setEndAt("");
      })
      .catch((error) => {
        if (mounted) {
          setMessage(
            getApiErrorMessage(error, "신고 상세를 불러오지 못했습니다."),
          );
        }
      });

    return () => {
      mounted = false;
    };
  }, [selectedReportId]);

  const counts = useMemo(() => countReports(reports), [reports]);
  const followUpActions = selectedReport
    ? followUpActionsFor(selectedReport.targetType)
    : [];

  const handleSelectFilter = (value: StatusFilter) => {
    setStatusFilter(value);
    setSelectedReportId(null);
    setSelectedReport(null);
  };

  const handleProcessReport = async () => {
    if (!selectedReport || selectedReport.status !== "PENDING") {
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      const processed = await processAdminReport(selectedReport.reportId, {
        status: reviewStatus,
        reviewNote: reviewNote.trim() || null,
      });
      setSelectedReport(processed);
      setReports((prev) =>
        prev.map((report) =>
          report.reportId === processed.reportId ? processed : report,
        ),
      );
      setMessage(
        reviewStatus === "APPROVED"
          ? "신고를 승인 처리했습니다."
          : "신고를 반려 처리했습니다.",
      );
    } catch (error) {
      setMessage(getApiErrorMessage(error, "신고 처리를 완료하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  const handleApplyFollowUp = async () => {
    if (!selectedReport || !followUpAction) {
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      const result = await applyAdminReportFollowUpAction(
        selectedReport.reportId,
        {
          actionType: followUpAction,
          sanctionType:
            followUpAction === "SANCTION_USER" ? sanctionType : null,
          reason:
            followUpAction === "SANCTION_USER"
              ? sanctionReason.trim() || null
              : null,
          startAt: followUpAction === "SANCTION_USER" ? startAt || null : null,
          endAt: followUpAction === "SANCTION_USER" ? endAt || null : null,
          memo: memo.trim() || null,
        },
      );
      const refreshed = await getAdminReport(selectedReport.reportId);
      setSelectedReport(refreshed);
      setReports((prev) =>
        prev.map((report) =>
          report.reportId === refreshed.reportId ? refreshed : report,
        ),
      );
      setMessage(
        result.applied
          ? "후속 조치를 적용했습니다."
          : "후속 조치 대상 상태를 확인했습니다.",
      );
    } catch (error) {
      setMessage(getApiErrorMessage(error, "후속 조치를 적용하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <AdminPageShell
      title="신고 관리"
      description="접수된 신고를 검토하고 승인된 신고에 필요한 후속 조치를 적용합니다."
      maxWidth="max-w-[920px]"
    >

        <section className="mt-6 grid grid-cols-2 gap-2 sm:grid-cols-4">
          {STATUS_FILTERS.map((filter) => (
            <button
              key={filter.value}
              type="button"
              onClick={() => handleSelectFilter(filter.value)}
              className={`admin-segment ${
                statusFilter === filter.value
                  ? "admin-segment-selected"
                  : "admin-segment-idle"
              }`}
            >
              {filter.label}
            </button>
          ))}
        </section>

        {message && (
          <p className="mt-4 rounded-m3-lg bg-m3-secondary-container px-4 py-3 text-m3-label-lg text-m3-on-secondary-container shadow-m3-1">
            {message}
          </p>
        )}

        <div className="mt-5 grid gap-5 lg:grid-cols-[340px_1fr]">
          <section className="admin-panel">
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-lg font-bold">신고 목록</h2>
              <span className="text-xs font-semibold text-gray-400">
                {loading ? "조회 중" : `${reports.length}건`}
              </span>
            </div>

            <div className="mt-3 grid grid-cols-3 gap-2 text-center text-xs">
              <SummaryCell label="대기" value={counts.pending} />
              <SummaryCell label="승인" value={counts.approved} />
              <SummaryCell label="반려" value={counts.rejected} />
            </div>

            {!loading && reports.length === 0 && (
              <p className="admin-muted-panel mt-5">
                표시할 신고가 없습니다.
              </p>
            )}

            <div className="mt-4 flex flex-col gap-3">
              {reports.map((report) => (
                <button
                  key={report.reportId}
                  type="button"
                  onClick={() => setSelectedReportId(report.reportId)}
                  className={`admin-list-item ${
                    selectedReportId === report.reportId
                      ? "admin-list-item-selected"
                      : "admin-list-item-idle"
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="font-bold">
                        {targetTypeLabel(report.targetType)} #{report.targetId}
                      </p>
                      <p className="mt-1 text-m3-body-sm text-m3-on-surface-variant">
                        {reasonLabel(report.reasonCode)}
                      </p>
                    </div>
                    <span className="rounded-m3-full bg-m3-surface-container-lowest px-2 py-1 text-m3-label-md text-m3-primary">
                      {statusLabel(report.status)}
                    </span>
                  </div>
                  <p className="mt-2 text-xs text-gray-400">
                    신고자 {report.reporterNickname} · {formatDate(report.createdAt)}
                  </p>
                </button>
              ))}
            </div>
          </section>

          <section className="admin-panel admin-panel-spacious">
            {!selectedReport && (
              <p className="text-m3-body-md text-m3-on-surface-variant">
                검토할 신고를 목록에서 선택해 주세요.
              </p>
            )}

            {selectedReport && (
              <div>
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <h2 className="text-xl font-bold">
                      신고 #{selectedReport.reportId}
                    </h2>
                    <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">
                      {targetTypeLabel(selectedReport.targetType)} #
                      {selectedReport.targetId} ·{" "}
                      {reasonLabel(selectedReport.reasonCode)}
                    </p>
                  </div>
                  <span className="w-fit rounded-m3-full bg-m3-secondary-container px-3 py-1 text-m3-label-md text-m3-on-secondary-container">
                    {statusLabel(selectedReport.status)}
                  </span>
                </div>

                <dl className="mt-5 grid gap-3 text-sm sm:grid-cols-2">
                  <InfoItem label="신고자" value={selectedReport.reporterNickname} />
                  <InfoItem
                    label="접수일"
                    value={formatDate(selectedReport.createdAt)}
                  />
                  <InfoItem
                    label="대상"
                    value={`${targetTypeLabel(selectedReport.targetType)} #${selectedReport.targetId}`}
                  />
                  <InfoItem
                    label="검토자"
                    value={selectedReport.reviewedByNickname ?? "미검토"}
                  />
                </dl>

                <div className="admin-muted-panel mt-5">
                  <p className="font-bold">신고 내용</p>
                  <p className="mt-2 text-m3-on-surface-variant">
                    {selectedReport.reasonText || "별도 설명이 없습니다."}
                  </p>
                </div>

                {selectedReport.status === "PENDING" && (
                  <section className="mt-5 border-t border-m3-outline-variant pt-5">
                    <h3 className="text-base font-bold">검토 처리</h3>
                    <div className="mt-3 grid grid-cols-2 gap-2">
                      {(["APPROVED", "REJECTED"] as const).map((status) => (
                        <button
                          key={status}
                          type="button"
                          onClick={() => setReviewStatus(status)}
                          className={`admin-segment ${
                            reviewStatus === status
                              ? "admin-segment-selected"
                              : "admin-segment-idle"
                          }`}
                        >
                          {statusLabel(status)}
                        </button>
                      ))}
                    </div>
                    <textarea
                      value={reviewNote}
                      onChange={(event) => setReviewNote(event.target.value)}
                      maxLength={255}
                      placeholder="검토 메모를 입력해 주세요."
                      className="admin-field mt-3 min-h-24 resize-none p-3"
                    />
                    <button
                      type="button"
                      onClick={handleProcessReport}
                      disabled={busy}
                      className="admin-action-warning mt-3 h-11 w-full"
                    >
                      {busy ? "처리 중" : "검토 저장"}
                    </button>
                  </section>
                )}

                {selectedReport.status !== "PENDING" && (
                  <section className="mt-5 border-t border-m3-outline-variant pt-5">
                    <h3 className="text-base font-bold">검토 결과</h3>
                    <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">
                      {selectedReport.reviewNote || "검토 메모가 없습니다."}
                    </p>
                  </section>
                )}

                {selectedReport.status === "APPROVED" && (
                  <section className="mt-5 border-t border-m3-outline-variant pt-5">
                    <h3 className="text-base font-bold">후속 조치</h3>
                    <div className="mt-3 grid gap-2 sm:grid-cols-2">
                      {followUpActions.map((action) => (
                        <button
                          key={action.value}
                          type="button"
                          onClick={() => setFollowUpAction(action.value)}
                          className={`admin-segment ${
                            followUpAction === action.value
                              ? "bg-m3-error text-m3-on-error shadow-m3-1"
                              : "bg-red-50 text-m3-error"
                          }`}
                        >
                          {action.label}
                        </button>
                      ))}
                    </div>

                    {followUpAction === "SANCTION_USER" && (
                      <div className="mt-3 grid gap-3 sm:grid-cols-2">
                        <label className="text-sm font-semibold">
                          제재 유형
                          <select
                            value={sanctionType}
                            onChange={(event) =>
                              setSanctionType(event.target.value as UserSanctionType)
                            }
                            className="admin-field mt-2 h-11"
                          >
                            {SANCTION_TYPES.map((type) => (
                              <option key={type.value} value={type.value}>
                                {type.label}
                              </option>
                            ))}
                          </select>
                        </label>
                        <label className="text-sm font-semibold">
                          제재 사유
                          <input
                            value={sanctionReason}
                            onChange={(event) =>
                              setSanctionReason(event.target.value)
                            }
                            className="admin-field mt-2 h-11"
                          />
                        </label>
                        <label className="text-sm font-semibold">
                          시작 시각
                          <input
                            value={startAt}
                            onChange={(event) => setStartAt(event.target.value)}
                            type="datetime-local"
                            className="admin-field mt-2 h-11"
                          />
                        </label>
                        <label className="text-sm font-semibold">
                          종료 시각
                          <input
                            value={endAt}
                            onChange={(event) => setEndAt(event.target.value)}
                            type="datetime-local"
                            className="admin-field mt-2 h-11"
                          />
                        </label>
                      </div>
                    )}

                    <textarea
                      value={memo}
                      onChange={(event) => setMemo(event.target.value)}
                      maxLength={255}
                      placeholder="후속 조치 메모를 입력해 주세요."
                      className="admin-field mt-3 min-h-20 resize-none p-3"
                    />
                    <button
                      type="button"
                      onClick={handleApplyFollowUp}
                      disabled={busy || !followUpAction}
                      className="admin-action-danger mt-3 h-11 w-full"
                    >
                      {busy ? "적용 중" : "후속 조치 적용"}
                    </button>
                  </section>
                )}
              </div>
            )}
          </section>
        </div>
    </AdminPageShell>
  );
}

function SummaryCell({ label, value }: { label: string; value: number }) {
  return (
    <div className="admin-info-cell">
      <p className="text-base font-bold">{value}</p>
      <p className="mt-1 text-m3-label-md text-m3-on-surface-variant">{label}</p>
    </div>
  );
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="admin-info-cell">
      <dt className="text-m3-label-md text-m3-on-surface-variant">{label}</dt>
      <dd className="mt-1 font-semibold">{value}</dd>
    </div>
  );
}

function countReports(reports: AdminReport[]) {
  return reports.reduce(
    (acc, report) => {
      if (report.status === "PENDING") {
        acc.pending += 1;
      }
      if (report.status === "APPROVED") {
        acc.approved += 1;
      }
      if (report.status === "REJECTED") {
        acc.rejected += 1;
      }
      return acc;
    },
    { pending: 0, approved: 0, rejected: 0 },
  );
}

function followUpActionsFor(targetType: ReportTargetType) {
  switch (targetType) {
    case "PLACE":
      return [
        { value: "HIDE_PLACE" as const, label: "장소 숨김" },
        { value: "DELETE_PLACE" as const, label: "장소 삭제" },
      ];
    case "COMMENT":
      return [
        { value: "BLIND_COMMENT" as const, label: "댓글 블라인드" },
        { value: "DELETE_COMMENT" as const, label: "댓글 삭제" },
      ];
    case "USER":
      return [{ value: "SANCTION_USER" as const, label: "사용자 제재" }];
  }
}

function targetTypeLabel(value: ReportTargetType) {
  switch (value) {
    case "PLACE":
      return "장소";
    case "COMMENT":
      return "댓글";
    case "USER":
      return "사용자";
  }
}

function statusLabel(value: ReportStatus) {
  switch (value) {
    case "PENDING":
      return "대기";
    case "APPROVED":
      return "승인";
    case "REJECTED":
      return "반려";
  }
}

function reasonLabel(value: string) {
  switch (value) {
    case "FAKE_INFO":
      return "잘못된 정보";
    case "FRANCHISE":
      return "프랜차이즈 의심";
    case "SPAM":
      return "스팸/홍보";
    case "ABUSE":
      return "부적절한 내용";
    case "OTHER":
      return "기타";
    default:
      return value;
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}
