export type AuthMode = "login" | "signup";

type Props = {
  authMode: AuthMode;
  setAuthMode: (mode: AuthMode) => void;
  email: string;
  setEmail: (value: string) => void;
  password: string;
  setPassword: (value: string) => void;
  nickname: string;
  setNickname: (value: string) => void;
  busy: boolean;
  onSubmit: () => void;
  className?: string;
};

export default function AuthCard({
  authMode,
  setAuthMode,
  email,
  setEmail,
  password,
  setPassword,
  nickname,
  setNickname,
  busy,
  onSubmit,
  className = "mt-6",
}: Props) {
  return (
    <section className={`${className} rounded-3xl bg-white p-4 shadow-sm`}>
      <h2 className="text-lg font-bold text-[#2b210f]">
        로그인이 필요한 화면이에요
      </h2>
      <p className="mt-2 text-sm leading-6 text-gray-600">
        로그인하면 내 동네와 활동 기록을 한눈에 볼 수 있어요.
      </p>

      <div className="mt-5 grid grid-cols-2 gap-2 rounded-full bg-[#fff8df] p-1">
        {(["login", "signup"] as AuthMode[]).map((mode) => (
          <button
            key={mode}
            type="button"
            onClick={() => setAuthMode(mode)}
            className={`h-10 rounded-full text-sm font-semibold transition ${
              authMode === mode
                ? "bg-[#f6b800] text-[#2b210f]"
                : "text-gray-500"
            }`}
          >
            {mode === "login" ? "로그인" : "회원가입"}
          </button>
        ))}
      </div>

      <div className="mt-5 flex flex-col gap-4">
        <FormField label="이메일" value={email} onChange={setEmail} type="email" />
        <FormField
          label="비밀번호"
          value={password}
          onChange={setPassword}
          type="password"
        />
        {authMode === "signup" && (
          <FormField label="닉네임" value={nickname} onChange={setNickname} />
        )}
      </div>

      <button
        type="button"
        onClick={onSubmit}
        disabled={busy}
        className="mt-5 h-12 w-full rounded-full bg-[#f6b800] px-5 text-sm font-semibold text-[#2b210f] transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50"
      >
        {busy
          ? "처리 중..."
          : authMode === "login"
            ? "로그인하기"
            : "회원가입하고 로그인하기"}
      </button>
    </section>
  );
}

function FormField({
  label,
  value,
  onChange,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: string;
}) {
  return (
    <label className="text-sm font-semibold text-[#2b210f]">
      {label}
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        type={type}
        className="mt-2 h-11 w-full rounded-2xl border border-gray-200 bg-[#fffaf0] px-3 text-sm outline-none focus:border-[#f6b800]"
      />
    </label>
  );
}
