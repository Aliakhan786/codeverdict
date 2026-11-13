import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { GitCompare, X } from "lucide-react";
import { toast } from "sonner";
import { useHistory, useProblems } from "@/api/hooks";
import { PageHeader } from "@/components/PageHeader";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Select } from "@/components/ui/input";
import { DifficultyBadge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { GradeBadge } from "@/components/GradeBadge";
import { EmptyState } from "@/components/EmptyState";
import { DIFFICULTIES, TOPICS, topicLabel } from "@/lib/constants";
import { cn, formatDate, scoreColor } from "@/lib/utils";

export default function HistoryPage() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const { data: problems } = useProblems();
  const [selected, setSelected] = useState<string[]>([]);
  const filters = {
    problemId: params.get("problemId") ?? undefined,
    topic: params.get("topic") ?? undefined,
    difficulty: params.get("difficulty") ?? undefined,
    minScore: params.get("minScore") ?? undefined,
    maxScore: params.get("maxScore") ?? undefined,
    from: params.get("from") ?? undefined,
    to: params.get("to") ?? undefined,
    page: Number(params.get("page") ?? 0),
    size: 15,
  };
  const { data, isLoading } = useHistory(filters);

  const setFilter = (k: string, v: string) => {
    const next = new URLSearchParams(params);
    if (v) next.set(k, v);
    else next.delete(k);
    next.delete("page");
    setParams(next);
  };

  const toggle = (id: string) =>
    setSelected((s) => (s.includes(id) ? s.filter((x) => x !== id) : s.length >= 2 ? [s[1], id] : [...s, id]));

  const compare = () => {
    const rows = data?.items.filter((r) => selected.includes(r.submissionId)) ?? [];
    if (rows.length !== 2) return;
    if (rows[0].problemId !== rows[1].problemId) {
      toast.error("Pick two attempts of the same problem");
      return;
    }
    const [a, b] = [...rows].sort((x, y) => x.attemptNo - y.attemptNo);
    navigate(`/history/compare?a=${a.submissionId}&b=${b.submissionId}`);
  };

  const hasFilters = ["problemId", "topic", "difficulty", "minScore", "maxScore", "from", "to"].some((k) => params.get(k));

  return (
    <div>
      <PageHeader
        eyebrow="Case history"
        title="Every verdict, on the record"
        subtitle="Tick two attempts of the same problem to compare them side by side."
        action={
          <Button variant="gold" disabled={selected.length !== 2} onClick={compare}>
            <GitCompare className="h-4 w-4" /> Compare attempts ({selected.length}/2)
          </Button>
        }
      />
      <div className="mb-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-6">
        <Select value={filters.problemId ?? ""} onChange={(e) => setFilter("problemId", e.target.value)} className="lg:col-span-2">
          <option value="">All problems</option>
          {problems?.map((p) => <option key={p.id} value={p.id}>{p.title}</option>)}
        </Select>
        <Select value={filters.topic ?? ""} onChange={(e) => setFilter("topic", e.target.value)}>
          <option value="">All topics</option>
          {TOPICS.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
        </Select>
        <Select value={filters.difficulty ?? ""} onChange={(e) => setFilter("difficulty", e.target.value)}>
          <option value="">Any difficulty</option>
          {DIFFICULTIES.map((d) => <option key={d} value={d}>{d}</option>)}
        </Select>
        <div className="flex gap-2">
          <Input type="number" min={0} max={100} placeholder="min" value={filters.minScore ?? ""} onChange={(e) => setFilter("minScore", e.target.value)} />
          <Input type="number" min={0} max={100} placeholder="max" value={filters.maxScore ?? ""} onChange={(e) => setFilter("maxScore", e.target.value)} />
        </div>
        <div className="flex gap-2">
          <Input type="date" value={filters.from ?? ""} onChange={(e) => setFilter("from", e.target.value)} title="From" />
          <Input type="date" value={filters.to ?? ""} onChange={(e) => setFilter("to", e.target.value)} title="To" />
        </div>
      </div>
      {hasFilters && (
        <Button variant="ghost" size="sm" className="mb-3" onClick={() => setParams({})}>
          <X className="h-3.5 w-3.5" /> Clear filters
        </Button>
      )}

      {isLoading ? (
        <Skeleton className="h-96" />
      ) : !data || data.items.length === 0 ? (
        <EmptyState title="No verdicts yet" message="Submit a solution and it will appear here." action={<Button variant="gold" onClick={() => navigate("/cases/new")}>New case</Button>} />
      ) : (
        <Card className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-line text-left text-xs uppercase tracking-wider text-muted">
                <th className="w-10 px-4 py-3" />
                <th className="px-4 py-3">Date</th>
                <th className="px-4 py-3">Problem</th>
                <th className="px-4 py-3">Topic</th>
                <th className="px-4 py-3 text-center">Attempt</th>
                <th className="px-4 py-3 text-right">Score</th>
                <th className="px-4 py-3 text-center">Grade</th>
                <th className="px-4 py-3 text-right">Bugs proven</th>
                <th className="px-4 py-3 text-right">Review time</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((r) => (
                <tr
                  key={r.submissionId}
                  onClick={() => navigate(`/verdicts/${r.submissionId}`)}
                  className={cn("cursor-pointer border-b border-line/60 transition hover:bg-raised", selected.includes(r.submissionId) && "bg-gold/5")}
                >
                  <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                    <input
                      type="checkbox"
                      className="h-4 w-4 accent-[rgb(var(--gold))]"
                      checked={selected.includes(r.submissionId)}
                      onChange={() => toggle(r.submissionId)}
                      aria-label="Select for comparison"
                    />
                  </td>
                  <td className="whitespace-nowrap px-4 py-3 text-muted">{formatDate(r.createdAt)}</td>
                  <td className="px-4 py-3 font-medium">
                    {r.problemTitle} <span className="ml-1 align-middle"><DifficultyBadge value={r.difficulty} /></span>
                  </td>
                  <td className="px-4 py-3 text-muted">{topicLabel(r.topic)}</td>
                  <td className="px-4 py-3 text-center tabular-nums">#{r.attemptNo}</td>
                  <td className={cn("px-4 py-3 text-right font-semibold tabular-nums", scoreColor(r.verdictScore))}>
                    {r.status === "DONE" ? r.verdictScore : <span className="text-xs font-normal text-muted">{r.status.toLowerCase()}</span>}
                  </td>
                  <td className="px-4 py-3 text-center"><GradeBadge grade={r.grade} size="sm" /></td>
                  <td className="px-4 py-3 text-right tabular-nums">{r.bugsProven}</td>
                  <td className="px-4 py-3 text-right tabular-nums text-muted">{r.durationMs ? `${(r.durationMs / 1000).toFixed(1)} s` : "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="flex items-center justify-between px-4 py-3 text-xs text-muted">
            <span>{data.total} verdict{data.total === 1 ? "" : "s"}</span>
            <div className="flex items-center gap-2">
              <Button size="sm" variant="ghost" disabled={data.page === 0} onClick={() => { const n = new URLSearchParams(params); n.set("page", String(data.page - 1)); setParams(n); }}>
                Previous
              </Button>
              <span>Page {data.page + 1} of {Math.max(1, data.totalPages)}</span>
              <Button size="sm" variant="ghost" disabled={data.page + 1 >= data.totalPages} onClick={() => { const n = new URLSearchParams(params); n.set("page", String(data.page + 1)); setParams(n); }}>
                Next
              </Button>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
}
