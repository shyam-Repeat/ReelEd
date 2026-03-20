layout
import { useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { Home, Settings, Send } from "lucide-react";
import { Button } from "./ui/button";

interface LayoutProps {
  children: React.ReactNode;
}

export default function Layout({ children }: LayoutProps) {
  const location = useLocation();
  const [feedbackSent, setFeedbackSent] = useState(false);

  const isActive = (path: string) => location.pathname === path;

  const handleSendFeedback = () => {
    setFeedbackSent(true);
    setTimeout(() => setFeedbackSent(false), 2000);
  };

  return (
    <div className="flex flex-col min-h-screen bg-gradient-to-br from-slate-50 via-slate-100 to-slate-50">
      {/* Top Header */}
      <div className="bg-white border-b border-slate-200 px-4 py-4 sm:px-6 flex items-center justify-between sticky top-0 z-10">
        <div className="flex-1">
          <h1 className="text-lg font-bold text-slate-900">Parent</h1>
          <p className="text-xs text-slate-600">Welcome back</p>
        </div>

        <Button
          onClick={handleSendFeedback}
          size="sm"
          className={`gap-2 transition-all ${
            feedbackSent
              ? "bg-green-600 hover:bg-green-600"
              : "bg-blue-600 hover:bg-blue-700"
          }`}
        >
          <Send className="w-4 h-4" />
          <span className="hidden sm:inline">
            {feedbackSent ? "Sent!" : "Feedback"}
          </span>
        </Button>
      </div>

      {/* Main Content Area */}
      <div className="flex-1 overflow-auto pb-20">
        {children}
      </div>

      {/* Bottom Tab Navigation */}
      <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-slate-200 flex items-center justify-around sm:justify-center sm:gap-8 px-4 py-3 z-50">
        <Link
          to="/"
          className={`flex flex-col items-center gap-1 px-4 py-2 transition-all ${
            isActive("/")
              ? "text-blue-600"
              : "text-slate-600 hover:text-slate-900"
          }`}
        >
          <Home className="w-6 h-6" />
          <span className="text-xs font-medium">Dashboard</span>
        </Link>

        <div className="w-px h-8 bg-slate-200" />

        <Link
          to="/controls"
          className={`flex flex-col items-center gap-1 px-4 py-2 transition-all ${
            isActive("/controls")
              ? "text-blue-600"
              : "text-slate-600 hover:text-slate-900"
          }`}
        >
          <Settings className="w-6 h-6" />
          <span className="text-xs font-medium">Controls</span>
        </Link>

        <div className="w-px h-8 bg-slate-200" />

        <Link
          to="/settings"
          className={`flex flex-col items-center gap-1 px-4 py-2 transition-all ${
            isActive("/settings")
              ? "text-blue-600"
              : "text-slate-600 hover:text-slate-900"
          }`}
        >
          <Settings className="w-6 h-6" />
          <span className="text-xs font-medium">Settings</span>
        </Link>
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
      },
    },
  },
  plugins: [require("tailwindcss-animate")],
} satisfies Config;
