import { Link } from "react-router-dom";
import type { ProgressData } from "@/types/api";

export function MistakeList({ mistakes }: { mistakes: ProgressData["topMistakes"] }) {
  const max = Math.max(1, ...mistakes.map((m) => m.count));
  return (
    <ul className="space-y-3">
      {mistakes.map((m) => (
        <li key={m.tag}>
          <div className="flex items-center justify-between text-sm">
            <span className="font-medium">{m.label}</span>
            <span className="tabular-nums text-guilty">× {m.count}</span>
          </div>
          <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-raised">
            <div className="h-full rounded-full bg-guilty/70" style={{ width: `${(m.count / max) * 100}%` }} />
          </div>
          <div className="mt-1 flex flex-wrap gap-1.5">
            {m.submissionIds.slice(0, 6).map((id, i) => (
              <Link key={id} to={`/verdicts/${id}`} className="text-[11px] text-muted hover:text-gold">
                case {i + 1}
              </Link>
            ))}
          </div>
        </li>
      ))}
    </ul>
  );
}
