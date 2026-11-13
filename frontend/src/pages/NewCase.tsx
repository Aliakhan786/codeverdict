import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Plus, Search } from "lucide-react";
import { useProblems } from "@/api/hooks";
import { PageHeader } from "@/components/PageHeader";
import { Button } from "@/components/ui/button";
import { Input, Select } from "@/components/ui/input";
import { Card } from "@/components/ui/card";
import { Badge, DifficultyBadge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { GradeBadge } from "@/components/GradeBadge";
import { EmptyState } from "@/components/EmptyState";
import { ProblemDialog } from "@/components/ProblemDialog";
import { DIFFICULTIES, TOPICS } from "@/lib/constants";
import { scoreColor, timeAgo } from "@/lib/utils";

export default function NewCasePage() {
  const { data: problems, isLoading } = useProblems();
  const navigate = useNavigate();
  const [q, setQ] = useState("");
  const [topic, setTopic] = useState("");
  const [difficulty, setDifficulty] = useState("");
  const [open, setOpen] = useState(false);

  const filtered = useMemo(
    () =>
      (problems ?? []).filter(
        (p) =>
          (!q || p.title.toLowerCase().includes(q.toLowerCase())) &&
          (!topic || p.topic === topic) &&
          (!difficulty || p.difficulty === difficulty),
      ),
    [problems, q, topic, difficulty],
  );

  return (
    <div>
      <PageHeader
        eyebrow="New case"
        title="Pick a problem"
        subtitle="Choose a problem to solve, or add your own from LeetCode, a book or an interview."
        action={<Button variant="gold" onClick={() => setOpen(true)}><Plus className="h-4 w-4" /> Add problem</Button>}
      />
      <div className="mb-5 flex flex-wrap gap-3">
        <div className="relative min-w-56 flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" />
          <Input className="pl-9" placeholder="Search problems" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        <Select className="w-44" value={topic} onChange={(e) => setTopic(e.target.value)}>
          <option value="">All topics</option>
          {TOPICS.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
        </Select>
        <Select className="w-36" value={difficulty} onChange={(e) => setDifficulty(e.target.value)}>
          <option value="">Any difficulty</option>
          {DIFFICULTIES.map((d) => <option key={d} value={d}>{d.charAt(0) + d.slice(1).toLowerCase()}</option>)}
        </Select>
      </div>

      {isLoading ? (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => <Skeleton key={i} className="h-36" />)}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          title={problems?.length ? "No problems match" : "No problems yet"}
          message={problems?.length ? "Try other filters." : "Add your first practice problem to start."}
          action={<Button variant="gold" onClick={() => setOpen(true)}>Add problem</Button>}
        />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {filtered.map((p) => (
            <Card
              key={p.id}
              role="button"
              tabIndex={0}
              onClick={() => navigate(`/cases/${p.id}`)}
              onKeyDown={(e) => e.key === "Enter" && navigate(`/cases/${p.id}`)}
              className="group cursor-pointer p-5 transition hover:-translate-y-0.5 hover:border-gold/40"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex flex-wrap gap-1.5">
                    <Badge>{p.topicLabel}</Badge>
                    <DifficultyBadge value={p.difficulty} />
                  </div>
                  <h3 className="mt-3 truncate text-base font-semibold group-hover:text-gold">{p.title}</h3>
                  <p className="mt-1 truncate font-mono text-xs text-muted">{p.methodSignature}</p>
                </div>
                <GradeBadge grade={p.bestGrade} />
              </div>
              <div className="mt-4 flex items-center justify-between text-xs text-muted">
                <span>{p.attempts ? `${p.attempts} attempt${p.attempts > 1 ? "s" : ""} · ${timeAgo(p.lastAttemptAt)}` : "Not attempted yet"}</span>
                {p.bestScore !== undefined && <span className={`font-semibold ${scoreColor(p.bestScore)}`}>best {p.bestScore}</span>}
              </div>
            </Card>
          ))}
        </div>
      )}
      <ProblemDialog open={open} onOpenChange={setOpen} onSaved={(p) => navigate(`/cases/${p.id}`)} />
    </div>
  );
}
