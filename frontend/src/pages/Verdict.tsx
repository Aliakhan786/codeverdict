import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, ArrowDown, ArrowUp, ChevronLeft, Download, Eye, Gauge, ListChecks, RefreshCw, Sparkles, Swords, Timer } from "lucide-react";
import { toast } from "sonner";
import { qk, useOptimal, useVerdict } from "@/api/hooks";
import { useSubmissionEvents } from "@/hooks/useSubmissionEvents";
import { useAuth } from "@/auth/AuthContext";
import { ProgressStepper } from "@/components/ProgressStepper";
import { VerdictStamp } from "@/components/VerdictStamp";
import { ScoreBreakdown } from "@/components/ScoreBreakdown";
import { ChargeCard } from "@/components/ChargeCard";
import { CodeEditor, type LineHighlight, type LineMarker } from "@/components/CodeEditor";
import { DiffViewer } from "@/components/DiffViewer";
import { HiddenTests } from "@/components/HiddenTests";
import { SpeedChart } from "@/components/SpeedChart";
import { QualityCard } from "@/components/QualityCard";
import { LevelUpModal } from "@/components/LevelUpModal";
import { EmptyState } from "@/components/EmptyState";
import { Card, CardBody, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge, DifficultyBadge } from "@/components/ui/badge";
import { Dialog } from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { celebrate, gavelSound } from "@/lib/feedback";
import { cn, formatDate } from "@/lib/utils";
import type { Charge, Review, Verdict } from "@/types/api";

const ORDER: Record<string, number> = { PROVEN: 0, ACQUITTED: 1, NOTED: 3, DISMISSED: 4, ERROR: 5 };

export default function VerdictPage() {
  const { submissionId } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const { user, refresh } = useAuth();
  const { data: verdict, isLoading, refetch } = useVerdict(submissionId);
  const running = verdict?.status === "QUEUED" || verdict?.status === "RUNNING";
  const [watchedLive, setWatchedLive] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const celebrated = useRef(false);

  const { events, connectionLost } = useSubmissionEvents(submissionId, running, async () => {
    setWatchedLive(true);
    await refetch();
    refresh();
    qc.invalidateQueries({ queryKey: qk.summary });
    qc.invalidateQueries({ queryKey: qk.problems });
  });

  // Fallback polling if the SSE connection drops.
  useEffect(() => {
    if (!running || !connectionLost) return;
    const t = setInterval(() => refetch(), 3000);
    return () => clearInterval(t);
  }, [running, connectionLost, refetch]);

  useEffect(() => {
    if (running) setWatchedLive(true);
  }, [running]);

  // Rewards: only when the verdict was just delivered, never when re-opening an old one.
  const review = verdict?.review;
  useEffect(() => {
    if (!review || !watchedLive || celebrated.current || verdict?.status !== "DONE") return;
    celebrated.current = true;
    if (user?.soundEnabled) setTimeout(gavelSound, 450);
    if (review.xpEarned > 0) setTimeout(() => toast.success(`+${review.xpEarned} XP`, { description: review.personalBest ? "New personal best!" : undefined }), 1900);
    const big = !!review.newLevel || review.newBadges.length > 0;
    if (review.personalBest || big) setTimeout(() => celebrate(big), 1900);
    if (big) setTimeout(() => setModalOpen(true), 2600);
  }, [review, watchedLive, user?.soundEnabled, verdict?.status]);

  if (isLoading || !verdict) {
    return (
      <div className="space-y-4">
        <Skeleton className="h-44" />
        <Skeleton className="h-96" />
      </div>
    );
  }

  if (running) {
    return (
      <div className="py-6">
        <ProgressStepper events={events} />
        {connectionLost && <p className="mt-6 text-center text-xs text-muted">Live updates paused - checking every few seconds.</p>}
      </div>
    );
  }

  if (verdict.status === "FAILED" || !review) {
    return (
      <EmptyState
        icon={<AlertTriangle className="h-6 w-6" />}
        title="Mistrial"
        message={verdict.failureReason ?? "The review could not be completed."}
        action={
          <Button variant="gold" onClick={() => navigate(`/cases/${verdict.problemId}`, { state: { code: verdict.code } })}>
            <RefreshCw className="h-4 w-4" /> Back to the editor
          </Button>
        }
      />
    );
  }

  return (
    <>
      <VerdictView verdict={verdict} review={review} animateIn={watchedLive} />
      <LevelUpModal open={modalOpen} onClose={() => setModalOpen(false)} level={review.newLevel} badges={review.newBadges} />
    </>
  );
}

function VerdictView({ verdict, review, animateIn }: { verdict: Verdict; review: Review; animateIn: boolean }) {
  const navigate = useNavigate();
  const [revealLine, setRevealLine] = useState<number | undefined>();
  const [previewFix, setPreviewFix] = useState<Charge | null>(null);
  const [showOptimal, setShowOptimal] = useState(false);
  const optimal = useOptimal(verdict.problemId, showOptimal);

  const charges = useMemo(() => [...review.charges].sort((a, b) => ORDER[a.status] - ORDER[b.status]), [review.charges]);
  const visible = charges.filter((c) => c.status !== "ERROR");
  const errored = charges.filter((c) => c.status === "ERROR");
  const guilty = charges.filter((c) => c.status === "PROVEN" || c.status === "ACQUITTED");

  const markers = useMemo<LineMarker[]>(() => {
    if (review.kind === "COMPILE_ERROR") {
      return review.compileErrors.map((e) => ({ lineStart: e.line, lineEnd: e.line, message: e.message, severity: "error" }));
    }
    return guilty.map((c) => ({
      lineStart: c.evidenceLine ?? c.lineStart,
      lineEnd: c.evidenceLine ?? c.lineEnd,
      message: `${c.status === "ACQUITTED" ? "Proven (fix verified)" : "Proven"}: ${c.title}\n${c.explanation ?? ""}`,
      severity: "error",
    }));
  }, [review, guilty]);

  const highlights = useMemo<LineHighlight[]>(
    () => guilty.map((c) => ({ lineStart: c.lineStart, lineEnd: c.lineEnd, kind: c.status === "PROVEN" ? "guilty" : "acquitted" })),
    [guilty],
  );

  const delta = verdict.previousAttempt ? review.verdictScore - verdict.previousAttempt.verdictScore : null;
  const perf = review.performance;

  const applyFix = (c: Charge) => {
    if (!c.suggestedFixCode) return;
    navigate(`/cases/${verdict.problemId}`, { state: { code: c.suggestedFixCode } });
    toast.message("Fix loaded into the editor", { description: "Review it, then submit to see your score go up." });
  };

  const saveNotes = () => {
    const lines = [
      `# ${verdict.problemTitle} - attempt ${verdict.attemptNo}`,
      `Verdict: ${review.verdictScore}/100 (${review.grade}) - ${formatDate(verdict.createdAt)}`,
      "",
      review.summary,
      "",
      "## Proven issues",
      ...(guilty.length ? guilty.map((c) => `- [${c.mistakeLabel}] ${c.title} (line ${c.lineStart}): ${c.explanation ?? ""}`) : ["- none"]),
      "",
      "## Performance",
      `Yours: ${perf.measuredComplexity ?? perf.claimedComplexity ?? "?"}${perf.complexityMeasured ? " (measured)" : " (estimated)"} | optimal: ${perf.optimalComplexity ?? "?"}`,
      perf.keyIdea ? `Key idea: ${perf.keyIdea}` : "",
      "",
      "## My code",
      "```java",
      verdict.code,
      "```",
    ];
    const blob = new Blob([lines.join("\n")], { type: "text/markdown" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = `${verdict.problemTitle.replace(/\W+/g, "-").toLowerCase()}-attempt-${verdict.attemptNo}.md`;
    a.click();
    URL.revokeObjectURL(a.href);
  };

  return (
    <div className="space-y-6">
      {/* header */}
      <div className="flex flex-wrap items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
          <ChevronLeft className="h-4 w-4" /> Back
        </Button>
        <div>
          <div className="text-[11px] font-semibold uppercase tracking-[0.2em] text-gold/80">Verdict · attempt {verdict.attemptNo}</div>
          <h1 className="font-serif-display text-2xl font-semibold">{verdict.problemTitle}</h1>
        </div>
        <Badge>{verdict.topicLabel}</Badge>
        <DifficultyBadge value={verdict.difficulty} />
        <div className="ml-auto flex flex-wrap gap-2">
          <Button variant="outline" size="sm" onClick={saveNotes}><Download className="h-4 w-4" /> Save to notes</Button>
          <Button variant="outline" size="sm" onClick={() => setShowOptimal(true)}><Eye className="h-4 w-4" /> See optimal solution</Button>
          <Button variant="gold" size="sm" onClick={() => navigate(`/cases/${verdict.problemId}`, { state: { code: verdict.code } })}>
            <Swords className="h-4 w-4" /> Rematch
          </Button>
        </div>
      </div>

      {review.warnings.length > 0 && (
        <div className="flex gap-3 rounded-xl border border-warn/30 bg-warn/5 p-4 text-sm">
          <AlertTriangle className="h-5 w-5 shrink-0 text-warn" />
          <ul className="space-y-1">{review.warnings.map((w) => <li key={w}>{w}</li>)}</ul>
        </div>
      )}

      {/* the verdict */}
      <div className="grid gap-6 lg:grid-cols-[1.2fr_1fr]">
        <Card className="relative overflow-hidden">
          <div className="pointer-events-none absolute -right-20 -top-20 h-60 w-60 rounded-full bg-gold/10 blur-3xl" />
          <CardBody className="flex flex-col gap-6 sm:flex-row sm:items-center">
            <VerdictStamp score={review.verdictScore} grade={review.grade} animateIn={animateIn} label={review.kind === "COMPILE_ERROR" ? "no compile" : "verdict"} />
            <div className="min-w-0">
              <p className="text-base leading-relaxed">{review.summary}</p>
              <div className="mt-3 flex flex-wrap items-center gap-2 text-xs">
                {delta !== null && (
                  <span className={cn("inline-flex items-center gap-1 rounded-md px-2 py-1 font-semibold",
                    delta > 0 ? "bg-acquitted/15 text-acquitted" : delta < 0 ? "bg-guilty/15 text-guilty" : "bg-raised text-muted")}>
                    {delta > 0 ? <ArrowUp className="h-3 w-3" /> : delta < 0 ? <ArrowDown className="h-3 w-3" /> : null}
                    {delta > 0 ? "+" : ""}{delta} vs last attempt
                  </span>
                )}
                {review.personalBest && <Badge className="border-gold/40 bg-gold/10 text-gold"><Sparkles className="h-3 w-3" /> Personal best</Badge>}
                {review.xpEarned > 0 && <Badge className="border-gold/40 text-gold">+{review.xpEarned} XP</Badge>}
                {review.referenceVerified && <Badge className="border-acquitted/30 text-acquitted">Reference verified</Badge>}
              </div>
            </div>
          </CardBody>
        </Card>
        <Card>
          <CardHeader title="Score breakdown" icon={<Gauge className="h-4 w-4" />} />
          <CardBody>
            <ScoreBreakdown scores={review.scores} />
          </CardBody>
        </Card>
      </div>

      {/* charges + code */}
      <div className="grid gap-6 xl:grid-cols-[1fr_1.1fr]">
        <div className="space-y-3">
          <div className="flex items-baseline justify-between">
            <h2 className="font-serif-display text-lg font-semibold">Charges</h2>
            <span className="text-xs text-muted">
              {guilty.length} proven · {charges.filter((c) => c.status === "DISMISSED").length} dismissed
            </span>
          </div>
          {review.kind === "COMPILE_ERROR" ? (
            <Card className="border-guilty/40 p-4 text-sm">
              <div className="mb-2 font-semibold text-guilty">The code did not compile, so no charges were filed.</div>
              {review.compileErrors.map((e, i) => (
                <div key={i} className="font-mono text-xs">Line {e.line}: {e.message}</div>
              ))}
            </Card>
          ) : visible.length === 0 ? (
            <Card className="p-6 text-center text-sm text-muted">No charges filed. The AI found nothing it could stand behind.</Card>
          ) : (
            visible.map((c, i) => (
              <ChargeCard key={c.id} charge={c} index={i} onShowLines={setRevealLine} onPreviewFix={setPreviewFix} onApplyFix={applyFix} />
            ))
          )}
          {errored.length > 0 && (
            <details className="rounded-xl border border-line bg-surface px-4 py-3 text-xs text-muted">
              <summary className="cursor-pointer">{errored.length} charge(s) could not be tested and were left out</summary>
              <ul className="mt-2 space-y-1">{errored.map((c) => <li key={c.id}>{c.title} - {c.statusReason}</li>)}</ul>
            </details>
          )}
        </div>
        <Card className="overflow-hidden">
          <div className="flex items-center justify-between border-b border-line px-4 py-2.5 text-xs text-muted">
            <span className="font-mono">Solution.java · attempt {verdict.attemptNo}</span>
            <span>red = proven bug · hover for details</span>
          </div>
          <div className="h-[520px]">
            <CodeEditor value={verdict.code} readOnly markers={markers} highlights={highlights} revealLine={revealLine} />
          </div>
        </Card>
      </div>

      {review.kind === "FULL" && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader title="Hidden tests" subtitle="Inputs by AI, expected outputs from the verified reference" icon={<ListChecks className="h-4 w-4" />} />
            <CardBody>
              <HiddenTests cases={review.hiddenTests.cases} passed={review.hiddenTests.passed} total={review.hiddenTests.total} />
            </CardBody>
          </Card>
          <Card>
            <CardHeader title="Speed report" subtitle="Median of 5 runs after 3 warm-ups, fixed random seed" icon={<Timer className="h-4 w-4" />} />
            <CardBody className="space-y-4">
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div className="rounded-lg border border-line bg-bg/40 p-3">
                  <div className="text-xs text-muted">Yours</div>
                  <div className="text-lg font-semibold text-gold">{perf.measuredComplexity ?? perf.claimedComplexity ?? "?"}</div>
                  <div className="text-[11px] text-muted">
                    {perf.complexityMeasured ? `measured (log-log slope ${perf.slope?.toFixed(2)})` : "estimated by AI"}
                  </div>
                </div>
                <div className="rounded-lg border border-line bg-bg/40 p-3">
                  <div className="text-xs text-muted">Optimal</div>
                  <div className="text-lg font-semibold text-acquitted">{perf.optimalComplexity ?? "?"}</div>
                  <div className="text-[11px] text-muted">
                    {perf.ratio ? `you are ${perf.ratio.toFixed(1)}× its time at n=${perf.comparedN?.toLocaleString()}` : "reference solution"}
                  </div>
                </div>
              </div>
              {perf.user.length > 0 ? <SpeedChart user={perf.user} optimal={perf.optimal} /> : <p className="text-sm text-muted">No benchmark for this review.</p>}
              {perf.keyIdea && (perf.ratio === undefined || perf.ratio > 2 || (review.scores.performance ?? 30) < 30) && (
                <div className="rounded-lg border border-gold/30 bg-gold/5 p-3 text-sm">
                  <span className="font-semibold text-gold">Key idea you missed: </span>
                  {perf.keyIdea}
                </div>
              )}
            </CardBody>
          </Card>
          <Card>
            <CardHeader title="Code quality" subtitle="AI-rated - the only part of the score that is opinion" />
            <CardBody><QualityCard quality={review.quality} /></CardBody>
          </Card>
          <Card>
            <CardHeader title="Court record" subtitle="How this verdict was produced" />
            <CardBody className="grid grid-cols-2 gap-3 text-sm">
              <Meta label="Reviewed in" value={`${(review.durationMs / 1000).toFixed(1)} s`} />
              <Meta label="AI model" value={review.aiModel ?? "-"} />
              <Meta label="AI calls" value={String(review.aiCalls)} />
              <Meta label="Tokens used" value={review.aiTokensUsed.toLocaleString()} />
              <Meta label="Reference" value={review.referenceVerified ? "verified" : "not verified"} />
              <Meta label="Submitted" value={formatDate(verdict.createdAt)} />
              {review.referenceNote && <p className="col-span-2 text-xs text-muted">Reference check: {review.referenceNote}</p>}
              <Link to={`/history?problemId=${verdict.problemId}`} className="col-span-2 text-xs text-gold hover:underline">
                All attempts for this problem →
              </Link>
            </CardBody>
          </Card>
        </div>
      )}

      <Dialog open={!!previewFix} onOpenChange={(o) => !o && setPreviewFix(null)} title="Fix preview" className="max-w-5xl"
        description={previewFix?.fixExplanation}>
        {previewFix?.suggestedFixCode && (
          <div className="space-y-4">
            <DiffViewer original={verdict.code} modified={previewFix.suggestedFixCode} leftLabel="Your code" rightLabel="With the fix" />
            <div className="flex justify-end">
              <Button variant="gold" onClick={() => applyFix(previewFix)}>Apply fix in editor</Button>
            </div>
          </div>
        )}
      </Dialog>

      <Dialog open={showOptimal} onOpenChange={setShowOptimal} title="Optimal solution" className="max-w-3xl">
        {optimal.isLoading && <Skeleton className="h-64" />}
        {optimal.error && <p className="text-sm text-muted">{(optimal.error as Error).message}</p>}
        {optimal.data && (
          <div className="space-y-3">
            <div className="text-sm text-muted">{optimal.data.complexity} · {optimal.data.verified ? "verified against brute force" : "not verified"}</div>
            {optimal.data.keyIdea && <p className="text-sm"><span className="font-semibold text-gold">Key idea: </span>{optimal.data.keyIdea}</p>}
            <DiffViewer original={verdict.code} modified={optimal.data.code} leftLabel="Your code" rightLabel="Optimal" height={460} />
          </div>
        )}
      </Dialog>
    </div>
  );
}

function Meta({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-[11px] uppercase tracking-wider text-muted">{label}</div>
      <div className="font-medium">{value}</div>
    </div>
  );
}
