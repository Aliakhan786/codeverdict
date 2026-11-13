import { Flame } from "lucide-react";
import { cn } from "@/lib/utils";

export function StreakFlame({ days, active, className }: { days: number; active?: boolean; className?: string }) {
  const lit = days > 0;
  return (
    <div
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-semibold",
        lit ? "border-warn/40 bg-warn/10 text-warn" : "border-line bg-raised text-muted",
        className,
      )}
      title={lit ? `${days}-day streak${active ? "" : " - practise today to keep it"}` : "No streak yet"}
    >
      <Flame className={cn("h-3.5 w-3.5", lit && "animate-flicker")} fill={lit ? "currentColor" : "none"} />
      {days} day{days === 1 ? "" : "s"}
    </div>
  );
}
