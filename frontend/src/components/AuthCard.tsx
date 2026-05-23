import { useTranslation } from "../hooks/useTranslation";

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
  const { t } = useTranslation();

  return (
    <section
      className={`${className} rounded-m3-xl bg-m3-surface-container-low p-4 text-m3-on-surface shadow-m3-1`}
    >
      <h2 className="text-m3-title-lg">{t("auth.loginRequired")}</h2>
      <p className="mt-2 text-m3-body-md text-m3-on-surface-variant">
        {t("auth.loginDesc")}
      </p>

      <div className="mt-5 grid grid-cols-2 gap-1 rounded-m3-full bg-m3-surface-container-high p-1">
        {(["login", "signup"] as AuthMode[]).map((mode) => (
          <button
            key={mode}
            type="button"
            onClick={() => setAuthMode(mode)}
            className={`h-10 rounded-m3-full text-m3-label-lg transition-colors ${
              authMode === mode
                ? "bg-m3-secondary-container text-m3-on-secondary-container shadow-m3-1"
                : "text-m3-on-surface-variant hover:bg-m3-surface-container-highest"
            }`}
          >
            {mode === "login" ? t("auth.login") : t("auth.signup")}
          </button>
        ))}
      </div>

      <div className="mt-5 flex flex-col gap-4">
        <FormField label={t("auth.email")} value={email} onChange={setEmail} type="email" />
        <FormField
          label={t("auth.password")}
          value={password}
          onChange={setPassword}
          type="password"
        />
        {authMode === "signup" && (
          <FormField label={t("auth.nickname")} value={nickname} onChange={setNickname} />
        )}
      </div>

      <button
        type="button"
        onClick={onSubmit}
        disabled={busy}
        className="mt-5 h-12 w-full rounded-m3-full bg-m3-primary px-5 text-m3-label-lg text-m3-on-primary shadow-m3-1 transition active:scale-[0.98] disabled:cursor-not-allowed disabled:bg-m3-surface-container-highest disabled:text-m3-on-surface-variant disabled:shadow-none"
      >
        {busy
          ? t("auth.processing")
          : authMode === "login"
            ? t("auth.doLogin")
            : t("auth.doSignup")}
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
    <label className="text-m3-label-lg text-m3-on-surface">
      {label}
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        type={type}
        className="mt-2 h-14 w-full rounded-m3-sm border border-m3-outline bg-m3-surface-container-lowest px-4 text-m3-body-lg text-m3-on-surface outline-none transition placeholder:text-m3-on-surface-variant focus:border-m3-primary focus:ring-2 focus:ring-m3-primary/20"
      />
    </label>
  );
}
