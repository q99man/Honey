import { useEffect, useMemo, useState } from "react";
import {
  getAdminPolicies,
  getAdminRegionPolicy,
  updateAdminPolicy,
  updateAdminRegionPolicy,
  type AdminPolicy,
  type AdminPolicyValueType,
  type AdminRegionPolicy,
} from "../api/adminApi";
import { getApiErrorMessage } from "../api/http";
import { AdminPageShell } from "../components/AdminShell";

const LOAD_ERROR = "정책 목록을 불러오지 못했습니다.";
const GROUP_FILTER_ALL = "ALL";

export default function AdminPoliciesPage() {
  const [policies, setPolicies] = useState<AdminPolicy[]>([]);
  const [regionPolicy, setRegionPolicy] =
    useState<AdminRegionPolicy | null>(null);
  const [selectedFullKey, setSelectedFullKey] = useState<string | null>(null);
  const [groupFilter, setGroupFilter] = useState(GROUP_FILTER_ALL);
  const [value, setValue] = useState("");
  const [memo, setMemo] = useState("");
  const [regionChangeCooldownDays, setRegionChangeCooldownDays] = useState("");
  const [registrationScope, setRegistrationScope] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;
    Promise.all([getAdminPolicies(), getAdminRegionPolicy()])
      .then(([nextPolicies, nextRegionPolicy]) => {
        if (!mounted) {
          return;
        }
        setPolicies(nextPolicies);
        setRegionPolicy(nextRegionPolicy);
        setRegionChangeCooldownDays(
          String(nextRegionPolicy.regionChangeCooldownDays),
        );
        setRegistrationScope(nextRegionPolicy.registrationScope);
        setSelectedFullKey(nextPolicies[0]?.fullKey ?? null);
        setValue(nextPolicies[0]?.policyValue ?? "");
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

  const groups = useMemo(() => {
    return Array.from(new Set(policies.map((policy) => policy.policyGroup)));
  }, [policies]);

  const selectedPolicy = useMemo(() => {
    return (
      policies.find((policy) => policy.fullKey === selectedFullKey) ?? null
    );
  }, [policies, selectedFullKey]);

  const filteredPolicies = useMemo(() => {
    if (groupFilter === GROUP_FILTER_ALL) {
      return policies;
    }
    return policies.filter((policy) => policy.policyGroup === groupFilter);
  }, [groupFilter, policies]);

  const handleSelectPolicy = (policy: AdminPolicy) => {
    setSelectedFullKey(policy.fullKey);
    setValue(policy.policyValue);
    setMemo("");
  };

  const handleUpdatePolicy = async () => {
    if (!selectedPolicy || !value.trim()) {
      setMessage("정책 값을 입력해 주세요.");
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      const updated = await updateAdminPolicy(selectedPolicy.fullKey, {
        value: value.trim(),
        memo: memo.trim() || null,
      });
      setPolicies((prev) =>
        prev.map((policy) => (policy.id === updated.id ? updated : policy)),
      );
      setSelectedFullKey(updated.fullKey);
      setValue(updated.policyValue);
      setMemo("");
      setMessage("정책 값을 저장했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "정책 값을 저장하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  const handleUpdateRegionPolicy = async () => {
    const cooldown = Number(regionChangeCooldownDays);
    if (!Number.isInteger(cooldown) || cooldown < 0 || !registrationScope.trim()) {
      setMessage("지역 정책 값을 다시 확인해 주세요.");
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      const updated = await updateAdminRegionPolicy({
        regionChangeCooldownDays: cooldown,
        registrationScope: registrationScope.trim().toUpperCase(),
      });
      const nextPolicies = await getAdminPolicies();
      setRegionPolicy(updated);
      setRegionChangeCooldownDays(String(updated.regionChangeCooldownDays));
      setRegistrationScope(updated.registrationScope);
      setPolicies(nextPolicies);
      const selected = nextPolicies.find(
        (policy) => policy.fullKey === selectedFullKey,
      );
      if (selected) {
        setValue(selected.policyValue);
      }
      setMessage("지역 정책을 저장했습니다.");
    } catch (error) {
      setMessage(getApiErrorMessage(error, "지역 정책을 저장하지 못했습니다."));
    } finally {
      setBusy(false);
    }
  };

  return (
    <AdminPageShell
      title="정책 관리"
      description="운영 정책 값을 확인하고 필요한 변경을 감사 로그와 함께 저장합니다."
      maxWidth="max-w-[980px]"
    >

        {message && (
          <p className="mt-4 rounded-m3-lg bg-m3-secondary-container px-4 py-3 text-m3-label-lg text-m3-on-secondary-container shadow-m3-1">
            {message}
          </p>
        )}

        {loading && (
          <section className="admin-panel admin-panel-spacious mt-6 text-m3-body-md text-m3-on-surface-variant">
            정책 목록을 불러오는 중입니다.
          </section>
        )}

        {!loading && (
          <div className="mt-6 grid gap-5 lg:grid-cols-[360px_1fr]">
            <section className="admin-panel">
              <div className="flex items-center justify-between gap-3">
                <h2 className="text-lg font-bold">정책 목록</h2>
                <span className="text-xs font-semibold text-gray-400">
                  {filteredPolicies.length}건
                </span>
              </div>

              <div className="mt-3 flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => setGroupFilter(GROUP_FILTER_ALL)}
                  className={`admin-segment h-9 px-3 ${
                    groupFilter === GROUP_FILTER_ALL
                      ? "admin-segment-selected"
                      : "admin-segment-idle"
                  }`}
                >
                  전체
                </button>
                {groups.map((group) => (
                  <button
                    key={group}
                    type="button"
                    onClick={() => setGroupFilter(group)}
                    className={`admin-segment h-9 px-3 ${
                      groupFilter === group
                        ? "admin-segment-selected"
                        : "admin-segment-idle"
                    }`}
                  >
                    {group}
                  </button>
                ))}
              </div>

              <div className="mt-4 flex max-h-[620px] flex-col gap-3 overflow-y-auto pr-1">
                {filteredPolicies.map((policy) => (
                  <button
                    key={policy.id}
                    type="button"
                    onClick={() => handleSelectPolicy(policy)}
                    className={`admin-list-item ${
                      selectedFullKey === policy.fullKey
                        ? "admin-list-item-selected"
                        : "admin-list-item-idle"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <p className="font-bold">{policy.fullKey}</p>
                        <p className="mt-1 text-m3-body-sm text-m3-on-surface-variant">
                          {valueTypeLabel(policy.valueType)}
                        </p>
                      </div>
                      <span
                        className={`rounded-full px-2 py-1 text-xs font-bold ${
                          policy.active
                            ? "bg-m3-secondary-container text-m3-on-secondary-container"
                            : "bg-gray-100 text-gray-400"
                        }`}
                      >
                        {policy.active ? "활성" : "비활성"}
                      </span>
                    </div>
                    <p className="mt-2 truncate text-xs text-gray-400">
                      현재 값: {policy.policyValue}
                    </p>
                  </button>
                ))}
              </div>
            </section>

            <div className="flex flex-col gap-5">
              <section className="admin-panel admin-panel-spacious">
                {!selectedPolicy && (
                  <p className="text-m3-body-md text-m3-on-surface-variant">
                    수정할 정책을 목록에서 선택해 주세요.
                  </p>
                )}

                {selectedPolicy && (
                  <>
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                      <div>
                        <h2 className="text-xl font-bold">
                          {selectedPolicy.fullKey}
                        </h2>
                        <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">
                          {selectedPolicy.description ?? "설명이 등록되지 않았습니다."}
                        </p>
                      </div>
                      <span className="w-fit rounded-m3-full bg-m3-secondary-container px-3 py-1 text-m3-label-md text-m3-on-secondary-container">
                        {valueTypeLabel(selectedPolicy.valueType)}
                      </span>
                    </div>

                    <dl className="mt-5 grid gap-3 text-sm sm:grid-cols-2">
                      <InfoItem label="그룹" value={selectedPolicy.policyGroup} />
                      <InfoItem label="키" value={selectedPolicy.policyKey} />
                      <InfoItem
                        label="수정자"
                        value={
                          selectedPolicy.updatedBy
                            ? `사용자 #${selectedPolicy.updatedBy}`
                            : "기록 없음"
                        }
                      />
                      <InfoItem
                        label="수정일"
                        value={formatDate(selectedPolicy.updatedAt)}
                      />
                    </dl>

                    <label className="mt-5 block text-sm font-semibold">
                      정책 값
                      <PolicyValueInput
                        value={value}
                        valueType={selectedPolicy.valueType}
                        onChange={setValue}
                      />
                    </label>

                    <label className="mt-3 block text-sm font-semibold">
                      변경 메모
                      <textarea
                        value={memo}
                        onChange={(event) => setMemo(event.target.value)}
                        maxLength={255}
                        placeholder="변경 이유를 입력해 주세요."
                        className="admin-field mt-2 min-h-20 resize-none p-3"
                      />
                    </label>

                    <button
                      type="button"
                      onClick={handleUpdatePolicy}
                      disabled={busy}
                      className="admin-action-warning mt-4 h-11 w-full"
                    >
                      {busy ? "저장 중" : "정책 저장"}
                    </button>
                  </>
                )}
              </section>

              <section className="admin-panel admin-panel-spacious">
                <div>
                  <h2 className="text-lg font-bold">지역 정책 빠른 수정</h2>
                  <p className="mt-1 text-m3-body-md text-m3-on-surface-variant">
                    지역 변경 주기와 장소 등록 범위를 함께 저장합니다.
                  </p>
                </div>

                <div className="mt-4 grid gap-3 sm:grid-cols-2">
                  <label className="text-sm font-semibold">
                    변경 제한 일수
                    <input
                      value={regionChangeCooldownDays}
                      onChange={(event) =>
                        setRegionChangeCooldownDays(event.target.value)
                      }
                      type="number"
                      min={0}
                      className="admin-field mt-2 h-11"
                    />
                  </label>
                  <label className="text-sm font-semibold">
                    등록 범위
                    <select
                      value={registrationScope}
                      onChange={(event) =>
                        setRegistrationScope(event.target.value)
                      }
                      className="admin-field mt-2 h-11"
                    >
                      <option value="DONG">동</option>
                      <option value="DISTRICT">구/군</option>
                      <option value="CITY">시/도</option>
                    </select>
                  </label>
                </div>

                <button
                  type="button"
                  onClick={handleUpdateRegionPolicy}
                  disabled={busy || !regionPolicy}
                  className="admin-action-primary mt-4 h-11 w-full"
                >
                  {busy ? "저장 중" : "지역 정책 저장"}
                </button>
              </section>
            </div>
          </div>
        )}
    </AdminPageShell>
  );
}

function PolicyValueInput({
  value,
  valueType,
  onChange,
}: {
  value: string;
  valueType: AdminPolicyValueType;
  onChange: (value: string) => void;
}) {
  if (valueType === "BOOLEAN") {
    return (
      <select
        value={value.toLowerCase()}
        onChange={(event) => onChange(event.target.value)}
        className="admin-field mt-2 h-11"
      >
        <option value="true">true</option>
        <option value="false">false</option>
      </select>
    );
  }

  return (
    <input
      value={value}
      onChange={(event) => onChange(event.target.value)}
      type={valueType === "INTEGER" || valueType === "DECIMAL" ? "number" : "text"}
      step={valueType === "DECIMAL" ? "0.01" : undefined}
      className="admin-field mt-2 h-11"
    />
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

function valueTypeLabel(value: AdminPolicyValueType) {
  switch (value) {
    case "INTEGER":
      return "정수";
    case "DECIMAL":
      return "숫자";
    case "BOOLEAN":
      return "참/거짓";
    case "STRING":
      return "문자";
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString("ko-KR", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}
