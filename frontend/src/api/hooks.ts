import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "./client";
import type {
  AuthConfig, Badge, CompareResponse, HistoryRow, Me, OptimalSolution, Page, PersonalBest, Problem,
  ProblemRequest, ProgressData, RunResponse, SubmitResponse, Summary, SystemStatus, Verdict,
} from "@/types/api";

export const qk = {
  me: ["me"] as const,
  problems: ["problems"] as const,
  problem: (id: string) => ["problems", id] as const,
  verdict: (id: string) => ["verdict", id] as const,
  history: (params: Record<string, unknown>) => ["history", params] as const,
  summary: ["summary"] as const,
  progress: ["progress"] as const,
  bests: ["bests"] as const,
  achievements: ["achievements"] as const,
};

export const useAuthConfig = () =>
  useQuery({ queryKey: ["auth-config"], queryFn: () => api<AuthConfig>("/auth/config"), staleTime: Infinity });

export const useProblems = () => useQuery({ queryKey: qk.problems, queryFn: () => api<Problem[]>("/problems") });

export const useProblem = (id: string | undefined) =>
  useQuery({ queryKey: qk.problem(id ?? ""), queryFn: () => api<Problem>(`/problems/${id}`), enabled: !!id });

export function useSaveProblem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, body }: { id?: string; body: ProblemRequest }) =>
      id ? api<Problem>(`/problems/${id}`, { method: "PUT", json: body }) : api<Problem>("/problems", { method: "POST", json: body }),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.problems }),
  });
}

export function useDeleteProblem() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api<void>(`/problems/${id}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.problems }),
  });
}

export const useRunCode = (problemId: string) =>
  useMutation({ mutationFn: (code: string) => api<RunResponse>(`/problems/${problemId}/run`, { method: "POST", json: { code } }) });

export const useSubmit = (problemId: string) =>
  useMutation({
    mutationFn: (code: string) => api<SubmitResponse>(`/problems/${problemId}/submissions`, { method: "POST", json: { code } }),
  });

export const useOptimal = (problemId: string, enabled: boolean) =>
  useQuery({
    queryKey: ["optimal", problemId],
    queryFn: () => api<OptimalSolution>(`/problems/${problemId}/optimal`),
    enabled,
    retry: false,
  });

export const useVerdict = (id: string | undefined) =>
  useQuery({ queryKey: qk.verdict(id ?? ""), queryFn: () => api<Verdict>(`/submissions/${id}`), enabled: !!id });

export function useHistory(params: Record<string, string | number | undefined>) {
  const qs = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== "") qs.set(k, String(v));
  });
  return useQuery({
    queryKey: qk.history(params),
    queryFn: () => api<Page<HistoryRow>>(`/submissions?${qs.toString()}`),
    placeholderData: (prev) => prev,
  });
}

export const useCompare = (a?: string, b?: string) =>
  useQuery({
    queryKey: ["compare", a, b],
    queryFn: () => api<CompareResponse>(`/submissions/compare?a=${a}&b=${b}`),
    enabled: !!a && !!b,
  });

export const useSummary = () => useQuery({ queryKey: qk.summary, queryFn: () => api<Summary>("/dashboard/summary") });
export const useProgress = () => useQuery({ queryKey: qk.progress, queryFn: () => api<ProgressData>("/dashboard/progress") });
export const usePersonalBests = () =>
  useQuery({ queryKey: qk.bests, queryFn: () => api<PersonalBest[]>("/dashboard/personal-bests") });
export const useAchievements = () => useQuery({ queryKey: qk.achievements, queryFn: () => api<Badge[]>("/achievements") });
export const useSystemStatus = () => useQuery({ queryKey: ["status"], queryFn: () => api<SystemStatus>("/settings/status") });

export function useUpdateSettings() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { displayName: string; theme: "dark" | "light"; soundEnabled: boolean; dailyGoal: number }) =>
      api<Me>("/settings", { method: "PUT", json: body }),
    onSuccess: (me) => qc.setQueryData(qk.me, me),
  });
}

export async function applyFix(chargeId: string) {
  return api<{ chargeId: string; problemId: string; code: string; fixVerified: boolean }>(`/charges/${chargeId}/apply-fix`, {
    method: "POST",
  });
}
