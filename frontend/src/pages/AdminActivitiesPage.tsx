import { useEffect, useMemo, useState } from "react";
import {
  blindAdminComment,
  deleteAdminComment,
  getAdminComments,
  getAdminRecommendations,
  getAdminVisits,
  invalidateAdminRecommendation,
  invalidateAdminVisit,
  type AdminComment,
  type AdminCommentStatus,
  type AdminRecommendation,
  type AdminRecommendationStatus,
  type AdminVisit,
} from "../api/adminApi";
import { getApiErrorMessage } from "../api/http";
import { AdminPageShell } from "../components/AdminShell";

type ActivityTab = "recommendations" | "visits" | "comments";
type ActivityStatusFilter = "ALL" | "ACTIVE_ONLY" | "INACTIVE_ONLY";

const LOAD_ERROR = "활동 기록을 불러오지 못했습니다.";

const TABS: { value: ActivityTab; label: string }[] = [
  { value: "recommendations", label: "추천" },
  { value: "visits", label: "방문" },
  { value: "comments", label: "댓글" },
];

const STATUS_FILTERS: { value: ActivityStatusFilter; label: string }[] = [
  { value: "ALL", label: "전체" },
  { value: "ACTIVE_ONLY", label: "유효" },
  { value: "INACTIVE_ONLY", label: "조치됨" },
];

export default function AdminActivitiesPage() {
  const [tab, setTab] = useState<ActivityTab>("recommendations");
  const [recommendations, setRecommendations] = useState<
    AdminRecommendation[]
  >([]);
  const [visits, setVisits] = useState<AdminVisit[]>([]);
  const [comments, setComments] = useState<AdminComment[]>([]);
  const [selectedRecommendationId, setSelectedRecommendationId] = useState<
    number | null
  >(null);
  const [selectedVisitId, setSelectedVisitId] = useState<number | null>(null);
  const [selectedCommentId, setSelectedCommentId] = useState<number | null>(
    null,
  );
  const [keyword, setKeyword] = useState("");
  const [statusFilter, setStatusFilter] =
    useState<ActivityStatusFilter>("ALL");
  const [memo, setMemo] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const loadActivities = async () => {
    const [nextRecommendations, nextVisits, nextComments] = await Promise.all([
      getAdminRecommendations(),
      getAdminVisits(),
      getAdminComments(),
    ]);
    setRecommendations(nextRecommendations);
    setVisits(nextVisits);
    setComments(nextComments);
    setSelectedRecommendationId(
      (current) => current ?? nextRecommendations[0]?.recommendationId ?? null,
    );
    setSelectedVisitId((current) => current ?? nextVisits[0]?.visitId ?? null);
    setSelectedCommentId(
      (current) => current ?? nextComments[0]?.commentId ?? null,
    );
  };

  useEffect(() => {
    let mounted = true;
    Promise.resolve()
      .then(() => loadActivities())
      .then(() => {
        if (mounted) {
          setMessage(null);
        }
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

  const selectedRecommendation = useMemo(() => {
    return (
      recommendations.find(
        (item) => item.recommendationId === selectedRecommendationId,
      ) ?? null
    );
  }, [recommendations, selectedRecommendationId]);

  const selectedVisit = useMemo(() => {
    return visits.find((item) => item.visitId === selectedVisitId) ?? null;
  }, [selectedVisitId, visits]);

  const selectedComment = useMemo(() => {
    return comments.find((item) => item.commentId === selectedCommentId) ?? null;
  }, [comments, selectedCommentId]);

  const filteredRecommendations = useMemo(() => {
    return recommendations.filter((item) => {
      return (
        matchesKeyword(
          keyword,
          item.nickname,
          item.placeName,
          item.categoryCode,
          String(item.userId),
          String(item.placeId),
        ) && matchesActiveFilter(statusFilter, item.status === "ACTIVE")
      );
    });
  }, [keyword, recommendations, statusFilter]);

  const filteredVisits = useMemo(() => {
    return visits.filter((item) => {
      return (
        matchesKeyword(
          keyword,
          item.nickname,
          item.placeName,
          item.categoryCode,
          String(item.userId),
          String(item.placeId),
        ) && matchesActiveFilter(statusFilter, item.valid)
      );
    });
  }, [keyword, statusFilter, visits]);

  const filteredComments = useMemo(() => {
    return comments.filter((item) => {
      return (
        matchesKeyword(
          keyword,
          item.nickname,
          item.placeName,
          item.categoryCode,
          item.content,
          String(item.userId),
          String(item.placeId),
        ) && matchesActiveFilter(statusFilter, item.status === "VISIBLE")
      );
    });
  }, [comments, keyword, statusFilter]);

  const summary = useMemo(() => {
    return {
      activeRecommendations: recommendations.filter(
        (item) => item.status === "ACTIVE",
      ).length,
      validVisits: visits.filter((item) => item.valid).length,
      visibleComments: comments.filter((item) => item.status === "VISIBLE")
        .length,
      moderatedComments: comments.filter((item) => item.status !== "VISIBLE")
        .length,
    };
  }, [comments, recommendations, visits]);

  const handleRefreshAfterAction = async (successMessage: string) => {
    await loadActivities();
    setMemo("");
    setMessage(successMessage);
  };

  const handleInvalidateRecommendation = async () => {
    if (!selectedRecommendation) {
      return;
    }
    if (selectedRecommendation.status !== "ACTIVE") {
      setMessage("활성 추천만 무효화할 수 있습니다.");
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      await invalidateAdminRecommendation(
        selectedRecommendation.recommendationId,
        { memo: memo.trim() || null },
      );
      await handleRefreshAfterAction("추천을 무효화했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "추천을 무효화하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  const handleInvalidateVisit = async () => {
    if (!selectedVisit) {
      return;
    }
    if (!selectedVisit.valid) {
      setMessage("이미 무효 처리된 방문입니다.");
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      await invalidateAdminVisit(selectedVisit.visitId, {
        memo: memo.trim() || null,
      });
      await handleRefreshAfterAction("방문 인증을 무효화했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "방문 인증을 무효화하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  const handleBlindComment = async () => {
    if (!selectedComment) {
      return;
    }
    if (selectedComment.status !== "VISIBLE") {
      setMessage("보이는 댓글만 블라인드할 수 있습니다.");
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      await blindAdminComment(selectedComment.commentId, {
        memo: memo.trim() || null,
      });
      await handleRefreshAfterAction("댓글을 블라인드했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "댓글을 블라인드하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  const handleDeleteComment = async () => {
    if (!selectedComment) {
      return;
    }
    if (selectedComment.status === "DELETED") {
      setMessage("이미 삭제된 댓글입니다.");
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      await deleteAdminComment(selectedComment.commentId, {
        memo: memo.trim() || null,
      });
      await handleRefreshAfterAction("댓글을 삭제했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "댓글을 삭제하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  const activeItems = {
    recommendations: filteredRecommendations,
    visits: filteredVisits,
    comments: filteredComments,
  }[tab];

  return (
    <AdminPageShell
      title="활동 관리"
      description="추천, 방문 인증, 댓글 기록을 확인하고 필요한 운영 조치를 적용합니다."
    >

        <section className="mt-6 grid grid-cols-2 gap-2 sm:grid-cols-4">
          <SummaryCell label="활성 추천" value={summary.activeRecommendations} />
          <SummaryCell label="유효 방문" value={summary.validVisits} />
          <SummaryCell label="보이는 댓글" value={summary.visibleComments} />
          <SummaryCell label="조치 댓글" value={summary.moderatedComments} />
        </section>

        <section className="mt-5 grid gap-2 sm:grid-cols-3">
          {TABS.map((item) => (
            <button
              key={item.value}
              type="button"
              onClick={() => {
                setTab(item.value);
                setMemo("");
              }}
              className={`admin-segment ${
                tab === item.value
                  ? "admin-segment-selected"
                  : "admin-segment-idle"
              }`}
            >
              {item.label}
            </button>
          ))}
        </section>

        {message && (
          <p className="mt-4 rounded-m3-lg bg-m3-secondary-container px-4 py-3 text-m3-label-lg text-m3-on-secondary-container shadow-m3-1">
            {message}
          </p>
        )}

        {loading && (
          <section className="admin-panel admin-panel-spacious mt-6 text-m3-body-md text-m3-on-surface-variant">
            활동 기록을 불러오는 중입니다.
          </section>
        )}

        {!loading && (
          <div className="mt-5 grid gap-5 lg:grid-cols-[360px_1fr]">
            <section className="admin-panel">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-bold">{tabLabel(tab)} 목록</h2>
                <span className="text-xs font-semibold text-gray-400">
                  {activeItems.length}건
                </span>
              </div>

              <label className="mt-4 block text-sm font-semibold">
                검색
                <input
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="사용자, 장소, 내용, ID"
                  className="admin-field mt-2 h-10"
                />
              </label>

              <div className="mt-3 grid gap-2 sm:grid-cols-3">
                {STATUS_FILTERS.map((filter) => (
                  <button
                    key={filter.value}
                    type="button"
                    onClick={() => setStatusFilter(filter.value)}
                    className={`admin-segment h-9 ${
                      statusFilter === filter.value
                        ? "admin-segment-selected"
                        : "admin-segment-idle"
                    }`}
                  >
                    {filter.label}
                  </button>
                ))}
              </div>

              {activeItems.length === 0 && (
                <p className="admin-muted-panel mt-5">
                  조건에 맞는 활동 기록이 없습니다.
                </p>
              )}

              <div className="mt-4 flex max-h-[700px] flex-col gap-3 overflow-y-auto pr-1">
                {tab === "recommendations" &&
                  filteredRecommendations.map((item) => (
                    <RecommendationListItem
                      key={item.recommendationId}
                      item={item}
                      selected={
                        item.recommendationId === selectedRecommendationId
                      }
                      onSelect={() => {
                        setSelectedRecommendationId(item.recommendationId);
                        setMemo("");
                      }}
                    />
                  ))}

                {tab === "visits" &&
                  filteredVisits.map((item) => (
                    <VisitListItem
                      key={item.visitId}
                      item={item}
                      selected={item.visitId === selectedVisitId}
                      onSelect={() => {
                        setSelectedVisitId(item.visitId);
                        setMemo("");
                      }}
                    />
                  ))}

                {tab === "comments" &&
                  filteredComments.map((item) => (
                    <CommentListItem
                      key={item.commentId}
                      item={item}
                      selected={item.commentId === selectedCommentId}
                      onSelect={() => {
                        setSelectedCommentId(item.commentId);
                        setMemo("");
                      }}
                    />
                  ))}
              </div>
            </section>

            <section className="admin-panel admin-panel-spacious">
              {tab === "recommendations" && (
                <RecommendationDetail
                  item={selectedRecommendation}
                  memo={memo}
                  busy={busy}
                  onMemoChange={setMemo}
                  onInvalidate={handleInvalidateRecommendation}
                />
              )}

              {tab === "visits" && (
                <VisitDetail
                  item={selectedVisit}
                  memo={memo}
                  busy={busy}
                  onMemoChange={setMemo}
                  onInvalidate={handleInvalidateVisit}
                />
              )}

              {tab === "comments" && (
                <CommentDetail
                  item={selectedComment}
                  memo={memo}
                  busy={busy}
                  onMemoChange={setMemo}
                  onBlind={handleBlindComment}
                  onDelete={handleDeleteComment}
                />
              )}
            </section>
          </div>
        )}
    </AdminPageShell>
  );
}

function RecommendationListItem({
  item,
  selected,
  onSelect,
}: {
  item: AdminRecommendation;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      className={`admin-list-item ${
        selected ? "admin-list-item-selected" : "admin-list-item-idle"
      }`}
    >
      <ListHeader
        title={item.placeName}
        badge={recommendationStatusLabel(item.status)}
        active={item.status === "ACTIVE"}
      />
      <p className="mt-1 text-m3-body-sm text-m3-on-surface-variant">
        {item.nickname} · {item.categoryCode} · 가중치{" "}
        {formatDecimal(item.recommendWeight)}
      </p>
      <p className="mt-2 text-xs text-gray-400">{formatDate(item.createdAt)}</p>
    </button>
  );
}

function VisitListItem({
  item,
  selected,
  onSelect,
}: {
  item: AdminVisit;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      className={`admin-list-item ${
        selected ? "admin-list-item-selected" : "admin-list-item-idle"
      }`}
    >
      <ListHeader
        title={item.placeName}
        badge={item.valid ? "유효" : "무효"}
        active={item.valid}
      />
      <p className="mt-1 text-m3-body-sm text-m3-on-surface-variant">
        {item.nickname} · {item.categoryCode} · 거리{" "}
        {item.distanceMeter === null ? "기록 없음" : `${item.distanceMeter}m`}
      </p>
      <p className="mt-2 text-xs text-gray-400">{formatDate(item.createdAt)}</p>
    </button>
  );
}

function CommentListItem({
  item,
  selected,
  onSelect,
}: {
  item: AdminComment;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      className={`admin-list-item ${
        selected ? "admin-list-item-selected" : "admin-list-item-idle"
      }`}
    >
      <ListHeader
        title={item.placeName}
        badge={commentStatusLabel(item.status)}
        active={item.status === "VISIBLE"}
      />
      <p className="mt-1 line-clamp-2 text-m3-body-sm text-m3-on-surface-variant">
        {item.content}
      </p>
      <p className="mt-2 text-xs text-gray-400">
        {item.nickname} · 신고 {item.reportCount}건 · {formatDate(item.createdAt)}
      </p>
    </button>
  );
}

function ListHeader({
  title,
  badge,
  active,
}: {
  title: string;
  badge: string;
  active: boolean;
}) {
  return (
    <div className="flex items-start justify-between gap-2">
      <p className="font-bold">{title}</p>
      <span
        className={`rounded-full px-2 py-1 text-xs font-bold ${
          active
            ? "bg-m3-surface-container-lowest text-m3-primary"
            : "bg-red-50 text-m3-error"
        }`}
      >
        {badge}
      </span>
    </div>
  );
}

function RecommendationDetail({
  item,
  memo,
  busy,
  onMemoChange,
  onInvalidate,
}: {
  item: AdminRecommendation | null;
  memo: string;
  busy: boolean;
  onMemoChange: (value: string) => void;
  onInvalidate: () => void;
}) {
  if (!item) {
    return <EmptyDetail label="추천 기록을 선택해 주세요." />;
  }

  return (
    <div>
      <DetailHeader
        title={`추천 #${item.recommendationId}`}
        subtitle={`${item.placeName} · ${item.nickname}`}
        badge={recommendationStatusLabel(item.status)}
      />
      <dl className="mt-5 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3">
        <InfoItem label="사용자" value={`${item.nickname} (#${item.userId})`} />
        <InfoItem label="장소" value={`${item.placeName} (#${item.placeId})`} />
        <InfoItem label="카테고리" value={item.categoryCode} />
        <InfoItem label="추천 가중치" value={formatDecimal(item.recommendWeight)} />
        <InfoItem label="등록일" value={formatDate(item.createdAt)} />
        <InfoItem label="수정일" value={formatDate(item.updatedAt)} />
      </dl>
      <MemoBox value={memo} onChange={onMemoChange} />
      <button
        type="button"
        onClick={onInvalidate}
        disabled={busy || item.status !== "ACTIVE"}
        className="admin-action-danger mt-4 h-11 w-full"
      >
        {busy ? "처리 중" : "추천 무효화"}
      </button>
    </div>
  );
}

function VisitDetail({
  item,
  memo,
  busy,
  onMemoChange,
  onInvalidate,
}: {
  item: AdminVisit | null;
  memo: string;
  busy: boolean;
  onMemoChange: (value: string) => void;
  onInvalidate: () => void;
}) {
  if (!item) {
    return <EmptyDetail label="방문 기록을 선택해 주세요." />;
  }

  return (
    <div>
      <DetailHeader
        title={`방문 #${item.visitId}`}
        subtitle={`${item.placeName} · ${item.nickname}`}
        badge={item.valid ? "유효" : "무효"}
      />
      <dl className="mt-5 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3">
        <InfoItem label="사용자" value={`${item.nickname} (#${item.userId})`} />
        <InfoItem label="장소" value={`${item.placeName} (#${item.placeId})`} />
        <InfoItem label="카테고리" value={item.categoryCode} />
        <InfoItem label="거리" value={item.distanceMeter === null ? "기록 없음" : `${item.distanceMeter}m`} />
        <InfoItem label="좌표" value={`${item.latitude}, ${item.longitude}`} />
        <InfoItem label="판정 사유" value={item.validReason ?? "기록 없음"} />
        <InfoItem label="등록일" value={formatDate(item.createdAt)} />
        <InfoItem label="수정일" value={formatDate(item.updatedAt)} />
      </dl>
      {item.imageUrl && (
        <a
          href={item.imageUrl}
          target="_blank"
          rel="noreferrer"
          className="admin-muted-panel mt-4 block break-all text-m3-label-lg text-m3-primary underline"
        >
          방문 이미지 열기
        </a>
      )}
      <MemoBox value={memo} onChange={onMemoChange} />
      <button
        type="button"
        onClick={onInvalidate}
        disabled={busy || !item.valid}
        className="admin-action-danger mt-4 h-11 w-full"
      >
        {busy ? "처리 중" : "방문 무효화"}
      </button>
    </div>
  );
}

function CommentDetail({
  item,
  memo,
  busy,
  onMemoChange,
  onBlind,
  onDelete,
}: {
  item: AdminComment | null;
  memo: string;
  busy: boolean;
  onMemoChange: (value: string) => void;
  onBlind: () => void;
  onDelete: () => void;
}) {
  if (!item) {
    return <EmptyDetail label="댓글 기록을 선택해 주세요." />;
  }

  return (
    <div>
      <DetailHeader
        title={`댓글 #${item.commentId}`}
        subtitle={`${item.placeName} · ${item.nickname}`}
        badge={commentStatusLabel(item.status)}
      />
      <div className="admin-muted-panel mt-5 text-m3-body-md">
        <p className="font-bold">댓글 내용</p>
        <p className="mt-2 whitespace-pre-wrap text-gray-600">{item.content}</p>
      </div>
      <dl className="mt-5 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3">
        <InfoItem label="사용자" value={`${item.nickname} (#${item.userId})`} />
        <InfoItem label="장소" value={`${item.placeName} (#${item.placeId})`} />
        <InfoItem label="카테고리" value={item.categoryCode} />
        <InfoItem label="신고 수" value={`${item.reportCount}건`} />
        <InfoItem label="등록일" value={formatDate(item.createdAt)} />
        <InfoItem label="수정일" value={formatDate(item.updatedAt)} />
        <InfoItem
          label="삭제일"
          value={item.deletedAt ? formatDate(item.deletedAt) : "기록 없음"}
        />
      </dl>
      <MemoBox value={memo} onChange={onMemoChange} />
      <div className="mt-4 grid gap-3 sm:grid-cols-2">
        <button
          type="button"
          onClick={onBlind}
          disabled={busy || item.status !== "VISIBLE"}
          className="admin-action-warning h-11"
        >
          {busy ? "처리 중" : "댓글 블라인드"}
        </button>
        <button
          type="button"
          onClick={onDelete}
          disabled={busy || item.status === "DELETED"}
          className="admin-action-danger h-11"
        >
          {busy ? "처리 중" : "댓글 삭제"}
        </button>
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
        <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">
          {subtitle}
        </p>
      </div>
      <span className="w-fit rounded-m3-full bg-m3-secondary-container px-3 py-1 text-m3-label-md text-m3-on-secondary-container">
        {badge}
      </span>
    </div>
  );
}

function MemoBox({
  value,
  onChange,
}: {
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="mt-5 block border-t border-m3-outline-variant pt-5 text-m3-label-lg">
      운영 메모
      <textarea
        value={value}
        onChange={(event) => onChange(event.target.value)}
        maxLength={255}
        placeholder="이번 조치의 운영 메모를 입력해 주세요."
        className="admin-field mt-3 min-h-20 resize-none p-3"
      />
    </label>
  );
}

function EmptyDetail({ label }: { label: string }) {
  return <p className="text-m3-body-md text-m3-on-surface-variant">{label}</p>;
}

function SummaryCell({ label, value }: { label: string; value: number }) {
  return (
    <div className="admin-panel text-center">
      <p className="text-2xl font-bold">{value.toLocaleString("ko-KR")}</p>
      <p className="mt-1 text-m3-label-md text-m3-on-surface-variant">{label}</p>
    </div>
  );
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="admin-info-cell">
      <dt className="text-m3-label-md text-m3-on-surface-variant">{label}</dt>
      <dd className="mt-1 break-words font-semibold">{value}</dd>
    </div>
  );
}

function matchesKeyword(keyword: string, ...values: string[]) {
  const normalizedKeyword = keyword.trim().toLowerCase();
  if (!normalizedKeyword) {
    return true;
  }
  return values.join(" ").toLowerCase().includes(normalizedKeyword);
}

function matchesActiveFilter(filter: ActivityStatusFilter, active: boolean) {
  if (filter === "ALL") {
    return true;
  }
  if (filter === "ACTIVE_ONLY") {
    return active;
  }
  return !active;
}

function tabLabel(value: ActivityTab) {
  switch (value) {
    case "recommendations":
      return "추천";
    case "visits":
      return "방문";
    case "comments":
      return "댓글";
  }
}

function recommendationStatusLabel(value: AdminRecommendationStatus) {
  switch (value) {
    case "ACTIVE":
      return "활성";
    case "CANCELED":
      return "취소";
    case "INVALIDATED":
      return "무효";
  }
}

function commentStatusLabel(value: AdminCommentStatus) {
  switch (value) {
    case "VISIBLE":
      return "보임";
    case "BLINDED":
      return "블라인드";
    case "DELETED":
      return "삭제";
  }
}

function formatDecimal(value: number) {
  return Number(value).toLocaleString("ko-KR", {
    maximumFractionDigits: 2,
  });
}

function formatDate(value: string) {
  return new Date(value).toLocaleString("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}
