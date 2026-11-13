import { useState } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { ChevronDown, FlaskConical, MapPin, Wand2 } from "lucide-react";
import type { Charge } from "@/types/api";
import { STATUS_META } from "@/lib/constants";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const CATEGORY: Record<string, string> = { BUG: "Bug", EDGE_CASE: "Edge case", PERFORMANCE: "Performance", STYLE: "Style" };

export function ChargeCard({ charge, index, onShowLines, onPreviewFix, onApplyFix }: {
  charge: Charge;
  index: number;
  onShowLines?: (line: number) => void;
  onPreviewFix?: (c: Charge) => void;
  onApplyFix?: (c: Charge) => void;
}) {
  const meta = STATUS_META[charge.status];
  const collapsedByDefault = charge.status === "DISMISSED" || charge.status === "NOTED" || charge.status === "ERROR";
  const [open, setOpen] = useState(!collapsedByDefault);
  const guilty = charge.status === "PROVEN" || charge.status === "ACQUITTED";

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.15 + index * 0.07 }}
      className={cn(
        "overflow-hidden rounded-xl border bg-surface",
        charge.status === "PROVEN" ? "border-guilty/40" : charge.status === "ACQUITTED" ? "border-acquitted/40" : "border-line",
      )}
    >
      <button className="flex w-full items-start gap-3 p-4 text-left" onClick={() => setOpen((o) => !o)}>
        <span className={cn("mt-0.5 rounded-md border px-2 py-0.5 text-[11px] font-bold uppercase tracking-wider", meta.className)}>
          {meta.label}
        </span>
        <div className="min-w-0 flex-1">
          <div className={cn("font-medium", !guilty && "text-muted")}>{charge.title}</div>
          <div className="mt-1 flex flex-wrap items-center gap-1.5 text-xs text-muted">
            <Badge>{CATEGORY[charge.category]}</Badge>
            <Badge>{charge.mistakeLabel}</Badge>
            <span
              role="link"
              tabIndex={0}
              className="inline-flex cursor-pointer items-center gap-1 hover:text-gold"
              onClick={(e) => {
                e.stopPropagation();
                onShowLines?.(charge.evidenceLine ?? charge.lineStart);
              }}
            >
              <MapPin className="h-3 w-3" /> line {charge.lineStart}{charge.lineEnd > charge.lineStart ? `-${charge.lineEnd}` : ""}
            </span>
            <span>· {meta.court}</span>
          </div>
        </div>
        <ChevronDown className={cn("mt-1 h-4 w-4 shrink-0 text-muted transition", open && "rotate-180")} />
      </button>
      <AnimatePresence initial={false}>
        {open && (
          <motion.div initial={{ height: 0 }} animate={{ height: "auto" }} exit={{ height: 0 }} className="overflow-hidden">
            <div className="space-y-3 border-t border-line px-4 pb-4 pt-3 text-sm">
              {charge.explanation && <p className="text-fg/90">{charge.explanation}</p>}
              {charge.statusReason && <p className="text-xs text-muted">Ruling: {charge.statusReason}</p>}
              {charge.proofTestCode && (
                <details className="group rounded-lg border border-line bg-bg/60" open={charge.status === "PROVEN"}>
                  <summary className="flex cursor-pointer items-center gap-2 px-3 py-2 text-xs font-medium text-muted">
                    <FlaskConical className="h-3.5 w-3.5 text-gold" /> Evidence: the proof test
                  </summary>
                  <pre className="max-h-64 overflow-auto border-t border-line px-3 py-2 font-mono text-[12px] leading-relaxed text-fg/90">
                    {charge.proofTestCode}
                  </pre>
                </details>
              )}
              {charge.proofOutput && (
                <div
                  className={cn(
                    "rounded-lg border px-3 py-2 font-mono text-[12px]",
                    guilty ? "border-guilty/30 bg-guilty/5 text-guilty" : "border-line bg-bg/60 text-muted",
                  )}
                >
                  {charge.proofOutput}
                </div>
              )}
              {charge.badEvidence && (
                <p className="text-xs text-dismissed">
                  The AI's test also failed on the trusted reference solution, so it was thrown out as bad evidence. This is the
                  double-check rule protecting your code.
                </p>
              )}
              {charge.suggestedFixCode && (
                <div
                  className={cn(
                    "rounded-lg border p-3",
                    charge.fixVerified ? "border-acquitted/30 bg-acquitted/5" : "border-warn/30 bg-warn/5",
                  )}
                >
                  <div className="mb-1 flex items-center gap-2 text-xs font-semibold">
                    <Wand2 className={cn("h-3.5 w-3.5", charge.fixVerified ? "text-acquitted" : "text-warn")} />
                    {charge.fixVerified ? "Suggested fix · test passes after fix" : "Suggested fix · NOT verified (the test still failed)"}
                  </div>
                  {charge.fixExplanation && <p className="text-xs text-fg/80">{charge.fixExplanation}</p>}
                  <div className="mt-2 flex gap-2">
                    <Button size="sm" variant="outline" onClick={() => onPreviewFix?.(charge)}>Preview fix</Button>
                    <Button size="sm" variant={charge.fixVerified ? "success" : "outline"} onClick={() => onApplyFix?.(charge)}>
                      Apply fix in editor
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
