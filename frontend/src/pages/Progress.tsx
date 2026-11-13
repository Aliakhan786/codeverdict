import { Activity, Bug, CheckCircle2, Crosshair, ShieldX, Target, TrendingUp } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useProgress } from "@/api/hooks";
import { PageHeader } from "@/components/PageHeader";
import { Card, CardBody, CardHeader } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Button } from "@/components/ui/button";
import { EmptyState } from "@/components/EmptyState";
import { TopicStrength, WeeklyScoreChart } from "@/components/charts";
import { ActivityHeatmap } from "@/components/ActivityHeatmap";
import { MistakeList } from "@/components/MistakeList";

export default function ProgressPage() {
  const { data, isLoading } = useProgress();
  const navigate = useNavigate();
  if (isLoading || !data) return <Skeleton className="h-[600px]" />;

  if (data.counters.totalCases === 0) {
    return (
      <div>
        <PageHeader eyebrow="Progress" title="Your growth, measured" />
        <EmptyState title="No verdicts yet" message="Your charts appear after your first review." action={<Button variant="gold" onClick={() => navigate("/cases/new")}>Open a case</Button>} />
      </div>
    );
  }

  const c = data.counters;
  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Progress" title="Your growth, measured" subtitle="Every number here comes from real test runs and timings." />
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
        <Counter icon={<Target className="h-4 w-4" />} label="Total cases" value={c.totalCases} />
        <Counter icon={<Bug className="h-4 w-4" />} label="Bugs proven" value={c.bugsProven} />
        <Counter icon={<CheckCircle2 className="h-4 w-4" />} label="Fixes verified" value={c.fixesVerified} />
        <Counter icon={<Crosshair className="h-4 w-4" />} label="Matched optimal" value={c.matchedOptimal} />
        <Counter icon={<ShieldX className="h-4 w-4" />} label="AI false alarms caught" value={c.aiFalseAlarmsCaught} hint="Charges the tests did not confirm" />
      </div>
      <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
        <Card>
          <CardHeader title="Average verdict per week" subtitle="Last 12 weeks" icon={<TrendingUp className="h-4 w-4" />} />
          <CardBody>{data.weeklyScores.length ? <WeeklyScoreChart data={data.weeklyScores} /> : <p className="text-sm text-muted">Not enough data yet.</p>}</CardBody>
        </Card>
        <Card>
          <CardHeader title="Top repeated mistakes" subtitle="Only mistakes proven by a test" icon={<Bug className="h-4 w-4" />} />
          <CardBody>{data.topMistakes.length ? <MistakeList mistakes={data.topMistakes} /> : <p className="text-sm text-muted">No proven mistakes yet. Nice.</p>}</CardBody>
        </Card>
      </div>
      <div className="grid gap-6 lg:grid-cols-[1fr_1.4fr]">
        <Card>
          <CardHeader title="Topic strength" subtitle="Weakest first - practise these" />
          <CardBody><TopicStrength data={data.topicStrength} /></CardBody>
        </Card>
        <Card>
          <CardHeader title="Activity" subtitle="Days you practised" icon={<Activity className="h-4 w-4" />} />
          <CardBody><ActivityHeatmap days={data.heatmap} /></CardBody>
        </Card>
      </div>
    </div>
  );
}

function Counter({ icon, label, value, hint }: { icon: React.ReactNode; label: string; value: number; hint?: string }) {
  return (
    <Card title={hint}>
      <CardBody>
        <div className="flex items-center gap-2 text-xs text-muted"><span className="text-gold">{icon}</span>{label}</div>
        <div className="mt-2 text-3xl font-semibold tabular-nums">{value}</div>
      </CardBody>
    </Card>
  );
}
