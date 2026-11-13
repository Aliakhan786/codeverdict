import { lazy, Suspense, type ReactNode } from "react";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { useAuth } from "@/auth/AuthContext";
import { AppShell } from "@/layouts/AppShell";
import { Skeleton } from "@/components/ui/skeleton";
import LoginPage from "@/pages/Login";
import HomePage from "@/pages/Home";
import NotFoundPage from "@/pages/NotFound";

// Heavy pages (Monaco editor, charts) are loaded on demand.
const NewCasePage = lazy(() => import("@/pages/NewCase"));
const CaseEditorPage = lazy(() => import("@/pages/CaseEditor"));
const VerdictPage = lazy(() => import("@/pages/Verdict"));
const HistoryPage = lazy(() => import("@/pages/History"));
const ComparePage = lazy(() => import("@/pages/Compare"));
const ProgressPage = lazy(() => import("@/pages/Progress"));
const RecordsPage = lazy(() => import("@/pages/Records"));
const AchievementsPage = lazy(() => import("@/pages/Achievements"));
const SettingsPage = lazy(() => import("@/pages/Settings"));

function RequireAuth({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();
  const location = useLocation();
  if (loading) {
    return (
      <div className="grid h-full place-items-center">
        <div className="font-serif-display animate-pulse text-xl text-gold">CodeVerdict</div>
      </div>
    );
  }
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  return <>{children}</>;
}

function PageFallback() {
  return (
    <div className="space-y-4 p-6">
      <Skeleton className="h-8 w-64" />
      <Skeleton className="h-40 w-full" />
      <Skeleton className="h-40 w-full" />
    </div>
  );
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        <Route index element={<HomePage />} />
        <Route path="cases/new" element={<Suspense fallback={<PageFallback />}><NewCasePage /></Suspense>} />
        <Route path="cases/:problemId" element={<Suspense fallback={<PageFallback />}><CaseEditorPage /></Suspense>} />
        <Route path="verdicts/:submissionId" element={<Suspense fallback={<PageFallback />}><VerdictPage /></Suspense>} />
        <Route path="history" element={<Suspense fallback={<PageFallback />}><HistoryPage /></Suspense>} />
        <Route path="history/compare" element={<Suspense fallback={<PageFallback />}><ComparePage /></Suspense>} />
        <Route path="progress" element={<Suspense fallback={<PageFallback />}><ProgressPage /></Suspense>} />
        <Route path="records" element={<Suspense fallback={<PageFallback />}><RecordsPage /></Suspense>} />
        <Route path="achievements" element={<Suspense fallback={<PageFallback />}><AchievementsPage /></Suspense>} />
        <Route path="settings" element={<Suspense fallback={<PageFallback />}><SettingsPage /></Suspense>} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
