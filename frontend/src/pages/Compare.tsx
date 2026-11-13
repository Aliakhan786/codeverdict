import { useNavigate, useSearchParams } from "react-router-dom";
import { ArrowRight, ChevronLeft } from "lucide-react";
import { useCompare } from "@/api/hooks";
import { PageHeader } from "@/components/PageHeader";
import { DiffViewer } from "@/components/DiffViewer";
import { GradeBadge } from "@/components/GradeBadge";
import { Card, CardBody } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { cn, formatDate, scoreColor } from "@/lib/utils";
import type { AttemptSide } from "@/types/api";

const ROWS: { key: keyof AttemptSide; label: string; max: number }[] = [
  { key: "correctness", label: "Correctness", max: 40 },
  { key: "performance", label: "Performance", max: 30 },
  { key: "edgeCases", label: "Edge cases", max: 10 },
  { key: "quality", label: "Code quality", max: 20 },
];

export default function ComparePage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const { data, isLoading, error } = useCompare(params.get("a") ?? undefined, params.get("b") ?? undefined);

  if (isLoading) return <Skeleton className="h-[600px]" />;
  if (error || !data) return <p className="text-muted">Could not load the comparison.</p>;

  const { a, b } = data;
  return (
    <div className="space-y-6">
      <Button variant="ghost" size="sm" onClick={() => navigate(-1)}><ChevronLeft className="h-4 w-4" /> Back</Button>
      <PageHeader eyebrow="Compare attempts" title={data.problemTitle} />
      <Card>
        <CardBody className="flex flex-wrap items-center justify-center gap-8">
          <Side s={a} />
          <div className="flex flex-col items-center">
            <ArrowRight className="h-6 w-6 text-muted" />
            {data.scoreDelta !== undefined && (
              <span className={cn("mt-1 text-2xl font-bold tabular-nums", data.scoreDelta > 0 ? "text-acquitted" : data.scoreDelta < 0 ? "text-guilty" : "text-muted")}>
                {data.scoreDelta > 0 ? "+" : ""}{data.scoreDelta}
              </span>
            )}
          </div>
          <Side s={b} />
        </CardBody>
        <div className="border-t border-line px-5 py-4">
          <table className="w-full text-sm">
            <tbody>
              {ROWS.map((r) => (
                <tr key={r.key} className="border-b border-line/50 last:border-0">
                  <td className="py-2 text-muted">{r.label}</td>
                  <td className="py-2 text-right tabular-nums">{(a[r.key] as number | undefined) ?? "-"} / {r.max}</td>
                  <td className="py-2 text-center text-muted">→</td>
                  <td className="py-2 tabular-nums">{(b[r.key] as number | undefined) ?? "-"} / {r.max}</td>
                </tr>
              ))}
              <tr>
                <td className="py-2 text-muted">Bugs proven</td>
                <td className="py-2 text-right tabular-nums">{a.bugsProven}</td>
                <td className="py-2 text-center text-muted">→</td>
                <td className="py-2 tabular-nums">{b.bugsProven}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </Card>
      <DiffViewer original={a.code} modified={b.code} leftLabel={`Attempt #${a.attemptNo}`} rightLabel={`Attempt #${b.attemptNo}`} height={560} />
    </div>
  );
}

function Side({ s }: { s: AttemptSide }) {
  return (
    <div className="flex items-center gap-3">
      <GradeBadge grade={s.grade} size="lg" />
      <div>
        <div className={cn("text-3xl font-semibold tabular-nums", scoreColor(s.verdictScore))}>{s.verdictScore ?? "-"}</div>
        <div className="text-xs text-muted">Attempt #{s.attemptNo} · {formatDate(s.createdAt)}</div>
      </div>
    </div>
  );
}
