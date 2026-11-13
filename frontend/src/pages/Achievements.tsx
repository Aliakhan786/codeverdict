import { useAchievements } from "@/api/hooks";
import { useAuth } from "@/auth/AuthContext";
import { PageHeader } from "@/components/PageHeader";
import { BadgeGrid } from "@/components/BadgeGrid";
import { Card, CardBody } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { XpBar } from "@/components/XpBar";
import { LEVELS } from "@/lib/constants";
import { cn } from "@/lib/utils";

export default function AchievementsPage() {
  const { data, isLoading } = useAchievements();
  const { user } = useAuth();
  const unlocked = data?.filter((b) => b.unlocked).length ?? 0;

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Achievements" title="Levels & badges" subtitle={data ? `${unlocked} of ${data.length} badges unlocked` : undefined} />
      {user && (
        <Card>
          <CardBody>
            <XpBar stats={user.stats} />
            <div className="mt-5 grid grid-cols-3 gap-2 sm:grid-cols-6">
              {LEVELS.map((l, i) => (
                <div key={l} className={cn("rounded-lg border px-2 py-2 text-center text-xs",
                  i === user.stats.levelIndex ? "border-gold bg-gold/10 font-semibold text-gold" : i < user.stats.levelIndex ? "border-line text-fg" : "border-line/60 text-muted")}>
                  <div className="text-[10px] uppercase tracking-wider opacity-70">Level {i + 1}</div>
                  {l}
                </div>
              ))}
            </div>
            <p className="mt-4 text-xs text-muted">
              XP: 10 + score/5 per review, +10 for a personal best, +15 for each mistake from your last attempt that you fixed, streak bonus
              every day you keep practising, and badge rewards.
            </p>
          </CardBody>
        </Card>
      )}
      {isLoading || !data ? <Skeleton className="h-96" /> : <BadgeGrid badges={data} />}
    </div>
  );
}
