/** @type {import('tailwindcss').Config} */
export default {
  darkMode: "class",
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ['"Inter Variable"', "Inter", "system-ui", "sans-serif"],
        mono: ['"JetBrains Mono Variable"', '"JetBrains Mono"', "ui-monospace", "monospace"],
      },
      colors: {
        bg: "rgb(var(--bg) / <alpha-value>)",
        surface: "rgb(var(--surface) / <alpha-value>)",
        raised: "rgb(var(--raised) / <alpha-value>)",
        line: "rgb(var(--line) / <alpha-value>)",
        fg: "rgb(var(--fg) / <alpha-value>)",
        muted: "rgb(var(--muted) / <alpha-value>)",
        gold: "rgb(var(--gold) / <alpha-value>)",
        guilty: "rgb(var(--guilty) / <alpha-value>)",
        acquitted: "rgb(var(--acquitted) / <alpha-value>)",
        dismissed: "rgb(var(--dismissed) / <alpha-value>)",
        warn: "rgb(var(--warn) / <alpha-value>)",
        info: "rgb(var(--info) / <alpha-value>)",
      },
      borderRadius: { xl: "0.875rem", "2xl": "1.125rem" },
      boxShadow: {
        card: "0 1px 0 rgb(255 255 255 / 0.03) inset, 0 8px 24px -12px rgb(0 0 0 / 0.45)",
        gold: "0 0 0 1px rgb(var(--gold) / 0.35), 0 10px 30px -10px rgb(var(--gold) / 0.45)",
      },
      keyframes: {
        flicker: {
          "0%, 100%": { transform: "scale(1) rotate(-2deg)", opacity: "1" },
          "50%": { transform: "scale(1.08) rotate(2deg)", opacity: "0.85" },
        },
        shimmer: { "100%": { transform: "translateX(100%)" } },
      },
      animation: {
        flicker: "flicker 1.6s ease-in-out infinite",
        shimmer: "shimmer 1.4s infinite",
      },
    },
  },
  plugins: [],
};
