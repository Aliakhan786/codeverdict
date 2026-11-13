import { Link, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { ArrowRight, Bug, CalendarCheck, Gauge, Plus, Sparkles, Target } from "lucide-react";
import { useSummary } from "@/api/hooks";
import { Card, CardBody, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge, DifficultyBadge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { XpBar } from "@/components/XpBar";
import { StreakFlame } from "@/components/StreakFlame";
import { GradeBadge } from "@/components/GradeBadge";
import { EmptyState } from "@/components/EmptyState";
import { scoreColor, timeAgo } from "@/lib/utils";

export default function HomePage() {
  const { data, isLoading } = useSummary();
  const navigate = useNavigate();

  if (isLoading || !data) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-36 w-full" />
        <div className="grid gap-4 md:grid-cols-3">
          <Skeleton className="h-28" /> <Skeleton className="h-28" /> <Skeleton className="h-28" />
        </div>
      </div>
    );
  }

  const greeting = new Date().getHours() < 12 ? "Good morning" : new Date().getHours() < 18 ? "Good afternoon" : "Good evening";
  const goalPct = Math.min(100, (data.todayCount / Math.max(1, data.dailyGoal)) * 100);

  return (
    <div className="space-y-6">
      {/* Hero */}
      <Card className="relative overflow-hidden">
        <div className="pointer-events-none absolute -right-16 -top-16 h-64 w-64 rounded-full bg-gold/10 blur-3xl" />
        <CardBody className="grid gap-6 md:grid-cols-[1fr_auto] md:items-center">
          <div>
            <div className="text-[11px] font-semibold uppercase tracking-[0.2em] text-gold/80">Courtroom lobby</div>
            <h1 className="mt-1 font-serif-display text-3xl font-semibold">
              {greeting}, {data.displayName.split(" ")[0]}.
            </h1>
            <div className="mt-3 flex flex-wrap items-center gap-2">
              <Badge className="border-gold/40 bg-gold/10 text-gold">Level {data.levelIndex + 1} · {data.level}</Badge>
              <StreakFlame days={data.currentStreak} active={data.activeToday} />
              {data.longestStreak > 0 && <span className="text-xs text-muted">best streak {data.longestStreak} days</span>}
            </div>
            <XpBar
              className="mt-5 max-w-lg"
              stats={{
                totalXp: data.totalXp, level: data.level, levelIndex: data.levelIndex, nextLevel: data.nextLevel ?? null,
                xpIntoLevel: data.xpIntoLevel, xpForNextLevel: data.xpForNextLevel, currentStreak: data.currentStreak,
                longestStreak: data.longestStreak,
              }}
            />
          </div>
          <Button variant="gold" size="lg" onClick={() => navigate("/cases/new")} className="md:min-w-48">
            <Plus className="h-5 w-5" /> New Case
          </Button>
        </CardBody>
      </Card>

      {/* Quick stats */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<CalendarCheck className="h-4 w-4" />} label="Today's goal" value={`${data.todayCount} / ${data.dailyGoal}`}>
          <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-raised">
            <div className="h-full rounded-full bg-acquitted transition-all" style={{ width: `${goalPct}%` }} />
          </div>
        </StatCard>
        <StatCard icon={<Target className="h-4 w-4" />} label="Reviews this week" value={data.week.reviews} />
        <StatCard
          icon={<Gauge className="h-4 w-4" />}
          label="Avg verdict this week"
          value={<span className={scoreColor(data.week.avgScore)}>{data.week.avgScore ?? "-"}</span>}
        />
        <StatCard icon={<Bug className="h-4 w-4" />} label="Bugs proven this week" value={data.week.bugsProven} />
      </div>

      <div className="grid gap-6 lg:grid-cols-[1.1fr_1fr]">
        {/* Today's challenge */}
        <Card>
          <CardHeader title="Today's challenge" subtitle="Picked from your weakest topic" icon={<Sparkles className="h-4 w-4" />} />
          <CardBody>
            {data.todaysChallenge ? (
              <motion.div
                initial={{ opacity: 0, scale: 0.98 }}
                animate={{ opacity: 1, scale: 1 }}
                className="rounded-xl border border-gold/30 bg-gradient-to-br from-gold/10 to-transparent p-5"
              >
                <div className="flex flex-wrap items-center gap-2">
                  <Badge>{data.todaysChallenge.topicLabel}</Badge>
                  <DifficultyBadge value={data.todaysChallenge.difficulty} />
                  {data.todaysChallenge.bestScore !== undefined && (
                    <span className="text-xs text-muted">best so far {data.todaysChallenge.bestScore}</span>
                  )}
                </div>
                <h3 className="mt-3 text-xl font-semibold">{data.todaysChallenge.title}</h3>
                <p className="mt-1 text-sm text-muted">{data.todaysChallenge.reason}</p>
                <Button variant="gold" className="mt-5" onClick={() => navigate(`/cases/${data.todaysChallenge!.problemId}`)}>
                  Take the case <ArrowRight className="h-4 w-4" />
                </Button>
              </motion.div>
            ) : (
              <EmptyState title="No problems yet" message="Add a practice problem to get daily challenges." />
            )}
          </CardBody>
        </Card>

        {/* Continue */}
        <Card>
          <CardHeader title="Continue where you left off" subtitle="Your last three cases" />
          <CardBody className="space-y-2">
            {data.continueCases.length === 0 ? (
              <EmptyState
                title="Your docket is empty"
                message="Submit your first solution and it will show up here."
                action={<Button variant="gold" onClick={() => navigate("/cases/new")}>Open a case</Button>}
              />
            ) : (
              data.continueCases.map((c) => (
                <Link
                  key={c.problemId}
                  to={`/cases/${c.problemId}`}
                  className="flex items-center gap-4 rounded-xl border border-line bg-bg/40 p-3 transition hover:border-gold/40 hover:bg-raised"
                >
                  <GradeBadge grade={c.bestGrade} />
                  <div className="min-w-0 flex-1">
                    <div className="truncate font-medium">{c.title}</div>
                    <div className="text-xs text-muted">
                      {c.topicLabel} · {c.attempts} attempt{c.attempts === 1 ? "" : "s"} · {timeAgo(c.lastAttemptAt)}
                    </div>
                  </div>
                  <div className={`text-lg font-semibold tabular-nums ${scoreColor(c.bestScore)}`}>{c.bestScore ?? "-"}</div>
                </Link>
              ))
            )}
          </CardBody>
        </Card>
      </div>
    </div>
  );
}

function StatCard({ icon, label, value, children }: { icon: React.ReactNode; label: string; value: React.ReactNode; children?: React.ReactNode }) {
  return (
    <Card>
      <CardBody>
        <div className="flex items-center gap-2 text-xs text-muted">
          <span className="text-gold">{icon}</span>
          {label}
        </div>
        <div className="mt-2 text-2xl font-semibold tabular-nums">{value}</div>
        {children}
      </CardBody>
    </Card>
  );
}
