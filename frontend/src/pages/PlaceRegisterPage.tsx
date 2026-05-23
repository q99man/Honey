import { useEffect, useMemo, useState } from "react";
import type { FormEvent, ReactNode } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { getApiErrorMessage, hasStoredAccessToken } from "../api/http";
import {
  createPlace,
  getPlace,
  getPlaceEditDetail,
  getPlaceRegistrationPolicy,
  updatePlaceDetails,
  type PlaceEditDetail,
  type PlaceRegistrationPolicy,
} from "../api/placeApi";
import { getMyRegion, type MyRegion } from "../api/regionApi";
import BottomNav from "../components/BottomNav";
import { FOOD_CATEGORIES } from "../constants/foodCategories";
import type { Place } from "../types/place";

type Props = {
  mode?: "create" | "edit";
  onPlaceCreated: () => Promise<void>;
  onPlaceUpdated?: (place: Place) => void;
};

const CATEGORY_OPTIONS = FOOD_CATEGORIES.filter(
  (category) => category.value !== "ALL",
);

const PRICE_OPTIONS = [
  { value: "", label: "선택 안 함" },
  { value: "UNDER_10000", label: "1만원 미만" },
  { value: "10000_20000", label: "1만원~2만원" },
  { value: "20000_30000", label: "2만원~3만원" },
  { value: "OVER_30000", label: "3만원 이상" },
];

const GEOLOCATION_UNAVAILABLE = "현재 위치를 확인할 수 없어요.";
const LOGIN_REQUIRED_CREATE = "로그인하면 장소를 등록할 수 있어요.";
const LOGIN_REQUIRED_EDIT = "로그인하면 장소를 수정할 수 있어요.";
const INPUT_CLASS =
  "w-full rounded-m3-md border border-m3-outline bg-m3-surface-container-lowest px-3 py-2.5 text-m3-body-md text-m3-on-surface outline-none transition placeholder:text-m3-on-surface-variant focus:border-m3-primary focus:ring-2 focus:ring-m3-primary/20";
const BUTTON_CLASS =
  "h-11 rounded-m3-full bg-m3-primary px-4 text-m3-label-lg text-m3-on-primary shadow-m3-1 transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50";
const OUTLINED_BUTTON_CLASS =
  "h-11 rounded-m3-full border border-m3-outline-variant bg-m3-surface-container-lowest px-4 text-m3-label-lg text-m3-on-surface transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50";

export default function PlaceRegisterPage({
  mode = "create",
  onPlaceCreated,
  onPlaceUpdated,
}: Props) {
  const navigate = useNavigate();
  const { id } = useParams();
  const placeId = Number(id);
  const editing = mode === "edit";
  const invalidEditId = editing && !placeId;
  const [region, setRegion] = useState<MyRegion | null>(null);
  const [policy, setPolicy] = useState<PlaceRegistrationPolicy | null>(null);
  const [editPlace, setEditPlace] = useState<PlaceEditDetail | null>(null);
  const [name, setName] = useState("");
  const [categoryCode, setCategoryCode] = useState("KOREAN");
  const [addressRoad, setAddressRoad] = useState("");
  const [addressJibun, setAddressJibun] = useState("");
  const [latitude, setLatitude] = useState("");
  const [longitude, setLongitude] = useState("");
  const [priceRangeCode, setPriceRangeCode] = useState("");
  const [recommendedMenu, setRecommendedMenu] = useState("");
  const [shortRecommendation, setShortRecommendation] = useState("");
  const [featureText, setFeatureText] = useState("");
  const [imageUrlText, setImageUrlText] = useState("");
  const [franchise, setFranchise] = useState(false);
  const [loading, setLoading] = useState(
    hasStoredAccessToken() && !invalidEditId,
  );
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const authenticated = hasStoredAccessToken();
  const imageUrls = useMemo(
    () =>
      imageUrlText
        .split("\n")
        .map((value) => value.trim())
        .filter(Boolean),
    [imageUrlText],
  );

  function applyEditDetail(detail: PlaceEditDetail) {
    setName(detail.name);
    setCategoryCode(detail.categoryCode);
    setAddressRoad(detail.addressRoad ?? "");
    setAddressJibun(detail.addressJibun ?? "");
    setLatitude(String(detail.latitude));
    setLongitude(String(detail.longitude));
    setPriceRangeCode(detail.priceRangeCode ?? "");
    setRecommendedMenu(detail.recommendedMenu ?? "");
    setShortRecommendation(detail.shortRecommendation);
    setFeatureText(detail.featureText ?? "");
    setImageUrlText(detail.imageUrls.join("\n"));
    setFranchise(detail.franchise);
  }

  useEffect(() => {
    if (!authenticated || invalidEditId) {
      return;
    }

    let mounted = true;
    Promise.resolve()
      .then(async () => {
        if (editing) {
          const detail = await getPlaceEditDetail(placeId);
          return { detail, nextRegion: null, nextPolicy: null };
        }

        const [nextRegion, nextPolicy] = await Promise.allSettled([
          getMyRegion(),
          getPlaceRegistrationPolicy(),
        ]);
        return { detail: null, nextRegion, nextPolicy };
      })
      .then(({ detail, nextRegion, nextPolicy }) => {
        if (!mounted) {
          return;
        }

        if (detail) {
          applyEditDetail(detail);
          setEditPlace(detail);
        } else {
          setRegion(nextRegion?.status === "fulfilled" ? nextRegion.value : null);
          setPolicy(nextPolicy?.status === "fulfilled" ? nextPolicy.value : null);
        }
        setMessage(null);
      })
      .catch((error) => {
        if (mounted) {
          setMessage(
            getApiErrorMessage(
              error,
              editing
                ? "수정할 장소 정보를 불러오지 못했습니다."
                : "등록 준비 정보를 불러오지 못했습니다.",
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
  }, [authenticated, editing, invalidEditId, placeId]);

  const handleUseCurrentLocation = async () => {
    if (!navigator.geolocation) {
      setMessage(GEOLOCATION_UNAVAILABLE);
      return;
    }

    setMessage(null);
    try {
      const position = await getCurrentPosition();
      setLatitude(position.coords.latitude.toFixed(7));
      setLongitude(position.coords.longitude.toFixed(7));
      setMessage("현재 위치를 입력했어요.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, GEOLOCATION_UNAVAILABLE));
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!authenticated) {
      setMessage(editing ? LOGIN_REQUIRED_EDIT : LOGIN_REQUIRED_CREATE);
      return;
    }
    if (!editing && !region) {
      setMessage("동네 인증 후 장소를 등록할 수 있어요.");
      return;
    }
    if (editing && (!editPlace || invalidEditId)) {
      setMessage("수정할 장소를 찾을 수 없어요.");
      return;
    }

    setSubmitting(true);
    setMessage(null);
    try {
      if (editing) {
        await updatePlaceDetails(placeId, {
          name: name.trim(),
          categoryCode,
          addressRoad: toOptionalValue(addressRoad),
          addressJibun: toOptionalValue(addressJibun),
          latitude: Number(latitude),
          longitude: Number(longitude),
          priceRangeCode: toOptionalValue(priceRangeCode),
          recommendedMenu: toOptionalValue(recommendedMenu),
          shortRecommendation: shortRecommendation.trim(),
          featureText: toOptionalValue(featureText),
          franchise,
          imageUrls,
        });
        const nextPlace = await getPlace(placeId);
        onPlaceUpdated?.(nextPlace);
        navigate(`/places/${placeId}`);
        return;
      }

      const result = await createPlace({
        name: name.trim(),
        categoryCode,
        dongId: region!.dongId,
        addressRoad: toOptionalValue(addressRoad),
        addressJibun: toOptionalValue(addressJibun),
        latitude: Number(latitude),
        longitude: Number(longitude),
        priceRangeCode: toOptionalValue(priceRangeCode),
        recommendedMenu: toOptionalValue(recommendedMenu),
        shortRecommendation: shortRecommendation.trim(),
        featureText: toOptionalValue(featureText),
        franchise,
        imageUrls,
      });
      await onPlaceCreated();
      navigate(`/places/${result.placeId}`);
    } catch (error) {
      setMessage(
        getApiErrorMessage(
          error,
          editing
            ? "장소 수정을 처리하지 못했습니다."
            : "장소 등록을 처리하지 못했습니다.",
        ),
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (invalidEditId) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-m3-surface px-6 text-center text-m3-on-surface">
        <StateCard title="수정할 장소를 찾을 수 없어요." />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-m3-surface pb-[100px] text-m3-on-surface">
      <main className="mx-auto max-w-[430px] px-4 pt-6">
        <div className="flex items-center justify-between gap-4">
          <div>
            <p className="text-m3-label-md text-m3-primary">
              {editing ? "장소 관리" : "장소 등록"}
            </p>
            <h1 className="mt-1 text-m3-title-lg text-m3-on-surface">
              {editing ? "꿀스팟 수정" : "꿀스팟 등록"}
            </h1>
          </div>
          <Link
            to={editing ? "/my" : "/"}
            className="rounded-m3-full border border-m3-outline-variant bg-m3-surface-container-lowest px-4 py-2 text-m3-label-lg text-m3-on-surface shadow-m3-1"
          >
            닫기
          </Link>
        </div>

        {!authenticated && (
          <section className="mt-6 rounded-m3-xl bg-m3-surface-container-lowest p-5 text-left shadow-m3-1">
            <p className="text-m3-body-md text-m3-on-surface-variant">
              {editing ? LOGIN_REQUIRED_EDIT : LOGIN_REQUIRED_CREATE}
            </p>
            <Link to="/my" className={`${BUTTON_CLASS} mt-4 flex items-center justify-center`}>
              로그인하러 가기
            </Link>
          </section>
        )}

        {authenticated && loading && (
          <StateCard
            title={
              editing
                ? "수정할 장소 정보를 불러오는 중입니다."
                : "등록 준비 정보를 불러오는 중입니다."
            }
          />
        )}

        {authenticated && !loading && (
          <>
            {!editing && (
              <section className="mt-6 rounded-m3-xl bg-m3-surface-container-lowest p-5 text-left shadow-m3-1">
                <h2 className="text-m3-title-md text-m3-on-surface">
                  등록 기준
                </h2>
                <div className="mt-4 grid grid-cols-2 gap-3">
                  <InfoCell
                    label="인증 동네"
                    value={region ? formatRegionName(region) : "미인증"}
                  />
                  <InfoCell
                    label="등록 가능"
                    value={
                      policy
                        ? `${policy.currentUsage}/${policy.registrationLimit}`
                        : "-"
                    }
                  />
                </div>
                <p className="mt-3 text-m3-body-sm text-m3-on-surface-variant">
                  {policy
                    ? `등록 범위: ${formatRegistrationScope(policy.registrationScope)}`
                    : "등록 정책 정보를 불러오지 못했습니다."}
                </p>
                {!region && (
                  <Link to="/my" className={`${BUTTON_CLASS} mt-4 flex items-center justify-center`}>
                    동네 인증하러 가기
                  </Link>
                )}
              </section>
            )}

            {editing && editPlace && (
              <section className="mt-6 rounded-m3-xl bg-m3-surface-container-lowest p-5 text-left shadow-m3-1">
                <h2 className="text-m3-title-md text-m3-on-surface">
                  수정 대상
                </h2>
                <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">
                  현재 등록된 장소 정보를 불러왔습니다. 소유 여부와 수정 가능 여부는 저장 시 다시 확인됩니다.
                </p>
              </section>
            )}

            <form
              className="mt-5 flex flex-col gap-5 rounded-m3-xl bg-m3-surface-container-lowest p-5 text-left shadow-m3-1"
              onSubmit={handleSubmit}
            >
              <FieldLabel label="장소 이름">
                <input
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  required
                  maxLength={120}
                  className={INPUT_CLASS}
                />
              </FieldLabel>

              <FieldLabel label="카테고리">
                <select
                  value={categoryCode}
                  onChange={(event) => setCategoryCode(event.target.value)}
                  className={INPUT_CLASS}
                >
                  {CATEGORY_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.emoji} {option.label}
                    </option>
                  ))}
                </select>
              </FieldLabel>

              <div className="grid grid-cols-2 gap-3">
                <FieldLabel label="위도">
                  <input
                    value={latitude}
                    onChange={(event) => setLatitude(event.target.value)}
                    required
                    inputMode="decimal"
                    className={INPUT_CLASS}
                  />
                </FieldLabel>
                <FieldLabel label="경도">
                  <input
                    value={longitude}
                    onChange={(event) => setLongitude(event.target.value)}
                    required
                    inputMode="decimal"
                    className={INPUT_CLASS}
                  />
                </FieldLabel>
              </div>

              <button
                type="button"
                onClick={handleUseCurrentLocation}
                disabled={submitting}
                className={OUTLINED_BUTTON_CLASS}
              >
                현재 위치 입력
              </button>

              <FieldLabel label="도로명 주소">
                <input
                  value={addressRoad}
                  onChange={(event) => setAddressRoad(event.target.value)}
                  maxLength={255}
                  className={INPUT_CLASS}
                />
              </FieldLabel>

              <FieldLabel label="지번 주소">
                <input
                  value={addressJibun}
                  onChange={(event) => setAddressJibun(event.target.value)}
                  maxLength={255}
                  className={INPUT_CLASS}
                />
              </FieldLabel>

              <FieldLabel label="가격대">
                <select
                  value={priceRangeCode}
                  onChange={(event) => setPriceRangeCode(event.target.value)}
                  className={INPUT_CLASS}
                >
                  {PRICE_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </FieldLabel>

              <FieldLabel label="추천 메뉴">
                <input
                  value={recommendedMenu}
                  onChange={(event) => setRecommendedMenu(event.target.value)}
                  maxLength={255}
                  className={INPUT_CLASS}
                />
              </FieldLabel>

              <FieldLabel label="한 줄 추천">
                <input
                  value={shortRecommendation}
                  onChange={(event) => setShortRecommendation(event.target.value)}
                  required
                  maxLength={255}
                  className={INPUT_CLASS}
                />
              </FieldLabel>

              <FieldLabel label="특징">
                <textarea
                  value={featureText}
                  onChange={(event) => setFeatureText(event.target.value)}
                  maxLength={500}
                  className={`${INPUT_CLASS} min-h-24 resize-none`}
                />
              </FieldLabel>

              <FieldLabel label="이미지 URL">
                <textarea
                  value={imageUrlText}
                  onChange={(event) => setImageUrlText(event.target.value)}
                  placeholder="한 줄에 하나씩 입력"
                  className={`${INPUT_CLASS} min-h-20 resize-none`}
                />
              </FieldLabel>

              <label className="flex items-center gap-2 text-m3-body-md text-m3-on-surface">
                <input
                  type="checkbox"
                  checked={franchise}
                  onChange={(event) => setFranchise(event.target.checked)}
                  className="h-4 w-4 accent-[var(--m3-sys-primary)]"
                />
                프랜차이즈 또는 체인점입니다.
              </label>

              <button
                type="submit"
                disabled={submitting || (!editing && !region)}
                className={BUTTON_CLASS}
              >
                {submitting
                  ? "저장 중"
                  : editing
                    ? "장소 수정"
                    : "장소 등록"}
              </button>
            </form>
          </>
        )}

        {message && (
          <p className="mt-4 rounded-m3-xl bg-m3-secondary-container px-4 py-3 text-m3-body-md text-m3-on-secondary-container shadow-m3-1">
            {message}
          </p>
        )}
      </main>

      <BottomNav />
    </div>
  );
}

function FieldLabel({
  label,
  children,
}: {
  label: string;
  children: ReactNode;
}) {
  return (
    <label className="flex flex-col gap-2 text-m3-label-lg text-m3-on-surface">
      {label}
      {children}
    </label>
  );
}

function InfoCell({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-m3-lg bg-m3-surface-container-low p-3">
      <p className="text-m3-label-md text-m3-on-surface-variant">{label}</p>
      <p className="mt-1 truncate text-m3-title-sm text-m3-on-surface">{value}</p>
    </div>
  );
}

function StateCard({ title, desc }: { title: string; desc?: string }) {
  return (
    <section className="mt-6 rounded-m3-xl bg-m3-surface-container-lowest p-5 text-center text-m3-on-surface shadow-m3-1">
      <p className="text-m3-title-sm">{title}</p>
      {desc && <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">{desc}</p>}
    </section>
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

function formatRegionName(region: MyRegion) {
  return `${region.cityName} ${region.districtName} ${region.dongName}`;
}

function formatRegistrationScope(value: string) {
  switch (value) {
    case "DONG":
      return "인증 동네";
    case "DISTRICT":
      return "같은 시군구";
    case "CITY":
      return "같은 시도";
    default:
      return value;
  }
}

function toOptionalValue(value: string) {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}
