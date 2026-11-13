import { motion } from "framer-motion";
import { Award, Briefcase, Crown, Flame, FlameKindling, Gavel, Landmark, Lock, ShieldCheck, Swords, TrendingUp, Zap, type LucideIcon } from "lucide-react";
import type { Badge } from "@/types/api";
import { cn, formatDate } from "@/lib/utils";

const ICONS: Record<string, LucideIcon> = {
  gavel: Gavel, "shield-check": ShieldCheck, zap: Zap, "trending-up": TrendingUp, swords: Swords, flame: Flame,
  "flame-kindling": FlameKindling, crown: Crown, briefcase: Briefcase, landmark: Landmark,
};

export function BadgeIcon({ icon, unlocked, className }: { icon: string; unlocked: boolean; className?: string }) {
  const Icon = ICONS[icon] ?? Award;
  return (
    <div className={cn("grid h-12 w-12 shrink-0 place-items-center rounded-2xl", unlocked ? "bg-gold/15 text-gold" : "bg-raised text-muted", className)}>
      {unlocked ? <Icon className="h-6 w-6" /> : <Lock className="h-5 w-5" />}
    </div>
  );
}

export function BadgeGrid({ badges }: { badges: Badge[] }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      {badges.map((b, i) => (
        <motion.div
          key={b.code}
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: i * 0.04 }}
          className={cn("flex items-center gap-4 rounded-2xl border p-4", b.unlocked ? "border-gold/30 bg-gradient-to-br from-gold/10 to-transparent" : "border-line bg-surface opacity-70")}
        >
          <BadgeIcon icon={b.icon} unlocked={b.unlocked} />
          <div className="min-w-0">
            <div className="font-semibold">{b.name}</div>
            <div className="text-xs text-muted">{b.description}</div>
            <div className="mt-1 text-[11px] text-muted">{b.unlocked ? `Unlocked ${formatDate(b.unlockedAt)}` : `+${b.xpReward} XP`}</div>
          </div>
        </motion.div>
      ))}
    </div>
  );
}
