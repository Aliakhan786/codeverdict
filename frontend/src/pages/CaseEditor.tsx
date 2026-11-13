import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { CheckCircle2, ChevronLeft, Eye, Gavel, Pencil, Play, RotateCcw, Save, XCircle } from "lucide-react";
import { toast } from "sonner";
import { useOptimal, useProblem, useRunCode, useSubmit } from "@/api/hooks";
import { ApiError } from "@/api/client";
import { CodeEditor, type LineMarker } from "@/components/CodeEditor";
import { Button } from "@/components/ui/button";
import { Badge, DifficultyBadge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Dialog } from "@/components/ui/dialog";
import { ProblemDialog } from "@/components/ProblemDialog";
import { clearDraft, loadDraft, saveDraft } from "@/lib/drafts";
import { cn } from "@/lib/utils";
import { useAuth } from "@/auth/AuthContext";
import type { RunResponse } from "@/types/api";

export default function CaseEditorPage() {
  const { problemId = "" } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { data: problem, isLoading } = useProblem(problemId);
  const run = useRunCode(problemId);
  const submit = useSubmit(problemId);
  const [code, setCode] = useState<string | null>(null);
  const [result, setResult] = useState<RunResponse | null>(null);
  const [savedAt, setSavedAt] = useState<Date | null>(null);
  const [showOptimal, setShowOptimal] = useState(false);
  const [editing, setEditing] = useState(false);
  const optimal = useOptimal(problemId, showOptimal);
  const dirty = useRef(false);

  // initial code: fix/rematch handed over > autosaved draft > starter code
  useEffect(() => {
    if (!problem || code !== null) return;
    const handed = (location.state as { code?: string } | null)?.code;
    setCode(handed ?? loadDraft(problem.id) ?? problem.starterCode);
  }, [problem, code, location.state]);

  // autosave every few seconds
  useEffect(() => {
    if (code === null || !problem) return;
    dirty.current = true;
    const t = setTimeout(() => {
      saveDraft(problem.id, code);
      setSavedAt(new Date());
      dirty.current = false;
    }, 1500);
    return () => clearTimeout(t);
  }, [code, problem]);

  const doRun = useCallback(async () => {
    if (code === null) return;
    try {
      setResult(await run.mutateAsync(code));
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Run failed");
    }
  }, [code, run]);

  const doSubmit = useCallback(async () => {
    if (code === null || submit.isPending) return;
    try {
      const res = await submit.mutateAsync(code);
      saveDraft(problemId, code);
      navigate(`/verdicts/${res.submissionId}`);
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Could not submit");
    }
  }, [code, submit, problemId, navigate]);

  const doSave = useCallback(() => {
    if (code === null) return;
    saveDraft(problemId, code);
    setSavedAt(new Date());
    toast.success("Draft saved");
  }, [code, problemId]);

  const markers = useMemo<LineMarker[]>(
    () => (result?.compileErrors ?? []).map((e) => ({ lineStart: e.line, lineEnd: e.line, message: e.message, severity: "error" })),
    [result],
  );

  if (isLoading || !problem || code === null) {
    return (
      <div className="grid gap-4 lg:grid-cols-[minmax(320px,0.8fr)_1.2fr]">
        <Skeleton className="h-[70vh]" />
        <Skeleton className="h-[70vh]" />
      </div>
    );
  }

  return (
    <div className="flex h-[calc(100vh-9rem)] min-h-[560px] flex-col">
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate("/cases/new")}>
          <ChevronLeft className="h-4 w-4" /> Problems
        </Button>
        <h1 className="font-serif-display text-xl font-semibold">{problem.title}</h1>
        <Badge>{problem.topicLabel}</Badge>
        <DifficultyBadge value={problem.difficulty} />
        <div className="ml-auto flex items-center gap-2 text-xs text-muted">
          {savedAt && <span>Draft saved {savedAt.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" })}</span>}
        </div>
      </div>

      <div className="grid min-h-0 flex-1 gap-4 lg:grid-cols-[minmax(300px,0.75fr)_1.25fr]">
        {/* Problem panel */}
        <div className="min-h-0 overflow-y-auto rounded-2xl border border-line bg-surface p-5">
          <div className="mb-3 flex items-center justify-between">
            <span className="text-[11px] font-semibold uppercase tracking-[0.2em] text-gold/80">The case</span>
            {!user?.readOnly && (
              <Button variant="ghost" size="sm" onClick={() => setEditing(true)}>
                <Pencil className="h-3.5 w-3.5" /> Edit
              </Button>
            )}
          </div>
          <p className="whitespace-pre-wrap text-sm leading-relaxed text-fg/90">{problem.statement}</p>
          <div className="mt-5 space-y-3">
            <Field label="Method">{problem.methodSignature}</Field>
            {problem.sampleInput && <Field label="Sample input">{problem.sampleInput}</Field>}
            {problem.sampleOutput && <Field label="Expected output">{problem.sampleOutput}</Field>}
          </div>
          <div className="mt-6 flex flex-wrap gap-2">
            {problem.attempts > 0 && (
              <>
                <Button variant="outline" size="sm" onClick={() => setShowOptimal(true)}>
                  <Eye className="h-3.5 w-3.5" /> See optimal solution
                </Button>
                <Link to={`/history?problemId=${problem.id}`}>
                  <Button variant="ghost" size="sm">{problem.attempts} past attempt{problem.attempts > 1 ? "s" : ""}</Button>
                </Link>
              </>
            )}
          </div>
          <div className="mt-6 rounded-lg border border-line bg-bg/50 p-3 text-xs text-muted">
            Write a plain <code className="text-gold">class Solution</code> with the method above. Allowed: java.util, java.util.stream,
            java.math. Shortcuts: <kbd className="text-fg">Ctrl+Enter</kbd> submit · <kbd className="text-fg">Ctrl+S</kbd> save.
          </div>
        </div>

        {/* Editor + console */}
        <div className="flex min-h-0 flex-col overflow-hidden rounded-2xl border border-line bg-[#0d1528]">
          <div className="flex items-center gap-2 border-b border-line bg-surface px-3 py-2">
            <span className="font-mono text-xs text-muted">Solution.java</span>
            <div className="ml-auto flex items-center gap-2">
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  if (confirm("Reset to the starter code? Your draft will be replaced.")) {
                    setCode(problem.starterCode);
                    clearDraft(problem.id);
                    setResult(null);
                  }
                }}
              >
                <RotateCcw className="h-3.5 w-3.5" /> Reset
              </Button>
              <Button variant="ghost" size="sm" onClick={doSave}>
                <Save className="h-3.5 w-3.5" /> Save
              </Button>
              <Button variant="outline" size="sm" onClick={doRun} disabled={run.isPending}>
                <Play className="h-3.5 w-3.5" /> {run.isPending ? "Running..." : "Run"}
              </Button>
              <Button variant="gold" size="sm" onClick={doSubmit} disabled={submit.isPending || user?.readOnly}>
                <Gavel className="h-3.5 w-3.5" /> {submit.isPending ? "Filing..." : "Submit for Verdict"}
              </Button>
            </div>
          </div>
          <div className="min-h-0 flex-1">
            <CodeEditor value={code} onChange={setCode} markers={markers} onCtrlEnter={doSubmit} onCtrlS={doSave} />
          </div>
          <RunConsole result={result} running={run.isPending} />
        </div>
      </div>

      <Dialog open={showOptimal} onOpenChange={setShowOptimal} title="Optimal solution" className="max-w-3xl"
        description={optimal.data ? `${optimal.data.complexity ?? ""}${optimal.data.verified ? " · verified" : " · not verified"}` : undefined}>
        {optimal.isLoading && <Skeleton className="h-64" />}
        {optimal.error && <p className="text-sm text-muted">{(optimal.error as Error).message}</p>}
        {optimal.data && (
          <div className="space-y-4">
            {optimal.data.keyIdea && (
              <div className="rounded-lg border border-gold/30 bg-gold/5 p-3 text-sm">
                <span className="font-semibold text-gold">Key idea: </span>
                {optimal.data.keyIdea}
              </div>
            )}
            <div className="h-80 overflow-hidden rounded-lg border border-line">
              <CodeEditor value={optimal.data.code} readOnly />
            </div>
          </div>
        )}
      </Dialog>
      <ProblemDialog open={editing} onOpenChange={setEditing} problem={problem} />
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <div className="mb-1 text-[11px] font-medium uppercase tracking-wider text-muted">{label}</div>
      <div className="break-all rounded-lg border border-line bg-bg/60 px-3 py-2 font-mono text-xs text-fg">{children}</div>
    </div>
  );
}

function RunConsole({ result, running }: { result: RunResponse | null; running: boolean }) {
  if (!result && !running) {
    return <div className="border-t border-line bg-surface px-4 py-2.5 text-xs text-muted">Run checks your code on the sample input in a sandbox - no AI, no score.</div>;
  }
  return (
    <div className="max-h-48 overflow-y-auto border-t border-line bg-surface px-4 py-3 font-mono text-xs">
      {running && <div className="text-muted">Compiling and running...</div>}
      {result && !result.compiled && (
        <div className="space-y-1 text-guilty">
          <div className="flex items-center gap-1.5 font-sans font-semibold"><XCircle className="h-4 w-4" /> Compilation failed</div>
          {result.compileErrors.map((e, i) => <div key={i}>Line {e.line}: {e.message}</div>)}
        </div>
      )}
      {result && result.compiled && (
        <div className="space-y-1.5">
          <div className={cn("flex items-center gap-1.5 font-sans font-semibold",
            result.passed === true ? "text-acquitted" : result.passed === false ? "text-guilty" : "text-fg")}>
            {result.passed === true ? <CheckCircle2 className="h-4 w-4" /> : result.passed === false ? <XCircle className="h-4 w-4" /> : null}
            {result.passed === true ? "Sample passed" : result.passed === false ? "Sample failed" : "Ran"} · {result.timeMs} ms
          </div>
          {result.error && <div className="text-guilty">{result.error}</div>}
          {result.output !== undefined && <div><span className="text-muted">output   </span>{result.output}</div>}
          {result.expected && <div><span className="text-muted">expected </span>{result.expected}</div>}
          {result.stdout && <div className="whitespace-pre-wrap text-muted">stdout: {result.stdout}</div>}
        </div>
      )}
    </div>
  );
}
