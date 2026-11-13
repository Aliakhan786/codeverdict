import { motion } from "framer-motion";
import { Crown } from "lucide-react";
import { Dialog } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import type { Badge as BadgeT } from "@/types/api";
import { BadgeIcon } from "@/components/BadgeGrid";

export function LevelUpModal({ open, onClose, level, badges }: { open: boolean; onClose: () => void; level?: string; badges: BadgeT[] }) {
  return (
    <Dialog open={open} onOpenChange={(o) => !o && onClose()} title={level ? "Promotion!" : "New badge unlocked"} className="max-w-md text-center">
      <div className="flex flex-col items-center gap-4 py-2">
        {level && (
          <motion.div initial={{ scale: 0.4, rotate: -20 }} animate={{ scale: 1, rotate: 0 }} transition={{ type: "spring", stiffness: 220, damping: 12 }}
            className="grid h-24 w-24 place-items-center rounded-3xl bg-gold/15 text-gold shadow-gold">
            <Crown className="h-12 w-12" />
          </motion.div>
        )}
        {level && <div className="font-serif-display text-3xl font-semibold text-gold">{level}</div>}
        {badges.length > 0 && (
          <div className="grid w-full gap-2">
            {badges.map((b, i) => (
              <motion.div key={b.code} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 + i * 0.15 }}
                className="flex items-center gap-3 rounded-xl border border-gold/30 bg-gold/5 p-3 text-left">
                <BadgeIcon icon={b.icon} unlocked />
                <div>
                  <div className="font-semibold">{b.name}</div>
                  <div className="text-xs text-muted">{b.description} · +{b.xpReward} XP</div>
                </div>
              </motion.div>
            ))}
          </div>
        )}
        <Button variant="gold" onClick={onClose} className="mt-2 w-full">Back to the verdict</Button>
      </div>
    </Dialog>
  );
}
