import type { Quality } from "@/types/api";
import { cn } from "@/lib/utils";

const AREAS = [
  { key: "naming", tip: "namingTip", label: "Naming" },
  { key: "readability", tip: "readabilityTip", label: "Readability" },
  { key: "structure", tip: "structureTip", label: "Structure" },
  { key: "duplication", tip: "duplicationTip", label: "No duplication" },
] as const;

export function QualityCard({ quality }: { quality?: Quality }) {
  if (!quality) return <p className="text-sm text-muted">Code quality was not rated (AI unavailable).</p>;
  return (
    <div className="space-y-3">
      {AREAS.map((a) => {
        const v = quality[a.key];
        return (
          <div key={a.key}>
            <div className="flex items-center justify-between text-xs">
              <span className="font-medium">{a.label}</span>
              <span className="flex gap-0.5">
                {Array.from({ length: 5 }).map((_, i) => (
                  <span key={i} className={cn("h-2 w-4 rounded-sm", i < v ? "bg-gold" : "bg-raised")} />
                ))}
              </span>
            </div>
            <p className="mt-1 text-xs text-muted">{quality[a.tip]}</p>
          </div>
        );
      })}
    </div>
  );
}
