import { useCallback, useEffect, useMemo, useRef, useState } from "react";
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
  type RankingRegionType,
} from "../api/rankingApi";
import { createReport, type ReportTargetType } from "../api/reportApi";
import {
  getKakaoMapJavaScriptKey,
  loadKakaoMapSdk,
  type KakaoMap,
  type KakaoMarker,
} from "../lib/loadKakaoMapSdk";
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
  | "comment-delete"
  | "report"
  | null;

const PLACE_NOT_FOUND = "해당 맛집을 찾을 수 없어요.";
const PLACE_LOAD_ERROR = "맛집 정보를 불러오지 못했어요.";
const LOGIN_REQUIRED = "로그인이 필요한 기능이에요.";
const GEOLOCATION_UNAVAILABLE = "현재 위치를 확인할 수 없어요.";
const PLACE_IMAGE_FALLBACK = "꿀맛집 이미지 준비 중";

const reportReasons = [
  { code: "FAKE_INFO", label: "잘못된 정보" },
  { code: "FRANCHISE", label: "프랜차이즈로 보여요" },
  { code: "SPAM", label: "스팸 또는 홍보" },
  { code: "ABUSE", label: "부적절한 내용" },
  { code: "OTHER", label: "기타" },
];

export default function DetailPage({
  wishedIds,
  onToggleWish,
  onPlaceUpdated,
}: Props) {
  const { id } = useParams();
  const navigate = useNavigate();
  const placeId = Number(id);
  const invalidPlaceId = !Number.isFinite(placeId);
  const authenticated = hasStoredAccessToken();

  const [place, setPlace] = useState<Place | null>(null);
  const [recommendationPolicy, setRecommendationPolicy] =
    useState<RecommendationPolicy | null>(null);
  const [visitPolicy, setVisitPolicy] = useState<VisitPolicy | null>(null);
  const [comments, setComments] = useState<CommentItem[]>([]);
  const [history, setHistory] = useState<PlaceRankingHistory | null>(null);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [busyAction, setBusyAction] = useState<BusyAction>(null);
  const [commentText, setCommentText] = useState("");
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [reportTarget, setReportTarget] = useState<{
    type: ReportTargetType;
    id: number;
  } | null>(null);
  const [reportReason, setReportReason] = useState(reportReasons[0].code);
  const [reportText, setReportText] = useState("");

  const refreshPlace = useCallback(async () => {
    const nextPlace = await getPlace(placeId);
    const nextPlaceWithWish = {
      ...nextPlace,
      isWished: wishedIds.has(nextPlace.id),
    };
    setPlace(nextPlaceWithWish);
    onPlaceUpdated(nextPlaceWithWish);
    return nextPlaceWithWish;
  }, [onPlaceUpdated, placeId, wishedIds]);

  const loadParticipation = useCallback(async () => {
    const [nextRecommendationPolicy, nextVisitPolicy, nextComments, nextHistory] =
      await Promise.all([
        settle(authenticated ? getRecommendationPolicy(placeId) : null),
        settle(authenticated ? getVisitPolicy(placeId) : null),
        settle(getPlaceComments(placeId)),
        settle(getPlaceRankingHistory(placeId)),
      ]);

    setRecommendationPolicy(
      nextRecommendationPolicy.ok ? nextRecommendationPolicy.value : null,
    );
    setVisitPolicy(nextVisitPolicy.ok ? nextVisitPolicy.value : null);
    setComments(nextComments.ok ? nextComments.value : []);
    setHistory(nextHistory.ok ? nextHistory.value : null);
  }, [authenticated, placeId]);

  useEffect(() => {
    if (invalidPlaceId) {
      return;
    }

    let mounted = true;
    Promise.resolve()
      .then(async () => {
        if (mounted) {
          setLoading(true);
          setErrorMessage(null);
        }
        await refreshPlace();
        await loadParticipation();
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
  }, [invalidPlaceId, loadParticipation, refreshPlace]);

  const isWished = place ? wishedIds.has(place.id) : false;
  const alreadyRecommended =
    recommendationPolicy?.reason === "ALREADY_RECOMMENDED";

  const recommendationStatusText = useMemo(() => {
    if (!authenticated) {
      return LOGIN_REQUIRED;
    }
    if (!recommendationPolicy) {
      return "추천 가능 여부를 확인하고 있어요.";
    }
    if (alreadyRecommended) {
      return "이미 추천한 맛집이에요.";
    }
    if (!recommendationPolicy.canRecommend) {
      return recommendationPolicy.reason ?? "지금은 추천할 수 없어요.";
    }
    return `오늘 추천 가능 ${recommendationPolicy.dailyRemainingCount}회`;
  }, [alreadyRecommended, authenticated, recommendationPolicy]);

  const visitStatusText = useMemo(() => {
    if (!authenticated) {
      return LOGIN_REQUIRED;
    }
    if (!visitPolicy) {
      return "방문 인증 가능 여부를 확인하고 있어요.";
    }
    if (visitPolicy.canVisitNow) {
      return `방문 인증 반경 ${visitPolicy.radiusMeter}m`;
    }
    if (visitPolicy.cooldownUntil) {
      return `다음 방문 가능: ${formatDateTime(visitPolicy.cooldownUntil)}`;
    }
    return "지금은 방문 인증할 수 없어요.";
  }, [authenticated, visitPolicy]);

  const handleRecommend = async () => {
    if (!place) {
      return;
    }
    if (!authenticated) {
      setMessage(LOGIN_REQUIRED);
      return;
    }

    setBusyAction("recommend");
    setMessage(null);
    try {
      const result = alreadyRecommended
        ? await cancelRecommendation(place.id)
        : await recommendPlace(place.id);
      const nextPlace = {
        ...place,
        recommendCount: result.recommendCount,
        isWished,
      };
      setPlace(nextPlace);
      onPlaceUpdated(nextPlace);
      await loadParticipation();
      setMessage(result.recommended ? "추천을 남겼어요." : "추천을 취소했어요.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "추천을 처리하지 못했어요."));
    } finally {
      setBusyAction(null);
    }
  };

  const handleVisit = async () => {
    if (!place) {
      return;
    }
    if (!authenticated) {
      setMessage(LOGIN_REQUIRED);
      return;
    }
    if (!navigator.geolocation) {
      setMessage(GEOLOCATION_UNAVAILABLE);
      return;
    }

    setBusyAction("visit");
    setMessage(null);
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          const result = await verifyVisit(
            place.id,
            position.coords.latitude,
            position.coords.longitude,
          );
          const nextPlace = {
            ...place,
            visitCount: result.visitCount,
            isWished,
          };
          setPlace(nextPlace);
          onPlaceUpdated(nextPlace);
          await loadParticipation();
          setMessage(
            `방문 인증 완료: ${Math.round(result.distanceMeter)}m, 경험치 +${result.expGained}`,
          );
        } catch (error) {
          setMessage(getApiErrorMessage(error, "방문 인증을 처리하지 못했어요."));
        } finally {
          setBusyAction(null);
        }
      },
      () => {
        setBusyAction(null);
        setMessage("위치 권한을 허용하면 방문 인증을 할 수 있어요.");
      },
      { enableHighAccuracy: true, timeout: 10000 },
    );
  };

  const handleCommentSubmit = async () => {
    if (!authenticated) {
      setMessage(LOGIN_REQUIRED);
      return;
    }
    const content = commentText.trim();
    if (!content) {
      setMessage("댓글 내용을 입력해주세요.");
      return;
    }

    setBusyAction("comment");
    setMessage(null);
    try {
      if (editingCommentId) {
        await updateComment(editingCommentId, content);
        setMessage("댓글을 수정했어요.");
      } else {
        await createComment(placeId, content);
        setMessage("댓글을 남겼어요.");
      }
      setCommentText("");
      setEditingCommentId(null);
      await refreshPlace();
      await loadParticipation();
    } catch (error) {
      setMessage(getApiErrorMessage(error, "댓글을 저장하지 못했어요."));
    } finally {
      setBusyAction(null);
    }
  };

  const handleCommentDelete = async (commentId: number) => {
    setBusyAction("comment-delete");
    setMessage(null);
    try {
      await deleteComment(commentId);
      setMessage("댓글을 삭제했어요.");
      await refreshPlace();
      await loadParticipation();
    } catch (error) {
      setMessage(getApiErrorMessage(error, "댓글을 삭제하지 못했어요."));
    } finally {
      setBusyAction(null);
    }
  };

  const handleReport = async () => {
    if (!reportTarget) {
      return;
    }
    if (!authenticated) {
      setMessage(LOGIN_REQUIRED);
      return;
    }

    setBusyAction("report");
    setMessage(null);
    try {
      await createReport({
        targetType: reportTarget.type,
        targetId: reportTarget.id,
        reasonCode: reportReason,
        reasonText: reportText.trim() || null,
      });
      setReportTarget(null);
      setReportText("");
      setReportReason(reportReasons[0].code);
      setMessage("신고를 접수했어요. 운영자가 확인할게요.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "신고를 접수하지 못했어요."));
    } finally {
      setBusyAction(null);
    }
  };

  if (invalidPlaceId) {
    return (
      <Shell
        message={PLACE_NOT_FOUND}
        actionLabel="홈으로 돌아가기"
        onAction={() => navigate("/")}
      />
    );
  }

  if (loading) {
    return <Shell message="맛집 정보를 불러오는 중이에요." />;
  }

  if (errorMessage || !place) {
    return (
      <Shell
        message={errorMessage ?? PLACE_NOT_FOUND}
        actionLabel="홈으로 돌아가기"
        onAction={() => navigate("/")}
      />
    );
  }

  return (
    <div className="min-h-screen bg-neutral-100">
      <main className="mx-auto min-h-screen max-w-[430px] bg-[#fffaf0] pb-24">
        <section className="relative h-64 bg-[#fff1bf]">
          {place.imageUrl ? (
            <img
              src={place.imageUrl}
              alt={`${place.title} 대표 이미지`}
              className="h-full w-full object-cover"
            />
          ) : (
            <div className="flex h-full items-center justify-center px-6 text-center text-sm font-semibold text-[#8a6315]">
              {PLACE_IMAGE_FALLBACK}
            </div>
          )}
          <button
            type="button"
            aria-label="뒤로가기"
            onClick={() => navigate(-1)}
            className="absolute left-4 top-5 flex h-10 w-10 items-center justify-center rounded-full bg-white/95 text-xl font-bold text-[#2b210f] shadow-sm transition active:scale-95"
          >
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M15 18l-6-6 6-6" />
            </svg>
          </button>
          <button
            type="button"
            aria-label={isWished ? "찜 해제" : "찜하기"}
            onClick={() => onToggleWish(place.id)}
            className="absolute right-4 top-5 flex h-10 w-10 items-center justify-center rounded-full border border-[#f6d365] bg-[#fff8df] text-[20px] font-black text-[#8a6315] shadow-sm transition active:scale-95"
          >
            {isWished ? "★" : "☆"}
          </button>
        </section>

        <div className="px-4">
          <section className="-mt-8 rounded-3xl bg-white p-5 shadow-sm">
            <p className="text-xs font-semibold text-[#d99a00]">
              {place.regionName || "우리 동네"}
            </p>
            <h1 className="mt-1 text-2xl font-bold text-[#2b210f]">
              {place.title}
            </h1>
            <p className="mt-3 text-sm leading-6 text-gray-600">
              {place.desc || "동네 사람들이 추천한 꿀맛집이에요."}
            </p>
            <div className="mt-4 grid grid-cols-3 gap-2 text-center">
              <Metric label="별점" value={`${place.rating || 0}`} />
              <Metric label="추천" value={`${place.recommendCount}`} />
              <Metric label="댓글" value={`${place.commentCount}`} />
            </div>
          </section>

          <section className="mt-5 rounded-3xl bg-white p-4 shadow-sm">
            <SectionHeader title="위치 정보" desc="방문 전에 주소를 확인해보세요." />
            <p className="mt-3 rounded-2xl bg-[#fffaf0] p-3 text-sm leading-6 text-[#2b210f]">
              {place.address || "주소 정보가 아직 준비되지 않았어요."}
            </p>
            <PlaceLocationMap place={place} />
          </section>

          <section className="mt-5 rounded-3xl bg-white p-4 shadow-sm">
            <SectionHeader
              title="참여"
              desc="추천과 방문 인증으로 동네 랭킹에 힘을 보탤 수 있어요."
            />
            <div className="mt-4 grid grid-cols-2 gap-3">
              <ActionButton
                label={alreadyRecommended ? "추천 취소" : "추천하기"}
                status={recommendationStatusText}
                busy={busyAction === "recommend"}
                onClick={handleRecommend}
              />
              <ActionButton
                label="방문 인증"
                status={visitStatusText}
                busy={busyAction === "visit"}
                onClick={handleVisit}
              />
            </div>
          </section>

          <section className="mt-5 rounded-3xl bg-white p-4 shadow-sm">
            <div className="flex items-start justify-between gap-3">
              <SectionHeader
                title="신고하기"
                desc="잘못된 정보가 있다면 운영자에게 알려주세요."
              />
              <button
                type="button"
                onClick={() =>
                  setReportTarget({ type: "PLACE", id: place.id })
                }
                className="shrink-0 whitespace-nowrap rounded-full border border-gray-200 px-4 py-2 text-sm font-semibold text-gray-600"
              >
                신고
              </button>
            </div>
          </section>

          <section className="mt-5 rounded-3xl bg-white p-4 shadow-sm">
            <SectionHeader title="댓글" desc="동네 이웃들의 한마디를 확인해보세요." />
            <div className="mt-4 flex flex-col gap-3">
              <textarea
                value={commentText}
                onChange={(event) => setCommentText(event.target.value)}
                disabled={!authenticated}
                placeholder={
                  authenticated
                    ? "이 맛집의 좋은 점을 짧게 남겨주세요."
                    : "로그인하면 댓글을 남길 수 있어요."
                }
                className="min-h-24 rounded-2xl border border-gray-200 bg-[#fffaf0] p-3 text-sm leading-6 outline-none focus:border-[#f6b800] disabled:opacity-60"
              />
              <button
                type="button"
                onClick={handleCommentSubmit}
                disabled={busyAction !== null || !authenticated}
                className="h-11 rounded-full bg-[#f6b800] text-sm font-semibold text-[#2b210f] disabled:cursor-not-allowed disabled:opacity-50"
              >
                {busyAction === "comment"
                  ? "저장 중..."
                  : editingCommentId
                    ? "댓글 수정"
                    : "댓글 남기기"}
              </button>
            </div>

            {comments.length === 0 ? (
              <p className="mt-5 text-sm leading-6 text-gray-500">
                아직 남겨진 댓글이 없어요.
              </p>
            ) : (
              <div className="mt-5 space-y-3">
                {comments.map((comment) => (
                  <article
                    key={comment.commentId}
                    className="rounded-2xl bg-[#fffaf0] p-3 text-sm"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="font-bold text-[#2b210f]">
                          {comment.nickname || "꿀벌님"}
                        </p>
                        <p className="mt-1 text-xs text-gray-400">
                          {formatDateTime(comment.createdAt)}
                        </p>
                      </div>
                      <div className="flex shrink-0 gap-2">
                        <button
                          type="button"
                          onClick={() => {
                            setEditingCommentId(comment.commentId);
                            setCommentText(comment.content);
                          }}
                          className="text-xs font-semibold text-gray-500"
                        >
                          수정
                        </button>
                        <button
                          type="button"
                          onClick={() => handleCommentDelete(comment.commentId)}
                          disabled={busyAction !== null}
                          className="text-xs font-semibold text-red-500 disabled:opacity-50"
                        >
                          삭제
                        </button>
                        <button
                          type="button"
                          onClick={() =>
                            setReportTarget({
                              type: "COMMENT",
                              id: comment.commentId,
                            })
                          }
                          className="text-xs font-semibold text-gray-500"
                        >
                          신고
                        </button>
                      </div>
                    </div>
                    <p className="mt-3 leading-6 text-gray-700">
                      {comment.content}
                    </p>
                  </article>
                ))}
              </div>
            )}
          </section>

          <section className="mt-5 rounded-3xl bg-white p-4 shadow-sm">
            <SectionHeader
              title="랭킹 히스토리"
              desc="확정된 시즌 랭킹 기록을 모아봤어요."
            />
            {!history || history.items.length === 0 ? (
              <p className="mt-4 text-sm leading-6 text-gray-500">
                아직 확정된 랭킹 기록이 없어요.
              </p>
            ) : (
              <div className="mt-4 space-y-3">
                {history.items.slice(0, 5).map((item) => (
                  <div
                    key={`${item.seasonId}-${item.regionType}-${item.regionId}`}
                    className="rounded-2xl bg-[#fffaf0] p-3 text-sm"
                  >
                    <div className="flex items-center justify-between gap-3">
                      <p className="font-bold text-[#2b210f]">
                        {item.seasonName}
                      </p>
                      <span className="rounded-full bg-white px-3 py-1 text-xs font-semibold text-[#8a6315]">
                        {regionTypeLabel(item.regionType)} {item.rank}위
                      </span>
                    </div>
                    <p className="mt-2 text-xs text-gray-500">
                      별 {item.starLevel} · {item.totalScore}점
                    </p>
                  </div>
                ))}
              </div>
            )}
          </section>

          {message && (
            <p className="mt-5 rounded-3xl bg-white px-4 py-3 text-sm font-semibold leading-6 text-[#5c3b13] shadow-sm">
              {message}
            </p>
          )}
        </div>

        {reportTarget && (
          <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/30 px-4">
            <section className="w-full max-w-[430px] rounded-t-3xl bg-white p-4 shadow-sm">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-lg font-bold text-[#2b210f]">
                    {reportTargetLabel(reportTarget.type)} 신고
                  </h2>
                  <p className="mt-1 text-sm text-gray-500">
                    운영자가 확인할 수 있도록 사유를 남겨주세요.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setReportTarget(null)}
                  className="rounded-full border border-gray-200 px-3 py-1 text-sm font-semibold text-gray-500"
                >
                  닫기
                </button>
              </div>

              <label className="mt-4 block text-sm font-semibold text-[#2b210f]">
                신고 사유
                <select
                  value={reportReason}
                  onChange={(event) => setReportReason(event.target.value)}
                  className="mt-2 h-11 w-full rounded-2xl border border-gray-200 bg-[#fffaf0] px-3 text-sm outline-none focus:border-[#f6b800]"
                >
                  {reportReasons.map((reason) => (
                    <option key={reason.code} value={reason.code}>
                      {reason.label}
                    </option>
                  ))}
                </select>
              </label>

              <textarea
                value={reportText}
                onChange={(event) => setReportText(event.target.value)}
                placeholder="운영자가 확인할 내용을 입력해주세요."
                className="mt-3 min-h-24 w-full rounded-2xl border border-gray-200 bg-[#fffaf0] p-3 text-sm leading-6 outline-none focus:border-[#f6b800]"
              />

              <button
                type="button"
                onClick={handleReport}
                disabled={busyAction !== null}
                className="mt-3 h-11 w-full rounded-full bg-[#f6b800] text-sm font-semibold text-[#2b210f] disabled:cursor-not-allowed disabled:opacity-50"
              >
                {busyAction === "report" ? "접수 중..." : "신고 접수"}
              </button>
            </section>
          </div>
        )}
      </main>
    </div>
  );
}

function Shell({
  message,
  actionLabel,
  onAction,
}: {
  message: string;
  actionLabel?: string;
  onAction?: () => void;
}) {
  return (
    <div className="min-h-screen bg-neutral-100">
      <main className="mx-auto flex min-h-screen max-w-[430px] items-center justify-center bg-[#fffaf0] px-4 pb-24">
        <section className="w-full rounded-3xl bg-white p-5 text-center shadow-sm">
          <p className="text-sm font-semibold leading-6 text-[#2b210f]">
            {message}
          </p>
          {actionLabel && onAction && (
            <button
              type="button"
              onClick={onAction}
              className="mt-4 h-11 rounded-full bg-[#f6b800] px-5 text-sm font-semibold text-[#2b210f]"
            >
              {actionLabel}
            </button>
          )}
        </section>
      </main>
    </div>
  );
}

function SectionHeader({ title, desc }: { title: string; desc: string }) {
  return (
    <div>
      <h2 className="text-lg font-bold text-[#2b210f]">{title}</h2>
      <p className="mt-1 text-sm leading-5 text-gray-500">{desc}</p>
    </div>
  );
}

function PlaceLocationMap({ place }: { place: Place }) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<KakaoMap | null>(null);
  const markerRef = useRef<KakaoMarker | null>(null);
  const [loadFailed, setLoadFailed] = useState(false);
  const appKey = getKakaoMapJavaScriptKey();
  const hasCoordinates =
    Number.isFinite(place.latitude) && Number.isFinite(place.longitude);

  useEffect(() => {
    if (!appKey || !hasCoordinates || !containerRef.current) {
      return;
    }

    let canceled = false;
    setLoadFailed(false);

    loadKakaoMapSdk(appKey)
      .then((kakao) => {
        if (canceled || !containerRef.current) {
          return;
        }

        const center = new kakao.maps.LatLng(place.latitude, place.longitude);
        const map =
          mapRef.current ??
          new kakao.maps.Map(containerRef.current, {
            center,
            level: 4,
          });

        markerRef.current?.setMap(null);
        markerRef.current = new kakao.maps.Marker({
          map,
          position: center,
          title: place.title,
        });
        map.setCenter(center);
        mapRef.current = map;
      })
      .catch(() => {
        if (!canceled) {
          mapRef.current = null;
          markerRef.current = null;
          setLoadFailed(true);
        }
      });

    return () => {
      canceled = true;
    };
  }, [appKey, hasCoordinates, place.latitude, place.longitude, place.title]);

  if (!hasCoordinates) {
    return <MapStatus title="위치 정보가 준비되지 않았어요." />;
  }

  if (!appKey) {
    return <MapStatus title="카카오맵 키가 설정되지 않았어요." />;
  }

  if (loadFailed) {
    return <MapStatus title="지도를 불러오지 못했어요." />;
  }

  return (
    <div
      ref={containerRef}
      aria-label={`${place.title} 지도`}
      className="mt-3 h-48 overflow-hidden rounded-2xl bg-[#eaf2e4]"
    />
  );
}

function MapStatus({ title }: { title: string }) {
  return (
    <div className="mt-3 flex h-48 items-center justify-center rounded-2xl bg-[#eaf2e4] px-4 text-center">
      <p className="text-sm font-semibold text-[#2b210f]">{title}</p>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl bg-[#fffaf0] p-3">
      <p className="text-lg font-bold text-[#2b210f]">{value}</p>
      <p className="mt-1 text-xs font-semibold text-gray-500">{label}</p>
    </div>
  );
}

function ActionButton({
  label,
  status,
  busy,
  onClick,
}: {
  label: string;
  status: string;
  busy: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={busy}
      className="rounded-2xl bg-[#fffaf0] p-3 text-left transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
    >
      <span className="block text-sm font-bold text-[#2b210f]">
        {busy ? "처리 중..." : label}
      </span>
      <span className="mt-2 block text-xs leading-5 text-gray-500">
        {status}
      </span>
    </button>
  );
}

function reportTargetLabel(value: ReportTargetType) {
  switch (value) {
    case "PLACE":
      return "맛집";
    case "COMMENT":
      return "댓글";
    case "USER":
      return "사용자";
  }
}

function regionTypeLabel(value: RankingRegionType) {
  switch (value) {
    case "dong":
      return "동네";
    case "district":
      return "구";
    case "city":
      return "시";
  }
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

async function settle<T>(promise: Promise<T> | null) {
  if (!promise) {
    return { ok: false as const };
  }

  try {
    return { ok: true as const, value: await promise };
  } catch {
    return { ok: false as const };
  }
}
