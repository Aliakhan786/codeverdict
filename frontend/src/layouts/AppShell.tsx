import { useEffect } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { AnimatePresence, motion } from "framer-motion";
import { Award, BarChart3, Gavel, History, Home, LogOut, Plus, Settings, Trophy } from "lucide-react";
import { useAuth } from "@/auth/AuthContext";
import { XpBar } from "@/components/XpBar";
import { StreakFlame } from "@/components/StreakFlame";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

const NAV = [
  { to: "/", label: "Home", icon: Home, end: true },
  { to: "/cases/new", label: "New Case", icon: Plus },
  { to: "/history", label: "History", icon: History },
  { to: "/progress", label: "Progress", icon: BarChart3 },
  { to: "/records", label: "Records", icon: Trophy },
  { to: "/achievements", label: "Achievements", icon: Award },
  { to: "/settings", label: "Settings", icon: Settings },
];

export function AppShell() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Alt+N anywhere: open a new case
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.altKey && (e.key === "n" || e.key === "N")) {
        e.preventDefault();
        navigate("/cases/new");
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [navigate]);

  if (!user) return null;

  return (
    <div className="flex h-full">
      {/* Sidebar */}
      <aside className="hidden w-60 shrink-0 flex-col border-r border-line bg-surface/60 backdrop-blur md:flex">
        <div className="flex items-center gap-2.5 px-5 py-5">
          <div className="grid h-9 w-9 place-items-center rounded-xl bg-gold/15 text-gold">
            <Gavel className="h-5 w-5" />
          </div>
          <div>
            <div className="font-serif-display text-lg font-semibold leading-none">CodeVerdict</div>
            <div className="mt-1 text-[10px] uppercase tracking-[0.18em] text-muted">Proof, not guesses</div>
          </div>
        </div>
        <nav className="flex-1 space-y-0.5 px-3">
          {NAV.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                cn(
                  "group flex items-center gap-3 rounded-lg px-3 py-2 text-sm transition",
                  isActive ? "bg-gold/10 font-medium text-gold" : "text-muted hover:bg-raised hover:text-fg",
                )
              }
            >
              <Icon className="h-4 w-4" />
              {label}
              {to === "/cases/new" && <kbd className="ml-auto rounded border border-line px-1 text-[10px] text-muted">Alt N</kbd>}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-line p-4">
          <div className="mb-3 flex items-center gap-3">
            <div className="grid h-9 w-9 place-items-center rounded-full bg-raised font-semibold text-gold">
              {user.displayName.charAt(0).toUpperCase()}
            </div>
            <div className="min-w-0">
              <div className="truncate text-sm font-medium">{user.displayName}</div>
              <div className="truncate text-xs text-muted">@{user.username}</div>
            </div>
          </div>
          <Button
            variant="ghost"
            size="sm"
            className="w-full justify-start"
            onClick={async () => {
              await logout();
              navigate("/login");
            }}
          >
            <LogOut className="h-4 w-4" /> Log out
          </Button>
        </div>
      </aside>

      {/* Main column */}
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 flex items-center gap-4 border-b border-line bg-bg/80 px-4 py-3 backdrop-blur md:px-8">
          <div className="flex items-center gap-2 md:hidden">
            <Gavel className="h-5 w-5 text-gold" />
            <span className="font-serif-display font-semibold">CodeVerdict</span>
          </div>
          <div className="hidden w-72 md:block">
            <XpBar stats={user.stats} />
          </div>
          <div className="ml-auto flex items-center gap-3">
            {user.readOnly && <Badge className="border-info/40 text-info">Demo · read-only</Badge>}
            <StreakFlame days={user.stats.currentStreak} />
            <Button variant="gold" size="sm" onClick={() => navigate("/cases/new")}>
              <Plus className="h-4 w-4" /> New Case
            </Button>
          </div>
        </header>
        {/* mobile nav */}
        <nav className="flex gap-1 overflow-x-auto border-b border-line px-2 py-2 md:hidden">
          {NAV.map(({ to, label, end }) => (
            <NavLink
              key={to}
              to={to}
              end={end}
              className={({ isActive }) =>
                cn("whitespace-nowrap rounded-md px-3 py-1.5 text-xs", isActive ? "bg-gold/15 text-gold" : "text-muted")
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>
        <main className="min-w-0 flex-1 overflow-y-auto">
          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 6 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.18 }}
              className="mx-auto w-full max-w-7xl px-4 py-6 md:px-8 md:py-8"
            >
              <Outlet />
            </motion.div>
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
}
