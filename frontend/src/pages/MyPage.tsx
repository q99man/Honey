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
import AuthCard, { type AuthMode } from "../components/AuthCard";
import BottomNav from "../components/BottomNav";
import SpaceCard from "../components/SpaceCard";
import type { Place } from "../types/place";

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
  const [summary, setSummary] = useState<MyActivitySummary>(DEFAULT_SUMMARY);
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
          setMessage(
            getApiErrorMessage(
              error,
              "내 정보를 불러오지 못했어요. 잠시 후 다시 시도해주세요.",
            ),
          );
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
          ? "회원가입과 로그인을 완료했어요."
          : "로그인했어요.",
      );
    } catch (error) {
      setMessage(
        getApiErrorMessage(
          error,
          authMode === "signup"
            ? "회원가입 요청을 처리하지 못했어요."
            : "로그인 요청을 처리하지 못했어요.",
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
      setMessage("로그아웃했어요.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "로그아웃을 처리하지 못했어요."));
    } finally {
      setBusyAction(null);
    }
  };

  const handleSendPhoneCode = async () => {
    setBusyAction("phone-send");
    setMessage(null);
    try {
      await sendPhoneVerificationCode(normalizePhone(phone));
      setMessage("인증번호를 보냈어요.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "인증번호 발송을 처리하지 못했어요."));
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
      setProfile(
        profile
          ? { ...profile, phoneVerified: phoneStatus.phoneVerified }
          : null,
      );
      await loadMe();
      setMessage("전화번호 인증이 완료됐어요.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "전화번호 인증을 처리하지 못했어요."));
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
      setMessage(`${formatRegionName(nextRegion)} 동네 인증이 완료됐어요.`);
    } catch (error) {
      setMessage(getApiErrorMessage(error, "동네 인증을 처리하지 못했어요."));
    } finally {
      setBusyAction(null);
    }
  };

  const handleDeletePlace = async (place: Place) => {
    const confirmed = window.confirm(
      `${place.title} 맛집을 삭제할까요?\n삭제한 맛집은 목록과 상세 화면에서 보이지 않아요.`,
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
      setMessage("맛집을 삭제했어요.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "맛집 삭제를 처리하지 못했어요."));
    } finally {
      setDeletingPlaceId(null);
    }
  };

  return (
    <div className="min-h-screen bg-m3-surface">
      <main className="mx-auto min-h-screen max-w-[430px] bg-m3-surface px-4 pb-24 pt-6 text-m3-on-surface">
        <header>
          <p className="text-m3-label-md text-m3-primary">Honeytong</p>
          <h1 className="mt-1 text-m3-title-lg text-m3-on-surface">
            마이페이지
          </h1>
          <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">
            내 활동과 동네 인증 상태를 확인해보세요.
          </p>
        </header>

        {loading && <LoadingCard />}

        {!loading && !profile && (
          <AuthCard
            authMode={authMode}
            setAuthMode={setAuthMode}
            email={email}
            setEmail={setEmail}
            password={password}
            setPassword={setPassword}
            nickname={nickname}
            setNickname={setNickname}
            busy={busyAction === "auth"}
            onSubmit={handleAuthSubmit}
          />
        )}

        {!loading && profile && (
          <div className="mt-6 space-y-5">
            <ProfileSummaryCard
              profile={profile}
              status={status}
              region={region}
            />
            <ActivitySummaryCard summary={summary} />

            <section className="rounded-m3-xl bg-m3-surface-container-lowest p-4 shadow-m3-1">
              <SectionHeader
                title="전화번호 인증"
                desc={
                  profile.phoneVerified
                    ? "믿을 수 있는 동네 활동 준비가 끝났어요."
                    : "추천, 댓글, 맛집 등록을 위해 인증이 필요해요."
                }
              />
              <div className="mt-4 flex flex-col gap-3">
                <input
                  value={phone}
                  onChange={(event) => setPhone(event.target.value)}
                  inputMode="numeric"
                  placeholder="01012345678"
                  className="h-12 w-full rounded-m3-sm border border-m3-outline bg-m3-surface-container-lowest px-4 text-m3-body-md outline-none transition focus:border-m3-primary focus:ring-2 focus:ring-m3-primary/20"
                />
                <div className="grid grid-cols-[1fr_auto] gap-2">
                  <input
                    value={code}
                    onChange={(event) => setCode(event.target.value)}
                    inputMode="numeric"
                    placeholder="인증번호"
                    className="h-12 w-full rounded-m3-sm border border-m3-outline bg-m3-surface-container-lowest px-4 text-m3-body-md outline-none transition focus:border-m3-primary focus:ring-2 focus:ring-m3-primary/20"
                  />
                  <button
                    type="button"
                    onClick={handleSendPhoneCode}
                    disabled={busyAction !== null || profile.phoneVerified}
                    className="h-12 rounded-m3-full border border-m3-outline-variant bg-m3-surface-container-lowest px-4 text-m3-label-lg text-m3-on-surface-variant disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    발송
                  </button>
                </div>
                <button
                  type="button"
                  onClick={handleVerifyPhoneCode}
                  disabled={busyAction !== null || profile.phoneVerified}
                  className="h-12 rounded-m3-full bg-m3-primary text-m3-label-lg text-m3-on-primary shadow-m3-1 transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {busyAction === "phone-verify" ? "확인 중..." : "인증 완료"}
                </button>
              </div>
            </section>

            <section className="rounded-m3-xl bg-m3-surface-container-lowest p-4 shadow-m3-1">
              <div className="flex items-start justify-between gap-3">
                <SectionHeader
                  title="동네 인증"
                  desc={
                    status?.regionVerified
                      ? "인증한 동네 기준으로 활동할 수 있어요."
                      : "현재 위치로 내 동네를 인증해보세요."
                  }
                />
                <Badge tone={region?.verified ? "green" : "gray"}>
                  {region?.verified ? "인증 완료" : "인증 필요"}
                </Badge>
              </div>

              <div className="mt-4 rounded-m3-lg bg-m3-surface-container-low p-4">
                <p className="text-m3-label-md text-m3-on-surface-variant">
                  현재 인증 동네
                </p>
                <p className="mt-1 text-m3-title-md text-m3-on-surface">
                  {region ? formatRegionName(region) : "아직 인증한 동네가 없어요."}
                </p>
                <p className="mt-2 text-m3-body-sm text-m3-on-surface-variant">
                  {formatRegionPolicy(regionPolicy)}
                </p>
              </div>

              <button
                type="button"
                onClick={handleVerifyRegion}
                disabled={busyAction !== null}
                className="mt-4 h-12 w-full rounded-m3-full bg-m3-primary text-m3-label-lg text-m3-on-primary shadow-m3-1 transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
              >
                {busyAction === "region" ? "위치 확인 중..." : "현재 위치로 인증"}
              </button>
            </section>

            <MyPlacesSection
              places={myPlaces}
              deletingPlaceId={deletingPlaceId}
              onDelete={handleDeletePlace}
            />
            <ReportsSection reports={myReports} />

            <section className="rounded-m3-xl bg-m3-surface-container-lowest p-4 shadow-m3-1">
              <SectionHeader
                title="설정"
                desc="계정 이용 상태를 관리할 수 있어요."
              />
              <button
                type="button"
                onClick={handleLogout}
                disabled={busyAction !== null}
                className="mt-4 flex h-12 w-full items-center justify-between rounded-m3-full border border-red-100 bg-red-50 px-4 text-left text-m3-label-lg text-red-600 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <span>
                  {busyAction === "logout" ? "로그아웃 중..." : "로그아웃"}
                </span>
                <span aria-hidden="true">›</span>
              </button>
            </section>
          </div>
        )}

        {message && (
          <p className="mt-5 rounded-m3-lg bg-m3-primary-container px-4 py-3 text-m3-label-lg text-m3-on-primary-container shadow-m3-1">
            {message}
          </p>
        )}
      </main>
      <BottomNav />
    </div>
  );
}

function LoadingCard() {
  return (
    <section className="mt-6 rounded-m3-xl bg-m3-surface-container-lowest p-4 shadow-m3-1">
      <p className="text-m3-label-lg text-m3-on-surface">
        내 정보를 불러오는 중이에요.
      </p>
      <div className="mt-4 space-y-3">
        <div className="h-4 w-3/4 animate-pulse rounded-m3-full bg-m3-surface-container-high" />
        <div className="h-4 w-1/2 animate-pulse rounded-m3-full bg-m3-surface-container-high" />
        <div className="h-16 animate-pulse rounded-m3-lg bg-m3-surface-container-low" />
      </div>
    </section>
  );
}

function ProfileSummaryCard({
  profile,
  status,
  region,
}: {
  profile: MyProfile;
  status: MyStatus | null;
  region: MyRegion | null;
}) {
  const displayName = profile.nickname?.trim() || "꿀벌님";

  return (
    <section className="rounded-m3-xl bg-m3-surface-container-lowest p-4 shadow-m3-1">
      <div className="flex items-center gap-4">
        <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-m3-full bg-m3-primary text-m3-label-lg text-m3-on-primary shadow-m3-1">
          꿀벌
        </div>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="truncate text-m3-title-lg text-m3-on-surface">
              {displayName}
            </h2>
            <Badge tone={profile.phoneVerified ? "green" : "gray"}>
              {profile.phoneVerified ? "전화 인증 완료" : "전화 인증 필요"}
            </Badge>
          </div>
          <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">
            우리 동네 꿀맛집을 찾는 중이에요.
          </p>
        </div>
      </div>

      <div className="mt-4 grid gap-2 text-sm">
        <ProfileInfoRow label="이메일" value="이메일 정보는 아직 제공되지 않아요." />
        <ProfileInfoRow
          label="동네"
          value={region ? formatRegionName(region) : "아직 인증한 동네가 없어요."}
        />
        <ProfileInfoRow
          label="인증 상태"
          value={status?.regionVerified ? "동네 인증 완료" : "동네 인증 필요"}
        />
      </div>
    </section>
  );
}

function ActivitySummaryCard({ summary }: { summary: MyActivitySummary }) {
  const items = [
    { label: "추천한 맛집", value: summary.recommendedCount },
    { label: "방문 인증", value: summary.visitCount },
    { label: "댓글", value: summary.commentCount },
    { label: "등록 맛집", value: summary.registeredPlaceCount },
  ];

  return (
    <section className="rounded-m3-xl bg-m3-surface-container-lowest p-4 shadow-m3-1">
      <SectionHeader
        title="내 활동 요약"
        desc="Honeytong에서 남긴 활동을 모아봤어요."
      />
      <div className="mt-4 grid grid-cols-2 gap-3">
        {items.map((item) => (
          <div key={item.label} className="rounded-m3-lg bg-m3-surface-container-low p-3">
            <p className="text-2xl font-bold text-m3-on-surface">{item.value}</p>
            <p className="mt-1 text-m3-label-md text-m3-on-surface-variant">
              {item.label}
            </p>
          </div>
        ))}
      </div>
    </section>
  );
}

function MyPlacesSection({
  places,
  deletingPlaceId,
  onDelete,
}: {
  places: Place[];
  deletingPlaceId: number | null;
  onDelete: (place: Place) => void;
}) {
  return (
    <section className="rounded-m3-xl bg-m3-surface-container-lowest p-4 shadow-m3-1">
      <div className="flex items-center justify-between gap-3">
        <SectionHeader
          title="내가 등록한 맛집"
          desc="직접 소개한 동네 맛집을 확인해보세요."
        />
        <Link
          to="/places/new"
          className="rounded-m3-full bg-m3-primary px-4 py-2 text-m3-label-lg text-m3-on-primary shadow-m3-1"
        >
          등록
        </Link>
      </div>

      {places.length === 0 ? (
        <EmptyText text="아직 등록한 맛집이 없어요." />
      ) : (
        <div className="mt-4 flex flex-col gap-3">
          {places.slice(0, 5).map((place) => (
            <article
              key={place.id}
              className="rounded-m3-xl border border-m3-outline-variant bg-m3-surface-container-low p-3"
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
                  className="flex h-10 items-center justify-center rounded-m3-full border border-m3-outline-variant bg-m3-surface-container-lowest text-m3-label-lg text-m3-on-surface-variant"
                >
                  수정
                </Link>
                <button
                  type="button"
                  onClick={() => onDelete(place)}
                  disabled={deletingPlaceId !== null}
                  className="h-10 rounded-m3-full border border-red-100 bg-red-50 text-m3-label-lg text-red-600 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {deletingPlaceId === place.id ? "삭제 중..." : "삭제"}
                </button>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function ReportsSection({ reports }: { reports: MyReport[] }) {
  return (
    <section className="rounded-m3-xl bg-m3-surface-container-lowest p-4 shadow-m3-1">
      <SectionHeader
        title="내 신고 내역"
        desc="접수한 신고 처리 상태를 확인할 수 있어요."
      />

      {reports.length === 0 ? (
        <EmptyText text="아직 접수한 신고가 없어요." />
      ) : (
        <div className="mt-4 flex flex-col gap-3">
          {reports.slice(0, 5).map((report) => (
            <article
              key={report.reportId}
              className="rounded-m3-lg border border-m3-outline-variant bg-m3-surface-container-low p-3 text-m3-body-md"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-bold text-m3-on-surface">
                    {reportTargetLabel(report.targetType)} #{report.targetId}
                  </p>
                  <p className="mt-1 text-m3-on-surface-variant">
                    {reportReasonLabel(report.reasonCode)}
                  </p>
                </div>
                <Badge tone={report.status === "APPROVED" ? "green" : "gray"}>
                  {reportStatusLabel(report.status)}
                </Badge>
              </div>
              {report.reasonText && (
                <p className="mt-2 text-m3-on-surface-variant">{report.reasonText}</p>
              )}
              <p className="mt-2 text-m3-body-sm text-m3-on-surface-variant">
                {formatDateTime(report.createdAt)}
              </p>
              {report.reviewNote && (
                <p className="mt-2 text-m3-body-sm text-m3-on-surface-variant">
                  검토 메모: {report.reviewNote}
                </p>
              )}
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

function SectionHeader({ title, desc }: { title: string; desc: string }) {
  return (
    <div>
      <h2 className="text-m3-title-md text-m3-on-surface">{title}</h2>
      <p className="mt-1 text-m3-body-md text-m3-on-surface-variant">{desc}</p>
    </div>
  );
}

function ProfileInfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-start justify-between gap-3 rounded-m3-lg bg-m3-surface-container-low px-3 py-2">
      <span className="shrink-0 text-m3-label-md text-m3-on-surface-variant">
        {label}
      </span>
      <span className="text-right text-m3-label-md text-m3-on-surface">
        {value}
      </span>
    </div>
  );
}

function EmptyText({ text }: { text: string }) {
  return <p className="mt-4 text-m3-body-md text-m3-on-surface-variant">{text}</p>;
}

function Badge({
  children,
  tone,
}: {
  children: string;
  tone: "green" | "gray";
}) {
  const toneClass =
    tone === "green"
      ? "bg-m3-primary text-m3-on-primary"
      : "bg-m3-secondary-container text-m3-on-secondary-container";

  return (
    <span
      className={`inline-flex shrink-0 items-center rounded-m3-full px-3 py-1 text-m3-label-md ${toneClass}`}
    >
      {children}
    </span>
  );
}

function reportTargetLabel(value: MyReport["targetType"]) {
  switch (value) {
    case "PLACE":
      return "맛집";
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
      return "스팸 또는 홍보";
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
    throw new Error("브라우저에서 위치 정보를 사용할 수 없어요.");
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
    return "동네 변경 가능 상태를 불러오지 못했어요.";
  }
  if (policy.changeAllowed) {
    return `동네 변경 가능, 기준 주기 ${policy.cooldownDays}일`;
  }
  if (policy.nextAvailableAt) {
    return `다음 변경 가능 시간: ${formatDateTime(policy.nextAvailableAt)}`;
  }
  return "현재는 동네를 변경할 수 없어요.";
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}
