import { useEffect, useState } from "react";
import { animate, motion, useMotionValue, useTransform } from "framer-motion";
import { GradeBadge } from "@/components/GradeBadge";
import { cn, scoreColor } from "@/lib/utils";

/** The reveal: the stamp lands, the score counts up from 0, the grade pops in last. */
export function VerdictStamp({ score, grade, animateIn = true, label }: { score: number; grade: string; animateIn?: boolean; label: string }) {
  const count = useMotionValue(animateIn ? 0 : score);
  const rounded = useTransform(count, (v) => Math.round(v));
  const [display, setDisplay] = useState(animateIn ? 0 : score);
  const [showGrade, setShowGrade] = useState(!animateIn);

  useEffect(() => rounded.on("change", setDisplay), [rounded]);

  useEffect(() => {
    if (!animateIn) return;
    const controls = animate(count, score, { duration: 1.4, delay: 0.45, ease: "easeOut", onComplete: () => setShowGrade(true) });
    return () => controls.stop();
  }, [animateIn, count, score]);

  return (
    <div className="flex items-center gap-6">
      <motion.div
        initial={animateIn ? { scale: 2.4, rotate: -24, opacity: 0 } : false}
        animate={{ scale: 1, rotate: -8, opacity: 1 }}
        transition={{ type: "spring", stiffness: 260, damping: 14 }}
        className={cn(
          "grid h-36 w-36 shrink-0 place-items-center rounded-full border-[6px] border-double",
          score >= 70 ? "border-acquitted/70" : score >= 50 ? "border-warn/70" : "border-guilty/70",
        )}
      >
        <div className="text-center">
          <div className={cn("font-serif-display text-5xl font-bold tabular-nums", scoreColor(score))}>{display}</div>
          <div className="text-[10px] font-semibold uppercase tracking-[0.25em] text-muted">{label}</div>
        </div>
      </motion.div>
      <motion.div initial={false} animate={showGrade ? { scale: [0.3, 1.25, 1], opacity: 1 } : { scale: 0.3, opacity: 0 }} transition={{ duration: 0.45 }}>
        <GradeBadge grade={grade} size="lg" />
      </motion.div>
    </div>
  );
}
