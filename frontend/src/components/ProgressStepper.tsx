import { motion } from "framer-motion";
import { Check, Gavel, Loader2 } from "lucide-react";
import { STAGES } from "@/lib/constants";
import type { ProgressEvent } from "@/types/api";
import { cn } from "@/lib/utils";

/** Live review stages ("Gathering evidence...") with a swinging gavel. */
export function ProgressStepper({ events }: { events: ProgressEvent[] }) {
  const reached = new Set(events.map((e) => e.stage));
  const current = events[events.length - 1];
  const progress = current?.progress ?? 0;
  const currentIndex = STAGES.findIndex((s) => s.stage === current?.stage);

  return (
    <div className="mx-auto max-w-2xl">
      <div className="mb-8 flex flex-col items-center text-center">
        <motion.div
          animate={{ rotate: [0, -28, 0] }}
          transition={{ repeat: Infinity, duration: 1.1, ease: "easeInOut", times: [0, 0.35, 1] }}
          style={{ originX: 0.8, originY: 0.8 }}
          className="mb-4 grid h-16 w-16 place-items-center rounded-2xl bg-gold/15 text-gold"
        >
          <Gavel className="h-8 w-8" />
        </motion.div>
        <h2 className="font-serif-display text-2xl font-semibold">Court is in session</h2>
        <p className="mt-1 text-sm text-muted">
          {currentIndex >= 0 ? STAGES[currentIndex].courtroom : "Waiting for a free courtroom"}...
          {current?.detail && <span className="text-fg"> · {current.detail}</span>}
        </p>
      </div>
      <div className="mb-6 h-2 overflow-hidden rounded-full bg-raised">
        <motion.div className="h-full bg-gradient-to-r from-gold/60 to-gold" animate={{ width: `${progress}%` }} transition={{ duration: 0.6 }} />
      </div>
      <ol className="grid gap-2 sm:grid-cols-2">
        {STAGES.map((s, i) => {
          const done = reached.has(s.stage) && i < currentIndex;
          const active = i === currentIndex;
          return (
            <li
              key={s.stage}
              className={cn(
                "flex items-center gap-3 rounded-xl border px-3 py-2.5 text-sm transition",
                active ? "border-gold/50 bg-gold/10 text-fg" : done ? "border-line bg-surface text-fg" : "border-line/60 text-muted",
              )}
            >
              <span
                className={cn(
                  "grid h-6 w-6 shrink-0 place-items-center rounded-full text-xs",
                  done ? "bg-acquitted/20 text-acquitted" : active ? "bg-gold/20 text-gold" : "bg-raised",
                )}
              >
                {done ? <Check className="h-3.5 w-3.5" /> : active ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : i + 1}
              </span>
              {s.label}
            </li>
          );
        })}
      </ol>
    </div>
  );
}
