/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        m3: {
          primary: "var(--m3-sys-primary)",
          "on-primary": "var(--m3-sys-on-primary)",
          "primary-container": "var(--m3-sys-primary-container)",
          "on-primary-container": "var(--m3-sys-on-primary-container)",
          secondary: "var(--m3-sys-secondary)",
          "on-secondary": "var(--m3-sys-on-secondary)",
          "secondary-container": "var(--m3-sys-secondary-container)",
          "on-secondary-container": "var(--m3-sys-on-secondary-container)",
          tertiary: "var(--m3-sys-tertiary)",
          "on-tertiary": "var(--m3-sys-on-tertiary)",
          error: "var(--m3-sys-error)",
          "on-error": "var(--m3-sys-on-error)",
          surface: "var(--m3-sys-surface)",
          "on-surface": "var(--m3-sys-on-surface)",
          "surface-variant": "var(--m3-sys-surface-variant)",
          "on-surface-variant": "var(--m3-sys-on-surface-variant)",
          "surface-container-lowest":
            "var(--m3-sys-surface-container-lowest)",
          "surface-container-low": "var(--m3-sys-surface-container-low)",
          "surface-container": "var(--m3-sys-surface-container)",
          "surface-container-high": "var(--m3-sys-surface-container-high)",
          "surface-container-highest":
            "var(--m3-sys-surface-container-highest)",
          outline: "var(--m3-sys-outline)",
          "outline-variant": "var(--m3-sys-outline-variant)",
        },
      },
      borderRadius: {
        "m3-xs": "4px",
        "m3-sm": "8px",
        "m3-md": "12px",
        "m3-lg": "16px",
        "m3-xl": "28px",
        "m3-full": "999px",
      },
      boxShadow: {
        "m3-1": "var(--m3-elevation-1)",
        "m3-2": "var(--m3-elevation-2)",
        "m3-3": "var(--m3-elevation-3)",
      },
      fontSize: {
        "m3-title-lg": ["22px", { lineHeight: "28px", fontWeight: "500" }],
        "m3-title-md": ["16px", { lineHeight: "24px", fontWeight: "500" }],
        "m3-title-sm": ["14px", { lineHeight: "20px", fontWeight: "500" }],
        "m3-label-lg": ["14px", { lineHeight: "20px", fontWeight: "500" }],
        "m3-label-md": ["12px", { lineHeight: "16px", fontWeight: "500" }],
        "m3-body-lg": ["16px", { lineHeight: "24px", fontWeight: "400" }],
        "m3-body-md": ["14px", { lineHeight: "20px", fontWeight: "400" }],
        "m3-body-sm": ["12px", { lineHeight: "16px", fontWeight: "400" }],
      },
    },
  },
  plugins: [],
};
