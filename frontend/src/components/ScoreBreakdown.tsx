import { motion } from "framer-motion";
import { Tip } from "@/components/ui/tooltip";
import { Info } from "lucide-react";
import { cn } from "@/lib/utils";

const PARTS = [
  { key: "correctness", label: "Correctness", max: 40, how: "40 × hidden tests passed ÷ total", source: "Measured" },
  { key: "performance", label: "Performance", max: 30, how: "vs the optimal solution at the largest input: ≤2× → 30, ≤10× → 20, finishes → 10", source: "Measured" },
  { key: "edgeCases", label: "Edge cases", max: 10, how: "10 − 3 per proven edge-case charge", source: "Measured" },
  { key: "quality", label: "Code quality", max: 20, how: "naming, readability, structure, duplication (0-5 each)", source: "AI-rated" },
] as const;

type Scores = { correctness?: number; performance?: number; edgeCases?: number; quality?: number; partial: boolean };

export function ScoreBreakdown({ scores }: { scores: Scores }) {
  return (
    <div className="space-y-3.5">
      {PARTS.map((p, i) => {
        const v = scores[p.key];
        const pct = v === undefined ? 0 : (v / p.max) * 100;
        return (
          <div key={p.key}>
            <div className="mb-1 flex items-center justify-between text-xs">
              <span className="flex items-center gap-1.5 font-medium text-fg">
                {p.label}
                <Tip content={<span>{p.how}</span>}>
                  <Info className="h-3 w-3 text-muted" />
                </Tip>
                <span className={cn("rounded px-1 text-[10px]", p.source === "Measured" ? "bg-acquitted/10 text-acquitted" : "bg-info/10 text-info")}>
                  {p.source}
                </span>
              </span>
              <span className="tabular-nums text-muted">{v === undefined ? "not measured" : `${v} / ${p.max}`}</span>
            </div>
            <div className="h-2.5 overflow-hidden rounded-full bg-raised">
              <motion.div
                className={cn("h-full rounded-full", pct >= 80 ? "bg-acquitted" : pct >= 50 ? "bg-gold" : "bg-guilty")}
                initial={{ width: 0 }}
                animate={{ width: `${pct}%` }}
                transition={{ duration: 0.8, delay: 0.2 + i * 0.12 }}
              />
            </div>
          </div>
        );
      })}
      <p className="pt-1 text-[11px] text-muted">
        80% of the score is measured by running code; only code quality is AI opinion.
        {scores.partial && " Some parts could not be computed this time, so the score is scaled over the parts that were."}
      </p>
    </div>
  );
}
