import { useEffect, useMemo, useState, type ReactNode } from "react";
import { Link } from "react-router-dom";
import {
  adjustAdminPlaceScore,
  changeAdminPlaceApproval,
  changeAdminPlaceExposure,
  changeAdminPlaceFranchiseStatus,
  changeAdminPlaceRankingExclusion,
  getAdminPlace,
  getAdminPlaces,
  type AdminPlaceApprovalStatus,
  type AdminPlaceDetail,
  type AdminPlaceExposureStatus,
  type AdminPlaceFranchiseReviewStatus,
  type AdminPlaceListItem,
} from "../api/adminApi";
import { getApiErrorMessage } from "../api/http";

const LOAD_ERROR = "장소 목록을 불러오지 못했습니다.";
const FILTER_ALL = "ALL";

const APPROVAL_FILTERS: {
  value: AdminPlaceApprovalStatus | typeof FILTER_ALL;
  label: string;
}[] = [
  { value: FILTER_ALL, label: "전체" },
  { value: "APPROVED", label: "승인" },
  { value: "PENDING", label: "대기" },
  { value: "REJECTED", label: "반려" },
];

const EXPOSURE_FILTERS: {
  value: AdminPlaceExposureStatus | typeof FILTER_ALL;
  label: string;
}[] = [
  { value: FILTER_ALL, label: "전체" },
  { value: "VISIBLE", label: "노출" },
  { value: "HIDDEN", label: "숨김" },
];

const FRANCHISE_FILTERS: {
  value: AdminPlaceFranchiseReviewStatus | typeof FILTER_ALL;
  label: string;
}[] = [
  { value: FILTER_ALL, label: "전체" },
  { value: "PENDING", label: "대기" },
  { value: "APPROVED", label: "통과" },
  { value: "REJECTED", label: "반려" },
];

const RANKING_FILTERS = [
  { value: FILTER_ALL, label: "전체" },
  { value: "INCLUDED", label: "랭킹 포함" },
  { value: "EXCLUDED", label: "랭킹 제외" },
] as const;

export default function AdminPlacesPage() {
  const [places, setPlaces] = useState<AdminPlaceListItem[]>([]);
  const [selectedPlaceId, setSelectedPlaceId] = useState<number | null>(null);
  const [selectedPlace, setSelectedPlace] = useState<AdminPlaceDetail | null>(
    null,
  );
  const [keyword, setKeyword] = useState("");
  const [approvalFilter, setApprovalFilter] = useState<
    AdminPlaceApprovalStatus | typeof FILTER_ALL
  >(FILTER_ALL);
  const [exposureFilter, setExposureFilter] = useState<
    AdminPlaceExposureStatus | typeof FILTER_ALL
  >(FILTER_ALL);
  const [franchiseFilter, setFranchiseFilter] = useState<
    AdminPlaceFranchiseReviewStatus | typeof FILTER_ALL
  >(FILTER_ALL);
  const [rankingFilter, setRankingFilter] = useState<
    (typeof RANKING_FILTERS)[number]["value"]
  >(FILTER_ALL);
  const [approvalStatus, setApprovalStatus] =
    useState<AdminPlaceApprovalStatus>("APPROVED");
  const [exposureStatus, setExposureStatus] =
    useState<AdminPlaceExposureStatus>("VISIBLE");
  const [franchiseReviewStatus, setFranchiseReviewStatus] =
    useState<AdminPlaceFranchiseReviewStatus>("PENDING");
  const [rankingExcluded, setRankingExcluded] = useState(false);
  const [scoreDelta, setScoreDelta] = useState("");
  const [memo, setMemo] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  function fillFormsFromPlace(place: AdminPlaceDetail) {
    setApprovalStatus(place.approvalStatus);
    setExposureStatus(place.exposureStatus);
    setFranchiseReviewStatus(place.franchiseReviewStatus);
    setRankingExcluded(place.rankingExcluded);
    setScoreDelta("");
    setMemo("");
  }

  const refreshSelectedPlace = async (placeId: number) => {
    const [nextPlaces, refreshed] = await Promise.all([
      getAdminPlaces(),
      getAdminPlace(placeId),
    ]);
    setPlaces(nextPlaces);
    setSelectedPlace(refreshed);
    fillFormsFromPlace(refreshed);
    return refreshed;
  };

  useEffect(() => {
    let mounted = true;
    Promise.resolve()
      .then(() => getAdminPlaces())
      .then((nextPlaces) => {
        if (!mounted) {
          return;
        }
        setPlaces(nextPlaces);
        setSelectedPlaceId(nextPlaces[0]?.placeId ?? null);
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

  useEffect(() => {
    if (!selectedPlaceId) {
      return;
    }

    let mounted = true;
    Promise.resolve()
      .then(() => getAdminPlace(selectedPlaceId))
      .then((place) => {
        if (!mounted) {
          return;
        }
        setSelectedPlace(place);
        fillFormsFromPlace(place);
        setMessage(null);
      })
      .catch((error) => {
        if (mounted) {
          setMessage(
            getApiErrorMessage(error, "장소 상세를 불러오지 못했습니다."),
          );
        }
      });

    return () => {
      mounted = false;
    };
  }, [selectedPlaceId]);

  const filteredPlaces = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return places.filter((place) => {
      const keywordMatched =
        !normalizedKeyword ||
        [
          place.name,
          place.categoryCode,
          place.createdByNickname,
          place.cityName,
          place.districtName,
          place.dongName,
        ]
          .join(" ")
          .toLowerCase()
          .includes(normalizedKeyword);
      const approvalMatched =
        approvalFilter === FILTER_ALL ||
        place.approvalStatus === approvalFilter;
      const exposureMatched =
        exposureFilter === FILTER_ALL ||
        place.exposureStatus === exposureFilter;
      const franchiseMatched =
        franchiseFilter === FILTER_ALL ||
        place.franchiseReviewStatus === franchiseFilter;
      const rankingMatched =
        rankingFilter === FILTER_ALL ||
        (rankingFilter === "EXCLUDED" && place.rankingExcluded) ||
        (rankingFilter === "INCLUDED" && !place.rankingExcluded);

      return (
        keywordMatched &&
        approvalMatched &&
        exposureMatched &&
        franchiseMatched &&
        rankingMatched
      );
    });
  }, [
    approvalFilter,
    exposureFilter,
    franchiseFilter,
    keyword,
    places,
    rankingFilter,
  ]);

  const summary = useMemo(() => countPlaces(places), [places]);

  const handleSelectPlace = (placeId: number) => {
    setSelectedPlaceId(placeId);
    setSelectedPlace(null);
    setMemo("");
    setScoreDelta("");
  };

  const handleChangeExposure = async () => {
    if (!selectedPlace) {
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      await changeAdminPlaceExposure(selectedPlace.placeId, {
        exposureStatus,
        memo: memo.trim() || null,
      });
      await refreshSelectedPlace(selectedPlace.placeId);
      setMessage("장소 노출 상태를 저장했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "노출 상태를 저장하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  const handleChangeApproval = async () => {
    if (!selectedPlace) {
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      await changeAdminPlaceApproval(selectedPlace.placeId, {
        approvalStatus,
        memo: memo.trim() || null,
      });
      await refreshSelectedPlace(selectedPlace.placeId);
      setMessage("장소 승인 상태를 저장했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "승인 상태를 저장하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  const handleChangeFranchiseStatus = async () => {
    if (!selectedPlace) {
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      await changeAdminPlaceFranchiseStatus(selectedPlace.placeId, {
        franchiseReviewStatus,
        memo: memo.trim() || null,
      });
      await refreshSelectedPlace(selectedPlace.placeId);
      setMessage("프랜차이즈 심사 상태를 저장했습니다.");
    } catch (error) {
      setMessage(
        getApiErrorMessage(error, "프랜차이즈 심사 상태를 저장하지 못했습니다."),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleAdjustScore = async () => {
    if (!selectedPlace) {
      return;
    }

    const nextScoreDelta = Number(scoreDelta);
    if (!Number.isFinite(nextScoreDelta) || nextScoreDelta === 0) {
      setMessage("점수 조정값은 0이 아닌 숫자로 입력해 주세요.");
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      await adjustAdminPlaceScore(selectedPlace.placeId, {
        scoreDelta: nextScoreDelta,
        memo: memo.trim() || null,
      });
      await refreshSelectedPlace(selectedPlace.placeId);
      setMessage("수동 점수 조정을 저장했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "점수 조정을 저장하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  const handleChangeRankingExclusion = async () => {
    if (!selectedPlace) {
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      await changeAdminPlaceRankingExclusion(selectedPlace.placeId, {
        excluded: rankingExcluded,
        memo: memo.trim() || null,
      });
      await refreshSelectedPlace(selectedPlace.placeId);
      setMessage(
        rankingExcluded
          ? "장소를 랭킹 제외 상태로 저장했습니다."
          : "장소를 랭킹 후보로 되돌렸습니다.",
      );
    } catch (error) {
      setMessage(getApiErrorMessage(error, "랭킹 제외 상태를 저장하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-10">
      <main className="mx-auto max-w-[1120px] px-5 py-8">
        <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-bold text-[#2f6f5f]">관리자</p>
            <h1 className="mt-1 text-2xl font-bold">장소 관리</h1>
            <p className="mt-2 text-sm text-gray-500">
              등록된 장소의 운영 상태와 랭킹 참여 상태를 확인합니다.
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Link
              to="/admin"
              className="h-10 rounded-lg border border-yellow-300 px-4 pt-2 text-center text-sm font-bold"
            >
              대시보드
            </Link>
            <Link
              to="/admin/activities"
              className="h-10 rounded-lg border border-emerald-100 px-4 pt-2 text-center text-sm font-bold text-[#2f6f5f]"
            >
              활동 관리
            </Link>
            <Link
              to="/admin/users"
              className="h-10 rounded-lg border border-emerald-100 px-4 pt-2 text-center text-sm font-bold text-[#2f6f5f]"
            >
              사용자 관리
            </Link>
            <Link
              to="/admin/reports"
              className="h-10 rounded-lg border border-red-100 px-4 pt-2 text-center text-sm font-bold text-red-500"
            >
              신고 관리
            </Link>
            <Link
              to="/admin/policies"
              className="h-10 rounded-lg border border-emerald-100 px-4 pt-2 text-center text-sm font-bold text-[#2f6f5f]"
            >
              정책 관리
            </Link>
          </div>
        </header>

        {message && (
          <p className="mt-4 rounded-lg bg-white px-4 py-3 text-sm font-semibold text-[#2f6f5f] shadow-sm">
            {message}
          </p>
        )}

        {loading && (
          <section className="mt-6 rounded-xl bg-white p-5 text-sm text-gray-500 shadow-sm">
            장소 목록을 불러오는 중입니다.
          </section>
        )}

        {!loading && (
          <div className="mt-6 grid gap-5 xl:grid-cols-[380px_1fr]">
            <section className="rounded-xl bg-white p-4 shadow-sm">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-bold">장소 목록</h2>
                <span className="text-xs font-semibold text-gray-400">
                  {filteredPlaces.length}건
                </span>
              </div>

              <div className="mt-3 grid grid-cols-4 gap-2 text-center text-xs">
                <SummaryCell label="전체" value={summary.total} />
                <SummaryCell label="숨김" value={summary.hidden} />
                <SummaryCell label="반려" value={summary.rejected} />
                <SummaryCell label="랭킹 제외" value={summary.rankingExcluded} />
              </div>

              <label className="mt-4 block text-sm font-semibold">
                검색
                <input
                  value={keyword}
                  onChange={(event) => setKeyword(event.target.value)}
                  placeholder="장소명, 등록자, 지역"
                  className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                />
              </label>

              <div className="mt-4 grid gap-2 sm:grid-cols-2">
                <FilterSelect
                  label="승인"
                  value={approvalFilter}
                  options={APPROVAL_FILTERS}
                  onChange={(value) =>
                    setApprovalFilter(
                      value as AdminPlaceApprovalStatus | typeof FILTER_ALL,
                    )
                  }
                />
                <FilterSelect
                  label="노출"
                  value={exposureFilter}
                  options={EXPOSURE_FILTERS}
                  onChange={(value) =>
                    setExposureFilter(
                      value as AdminPlaceExposureStatus | typeof FILTER_ALL,
                    )
                  }
                />
                <FilterSelect
                  label="프랜차이즈"
                  value={franchiseFilter}
                  options={FRANCHISE_FILTERS}
                  onChange={(value) =>
                    setFranchiseFilter(
                      value as AdminPlaceFranchiseReviewStatus | typeof FILTER_ALL,
                    )
                  }
                />
                <FilterSelect
                  label="랭킹"
                  value={rankingFilter}
                  options={RANKING_FILTERS}
                  onChange={(value) =>
                    setRankingFilter(
                      value as (typeof RANKING_FILTERS)[number]["value"],
                    )
                  }
                />
              </div>

              {!loading && filteredPlaces.length === 0 && (
                <p className="mt-5 rounded-lg bg-[#FFFBEB] p-4 text-sm text-gray-500">
                  조건에 맞는 장소가 없습니다.
                </p>
              )}

              <div className="mt-4 flex max-h-[720px] flex-col gap-3 overflow-y-auto pr-1">
                {filteredPlaces.map((place) => (
                  <button
                    key={place.placeId}
                    type="button"
                    onClick={() => handleSelectPlace(place.placeId)}
                    className={`rounded-lg border p-3 text-left text-sm ${
                      selectedPlaceId === place.placeId
                        ? "border-yellow-400 bg-yellow-50"
                        : "border-yellow-100 bg-[#FFFBEB]"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <p className="font-bold">{place.name}</p>
                        <p className="mt-1 text-xs text-gray-500">
                          {place.districtName} {place.dongName} ·{" "}
                          {place.categoryCode}
                        </p>
                      </div>
                      <span
                        className={`rounded-full px-2 py-1 text-xs font-bold ${
                          place.exposureStatus === "VISIBLE"
                            ? "bg-white text-[#2f6f5f]"
                            : "bg-red-50 text-red-500"
                        }`}
                      >
                        {exposureStatusLabel(place.exposureStatus)}
                      </span>
                    </div>
                    <p className="mt-2 text-xs text-gray-400">
                      {approvalStatusLabel(place.approvalStatus)} ·{" "}
                      {place.rankingExcluded ? "랭킹 제외" : "랭킹 포함"} ·{" "}
                      추천 {place.recommendCount} / 방문 {place.visitCount}
                    </p>
                  </button>
                ))}
              </div>
            </section>

            <section className="rounded-xl bg-white p-5 shadow-sm">
              {!selectedPlace && (
                <p className="text-sm text-gray-500">
                  확인할 장소를 목록에서 선택해 주세요.
                </p>
              )}

              {selectedPlace && (
                <div>
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <h2 className="text-xl font-bold">{selectedPlace.name}</h2>
                      <p className="mt-2 text-sm text-gray-500">
                        장소 #{selectedPlace.placeId} · 등록자{" "}
                        {selectedPlace.createdByNickname} ·{" "}
                        {selectedPlace.categoryCode}
                      </p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <StatusPill label={approvalStatusLabel(selectedPlace.approvalStatus)} />
                      <StatusPill label={exposureStatusLabel(selectedPlace.exposureStatus)} />
                      <StatusPill
                        label={
                          selectedPlace.rankingExcluded
                            ? "랭킹 제외"
                            : "랭킹 포함"
                        }
                      />
                    </div>
                  </div>

                  <dl className="mt-5 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4">
                    <InfoItem label="지역" value={regionLabel(selectedPlace)} />
                    <InfoItem
                      label="도로명 주소"
                      value={selectedPlace.addressRoad ?? "기록 없음"}
                    />
                    <InfoItem
                      label="지번 주소"
                      value={selectedPlace.addressJibun ?? "기록 없음"}
                    />
                    <InfoItem
                      label="좌표"
                      value={`${selectedPlace.latitude}, ${selectedPlace.longitude}`}
                    />
                    <InfoItem
                      label="가격대"
                      value={selectedPlace.priceRangeCode ?? "기록 없음"}
                    />
                    <InfoItem
                      label="추천 메뉴"
                      value={selectedPlace.recommendedMenu ?? "기록 없음"}
                    />
                    <InfoItem
                      label="별/등급"
                      value={`${selectedPlace.starLevel}성 · ${selectedPlace.flowerGrade}`}
                    />
                    <InfoItem
                      label="프랜차이즈"
                      value={
                        selectedPlace.franchise
                          ? `예 · ${franchiseStatusLabel(
                              selectedPlace.franchiseReviewStatus,
                            )}`
                          : `아니오 · ${franchiseStatusLabel(
                              selectedPlace.franchiseReviewStatus,
                            )}`
                      }
                    />
                  </dl>

                  <section className="mt-5 rounded-lg bg-[#FFFBEB] p-4">
                    <h3 className="text-base font-bold">장소 소개</h3>
                    <p className="mt-2 text-sm font-semibold">
                      {selectedPlace.shortRecommendation}
                    </p>
                    <p className="mt-2 text-sm text-gray-600">
                      {selectedPlace.featureText || "특징 설명이 없습니다."}
                    </p>
                  </section>

                  <section className="mt-5 rounded-lg bg-[#FFFBEB] p-4">
                    <h3 className="text-base font-bold">활동과 점수</h3>
                    <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4">
                      <InfoItem
                        label="추천"
                        value={formatNumber(selectedPlace.recommendCount)}
                      />
                      <InfoItem
                        label="방문"
                        value={formatNumber(selectedPlace.visitCount)}
                      />
                      <InfoItem
                        label="댓글"
                        value={formatNumber(selectedPlace.commentCount)}
                      />
                      <InfoItem
                        label="참여자"
                        value={formatNumber(selectedPlace.uniqueUserCount)}
                      />
                      <InfoItem
                        label="자동 점수"
                        value={formatDecimal(selectedPlace.scoreTotal)}
                      />
                      <InfoItem
                        label="수동 조정"
                        value={formatDecimal(selectedPlace.manualAdjustmentScore)}
                      />
                      <InfoItem
                        label="최근성"
                        value={formatDecimal(selectedPlace.recentScore)}
                      />
                      <InfoItem
                        label="신뢰 가중"
                        value={formatDecimal(selectedPlace.trustWeightedScore)}
                      />
                    </dl>
                    <p className="mt-3 text-xs text-gray-500">
                      마지막 활동:{" "}
                      {selectedPlace.lastActivityAt
                        ? formatDate(selectedPlace.lastActivityAt)
                        : "기록 없음"}
                    </p>
                  </section>

                  {selectedPlace.imageUrls.length > 0 && (
                    <section className="mt-5 rounded-lg bg-[#FFFBEB] p-4">
                      <h3 className="text-base font-bold">이미지 URL</h3>
                      <div className="mt-3 flex flex-col gap-2 text-sm">
                        {selectedPlace.imageUrls.map((imageUrl) => (
                          <a
                            key={imageUrl}
                            href={imageUrl}
                            target="_blank"
                            rel="noreferrer"
                            className="break-all text-[#2f6f5f] underline"
                          >
                            {imageUrl}
                          </a>
                        ))}
                      </div>
                    </section>
                  )}

                  <section className="mt-5 border-t border-yellow-100 pt-5">
                    <h3 className="text-base font-bold">운영 메모</h3>
                    <textarea
                      value={memo}
                      onChange={(event) => setMemo(event.target.value)}
                      maxLength={255}
                      placeholder="이번 조치의 운영 메모를 입력해 주세요."
                      className="mt-3 min-h-20 w-full resize-none rounded-lg border border-yellow-100 bg-[#FFFBEB] p-3 text-sm outline-none focus:border-yellow-400"
                    />
                  </section>

                  <section className="mt-5 grid gap-4 xl:grid-cols-2">
                    <ActionPanel title="노출 상태">
                      <select
                        value={exposureStatus}
                        onChange={(event) =>
                          setExposureStatus(
                            event.target.value as AdminPlaceExposureStatus,
                          )
                        }
                        className="mt-3 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                      >
                        <option value="VISIBLE">노출</option>
                        <option value="HIDDEN">숨김</option>
                      </select>
                      <ActionButton
                        busy={busy}
                        label="노출 상태 저장"
                        busyLabel="저장 중"
                        onClick={handleChangeExposure}
                      />
                    </ActionPanel>

                    <ActionPanel title="승인 상태">
                      <select
                        value={approvalStatus}
                        onChange={(event) =>
                          setApprovalStatus(
                            event.target.value as AdminPlaceApprovalStatus,
                          )
                        }
                        className="mt-3 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                      >
                        <option value="APPROVED">승인</option>
                        <option value="PENDING">대기</option>
                        <option value="REJECTED">반려</option>
                      </select>
                      <ActionButton
                        busy={busy}
                        label="승인 상태 저장"
                        busyLabel="저장 중"
                        onClick={handleChangeApproval}
                      />
                    </ActionPanel>

                    <ActionPanel title="프랜차이즈 심사">
                      <select
                        value={franchiseReviewStatus}
                        onChange={(event) =>
                          setFranchiseReviewStatus(
                            event.target
                              .value as AdminPlaceFranchiseReviewStatus,
                          )
                        }
                        className="mt-3 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                      >
                        <option value="PENDING">대기</option>
                        <option value="APPROVED">통과</option>
                        <option value="REJECTED">반려</option>
                      </select>
                      <ActionButton
                        busy={busy}
                        label="심사 상태 저장"
                        busyLabel="저장 중"
                        onClick={handleChangeFranchiseStatus}
                      />
                    </ActionPanel>

                    <ActionPanel title="랭킹 제외">
                      <select
                        value={rankingExcluded ? "true" : "false"}
                        onChange={(event) =>
                          setRankingExcluded(event.target.value === "true")
                        }
                        className="mt-3 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                      >
                        <option value="false">랭킹 포함</option>
                        <option value="true">랭킹 제외</option>
                      </select>
                      <ActionButton
                        busy={busy}
                        label="랭킹 상태 저장"
                        busyLabel="저장 중"
                        onClick={handleChangeRankingExclusion}
                      />
                    </ActionPanel>

                    <ActionPanel title="수동 점수 조정">
                      <input
                        value={scoreDelta}
                        onChange={(event) => setScoreDelta(event.target.value)}
                        type="number"
                        step="0.01"
                        placeholder="예: 1.25 또는 -0.50"
                        className="mt-3 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                      />
                      <ActionButton
                        busy={busy}
                        label="점수 조정 저장"
                        busyLabel="저장 중"
                        onClick={handleAdjustScore}
                      />
                    </ActionPanel>
                  </section>
                </div>
              )}
            </section>
          </div>
        )}
      </main>
    </div>
  );
}

function FilterSelect({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string;
  options: readonly { value: string; label: string }[];
  onChange: (value: string) => void;
}) {
  return (
    <label className="text-sm font-semibold">
      {label}
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}

function SummaryCell({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg bg-[#FFFBEB] p-3">
      <p className="text-base font-bold">{value}</p>
      <p className="mt-1 text-gray-500">{label}</p>
    </div>
  );
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-white p-3">
      <dt className="text-xs text-gray-500">{label}</dt>
      <dd className="mt-1 break-words font-semibold">{value}</dd>
    </div>
  );
}

function StatusPill({ label }: { label: string }) {
  return (
    <span className="rounded-full bg-yellow-50 px-3 py-1 text-xs font-bold text-[#2f6f5f]">
      {label}
    </span>
  );
}

function ActionPanel({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  return (
    <div className="rounded-lg border border-yellow-100 p-4">
      <h3 className="text-base font-bold">{title}</h3>
      {children}
    </div>
  );
}

function ActionButton({
  busy,
  label,
  busyLabel,
  onClick,
}: {
  busy: boolean;
  label: string;
  busyLabel: string;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={busy}
      className="mt-4 h-10 w-full rounded-lg bg-yellow-400 text-sm font-bold text-black disabled:opacity-50"
    >
      {busy ? busyLabel : label}
    </button>
  );
}

function countPlaces(places: AdminPlaceListItem[]) {
  return places.reduce(
    (acc, place) => {
      acc.total += 1;
      if (place.exposureStatus === "HIDDEN") {
        acc.hidden += 1;
      }
      if (place.approvalStatus === "REJECTED") {
        acc.rejected += 1;
      }
      if (place.rankingExcluded) {
        acc.rankingExcluded += 1;
      }
      return acc;
    },
    { total: 0, hidden: 0, rejected: 0, rankingExcluded: 0 },
  );
}

function regionLabel(place: AdminPlaceListItem) {
  return `${place.cityName} ${place.districtName} ${place.dongName}`;
}

function approvalStatusLabel(value: AdminPlaceApprovalStatus) {
  switch (value) {
    case "APPROVED":
      return "승인";
    case "PENDING":
      return "대기";
    case "REJECTED":
      return "반려";
  }
}

function exposureStatusLabel(value: AdminPlaceExposureStatus) {
  switch (value) {
    case "VISIBLE":
      return "노출";
    case "HIDDEN":
      return "숨김";
  }
}

function franchiseStatusLabel(value: AdminPlaceFranchiseReviewStatus) {
  switch (value) {
    case "PENDING":
      return "심사 대기";
    case "APPROVED":
      return "심사 통과";
    case "REJECTED":
      return "심사 반려";
  }
}

function formatNumber(value: number) {
  return Number(value).toLocaleString("ko-KR");
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
