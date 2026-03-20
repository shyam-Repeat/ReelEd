export function MonkeyIllustration() {
  return (
    <svg
      width="180"
      height="180"
      viewBox="0 0 180 180"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className="mx-auto"
    >
      {/* Head */}
      <circle cx="90" cy="90" r="60" fill="#D4A574" />

      {/* Ears */}
      <circle cx="50" cy="50" r="18" fill="#D4A574" />
      <circle cx="130" cy="50" r="18" fill="#D4A574" />
      <circle cx="50" cy="50" r="12" fill="#C9926B" />
      <circle cx="130" cy="50" r="12" fill="#C9926B" />

      {/* Face shape - lighter area */}
      <ellipse cx="90" cy="100" rx="48" ry="52" fill="#E8C9AA" />

      {/* Eyes white */}
      <circle cx="70" cy="80" r="12" fill="white" />
      <circle cx="110" cy="80" r="12" fill="white" />

      {/* Pupils */}
      <circle cx="72" cy="82" r="7" fill="#000000" />
      <circle cx="112" cy="82" r="7" fill="#000000" />

      {/* Eye shine */}
      <circle cx="74" cy="79" r="3" fill="white" />
      <circle cx="114" cy="79" r="3" fill="white" />

      {/* Nose */}
      <ellipse cx="90" cy="105" rx="8" ry="10" fill="#B8956A" />

      {/* Mouth - smile */}
      <path
        d="M 90 110 Q 85 120 75 118"
        stroke="#B8956A"
        strokeWidth="2.5"
        fill="none"
        strokeLinecap="round"
      />
      <path
        d="M 90 110 Q 95 120 105 118"
        stroke="#B8956A"
        strokeWidth="2.5"
        fill="none"
        strokeLinecap="round"
      />

      {/* Cheeks blush */}
      <circle cx="55" cy="100" r="10" fill="#F0A080" opacity="0.6" />
      <circle cx="125" cy="100" r="10" fill="#F0A080" opacity="0.6" />

      {/* Mouth inner area */}
      <ellipse cx="90" cy="112" rx="10" ry="6" fill="#F5D5B8" />
    </svg>
  );
}
TAILWIND CONFIG
import type { Config } from "tailwindcss";

export default {
  darkMode: ["class"],
  content: ["./client/**/*.{ts,tsx}"],
  prefix: "",
  theme: {
    container: {
      center: true,
      padding: "2rem",
      screens: {
        "2xl": "1400px",
      },
    },
    extend: {
      colors: {
        border: "hsl(var(--border))",
        input: "hsl(var(--input))",
        ring: "hsl(var(--ring))",
        background: "hsl(var(--background))",
        foreground: "hsl(var(--foreground))",
        primary: {
          DEFAULT: "hsl(var(--primary))",
          foreground: "hsl(var(--primary-foreground))",
        },
        secondary: {
          DEFAULT: "hsl(var(--secondary))",
          foreground: "hsl(var(--secondary-foreground))",
        },
        destructive: {
          DEFAULT: "hsl(var(--destructive))",
          foreground: "hsl(var(--destructive-foreground))",
        },
        muted: {
          DEFAULT: "hsl(var(--muted))",
          foreground: "hsl(var(--muted-foreground))",
        },
        accent: {
          DEFAULT: "hsl(var(--accent))",
          foreground: "hsl(var(--accent-foreground))",
        },
        popover: {
          DEFAULT: "hsl(var(--popover))",
          foreground: "hsl(var(--popover-foreground))",
        },
        card: {
          DEFAULT: "hsl(var(--card))",
          foreground: "hsl(var(--card-foreground))",
        },
        sidebar: {
          DEFAULT: "hsl(var(--sidebar-background))",
          foreground: "hsl(var(--sidebar-foreground))",
          primary: "hsl(var(--sidebar-primary))",
          "primary-foreground": "hsl(var(--sidebar-primary-foreground))",
          accent: "hsl(var(--sidebar-accent))",
          "accent-foreground": "hsl(var(--sidebar-accent-foreground))",
          border: "hsl(var(--sidebar-border))",
          ring: "hsl(var(--sidebar-ring))",
        },
      },
      borderRadius: {
        lg: "var(--radius)",
        md: "calc(var(--radius) - 2px)",
        sm: "calc(var(--radius) - 4px)",
      },
      keyframes: {
        "accordion-down": {
          from: {
            height: "0",
          },
          to: {
            height: "var(--radix-accordion-content-height)",
          },
        },
        "accordion-up": {
          from: {
            height: "var(--radix-accordion-content-height)",
          },
          to: {
            height: "0",
          },
        },
      },
      animation: {
        "accordion-down": "accordion-down 0.2s ease-out",
        "accordion-up": "accordion-up 0.2s ease-out",
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
} satisfies Config;
