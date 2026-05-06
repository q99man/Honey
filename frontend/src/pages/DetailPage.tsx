import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getApiErrorMessage, hasStoredAccessToken } from "../api/http";
import {
  cancelRecommendation,
  createComment,
  deleteComment,
  getPlaceComments,
  getRecommendationPolicy,
  getVisitPolicy,
  recommendPlace,
  updateComment,
  verifyVisit,
  type CommentItem,
  type RecommendationPolicy,
  type VisitPolicy,
} from "../api/participationApi";
import { getPlace } from "../api/placeApi";
import {
  getPlaceRankingHistory,
  type PlaceRankingHistory,
} from "../api/rankingApi";
import { createReport, type ReportTargetType } from "../api/reportApi";
import { getMyProfile } from "../api/userApi";
import BottomNav from "../components/BottomNav";
import type { Place } from "../types/place";

type Props = {
  wishedIds: Set<number>;
  onToggleWish: (id: number) => void;
  onPlaceUpdated: (place: Place) => void;
};

type BusyAction =
  | "recommend"
  | "visit"
  | "comment"
  | "delete"
  | "report"
  | null;

type ReportTarget = {
  type: ReportTargetType;
  id: number;
  label: string;
};

const PLACE_NOT_FOUND = "해당 맛집을 찾을 수 없어요.";
const PLACE_LOAD_ERROR = "맛집 정보를 불러오지 못했어요.";
const LOGIN_REQUIRED = "로그인이 필요한 기능이에요.";
const GEOLOCATION_UNAVAILABLE = "현재 위치를 확인할 수 없어요.";
const PLACE_IMAGE_FALLBACK = "꿀맛집 이미지 준비 중";
const REPORT_REASON_OPTIONS = [
  { value: "FAKE_INFO", label: "잘못된 정보" },
  { value: "FRANCHISE", label: "프랜차이즈 의심" },
  { value: "SPAM", label: "스팸/홍보" },
  { value: "ABUSE", label: "부적절한 내용" },
  { value: "OTHER", label: "기타" },
];

export default function DetailPage({
  wishedIds,
  onToggleWish,
  onPlaceUpdated,
}: Props) {
  const { id } = useParams();
  const navigate = useNavigate();
  const placeId = Number(id);
  const invalidPlaceId = !placeId;
  const [place, setPlace] = useState<Place | null>(null);
  const [history, setHistory] = useState<PlaceRankingHistory | null>(null);
  const [comments, setComments] = useState<CommentItem[]>([]);
  const [currentUserId, setCurrentUserId] = useState<number | null>(null);
  const [recommendationPolicy, setRecommendationPolicy] =
    useState<RecommendationPolicy | null>(null);
  const [visitPolicy, setVisitPolicy] = useState<VisitPolicy | null>(null);
  const [commentText, setCommentText] = useState("");
  const [reportTarget, setReportTarget] = useState<ReportTarget | null>(null);
  const [reportReasonCode, setReportReasonCode] = useState("OTHER");
  const [reportReasonText, setReportReasonText] = useState("");
  const [loading, setLoading] = useState(!invalidPlaceId);
  const [busyAction, setBusyAction] = useState<BusyAction>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);
  const [failedImageUrl, setFailedImageUrl] = useState<string | null>(null);

  const authenticated = hasStoredAccessToken();
  const myComment = useMemo(
    () => comments.find((comment) => comment.userId === currentUserId) ?? null,
    [comments, currentUserId],
  );
  const alreadyRecommended =
    recommendationPolicy?.reason === "ALREADY_RECOMMENDED";
  const showHeroImage = Boolean(
    place?.imageUrl && place.imageUrl !== failedImageUrl,
  );

  useEffect(() => {
    if (invalidPlaceId) {
      return;
    }

    let mounted = true;
    Promise.resolve()
      .then(async () => {
        setLoading(true);
        const [item, rankingHistory, commentItems] = await Promise.all([
          getPlace(placeId),
          getPlaceRankingHistory(placeId),
          getPlaceComments(placeId),
        ]);

        const optional = authenticated
          ? await Promise.allSettled([
              getMyProfile(),
              getRecommendationPolicy(placeId),
              getVisitPolicy(placeId),
            ])
          : [];

        return { item, rankingHistory, commentItems, optional };
      })
      .then(({ item, rankingHistory, commentItems, optional }) => {
        if (!mounted) {
          return;
        }

        const nextPlace = { ...item, isWished: wishedIds.has(item.id) };
        setPlace(nextPlace);
        setHistory(rankingHistory);
        setComments(commentItems);
        setErrorMessage(null);

        const profileResult = optional[0];
        const recommendationResult = optional[1];
        const visitResult = optional[2];

        if (profileResult?.status === "fulfilled") {
          setCurrentUserId(profileResult.value.id);
          const ownComment = commentItems.find(
            (comment) => comment.userId === profileResult.value.id,
          );
          setCommentText(ownComment?.content ?? "");
        } else {
          setCurrentUserId(null);
        }
        if (recommendationResult?.status === "fulfilled") {
          setRecommendationPolicy(recommendationResult.value);
        } else {
          setRecommendationPolicy(null);
        }
        if (visitResult?.status === "fulfilled") {
          setVisitPolicy(visitResult.value);
        } else {
          setVisitPolicy(null);
        }
      })
      .catch((error) => {
        if (mounted) {
          setErrorMessage(getApiErrorMessage(error, PLACE_LOAD_ERROR));
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
  }, [authenticated, invalidPlaceId, placeId, wishedIds]);

  if (invalidPlaceId) {
    return (
      <div className="min-h-screen bg-neutral-100">
        <main className="mx-auto flex min-h-screen max-w-[430px] items-center justify-center bg-[#fffaf0] px-4 pb-24 text-center">
          <div className="rounded-3xl bg-white p-6 shadow-sm">
            <p className="text-sm font-semibold text-[#2b210f]">
              해당 맛집을 찾을 수 없어요.
            </p>
            <button
              type="button"
              onClick={() => navigate("/")}
              className="mt-4 rounded-full bg-[#f6b800] px-5 py-3 text-sm font-semibold text-[#2b210f] transition duration-200 active:scale-[0.98]"
            >
              홈으로 돌아가기
            </button>
          </div>
        </main>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-neutral-100">
        <main className="mx-auto min-h-screen max-w-[430px] bg-[#fffaf0] pb-24">
          <div className="animate-pulse">
            <div className="aspect-[16/10] w-full rounded-b-3xl bg-[#fff1bf]" />
            <div className="space-y-4 px-4 pt-5">
              <div className="rounded-3xl bg-white p-5 text-sm text-gray-500 shadow-sm">
                맛집 정보를 불러오는 중이에요...
              </div>
              <div className="rounded-3xl bg-white p-4 shadow-sm">
                <div className="h-5 w-2/3 rounded-full bg-gray-100" />
                <div className="mt-3 h-3 w-full rounded-full bg-gray-100" />
                <div className="mt-2 h-3 w-1/2 rounded-full bg-gray-100" />
              </div>
            </div>
          </div>
        </main>
      </div>
    );
  }

  if (!place || errorMessage) {
    return (
      <div className="min-h-screen bg-neutral-100">
        <main className="mx-auto flex min-h-screen max-w-[430px] items-center justify-center bg-[#fffaf0] px-4 pb-24 text-center">
          <div className="rounded-3xl bg-white p-6 shadow-sm">
            <p className="text-sm font-semibold text-[#2b210f]">
              {errorMessage
                ? "맛집 정보를 불러오지 못했어요."
                : PLACE_NOT_FOUND}
            </p>
            <p className="mt-2 text-sm text-gray-500">
              {errorMessage ?? "홈에서 다른 맛집을 둘러볼 수 있어요."}
            </p>
            <button
              type="button"
              onClick={() => (errorMessage ? navigate(0) : navigate("/"))}
              className="mt-4 rounded-full bg-[#f6b800] px-5 py-3 text-sm font-semibold text-[#2b210f] transition duration-200 active:scale-[0.98]"
            >
              {errorMessage ? "다시 불러오기" : "홈으로 돌아가기"}
            </button>
          </div>
        </main>
      </div>
    );
  }

  const handleRecommendation = async () => {
    if (!authenticated) {
      setActionMessage(LOGIN_REQUIRED);
      return;
    }

    setBusyAction("recommend");
    setActionMessage(null);
    try {
      const result = alreadyRecommended
        ? await cancelRecommendation(place.id)
        : await recommendPlace(place.id);
      const nextPolicy = await getRecommendationPolicy(place.id);
      const nextPlace = {
        ...place,
        recommendCount: result.recommendCount,
      };
      setPlace(nextPlace);
      setRecommendationPolicy(nextPolicy);
      onPlaceUpdated(nextPlace);
      setActionMessage(
        result.recommended
          ? "추천을 남겼어요."
          : "추천을 취소했어요.",
      );
    } catch (error) {
      setActionMessage(
        getApiErrorMessage(error, "추천 요청을 처리하지 못했어요."),
      );
    } finally {
      setBusyAction(null);
    }
  };

  const handleVisit = async () => {
    if (!authenticated) {
      setActionMessage(LOGIN_REQUIRED);
      return;
    }
    if (!navigator.geolocation) {
      setActionMessage(GEOLOCATION_UNAVAILABLE);
      return;
    }

    setBusyAction("visit");
    setActionMessage(null);
    try {
      const position = await getCurrentPosition();
      const result = await verifyVisit(
        place.id,
        position.coords.latitude,
        position.coords.longitude,
      );
      const nextPolicy = await getVisitPolicy(place.id);
      const nextPlace = {
        ...place,
        visitCount: result.visitCount,
      };
      setPlace(nextPlace);
      setVisitPolicy(nextPolicy);
      onPlaceUpdated(nextPlace);
      setActionMessage(
        `방문 인증 완료: ${result.distanceMeter}m, EXP +${result.expGained}`,
      );
    } catch (error) {
      setActionMessage(
        getApiErrorMessage(error, "방문 인증을 처리하지 못했어요."),
      );
    } finally {
      setBusyAction(null);
    }
  };

  const handleSubmitComment = async () => {
    if (!authenticated) {
      setActionMessage(LOGIN_REQUIRED);
      return;
    }

    const content = commentText.trim();
    if (!content) {
      setActionMessage("평가 내용을 입력해 주세요.");
      return;
    }

    setBusyAction("comment");
    setActionMessage(null);
    try {
      if (myComment) {
        await updateComment(myComment.commentId, content);
      } else {
        await createComment(place.id, content);
      }
      await refreshPlaceAndComments();
      setActionMessage(myComment ? "평가를 수정했어요." : "평가를 남겼어요.");
    } catch (error) {
      setActionMessage(
        getApiErrorMessage(error, "평가 요청을 처리하지 못했어요."),
      );
    } finally {
      setBusyAction(null);
    }
  };

  const handleDeleteComment = async () => {
    if (!myComment) {
      return;
    }

    setBusyAction("delete");
    setActionMessage(null);
    try {
      await deleteComment(myComment.commentId);
      setCommentText("");
      await refreshPlaceAndComments();
      setActionMessage("평가를 삭제했어요.");
    } catch (error) {
      setActionMessage(
        getApiErrorMessage(error, "평가 삭제를 처리하지 못했어요."),
      );
    } finally {
      setBusyAction(null);
    }
  };

  const handleOpenReport = (target: ReportTarget) => {
    if (!authenticated) {
      setActionMessage(LOGIN_REQUIRED);
      return;
    }

    setReportTarget(target);
    setReportReasonCode("OTHER");
    setReportReasonText("");
    setActionMessage(null);
  };

  const handleSubmitReport = async () => {
    if (!authenticated) {
      setActionMessage(LOGIN_REQUIRED);
      return;
    }
    if (!reportTarget) {
      return;
    }

    setBusyAction("report");
    setActionMessage(null);
    try {
      await createReport({
        targetType: reportTarget.type,
        targetId: reportTarget.id,
        reasonCode: reportReasonCode,
        reasonText: reportReasonText.trim() || null,
      });
      setReportTarget(null);
      setReportReasonText("");
      setActionMessage("신고를 접수했어요. 운영자가 확인할게요.");
    } catch (error) {
      setActionMessage(
        getApiErrorMessage(error, "신고 요청을 처리하지 못했어요."),
      );
    } finally {
      setBusyAction(null);
    }
  };

  const refreshPlaceAndComments = async () => {
    const [nextPlaceResponse, nextComments] = await Promise.all([
      getPlace(place.id),
      getPlaceComments(place.id),
    ]);
    const nextPlace = {
      ...nextPlaceResponse,
      isWished: wishedIds.has(nextPlaceResponse.id),
    };
    setPlace(nextPlace);
    setComments(nextComments);
    onPlaceUpdated(nextPlace);
  };

  return (
    <div className="min-h-screen bg-neutral-100">
      <main className="mx-auto min-h-screen max-w-[430px] bg-[#fffaf0] pb-24">
        <div className="relative flex aspect-[16/10] w-full items-center justify-center overflow-hidden rounded-b-3xl bg-[#fff1bf] text-center text-sm font-semibold leading-5 text-[#5c3b13]">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="absolute left-4 top-4 z-10 flex h-11 w-11 items-center justify-center rounded-full bg-white/90 text-lg font-semibold text-[#2b210f] shadow-sm transition duration-200 active:scale-[0.96]"
            aria-label="뒤로가기"
          >
            ←
          </button>

          <button
            type="button"
            onClick={(event) => {
              event.preventDefault();
              event.stopPropagation();
              onToggleWish(place.id);
            }}
            className={`absolute right-4 top-4 z-10 flex h-11 w-11 items-center justify-center rounded-full bg-white/90 text-lg shadow-sm transition duration-200 active:scale-[0.96] ${
              place.isWished ? "text-[#d99a00]" : "text-gray-500"
            }`}
            aria-label={place.isWished ? "찜 해제" : "찜하기"}
            aria-pressed={place.isWished}
          >
            {place.isWished ? "♥" : "♡"}
          </button>

          {showHeroImage ? (
            <img
              src={place.imageUrl}
              alt={`${place.title} 대표 이미지`}
              className="h-full w-full object-cover"
              onError={() => setFailedImageUrl(place.imageUrl ?? null)}
            />
          ) : (
            <span>{PLACE_IMAGE_FALLBACK}</span>
          )}
        </div>

        <div className="space-y-5 px-4 pt-5">
          <section className="rounded-3xl bg-white p-4 text-left shadow-sm">
            <div className="flex flex-wrap gap-2">
              {place.regionName && (
                <span className="rounded-full bg-[#fff1bf] px-3 py-1 text-xs font-semibold text-[#5c3b13]">
                  {place.regionName}
                </span>
              )}
              {place.category && (
                <span className="rounded-full bg-gray-100 px-3 py-1 text-xs font-semibold text-gray-600">
                  {place.category}
                </span>
              )}
            </div>
            <h1 className="mt-3 break-words text-2xl font-bold leading-8 text-[#2b210f]">
              {place.title}
            </h1>

            <p className="mt-2 text-sm text-gray-500">
              {place.distance} · 별 {place.rating} · 평가 {place.reviewCount}개
            </p>

            <p className="mt-4 inline-flex rounded-full bg-[#fff1bf] px-3 py-1 text-sm font-semibold text-[#5c3b13]">
              {place.price}
            </p>
            <p className="mt-3 break-words text-sm leading-5 text-gray-700">
              {place.desc || "동네 사람들이 추천한 맛집이에요."}
            </p>
            <button
              type="button"
              onClick={() =>
                handleOpenReport({
                  type: "PLACE",
                  id: place.id,
                  label: place.title,
                })
              }
              disabled={busyAction !== null}
              className="mt-4 rounded-full border border-red-100 bg-white px-4 py-2 text-sm font-semibold text-red-500 transition duration-200 active:scale-[0.98] disabled:opacity-50"
            >
              맛집 신고
            </button>
          </section>

          <section className="rounded-3xl bg-white p-4 text-left shadow-sm">
            <h2 className="text-lg font-bold text-[#2b210f]">이 맛집은요</h2>
            <p className="mt-3 break-words text-sm leading-6 text-gray-700">
              {place.desc || "아직 상세 정보가 준비되지 않았어요."}
            </p>
          </section>

          <section className="rounded-3xl bg-white p-4 text-left shadow-sm">
            <h2 className="text-lg font-bold text-[#2b210f]">위치 정보</h2>
            <p className="mt-3 break-words text-sm leading-6 text-gray-700">
              {place.address || "아직 위치 정보가 준비되지 않았어요."}
            </p>
          </section>

          <section className="rounded-3xl bg-white p-4 text-left shadow-sm">
            <h2 className="text-lg font-bold text-[#2b210f]">추천 정보</h2>
            <div className="mt-4 grid grid-cols-3 gap-3 text-center">
              <div className="rounded-2xl bg-[#fffaf0] px-2 py-3">
                <p className="text-xl font-bold text-[#2b210f]">
                  {place.recommendCount}
                </p>
                <p className="text-xs text-gray-500">추천</p>
              </div>
              <div className="rounded-2xl bg-[#fffaf0] px-2 py-3">
                <p className="text-xl font-bold text-[#2b210f]">
                  {place.visitCount}
                </p>
                <p className="text-xs text-gray-500">방문</p>
              </div>
              <div className="rounded-2xl bg-[#fffaf0] px-2 py-3">
                <p className="text-xl font-bold text-[#2b210f]">
                  {place.commentCount}
                </p>
                <p className="text-xs text-gray-500">평가</p>
              </div>
            </div>
          </section>

          <section className="rounded-3xl bg-white p-4 text-left shadow-sm">
            <h2 className="text-lg font-bold text-[#2b210f]">참여</h2>
            <div className="mt-4 grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={handleRecommendation}
                disabled={busyAction !== null}
                className="h-12 rounded-full bg-[#f6b800] text-sm font-semibold text-[#2b210f] transition duration-200 active:scale-[0.98] disabled:opacity-50"
              >
                {busyAction === "recommend"
                  ? "처리 중"
                  : alreadyRecommended
                    ? "추천 취소"
                    : "추천하기"}
              </button>
              <button
                type="button"
                onClick={handleVisit}
                disabled={busyAction !== null}
                className="h-12 rounded-full bg-[#2f6f5f] text-sm font-semibold text-white transition duration-200 active:scale-[0.98] disabled:opacity-50"
              >
                {busyAction === "visit" ? "확인 중" : "방문 인증"}
              </button>
            </div>

            <div className="mt-3 text-sm text-gray-500">
              {recommendationPolicy && (
                <p>{recommendationStatusText(recommendationPolicy)}</p>
              )}
              {visitPolicy && <p>{visitStatusText(visitPolicy)}</p>}
              {actionMessage && (
                <p className="mt-2 font-semibold text-[#2f6f5f]">
                  {actionMessage}
                </p>
              )}
            </div>
          </section>

          {reportTarget && (
            <section className="rounded-3xl bg-white p-4 text-left shadow-sm">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-lg font-bold text-[#2b210f]">
                    신고하기
                  </h2>
                  <p className="mt-1 text-sm text-gray-500">
                    {targetTypeLabel(reportTarget.type)} · {reportTarget.label}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setReportTarget(null)}
                  disabled={busyAction !== null}
                  className="rounded-full px-3 py-1 text-sm font-bold text-gray-400 transition duration-200 active:scale-[0.98] disabled:opacity-50"
                >
                  닫기
                </button>
              </div>

              <label className="mt-4 block text-sm font-semibold text-[#2b210f]">
                신고 사유
                <select
                  value={reportReasonCode}
                  onChange={(event) => setReportReasonCode(event.target.value)}
                  className="mt-2 h-11 w-full rounded-2xl border border-gray-200 bg-[#fffaf0] px-3 text-sm outline-none focus:border-[#f6b800]"
                >
                  {REPORT_REASON_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>

              <textarea
                value={reportReasonText}
                onChange={(event) => setReportReasonText(event.target.value)}
                maxLength={255}
                placeholder="운영자가 확인할 내용을 입력해 주세요."
                className="mt-3 min-h-24 w-full resize-none rounded-2xl border border-gray-200 bg-[#fffaf0] p-3 text-sm outline-none focus:border-[#f6b800]"
              />

              <button
                type="button"
                onClick={handleSubmitReport}
                disabled={busyAction !== null}
                className="mt-3 h-11 w-full rounded-full bg-red-500 text-sm font-bold text-white transition duration-200 active:scale-[0.98] disabled:opacity-50"
              >
                {busyAction === "report" ? "접수 중" : "신고 접수"}
              </button>
            </section>
          )}

          <section className="rounded-3xl bg-white p-4 text-left shadow-sm">
            <h2 className="text-lg font-bold text-[#2b210f]">평가</h2>

            <div className="mt-4">
              <textarea
                value={commentText}
                onChange={(event) => setCommentText(event.target.value)}
                placeholder={
                  authenticated
                    ? "이 맛집의 좋은 점을 짧게 남겨 주세요."
                    : "로그인하면 평가를 남길 수 있어요."
                }
                className="min-h-24 w-full resize-none rounded-2xl border border-gray-200 bg-[#fffaf0] p-3 text-sm outline-none focus:border-[#f6b800]"
              />
              <div className="mt-3 flex gap-2">
                <button
                  type="button"
                  onClick={handleSubmitComment}
                  disabled={busyAction !== null}
                  className="h-10 flex-1 rounded-full bg-[#f6b800] text-sm font-bold text-[#2b210f] transition duration-200 active:scale-[0.98] disabled:opacity-50"
                >
                  {busyAction === "comment"
                    ? "저장 중"
                    : myComment
                      ? "평가 수정"
                      : "평가 남기기"}
                </button>
                {myComment && (
                  <button
                    type="button"
                    onClick={handleDeleteComment}
                    disabled={busyAction !== null}
                    className="h-10 rounded-full border border-red-100 px-4 text-sm font-bold text-red-500 transition duration-200 active:scale-[0.98] disabled:opacity-50"
                  >
                    삭제
                  </button>
                )}
              </div>
            </div>

            {comments.length === 0 && (
              <p className="mt-5 text-sm text-gray-500">
                아직 남겨진 평가가 없어요.
              </p>
            )}

            <div className="mt-5 flex flex-col gap-4">
              {comments.map((comment) => (
                <article
                  key={comment.commentId}
                  className="border-t border-gray-100 pt-4"
                >
                  <div className="flex items-center justify-between gap-3">
                    <p className="font-semibold text-[#2b210f]">
                      {comment.nickname}
                    </p>
                    <div className="flex shrink-0 items-center gap-2">
                      <p className="text-xs text-gray-400">
                        {formatDate(comment.updatedAt)}
                      </p>
                      {comment.userId !== currentUserId && (
                        <button
                          type="button"
                          onClick={() =>
                            handleOpenReport({
                              type: "COMMENT",
                              id: comment.commentId,
                              label: `${comment.nickname}님의 평가`,
                            })
                          }
                          disabled={busyAction !== null}
                          className="text-xs font-bold text-red-500 transition duration-200 active:scale-[0.98] disabled:opacity-50"
                        >
                          신고
                        </button>
                      )}
                    </div>
                  </div>
                  <p className="mt-2 break-words text-sm leading-5 text-gray-600">
                    {comment.content}
                  </p>
                </article>
              ))}
            </div>
          </section>

          <section className="rounded-3xl bg-white p-4 text-left shadow-sm">
            <h2 className="text-lg font-bold text-[#2b210f]">랭킹 히스토리</h2>

            {history?.items.length === 0 && (
              <p className="mt-3 text-sm text-gray-500">
                아직 확정된 랭킹 기록이 없어요.
              </p>
            )}

            <div className="mt-4 flex flex-col gap-3">
              {history?.items.slice(0, 6).map((item) => (
                <div
                  key={`${item.seasonId}-${item.regionType}-${item.regionId}`}
                  className="flex items-center justify-between gap-4 border-t border-gray-100 pt-3 text-sm"
                >
                  <div>
                    <p className="font-semibold text-[#2b210f]">
                      {item.seasonName}
                    </p>
                    <p className="mt-1 text-gray-500">
                      {regionLabel(item.regionType)} · {item.rank}위
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="font-bold text-[#2b210f]">
                      별 {item.starLevel}
                    </p>
                    <p className="mt-1 text-gray-500">
                      {formatScore(item.totalScore)}점
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
      </main>

      <BottomNav />
    </div>
  );
}

function getCurrentPosition(): Promise<GeolocationPosition> {
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 10000,
    });
  });
}

function recommendationStatusText(policy: RecommendationPolicy) {
  if (policy.reason === "ALREADY_RECOMMENDED") {
    return "이미 추천한 맛집이에요.";
  }
  if (!policy.canRecommend) {
    return "지금은 추천할 수 없어요.";
  }
  return `오늘 추천 가능 ${policy.dailyRemainingCount}회`;
}

function visitStatusText(policy: VisitPolicy) {
  if (policy.canVisitNow) {
    return `방문 인증 반경 ${policy.radiusMeter}m`;
  }
  if (policy.cooldownUntil) {
    return `다음 방문 가능: ${formatDate(policy.cooldownUntil)}`;
  }
  return "지금은 방문 인증할 수 없어요.";
}

function regionLabel(regionType: string) {
  if (regionType === "dong") {
    return "동";
  }
  if (regionType === "district") {
    return "구";
  }
  return "시";
}

function targetTypeLabel(targetType: ReportTargetType) {
  switch (targetType) {
    case "PLACE":
      return "맛집";
    case "COMMENT":
      return "평가";
    case "USER":
      return "사용자";
  }
}

function formatScore(score: number) {
  return Number(score).toLocaleString("ko-KR", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  });
}

function formatDate(value: string) {
  return new Date(value).toLocaleString("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}
