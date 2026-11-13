import { motion } from "framer-motion";
import type { Stats } from "@/types/api";
import { cn } from "@/lib/utils";

export function XpBar({ stats, compact = false, className }: { stats: Stats; compact?: boolean; className?: string }) {
  const pct = stats.xpForNextLevel > 0 ? Math.min(100, (stats.xpIntoLevel / stats.xpForNextLevel) * 100) : 100;
  return (
    <div className={cn("min-w-0", className)}>
      {!compact && (
        <div className="mb-1 flex items-baseline justify-between text-xs">
          <span className="font-semibold text-gold">{stats.level}</span>
          <span className="text-muted">
            {stats.nextLevel ? `${stats.xpIntoLevel} / ${stats.xpForNextLevel} XP to ${stats.nextLevel}` : `${stats.totalXp} XP · max level`}
          </span>
        </div>
      )}
      <div className={cn("overflow-hidden rounded-full bg-raised", compact ? "h-1.5" : "h-2.5")}>
        <motion.div
          className="h-full rounded-full bg-gradient-to-r from-gold/70 to-gold"
          initial={{ width: 0 }}
          animate={{ width: `${pct}%` }}
          transition={{ duration: 0.9, ease: "easeOut" }}
        />
      </div>
    </div>
  );
}
