import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  adjustAdminUserRecommendWeight,
  adjustAdminUserTrust,
  createAdminUserSanction,
  getAdminUser,
  getAdminUsers,
  type AdminTrustGrade,
  type AdminUserDetail,
  type AdminUserListItem,
  type AdminUserRole,
  type AdminUserSanctionType,
  type AdminUserStatus,
} from "../api/adminApi";
import { getApiErrorMessage } from "../api/http";

const LOAD_ERROR = "사용자 목록을 불러오지 못했습니다.";
const FILTER_ALL = "ALL";

const ROLE_FILTERS: { value: AdminUserRole | typeof FILTER_ALL; label: string }[] =
  [
    { value: FILTER_ALL, label: "전체" },
    { value: "USER", label: "일반" },
    { value: "ADMIN", label: "관리자" },
    { value: "SUPER_ADMIN", label: "최고 관리자" },
  ];

const STATUS_FILTERS: {
  value: AdminUserStatus | typeof FILTER_ALL;
  label: string;
}[] = [
  { value: FILTER_ALL, label: "전체" },
  { value: "ACTIVE", label: "활성" },
  { value: "SUSPENDED", label: "정지" },
  { value: "DELETED", label: "삭제" },
];

const TRUST_GRADES: { value: AdminTrustGrade; label: string }[] = [
  { value: "SEED_BEE", label: "씨앗 벌" },
  { value: "VERIFIED_BEE", label: "인증 벌" },
  { value: "LOCAL_BEE", label: "동네 벌" },
  { value: "ACTIVE_BEE", label: "활동 벌" },
  { value: "TRUSTED_BEE", label: "신뢰 벌" },
  { value: "INFLUENCER_BEE", label: "영향력 벌" },
];

const SANCTION_TYPES: { value: AdminUserSanctionType; label: string }[] = [
  { value: "WARNING", label: "경고" },
  { value: "TEMPORARY_RESTRICTION", label: "일시 제한" },
  { value: "PERMANENT_RESTRICTION", label: "영구 제한" },
];

export default function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUserListItem[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [selectedUser, setSelectedUser] = useState<AdminUserDetail | null>(null);
  const [roleFilter, setRoleFilter] = useState<AdminUserRole | typeof FILTER_ALL>(
    FILTER_ALL,
  );
  const [statusFilter, setStatusFilter] = useState<
    AdminUserStatus | typeof FILTER_ALL
  >(FILTER_ALL);
  const [trustScore, setTrustScore] = useState("");
  const [trustGrade, setTrustGrade] = useState<AdminTrustGrade>("SEED_BEE");
  const [recommendWeight, setRecommendWeight] = useState("");
  const [sanctionType, setSanctionType] =
    useState<AdminUserSanctionType>("WARNING");
  const [sanctionReason, setSanctionReason] = useState("");
  const [sanctionStartAt, setSanctionStartAt] = useState("");
  const [sanctionEndAt, setSanctionEndAt] = useState("");
  const [memo, setMemo] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  function fillFormsFromUser(user: AdminUserDetail) {
    setTrustScore(String(user.trust?.trustScore ?? 0));
    setTrustGrade(user.trust?.trustGrade ?? "SEED_BEE");
    setRecommendWeight(String(user.trust?.recommendWeight ?? 1));
    setSanctionType("WARNING");
    setSanctionReason("");
    setSanctionStartAt("");
    setSanctionEndAt("");
    setMemo("");
  }

  useEffect(() => {
    let mounted = true;
    Promise.resolve()
      .then(() => getAdminUsers())
      .then((nextUsers) => {
        if (!mounted) {
          return;
        }
        setUsers(nextUsers);
        setSelectedUserId(nextUsers[0]?.userId ?? null);
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
    if (!selectedUserId) {
      return;
    }

    let mounted = true;
    Promise.resolve()
      .then(() => getAdminUser(selectedUserId))
      .then((user) => {
        if (!mounted) {
          return;
        }
        setSelectedUser(user);
        fillFormsFromUser(user);
        setMessage(null);
      })
      .catch((error) => {
        if (mounted) {
          setMessage(
            getApiErrorMessage(error, "사용자 상세를 불러오지 못했습니다."),
          );
        }
      });

    return () => {
      mounted = false;
    };
  }, [selectedUserId]);

  const filteredUsers = useMemo(() => {
    return users.filter((user) => {
      const roleMatched = roleFilter === FILTER_ALL || user.role === roleFilter;
      const statusMatched =
        statusFilter === FILTER_ALL || user.status === statusFilter;
      return roleMatched && statusMatched;
    });
  }, [roleFilter, statusFilter, users]);

  const summary = useMemo(() => countUsers(users), [users]);
  const canMutateUser =
    selectedUser?.role === "USER" && selectedUser.status === "ACTIVE";

  const handleSelectUser = (userId: number) => {
    setSelectedUserId(userId);
    setSelectedUser(null);
    setMemo("");
  };

  const refreshSelectedUser = async () => {
    if (!selectedUserId) {
      return null;
    }
    const refreshed = await getAdminUser(selectedUserId);
    setSelectedUser(refreshed);
    fillFormsFromUser(refreshed);
    return refreshed;
  };

  const handleCreateSanction = async () => {
    if (!selectedUser || !canMutateUser) {
      setMessage("활성 일반 사용자만 제재할 수 있습니다.");
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      await createAdminUserSanction(selectedUser.userId, {
        sanctionType,
        reason: sanctionReason.trim() || null,
        startAt: sanctionStartAt || null,
        endAt: sanctionEndAt || null,
        memo: memo.trim() || null,
      });
      await refreshSelectedUser();
      setSanctionReason("");
      setSanctionStartAt("");
      setSanctionEndAt("");
      setMemo("");
      setMessage("사용자 제재를 등록했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "사용자 제재를 등록하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  const handleAdjustTrust = async () => {
    if (!selectedUser || !canMutateUser) {
      setMessage("활성 일반 사용자만 신뢰도를 조정할 수 있습니다.");
      return;
    }

    const nextTrustScore = Number(trustScore);
    if (!Number.isInteger(nextTrustScore) || nextTrustScore < 0) {
      setMessage("신뢰 점수는 0 이상의 정수로 입력해 주세요.");
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      await adjustAdminUserTrust(selectedUser.userId, {
        trustScore: nextTrustScore,
        trustGrade,
        memo: memo.trim() || null,
      });
      await refreshSelectedUser();
      setMemo("");
      setMessage("사용자 신뢰도를 저장했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "신뢰도를 저장하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  const handleAdjustRecommendWeight = async () => {
    if (!selectedUser || !canMutateUser) {
      setMessage("활성 일반 사용자만 추천 가중치를 조정할 수 있습니다.");
      return;
    }

    const nextRecommendWeight = Number(recommendWeight);
    if (!Number.isFinite(nextRecommendWeight) || nextRecommendWeight < 0) {
      setMessage("추천 가중치는 0 이상의 숫자로 입력해 주세요.");
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      await adjustAdminUserRecommendWeight(selectedUser.userId, {
        recommendWeight: nextRecommendWeight,
        memo: memo.trim() || null,
      });
      await refreshSelectedUser();
      setMemo("");
      setMessage("추천 가중치를 저장했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "추천 가중치를 저장하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-10">
      <main className="mx-auto max-w-[1040px] px-5 py-8">
        <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-bold text-[#2f6f5f]">관리자</p>
            <h1 className="mt-1 text-2xl font-bold">사용자 관리</h1>
            <p className="mt-2 text-sm text-gray-500">
              사용자 상태와 신뢰 정보를 확인하고 필요한 운영 조치를 기록합니다.
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
              to="/admin/places"
              className="h-10 rounded-lg border border-emerald-100 px-4 pt-2 text-center text-sm font-bold text-[#2f6f5f]"
            >
              장소 관리
            </Link>
            <Link
              to="/admin/audit-logs"
              className="h-10 rounded-lg border border-emerald-100 px-4 pt-2 text-center text-sm font-bold text-[#2f6f5f]"
            >
              감사 로그
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
            사용자 목록을 불러오는 중입니다.
          </section>
        )}

        {!loading && (
          <div className="mt-6 grid gap-5 lg:grid-cols-[360px_1fr]">
            <section className="rounded-xl bg-white p-4 shadow-sm">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-bold">사용자 목록</h2>
                <span className="text-xs font-semibold text-gray-400">
                  {filteredUsers.length}명
                </span>
              </div>

              <div className="mt-3 grid grid-cols-3 gap-2 text-center text-xs">
                <SummaryCell label="활성" value={summary.active} />
                <SummaryCell label="일반" value={summary.normal} />
                <SummaryCell label="관리자" value={summary.admin} />
              </div>

              <div className="mt-4 grid gap-2 sm:grid-cols-2">
                <label className="text-sm font-semibold">
                  권한
                  <select
                    value={roleFilter}
                    onChange={(event) =>
                      setRoleFilter(
                        event.target.value as AdminUserRole | typeof FILTER_ALL,
                      )
                    }
                    className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                  >
                    {ROLE_FILTERS.map((filter) => (
                      <option key={filter.value} value={filter.value}>
                        {filter.label}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="text-sm font-semibold">
                  상태
                  <select
                    value={statusFilter}
                    onChange={(event) =>
                      setStatusFilter(
                        event.target.value as AdminUserStatus | typeof FILTER_ALL,
                      )
                    }
                    className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                  >
                    {STATUS_FILTERS.map((filter) => (
                      <option key={filter.value} value={filter.value}>
                        {filter.label}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <div className="mt-4 flex max-h-[640px] flex-col gap-3 overflow-y-auto pr-1">
                {filteredUsers.map((user) => (
                  <button
                    key={user.userId}
                    type="button"
                    onClick={() => handleSelectUser(user.userId)}
                    className={`rounded-lg border p-3 text-left text-sm ${
                      selectedUserId === user.userId
                        ? "border-yellow-400 bg-yellow-50"
                        : "border-yellow-100 bg-[#FFFBEB]"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <p className="font-bold">{user.nickname}</p>
                        <p className="mt-1 text-xs text-gray-500">
                          {user.email ?? "이메일 없음"}
                        </p>
                      </div>
                      <span className="rounded-full bg-white px-2 py-1 text-xs font-bold text-[#2f6f5f]">
                        {roleLabel(user.role)}
                      </span>
                    </div>
                    <p className="mt-2 text-xs text-gray-400">
                      {statusLabel(user.status)} ·{" "}
                      {user.phoneVerified ? "전화 인증" : "전화 미인증"} ·{" "}
                      {formatDate(user.createdAt)}
                    </p>
                  </button>
                ))}
              </div>
            </section>

            <section className="rounded-xl bg-white p-5 shadow-sm">
              {!selectedUser && (
                <p className="text-sm text-gray-500">
                  확인할 사용자를 목록에서 선택해 주세요.
                </p>
              )}

              {selectedUser && (
                <div>
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                    <div>
                      <h2 className="text-xl font-bold">
                        {selectedUser.nickname}
                      </h2>
                      <p className="mt-2 text-sm text-gray-500">
                        사용자 #{selectedUser.userId} ·{" "}
                        {selectedUser.email ?? "이메일 없음"}
                      </p>
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <span className="rounded-full bg-yellow-50 px-3 py-1 text-xs font-bold text-[#2f6f5f]">
                        {roleLabel(selectedUser.role)}
                      </span>
                      <span className="rounded-full bg-yellow-50 px-3 py-1 text-xs font-bold text-[#2f6f5f]">
                        {statusLabel(selectedUser.status)}
                      </span>
                    </div>
                  </div>

                  <dl className="mt-5 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-3">
                    <InfoItem label="전화" value={selectedUser.phone ?? "없음"} />
                    <InfoItem
                      label="전화 인증"
                      value={selectedUser.phoneVerified ? "완료" : "미완료"}
                    />
                    <InfoItem
                      label="언어"
                      value={selectedUser.languagePreference ?? "기본값"}
                    />
                    <InfoItem
                      label="마케팅 동의"
                      value={selectedUser.marketingAgreed ? "동의" : "미동의"}
                    />
                    <InfoItem
                      label="가입일"
                      value={formatDate(selectedUser.createdAt)}
                    />
                    <InfoItem
                      label="수정일"
                      value={formatDate(selectedUser.updatedAt)}
                    />
                  </dl>

                  <section className="mt-5 rounded-lg bg-[#FFFBEB] p-4">
                    <h3 className="text-base font-bold">신뢰 정보</h3>
                    {selectedUser.trust ? (
                      <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4">
                        <InfoItem
                          label="신뢰 점수"
                          value={String(selectedUser.trust.trustScore)}
                        />
                        <InfoItem
                          label="신뢰 등급"
                          value={trustGradeLabel(selectedUser.trust.trustGrade)}
                        />
                        <InfoItem
                          label="추천 가중치"
                          value={String(selectedUser.trust.recommendWeight)}
                        />
                        <InfoItem
                          label="제재 횟수"
                          value={String(selectedUser.trust.sanctionCount)}
                        />
                        <InfoItem
                          label="신고 접수"
                          value={String(selectedUser.trust.reportReceivedCount)}
                        />
                        <InfoItem
                          label="신고 확정"
                          value={String(selectedUser.trust.reportConfirmedCount)}
                        />
                        <InfoItem
                          label="지역 인증"
                          value={selectedUser.trust.regionVerified ? "완료" : "미완료"}
                        />
                        <InfoItem
                          label="마지막 평가"
                          value={
                            selectedUser.trust.lastEvaluatedAt
                              ? formatDate(selectedUser.trust.lastEvaluatedAt)
                              : "기록 없음"
                          }
                        />
                      </dl>
                    ) : (
                      <p className="mt-2 text-sm text-gray-500">
                        신뢰 정보가 없습니다.
                      </p>
                    )}
                  </section>

                  <section className="mt-5 rounded-lg bg-[#FFFBEB] p-4">
                    <h3 className="text-base font-bold">성장 정보</h3>
                    {selectedUser.level ? (
                      <dl className="mt-3 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4">
                        <InfoItem
                          label="레벨"
                          value={String(selectedUser.level.level)}
                        />
                        <InfoItem label="EXP" value={String(selectedUser.level.exp)} />
                        <InfoItem
                          label="누적 EXP"
                          value={String(selectedUser.level.totalExp)}
                        />
                        <InfoItem
                          label="랭크 점수"
                          value={String(selectedUser.level.rankScore)}
                        />
                      </dl>
                    ) : (
                      <p className="mt-2 text-sm text-gray-500">
                        성장 정보가 없습니다.
                      </p>
                    )}
                  </section>

                  {!canMutateUser && (
                    <p className="mt-5 rounded-lg bg-red-50 px-4 py-3 text-sm font-semibold text-red-500">
                      제재와 신뢰도 조정은 활성 일반 사용자에게만 적용할 수 있습니다.
                    </p>
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

                  <section className="mt-5 grid gap-4 xl:grid-cols-3">
                    <div className="rounded-lg border border-yellow-100 p-4">
                      <h3 className="text-base font-bold">사용자 제재</h3>
                      <label className="mt-3 block text-sm font-semibold">
                        제재 유형
                        <select
                          value={sanctionType}
                          onChange={(event) =>
                            setSanctionType(
                              event.target.value as AdminUserSanctionType,
                            )
                          }
                          className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                        >
                          {SANCTION_TYPES.map((type) => (
                            <option key={type.value} value={type.value}>
                              {type.label}
                            </option>
                          ))}
                        </select>
                      </label>
                      <label className="mt-3 block text-sm font-semibold">
                        사유
                        <input
                          value={sanctionReason}
                          onChange={(event) =>
                            setSanctionReason(event.target.value)
                          }
                          className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                        />
                      </label>
                      <label className="mt-3 block text-sm font-semibold">
                        시작 시각
                        <input
                          value={sanctionStartAt}
                          onChange={(event) =>
                            setSanctionStartAt(event.target.value)
                          }
                          type="datetime-local"
                          className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                        />
                      </label>
                      <label className="mt-3 block text-sm font-semibold">
                        종료 시각
                        <input
                          value={sanctionEndAt}
                          onChange={(event) =>
                            setSanctionEndAt(event.target.value)
                          }
                          type="datetime-local"
                          className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                        />
                      </label>
                      <button
                        type="button"
                        onClick={handleCreateSanction}
                        disabled={busy || !canMutateUser}
                        className="mt-4 h-10 w-full rounded-lg bg-red-500 text-sm font-bold text-white disabled:opacity-50"
                      >
                        {busy ? "등록 중" : "제재 등록"}
                      </button>
                    </div>

                    <div className="rounded-lg border border-yellow-100 p-4">
                      <h3 className="text-base font-bold">신뢰도 조정</h3>
                      <label className="mt-3 block text-sm font-semibold">
                        신뢰 점수
                        <input
                          value={trustScore}
                          onChange={(event) => setTrustScore(event.target.value)}
                          type="number"
                          min={0}
                          className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                        />
                      </label>
                      <label className="mt-3 block text-sm font-semibold">
                        신뢰 등급
                        <select
                          value={trustGrade}
                          onChange={(event) =>
                            setTrustGrade(event.target.value as AdminTrustGrade)
                          }
                          className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                        >
                          {TRUST_GRADES.map((grade) => (
                            <option key={grade.value} value={grade.value}>
                              {grade.label}
                            </option>
                          ))}
                        </select>
                      </label>
                      <button
                        type="button"
                        onClick={handleAdjustTrust}
                        disabled={busy || !canMutateUser}
                        className="mt-4 h-10 w-full rounded-lg bg-yellow-400 text-sm font-bold text-black disabled:opacity-50"
                      >
                        {busy ? "저장 중" : "신뢰도 저장"}
                      </button>
                    </div>

                    <div className="rounded-lg border border-yellow-100 p-4">
                      <h3 className="text-base font-bold">추천 가중치</h3>
                      <label className="mt-3 block text-sm font-semibold">
                        가중치
                        <input
                          value={recommendWeight}
                          onChange={(event) =>
                            setRecommendWeight(event.target.value)
                          }
                          type="number"
                          min={0}
                          step="0.01"
                          className="mt-2 h-10 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                        />
                      </label>
                      <p className="mt-3 text-xs text-gray-500">
                        추천 영향력 값은 서버의 스키마와 검증 규칙을 따릅니다.
                      </p>
                      <button
                        type="button"
                        onClick={handleAdjustRecommendWeight}
                        disabled={busy || !canMutateUser}
                        className="mt-4 h-10 w-full rounded-lg bg-[#2f6f5f] text-sm font-bold text-white disabled:opacity-50"
                      >
                        {busy ? "저장 중" : "가중치 저장"}
                      </button>
                    </div>
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

function countUsers(users: AdminUserListItem[]) {
  return users.reduce(
    (acc, user) => {
      if (user.status === "ACTIVE") {
        acc.active += 1;
      }
      if (user.role === "USER") {
        acc.normal += 1;
      }
      if (user.role === "ADMIN" || user.role === "SUPER_ADMIN") {
        acc.admin += 1;
      }
      return acc;
    },
    { active: 0, normal: 0, admin: 0 },
  );
}

function roleLabel(value: AdminUserRole) {
  switch (value) {
    case "USER":
      return "일반";
    case "ADMIN":
      return "관리자";
    case "SUPER_ADMIN":
      return "최고 관리자";
  }
}

function statusLabel(value: AdminUserStatus) {
  switch (value) {
    case "ACTIVE":
      return "활성";
    case "SUSPENDED":
      return "정지";
    case "DELETED":
      return "삭제";
  }
}

function trustGradeLabel(value: AdminTrustGrade) {
  return TRUST_GRADES.find((grade) => grade.value === value)?.label ?? value;
}

function formatDate(value: string) {
  return new Date(value).toLocaleString("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}
