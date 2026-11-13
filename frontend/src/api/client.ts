import type { ApiErrorBody } from "@/types/api";

/** Empty = same origin (Vite proxy in dev, nginx/Vercel rewrite in production). */
export const API_BASE = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, "") ?? "";

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string, public details: unknown[] = []) {
    super(message);
  }
}

let onUnauthorized: (() => void) | null = null;
export function setUnauthorizedHandler(fn: () => void) {
  onUnauthorized = fn;
}

/**
 * fetch wrapper: sends the httpOnly auth cookie (credentials: include),
 * turns the backend's { error, message } body into an ApiError.
 */
export async function api<T>(path: string, init: RequestInit & { json?: unknown } = {}): Promise<T> {
  const { json, headers, ...rest } = init;
  const res = await fetch(`${API_BASE}/api/v1${path}`, {
    credentials: "include",
    ...rest,
    headers: {
      Accept: "application/json",
      ...(json !== undefined ? { "Content-Type": "application/json" } : {}),
      ...headers,
    },
    body: json !== undefined ? JSON.stringify(json) : rest.body,
  });
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  const body = text ? safeJson(text) : null;
  if (!res.ok) {
    const err = (body ?? {}) as Partial<ApiErrorBody>;
    if (res.status === 401 && !path.startsWith("/auth/")) onUnauthorized?.();
    throw new ApiError(res.status, err.error ?? "HTTP_" + res.status, err.message ?? res.statusText, err.details ?? []);
  }
  return body as T;
}

function safeJson(text: string): unknown {
  try {
    return JSON.parse(text);
  } catch {
    return { error: "BAD_RESPONSE", message: text.slice(0, 200) };
  }
}

export function eventsUrl(submissionId: string) {
  return `${API_BASE}/api/v1/submissions/${submissionId}/events`;
}

export function exportUrl() {
  return `${API_BASE}/api/v1/export/history.csv`;
}
