import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login } from "../api/authApi";
import { getApiErrorMessage } from "../api/http";

const LOGIN_ERROR = "관리자 로그인을 완료하지 못했습니다.";

export default function AdminLoginPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const handleSubmit = async () => {
    if (!email.trim() || !password) {
      setMessage("관리자 이메일과 비밀번호를 입력해 주세요.");
      return;
    }

    setBusy(true);
    setMessage(null);
    try {
      await login(email.trim(), password);
      navigate("/admin", { replace: true });
    } catch (error) {
      setMessage(getApiErrorMessage(error, LOGIN_ERROR));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen bg-m3-surface px-4 py-8 text-m3-on-surface">
      <main className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-[420px] flex-col justify-center">
        <section className="rounded-m3-xl bg-m3-surface-container-lowest p-5 shadow-m3-2">
          <p className="text-m3-label-md text-m3-primary">Honeytong Admin</p>
          <h1 className="mt-1 text-m3-title-lg">관리자 로그인</h1>
          <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">
            별도 관리자 계정으로 로그인한 뒤 운영 화면에 접근합니다.
          </p>

          <div className="mt-6 flex flex-col gap-4">
            <AdminLoginField
              label="관리자 이메일"
              value={email}
              onChange={setEmail}
              type="email"
              autoComplete="username"
            />
            <AdminLoginField
              label="비밀번호"
              value={password}
              onChange={setPassword}
              type="password"
              autoComplete="current-password"
            />
          </div>

          {message && (
            <p className="mt-4 rounded-m3-lg bg-m3-error-container px-4 py-3 text-m3-label-lg text-m3-on-error-container">
              {message}
            </p>
          )}

          <button
            type="button"
            onClick={handleSubmit}
            disabled={busy}
            className="mt-5 h-12 w-full rounded-m3-full bg-m3-primary px-5 text-m3-label-lg text-m3-on-primary shadow-m3-1 transition active:scale-[0.98] disabled:cursor-not-allowed disabled:bg-m3-surface-container-highest disabled:text-m3-on-surface-variant disabled:shadow-none"
          >
            {busy ? "로그인 중..." : "관리자 로그인"}
          </button>

          <div className="mt-4 flex items-center justify-between gap-3 text-m3-label-md">
            <Link to="/" className="text-m3-on-surface-variant">
              사용자 홈
            </Link>
            <Link to="/admin/policies" className="text-m3-primary">
              정책 관리로 이동
            </Link>
          </div>
        </section>
      </main>
    </div>
  );
}

function AdminLoginField({
  label,
  value,
  onChange,
  type,
  autoComplete,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type: string;
  autoComplete: string;
}) {
  return (
    <label className="text-m3-label-lg text-m3-on-surface">
      {label}
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        type={type}
        autoComplete={autoComplete}
        className="mt-2 h-12 w-full rounded-m3-sm border border-m3-outline bg-m3-surface-container-lowest px-4 text-m3-body-lg text-m3-on-surface outline-none transition placeholder:text-m3-on-surface-variant focus:border-m3-primary focus:ring-2 focus:ring-m3-primary/20"
      />
    </label>
  );
}
