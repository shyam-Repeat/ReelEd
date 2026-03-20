export default function PandaTrainHero() {
  return (
    <div className="relative w-full bg-gradient-to-b from-[hsl(var(--sky-light))] via-white to-white dark:from-[hsl(var(--sky-bright))] dark:via-slate-900 dark:to-slate-900 pt-12 pb-12 sm:pt-16 sm:pb-16 overflow-hidden">
      {/* Decorative background elements */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        {/* Small clouds */}
        <svg
          className="absolute top-6 left-4 w-16 h-8 text-white opacity-40 dark:opacity-20"
          viewBox="0 0 60 30"
          fill="currentColor"
        >
          <path d="M10 15 Q8 8, 15 8 Q18 2, 25 8 Q35 8, 35 15" />
        </svg>
        <svg
          className="absolute top-20 right-6 w-12 h-6 text-white opacity-30 dark:opacity-15"
          viewBox="0 0 60 30"
          fill="currentColor"
        >
          <path d="M10 15 Q8 8, 15 8 Q18 2, 25 8 Q35 8, 35 15" />
        </svg>

        {/* Decorative stars */}
        <svg
          className="absolute top-12 right-12 w-6 h-6 text-yellow-300"
          fill="currentColor"
          viewBox="0 0 24 24"
        >
          <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
        </svg>
        <svg
          className="absolute bottom-32 left-8 w-5 h-5 text-blue-300"
          fill="currentColor"
          viewBox="0 0 24 24"
        >
          <circle cx="12" cy="12" r="10" />
        </svg>
      </div>

      {/* Main hero container */}
      <div className="relative w-full max-w-md mx-auto px-4 h-72 sm:h-96">
        {/* Background circle for panda */}
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="absolute w-64 h-64 sm:w-80 sm:h-80 rounded-full bg-gradient-to-br from-blue-200 via-purple-200 to-pink-200 dark:from-blue-900 dark:via-purple-900 dark:to-pink-900 opacity-60 blur-2xl"></div>
        </div>

        {/* Train and Panda container */}
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="w-full h-40 sm:h-56 relative z-10">
            {/* Train Container - with bounce animation */}
            <div className="absolute inset-0 flex items-center justify-center animate-train-bounce">
              {/* Train SVG */}
              <svg
                viewBox="0 0 400 120"
                className="w-full h-auto max-w-lg drop-shadow-lg"
                fill="none"
              >
                {/* Track */}
                <g>
                  <line
                    x1="0"
                    y1="110"
                    x2="400"
                    y2="110"
                    stroke="#8B7355"
                    strokeWidth="3"
                  />
                  <circle cx="40" cy="115" r="8" fill="#6B5344" />
                  <circle cx="100" cy="115" r="8" fill="#6B5344" />
                  <circle cx="160" cy="115" r="8" fill="#6B5344" />
                  <circle cx="220" cy="115" r="8" fill="#6B5344" />
                  <circle cx="280" cy="115" r="8" fill="#6B5344" />
                  <circle cx="340" cy="115" r="8" fill="#6B5344" />
                </g>

                {/* Front Car (Red) - containing Panda */}
                <g className="animate-train-enter">
                  {/* Car body */}
                  <rect
                    x="20"
                    y="40"
                    width="80"
                    height="65"
                    rx="8"
                    fill="#EF4444"
                  />
                  {/* Car shine */}
                  <rect
                    x="25"
                    y="45"
                    width="70"
                    height="12"
                    rx="4"
                    fill="#FCA5A5"
                    opacity="0.6"
                  />
                  {/* Window */}
                  <rect
                    x="30"
                    y="55"
                    width="60"
                    height="35"
                    rx="4"
                    fill="#FEF3F2"
                  />

                  {/* Panda in window */}
                  <g className="animate-panda-bob">
                    {/* Head */}
                    <circle cx="60" cy="65" r="18" fill="#F5F5F5" />

                    {/* Ears */}
                    <circle cx="48" cy="52" r="8" fill="#1F2937" />
                    <circle cx="72" cy="52" r="8" fill="#1F2937" />

                    {/* Eyes */}
                    <circle cx="53" cy="63" r="4.5" fill="#1F2937" />
                    <circle cx="67" cy="63" r="4.5" fill="#1F2937" />
                    <circle cx="54.5" cy="61.5" r="1.5" fill="#FFFFFF" />
                    <circle cx="68.5" cy="61.5" r="1.5" fill="#FFFFFF" />

                    {/* Blush */}
                    <circle cx="45" cy="70" r="3.5" fill="#FCA5A5" opacity="0.7" />
                    <circle cx="75" cy="70" r="3.5" fill="#FCA5A5" opacity="0.7" />

                    {/* Mouth */}
                    <path d="M 60 75 L 58 78 L 62 78 Z" fill="#1F2937" />
                    <path d="M 60 78 Q 57 81 60 82 Q 63 81 60 78" fill="#EC4899" />

                    {/* Wave hand - right arm */}
                    <g className="animate-panda-wave origin-[60px_75px]">
                      <circle cx="72" cy="78" r="4" fill="#F5F5F5" />
                      <line
                        x1="72"
                        y1="78"
                        x2="78"
                        y2="68"
                        stroke="#F5F5F5"
                        strokeWidth="3"
                        strokeLinecap="round"
                      />
                    </g>
                  </g>

                  {/* Coupling */}
                  <line
                    x1="100"
                    y1="80"
                    x2="125"
                    y2="80"
                    stroke="#1F2937"
                    strokeWidth="2"
                  />
                </g>

                {/* Middle Car (Yellow) */}
                <g className="animate-train-enter" style={{ animationDelay: "0.1s" }}>
                  <rect
                    x="125"
                    y="50"
                    width="70"
                    height="55"
                    rx="6"
                    fill="#FBBF24"
                  />
                  <rect
                    x="130"
                    y="55"
                    width="60"
                    height="10"
                    rx="3"
                    fill="#FCD34D"
                    opacity="0.6"
                  />
                  <rect x="135" y="70" width="25" height="25" rx="3" fill="#FEF3F2" />
                  <rect x="165" y="70" width="25" height="25" rx="3" fill="#FEF3F2" />
                  {/* Coupling */}
                  <line
                    x1="195"
                    y1="80"
                    x2="220"
                    y2="80"
                    stroke="#1F2937"
                    strokeWidth="2"
                  />
                </g>

                {/* Back Car (Blue) */}
                <g className="animate-train-enter" style={{ animationDelay: "0.2s" }}>
                  <rect
                    x="220"
                    y="55"
                    width="70"
                    height="50"
                    rx="6"
                    fill="#3B82F6"
                  />
                  <rect
                    x="225"
                    y="60"
                    width="60"
                    height="10"
                    rx="3"
                    fill="#93C5FD"
                    opacity="0.6"
                  />
                  <rect x="230" y="75" width="30" height="22" rx="3" fill="#FEF3F2" />
                  <rect x="265" y="75" width="20" height="22" rx="3" fill="#FEF3F2" />
                </g>

                {/* Engine (Green) */}
                <g className="animate-train-enter" style={{ animationDelay: "0.3s" }}>
                  <rect
                    x="300"
                    y="50"
                    width="80"
                    height="60"
                    rx="6"
                    fill="#10B981"
                  />
                  {/* Boiler */}
                  <rect
                    x="310"
                    y="55"
                    width="60"
                    height="25"
                    rx="3"
                    fill="#34D399"
                  />
                  {/* Chimney */}
                  <rect x="335" y="35" width="8" height="20" fill="#6B7280" />
                  <rect x="333" y="32" width="12" height="5" rx="2" fill="#4B5563" />
                  {/* Wheels */}
                  <circle cx="320" cy="115" r="10" fill="#1F2937" />
                  <circle cx="370" cy="115" r="10" fill="#1F2937" />
                  <circle cx="320" cy="115" r="6" fill="#3B82F6" />
                  <circle cx="370" cy="115" r="6" fill="#3B82F6" />
                </g>
              </svg>
            </div>
          </div>
        </div>
      </div>

    </div>
  );
}
 

 tailwind config
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
        "train-enter": "train-enter 1.4s cubic-bezier(0.34, 1.56, 0.64, 1) forwards",
        "train-bounce": "train-bounce 1.4s cubic-bezier(0.34, 1.56, 0.64, 1) forwards",
        "panda-bob": "panda-bob 1.2s ease-in-out 1.4s forwards, panda-idle 3s ease-in-out 2.6s infinite",
        "panda-wave": "panda-wave 2s ease-in-out infinite",
      },
      keyframes: {
        "train-enter": {
          from: {
            transform: "translateX(-120%)",
          },
          to: {
            transform: "translateX(0)",
          },
        },
        "train-bounce": {
          "0%": {
            transform: "translateY(0)",
          },
          "50%": {
            transform: "translateY(-12px)",
          },
          "100%": {
            transform: "translateY(0)",
          },
        },
        "panda-bob": {
          from: {
            transform: "translateY(0)",
          },
          to: {
            transform: "translateY(-20px)",
          },
        },
        "panda-idle": {
          "0%, 100%": {
            transform: "translateY(0)",
          },
          "50%": {
            transform: "translateY(-8px)",
          },
        },
        "panda-wave": {
          "0%, 100%": {
            transform: "rotate(0deg)",
          },
          "25%": {
            transform: "rotate(-20deg)",
          },
          "75%": {
            transform: "rotate(20deg)",
          },
        },
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
} satisfies Config;
