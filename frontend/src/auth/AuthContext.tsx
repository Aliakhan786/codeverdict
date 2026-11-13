import { createContext, useCallback, useContext, useEffect, useMemo, type ReactNode } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { api, ApiError, setUnauthorizedHandler } from "@/api/client";
import { qk } from "@/api/hooks";
import type { AuthResponse, Me } from "@/types/api";

interface AuthState {
  user: Me | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<Me>;
  register: (body: { username: string; email: string; password: string; displayName?: string }) => Promise<Me>;
  logout: () => Promise<void>;
  refresh: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

/**
 * The JWT lives in an httpOnly cookie that JavaScript cannot read (safer against XSS).
 * On page load we simply ask the server "who am I?" via /auth/me.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const qc = useQueryClient();
  const meQuery = useQuery({
    queryKey: qk.me,
    queryFn: async () => {
      try {
        return await api<Me>("/auth/me");
      } catch (e) {
        if (e instanceof ApiError && e.status === 401) return null;
        throw e;
      }
    },
    staleTime: 30_000,
    retry: false,
  });

  useEffect(() => {
    setUnauthorizedHandler(() => qc.setQueryData(qk.me, null));
  }, [qc]);

  const user = meQuery.data ?? null;

  useEffect(() => {
    const theme = user?.theme ?? "dark";
    document.documentElement.classList.toggle("dark", theme === "dark");
    document.documentElement.classList.toggle("light", theme === "light");
  }, [user?.theme]);

  const login = useCallback(
    async (username: string, password: string) => {
      const res = await api<AuthResponse>("/auth/login", { method: "POST", json: { username, password } });
      qc.clear();
      qc.setQueryData(qk.me, res.user);
      return res.user;
    },
    [qc],
  );

  const register = useCallback(
    async (body: { username: string; email: string; password: string; displayName?: string }) => {
      const res = await api<AuthResponse>("/auth/register", { method: "POST", json: body });
      qc.clear();
      qc.setQueryData(qk.me, res.user);
      return res.user;
    },
    [qc],
  );

  const logout = useCallback(async () => {
    await api<void>("/auth/logout", { method: "POST" }).catch(() => undefined);
    qc.clear();
    qc.setQueryData(qk.me, null);
  }, [qc]);

  const value = useMemo<AuthState>(
    () => ({
      user,
      loading: meQuery.isLoading,
      login,
      register,
      logout,
      refresh: () => qc.invalidateQueries({ queryKey: qk.me }),
    }),
    [user, meQuery.isLoading, login, register, logout, qc],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
