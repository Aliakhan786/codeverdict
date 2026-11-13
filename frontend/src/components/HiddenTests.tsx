import { useState } from "react";
import { CheckCircle2, XCircle } from "lucide-react";
import type { HiddenTest } from "@/types/api";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

/** Failing inputs first; expected outputs come from the verified reference, never from AI guessing. */
export function HiddenTests({ cases, passed, total }: { cases: HiddenTest[]; passed: number; total: number }) {
  const [showAll, setShowAll] = useState(false);
  const failing = cases.filter((c) => !c.passed);
  const shown = showAll ? [...failing, ...cases.filter((c) => c.passed)] : failing.slice(0, 6);
  if (total === 0) return <p className="text-sm text-muted">Hidden tests were not available for this review.</p>;
  return (
    <div>
      <div className="mb-3 flex items-center gap-3">
        <div className={cn("text-3xl font-semibold tabular-nums", passed === total ? "text-acquitted" : "text-warn")}>
          {passed}<span className="text-lg text-muted"> / {total}</span>
        </div>
        <div className="text-xs text-muted">hidden tests passed<br />(normal + edge + random inputs)</div>
      </div>
      <div className="space-y-2">
        {shown.map((c, i) => (
          <div key={i} className={cn("rounded-lg border p-3 font-mono text-[12px]", c.passed ? "border-line" : "border-guilty/30 bg-guilty/5")}>
            <div className="mb-1 flex items-center gap-2 font-sans text-xs">
              {c.passed ? <CheckCircle2 className="h-3.5 w-3.5 text-acquitted" /> : <XCircle className="h-3.5 w-3.5 text-guilty" />}
              <span className="text-muted">{c.description ?? "test"}</span>
              {c.edgeCase && <Badge className="border-warn/30 text-warn">edge</Badge>}
            </div>
            <div className="break-all"><span className="text-muted">input    </span>{c.input}</div>
            <div className="break-all"><span className="text-muted">expected </span>{c.expected}</div>
            {!c.passed && (
              <div className="break-all text-guilty"><span className="text-muted">got      </span>{c.error ?? c.actual}</div>
            )}
          </div>
        ))}
      </div>
      {cases.length > shown.length && (
        <Button variant="ghost" size="sm" className="mt-2" onClick={() => setShowAll(true)}>
          Show all {cases.length} tests
        </Button>
      )}
      {failing.length === 0 && !showAll && <p className="text-sm text-acquitted">Every hidden test passed.</p>}
    </div>
  );
}
