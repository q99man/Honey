import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  getPhoneVerificationStatus,
  login,
  logout,
  sendPhoneVerificationCode,
  signup,
  verifyPhoneCode,
} from "../api/authApi";
import { getApiErrorMessage, hasStoredAccessToken } from "../api/http";
import { deletePlace, getMyRegisteredPlaces } from "../api/placeApi";
import {
  getMyRegion,
  getRegionChangePolicy,
  verifyRegion,
  type MyRegion,
  type RegionChangePolicy,
} from "../api/regionApi";
import { getMyReports, type MyReport } from "../api/reportApi";
import {
  getMyActivitySummary,
  getMyProfile,
  getMyStatus,
  type MyActivitySummary,
  type MyProfile,
  type MyStatus,
} from "../api/userApi";
import BottomNav from "../components/BottomNav";
import SpaceCard from "../components/SpaceCard";
import type { Place } from "../types/place";

type AuthMode = "login" | "signup";
type BusyAction =
  | "auth"
  | "phone-send"
  | "phone-verify"
  | "region"
  | "logout"
  | null;

const DEFAULT_SUMMARY: MyActivitySummary = {
  recommendedCount: 0,
  visitCount: 0,
  commentCount: 0,
  registeredPlaceCount: 0,
};

type Props = {
  onPlaceDeleted: (placeId: number) => void;
};

export default function MyPage({ onPlaceDeleted }: Props) {
  const [authMode, setAuthMode] = useState<AuthMode>("login");
  const [profile, setProfile] = useState<MyProfile | null>(null);
  const [status, setStatus] = useState<MyStatus | null>(null);
  const [region, setRegion] = useState<MyRegion | null>(null);
  const [regionPolicy, setRegionPolicy] =
    useState<RegionChangePolicy | null>(null);
  const [myPlaces, setMyPlaces] = useState<Place[]>([]);
  const [myReports, setMyReports] = useState<MyReport[]>([]);
  const [summary, setSummary] =
    useState<MyActivitySummary>(DEFAULT_SUMMARY);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [nickname, setNickname] = useState("");
  const [phone, setPhone] = useState("");
  const [code, setCode] = useState("");
  const [loading, setLoading] = useState(hasStoredAccessToken());
  const [busyAction, setBusyAction] = useState<BusyAction>(null);
  const [deletingPlaceId, setDeletingPlaceId] = useState<number | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadMe = useCallback(async () => {
    const [
      nextProfile,
      nextStatus,
      nextSummary,
      nextRegion,
      nextRegionPolicy,
      nextMyPlaces,
      nextMyReports,
    ] = await Promise.all([
      getMyProfile(),
      getMyStatus(),
      getMyActivitySummary(),
      settle(getMyRegion()),
      settle(getRegionChangePolicy()),
      settle(getMyRegisteredPlaces()),
      settle(getMyReports()),
    ]);
    setProfile(nextProfile);
    setStatus(nextStatus);
    setSummary(nextSummary);
    setRegion(nextRegion.ok ? nextRegion.value : null);
    setRegionPolicy(nextRegionPolicy.ok ? nextRegionPolicy.value : null);
    setMyPlaces(nextMyPlaces.ok ? nextMyPlaces.value : []);
    setMyReports(nextMyReports.ok ? nextMyReports.value : []);
  }, []);

  useEffect(() => {
    if (!hasStoredAccessToken()) {
      return;
    }

    let mounted = true;
    Promise.resolve()
      .then(() => loadMe())
      .catch((error) => {
        if (mounted) {
          setMessage(getApiErrorMessage(error, "내 정보를 불러오지 못했습니다."));
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
  }, [loadMe]);

  const handleAuthSubmit = async () => {
    setBusyAction("auth");
    setMessage(null);
    try {
      if (authMode === "signup") {
        await signup(email.trim(), password, nickname.trim());
      }
      await login(email.trim(), password);
      await loadMe();
      setMessage(
        authMode === "signup"
          ? "회원가입과 로그인을 완료했습니다."
          : "로그인했습니다.",
      );
    } catch (error) {
      setMessage(
        getApiErrorMessage(
          error,
          authMode === "signup"
            ? "회원가입 요청을 처리하지 못했습니다."
            : "로그인 요청을 처리하지 못했습니다.",
        ),
      );
    } finally {
      setBusyAction(null);
    }
  };

  const handleLogout = async () => {
    setBusyAction("logout");
    setMessage(null);
    try {
      await logout();
      setProfile(null);
      setStatus(null);
      setRegion(null);
      setRegionPolicy(null);
      setMyPlaces([]);
      setMyReports([]);
      setSummary(DEFAULT_SUMMARY);
      setPhone("");
      setCode("");
      setMessage("로그아웃했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "로그아웃을 처리하지 못했습니다."));
    } finally {
      setBusyAction(null);
    }
  };

  const handleSendPhoneCode = async () => {
    setBusyAction("phone-send");
    setMessage(null);
    try {
      await sendPhoneVerificationCode(normalizePhone(phone));
      setMessage("인증번호를 보냈습니다.");
    } catch (error) {
      setMessage(
        getApiErrorMessage(error, "인증번호 발송을 처리하지 못했습니다."),
      );
    } finally {
      setBusyAction(null);
    }
  };

  const handleVerifyPhoneCode = async () => {
    setBusyAction("phone-verify");
    setMessage(null);
    try {
      await verifyPhoneCode(normalizePhone(phone), code.trim());
      const phoneStatus = await getPhoneVerificationStatus();
      const nextProfile = profile
        ? { ...profile, phoneVerified: phoneStatus.phoneVerified }
        : null;
      setProfile(nextProfile);
      await loadMe();
      setMessage("전화 인증을 완료했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "전화 인증을 처리하지 못했습니다."));
    } finally {
      setBusyAction(null);
    }
  };

  const handleVerifyRegion = async () => {
    setBusyAction("region");
    setMessage(null);
    try {
      const position = await getCurrentPosition();
      const nextRegion = await verifyRegion(
        position.coords.latitude,
        position.coords.longitude,
      );
      setRegion(nextRegion);
      await loadMe();
      setMessage(`${formatRegionName(nextRegion)} 동네 인증을 완료했습니다.`);
    } catch (error) {
      setMessage(getApiErrorMessage(error, "동네 인증을 처리하지 못했습니다."));
    } finally {
      setBusyAction(null);
    }
  };

  const handleDeletePlace = async (place: Place) => {
    const confirmed = window.confirm(
      `${place.title} 장소를 삭제할까요?\n삭제한 장소는 목록과 상세 화면에서 보이지 않습니다.`,
    );
    if (!confirmed) {
      return;
    }

    setDeletingPlaceId(place.id);
    setMessage(null);
    try {
      await deletePlace(place.id);
      onPlaceDeleted(place.id);
      await loadMe();
      setMessage("장소가 삭제되었습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "장소 삭제를 처리하지 못했습니다."));
    } finally {
      setDeletingPlaceId(null);
    }
  };

  return (
    <div className="min-h-screen bg-[#FFFBEB] pb-[100px]">
      <main className="px-6 pt-8">
        <h1 className="text-2xl font-bold">마이페이지</h1>

        {loading && (
          <section className="mt-6 rounded-xl bg-white p-5 text-sm text-gray-500 shadow-sm">
            내 정보를 불러오는 중입니다.
          </section>
        )}

        {!loading && !profile && (
          <section className="mt-6 rounded-xl bg-white p-5 text-left shadow-sm">
            <div className="grid grid-cols-2 gap-2">
              {(["login", "signup"] as AuthMode[]).map((mode) => (
                <button
                  key={mode}
                  type="button"
                  onClick={() => setAuthMode(mode)}
                  className={`h-10 rounded-lg text-sm font-bold ${
                    authMode === mode
                      ? "bg-yellow-400 text-black"
                      : "bg-yellow-50 text-gray-500"
                  }`}
                >
                  {mode === "login" ? "로그인" : "회원가입"}
                </button>
              ))}
            </div>

            <div className="mt-5 flex flex-col gap-3">
              <label className="text-sm font-semibold">
                이메일
                <input
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  type="email"
                  className="mt-2 h-11 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                />
              </label>
              <label className="text-sm font-semibold">
                비밀번호
                <input
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  type="password"
                  className="mt-2 h-11 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                />
              </label>
              {authMode === "signup" && (
                <label className="text-sm font-semibold">
                  닉네임
                  <input
                    value={nickname}
                    onChange={(event) => setNickname(event.target.value)}
                    className="mt-2 h-11 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                  />
                </label>
              )}
            </div>

            <button
              type="button"
              onClick={handleAuthSubmit}
              disabled={busyAction !== null}
              className="mt-5 h-12 w-full rounded-lg bg-yellow-400 text-sm font-bold text-black disabled:opacity-50"
            >
              {busyAction === "auth"
                ? "처리 중"
                : authMode === "login"
                  ? "로그인"
                  : "회원가입 후 로그인"}
            </button>
          </section>
        )}

        {!loading && profile && (
          <>
            <section className="mt-6 rounded-xl bg-white p-5 text-left shadow-sm">
              <div className="flex items-center gap-4">
                <div className="flex h-16 w-16 items-center justify-center rounded-full bg-yellow-300 text-lg font-bold">
                  꿀벌
                </div>

                <div className="min-w-0 flex-1">
                  <h2 className="truncate text-lg font-bold">
                    {profile.nickname}
                  </h2>
                  <p className="text-sm text-gray-500">
                    {profile.phoneVerified
                      ? "전화 인증 완료"
                      : "전화 인증이 필요합니다."}
                  </p>
                </div>
              </div>
            </section>

            <section className="mt-5 rounded-xl bg-white p-5 text-left shadow-sm">
              <h2 className="text-lg font-bold">나의 상태</h2>
              <div className="mt-4 grid grid-cols-3 gap-3 text-center">
                <StatusCell label="레벨" value={status?.level ?? 1} />
                <StatusCell label="EXP" value={status?.exp ?? 0} />
                <StatusCell
                  label="신뢰도"
                  value={toTrustGrade(status?.trustGrade)}
                />
              </div>
            </section>

            <section className="mt-5 rounded-xl bg-white p-5 text-left shadow-sm">
              <h2 className="text-lg font-bold">나의 활동</h2>

              <div className="mt-4 grid grid-cols-4 gap-2 text-center">
                <StatusCell label="추천" value={summary.recommendedCount} />
                <StatusCell label="방문" value={summary.visitCount} />
                <StatusCell label="댓글" value={summary.commentCount} />
                <StatusCell label="등록" value={summary.registeredPlaceCount} />
              </div>
            </section>

            <section className="mt-5 rounded-xl bg-white p-5 text-left shadow-sm">
              <h2 className="text-lg font-bold">내 신고 내역</h2>

              {myReports.length === 0 && (
                <p className="mt-4 text-sm text-gray-500">
                  아직 접수한 신고가 없습니다.
                </p>
              )}

              <div className="mt-4 flex flex-col gap-3">
                {myReports.slice(0, 5).map((report) => (
                  <article
                    key={report.reportId}
                    className="rounded-lg border border-yellow-100 bg-[#FFFBEB] p-3 text-sm"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <p className="font-bold">
                          {reportTargetLabel(report.targetType)} #{report.targetId}
                        </p>
                        <p className="mt-1 text-gray-500">
                          {reportReasonLabel(report.reasonCode)}
                        </p>
                      </div>
                      <span className="rounded-full bg-white px-3 py-1 text-xs font-bold text-[#2f6f5f]">
                        {reportStatusLabel(report.status)}
                      </span>
                    </div>

                    {report.reasonText && (
                      <p className="mt-2 text-gray-600">{report.reasonText}</p>
                    )}
                    <p className="mt-2 text-xs text-gray-400">
                      {formatDateTime(report.createdAt)}
                    </p>
                    {report.reviewNote && (
                      <p className="mt-2 text-xs text-gray-500">
                        검토 메모: {report.reviewNote}
                      </p>
                    )}
                  </article>
                ))}
              </div>
            </section>

            <section className="mt-5 rounded-xl bg-white p-5 text-left shadow-sm">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-bold">내가 등록한 장소</h2>
                <Link
                  to="/places/new"
                  className="rounded-lg bg-yellow-400 px-3 py-2 text-sm font-bold text-black"
                >
                  등록
                </Link>
              </div>

              {myPlaces.length === 0 && (
                <p className="mt-4 text-sm text-gray-500">
                  아직 등록한 장소가 없습니다.
                </p>
              )}

              <div className="mt-4 flex flex-col gap-3">
                {myPlaces.slice(0, 5).map((place) => (
                  <article
                    key={place.id}
                    className="rounded-lg border border-yellow-100 bg-[#FFFBEB] p-3"
                  >
                    <Link to={`/places/${place.id}`} className="block">
                      <SpaceCard
                        title={place.title}
                        desc={place.desc}
                        distance={place.regionName}
                        rating={place.rating}
                        price={place.price}
                        imageUrl={place.imageUrl}
                      />
                    </Link>
                    <div className="mt-3 grid grid-cols-2 gap-2">
                      <Link
                        to={`/places/${place.id}/edit`}
                        className="h-10 rounded-lg border border-yellow-300 pt-2 text-center text-sm font-bold"
                      >
                        수정
                      </Link>
                      <button
                        type="button"
                        onClick={() => handleDeletePlace(place)}
                        disabled={deletingPlaceId !== null}
                        className="h-10 rounded-lg border border-red-100 text-sm font-bold text-red-500 disabled:opacity-50"
                      >
                        {deletingPlaceId === place.id ? "삭제 중" : "삭제"}
                      </button>
                    </div>
                  </article>
                ))}
              </div>
            </section>

            <section className="mt-5 rounded-xl bg-white p-5 text-left shadow-sm">
              <h2 className="text-lg font-bold">전화 인증</h2>
              <div className="mt-4 flex flex-col gap-3">
                <input
                  value={phone}
                  onChange={(event) => setPhone(event.target.value)}
                  inputMode="numeric"
                  placeholder="01012345678"
                  className="h-11 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                />
                <div className="grid grid-cols-[1fr_auto] gap-2">
                  <input
                    value={code}
                    onChange={(event) => setCode(event.target.value)}
                    inputMode="numeric"
                    placeholder="인증번호"
                    className="h-11 w-full rounded-lg border border-yellow-100 bg-[#FFFBEB] px-3 text-sm outline-none focus:border-yellow-400"
                  />
                  <button
                    type="button"
                    onClick={handleSendPhoneCode}
                    disabled={busyAction !== null || profile.phoneVerified}
                    className="h-11 rounded-lg border border-yellow-300 px-3 text-sm font-bold disabled:opacity-50"
                  >
                    발송
                  </button>
                </div>
                <button
                  type="button"
                  onClick={handleVerifyPhoneCode}
                  disabled={busyAction !== null || profile.phoneVerified}
                  className="h-11 rounded-lg bg-yellow-400 text-sm font-bold text-black disabled:opacity-50"
                >
                  {busyAction === "phone-verify" ? "확인 중" : "인증 완료"}
                </button>
              </div>
            </section>

            <section className="mt-5 rounded-xl bg-white p-5 text-left shadow-sm">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h2 className="text-lg font-bold">동네 인증</h2>
                  <p className="mt-1 text-sm text-gray-500">
                    {status?.regionVerified
                      ? "동네 인증 완료"
                      : "동네 인증이 필요합니다."}
                  </p>
                </div>
                <span className="rounded-full bg-yellow-50 px-3 py-1 text-xs font-bold text-[#2f6f5f]">
                  {region?.verified ? "인증됨" : "미인증"}
                </span>
              </div>

              <div className="mt-4 rounded-lg bg-[#FFFBEB] p-4">
                <p className="text-xs text-gray-500">현재 인증 동네</p>
                <p className="mt-1 text-base font-bold">
                  {region ? formatRegionName(region) : "아직 인증된 동네가 없습니다."}
                </p>
                <p className="mt-2 text-xs text-gray-500">
                  {formatRegionPolicy(regionPolicy)}
                </p>
              </div>

              <button
                type="button"
                onClick={handleVerifyRegion}
                disabled={busyAction !== null}
                className="mt-4 h-11 w-full rounded-lg bg-yellow-400 text-sm font-bold text-black disabled:opacity-50"
              >
                {busyAction === "region" ? "위치 확인 중" : "현재 위치로 인증"}
              </button>
            </section>

            <section className="mt-5 rounded-xl bg-white p-5 text-left shadow-sm">
              <h2 className="text-lg font-bold">설정</h2>

              <div className="mt-4 flex flex-col gap-4 text-sm">
                <button
                  type="button"
                  onClick={handleLogout}
                  disabled={busyAction !== null}
                  className="text-left text-red-500 disabled:opacity-50"
                >
                  로그아웃
                </button>
              </div>
            </section>
          </>
        )}

        {message && (
          <p className="mt-4 rounded-lg bg-white px-4 py-3 text-sm font-semibold text-[#2f6f5f] shadow-sm">
            {message}
          </p>
        )}
      </main>

      <BottomNav />
    </div>
  );
}

function StatusCell({
  label,
  value,
}: {
  label: string;
  value: string | number;
}) {
  return (
    <div>
      <p className="truncate text-xl font-bold">{value}</p>
      <p className="text-xs text-gray-500">{label}</p>
    </div>
  );
}

function reportTargetLabel(value: MyReport["targetType"]) {
  switch (value) {
    case "PLACE":
      return "장소";
    case "COMMENT":
      return "댓글";
    case "USER":
      return "사용자";
  }
}

function reportStatusLabel(value: MyReport["status"]) {
  switch (value) {
    case "PENDING":
      return "대기";
    case "APPROVED":
      return "승인";
    case "REJECTED":
      return "반려";
  }
}

function reportReasonLabel(value: string) {
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

function normalizePhone(value: string) {
  return value.replaceAll("-", "").trim();
}

async function settle<T>(promise: Promise<T>) {
  try {
    return { ok: true as const, value: await promise };
  } catch {
    return { ok: false as const };
  }
}

function getCurrentPosition() {
  if (!navigator.geolocation) {
    throw new Error("Geolocation is not supported.");
  }

  return new Promise<GeolocationPosition>((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: 10000,
    });
  });
}

function formatRegionName(region: MyRegion) {
  return `${region.cityName} ${region.districtName} ${region.dongName}`;
}

function formatRegionPolicy(policy: RegionChangePolicy | null) {
  if (!policy) {
    return "동네 변경 가능 상태를 불러오지 못했습니다.";
  }

  if (policy.changeAllowed) {
    return `동네 변경 가능 · 기준 주기 ${policy.cooldownDays}일`;
  }

  if (policy.nextAvailableAt) {
    return `다음 변경 가능 시간: ${formatDateTime(policy.nextAvailableAt)}`;
  }

  return "현재는 동네를 변경할 수 없습니다.";
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

function toTrustGrade(value?: string) {
  if (!value) {
    return "기본";
  }
  switch (value) {
    case "VERIFIED_BEE":
      return "인증";
    case "LOCAL_BEE":
      return "동네";
    case "ACTIVE_BEE":
      return "활동";
    case "TRUSTED_BEE":
      return "신뢰";
    case "INFLUENCER_BEE":
      return "영향";
    default:
      return "기본";
  }
}
