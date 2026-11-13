import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { Trophy } from "lucide-react";
import { usePersonalBests } from "@/api/hooks";
import { PageHeader } from "@/components/PageHeader";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { GradeBadge } from "@/components/GradeBadge";
import { EmptyState } from "@/components/EmptyState";
import { cn, formatMs, formatN } from "@/lib/utils";

const RATING = {
  GREEN: { ring: "border-acquitted/50 from-acquitted/15", label: "Matches optimal", text: "text-acquitted" },
  YELLOW: { ring: "border-gold/50 from-gold/10", label: "Close", text: "text-gold" },
  RED: { ring: "border-guilty/50 from-guilty/10", label: "Far from optimal", text: "text-guilty" },
} as const;

export default function RecordsPage() {
  const { data, isLoading } = usePersonalBests();
  const navigate = useNavigate();
  if (isLoading || !data) return <Skeleton className="h-[500px]" />;
  const green = data.filter((d) => d.rating === "GREEN").length;

  return (
    <div>
      <PageHeader
        eyebrow="Hall of Records"
        title="Turn every card green"
        subtitle={data.length ? `${green} of ${data.length} problems match the optimal solution.` : undefined}
      />
      {data.length === 0 ? (
        <EmptyState icon={<Trophy className="h-6 w-6" />} title="No records yet" message="Your best attempt per problem will be shown here." action={<Button variant="gold" onClick={() => navigate("/cases/new")}>Open a case</Button>} />
      ) : (
        <>
          <div className="mb-6 h-3 overflow-hidden rounded-full bg-raised">
            <motion.div className="h-full bg-gradient-to-r from-acquitted/70 to-acquitted" initial={{ width: 0 }} animate={{ width: `${(green / data.length) * 100}%` }} transition={{ duration: 1 }} />
          </div>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {data.map((r, i) => {
              const meta = RATING[r.rating];
              return (
                <motion.button
                  key={r.problemId}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.04 }}
                  onClick={() => navigate(`/verdicts/${r.bestSubmissionId}`)}
                  className={cn("rounded-2xl border bg-gradient-to-br to-transparent p-5 text-left transition hover:-translate-y-0.5", meta.ring)}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <Badge>{r.topicLabel}</Badge>
                      <h3 className="mt-2 truncate font-semibold">{r.title}</h3>
                      <div className={cn("mt-0.5 text-xs font-medium", meta.text)}>{meta.label}</div>
                    </div>
                    <GradeBadge grade={r.grade} />
                  </div>
                  <div className="mt-4 grid grid-cols-3 gap-2 text-center">
                    <Stat label="Best score" value={String(r.bestScore)} />
                    <Stat label={r.inputSize ? `You @ ${formatN(r.inputSize)}` : "Your time"} value={formatMs(r.userMs)} />
                    <Stat label="Optimal" value={formatMs(r.optimalMs)} />
                  </div>
                  <div className="mt-3 text-xs text-muted">
                    {r.attempts} attempt{r.attempts > 1 ? "s" : ""}{r.ratio ? ` · ${r.ratio}× optimal time` : ""}{r.allHiddenPassed ? " · all hidden tests passed" : ""}
                  </div>
                </motion.button>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-bg/50 px-2 py-2">
      <div className="text-sm font-semibold tabular-nums">{value}</div>
      <div className="text-[10px] uppercase tracking-wider text-muted">{label}</div>
    </div>
  );
}
