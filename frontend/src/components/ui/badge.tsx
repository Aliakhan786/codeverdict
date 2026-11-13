import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export function Badge({ className, ...props }: HTMLAttributes<HTMLSpanElement>) {
  return (
    <span
      className={cn("inline-flex items-center gap-1 rounded-md border border-line bg-raised px-2 py-0.5 text-[11px] font-medium text-muted", className)}
      {...props}
    />
  );
}

const DIFF: Record<string, string> = {
  EASY: "text-acquitted border-acquitted/30 bg-acquitted/10",
  MEDIUM: "text-warn border-warn/30 bg-warn/10",
  HARD: "text-guilty border-guilty/30 bg-guilty/10",
};

export function DifficultyBadge({ value }: { value: string }) {
  return <Badge className={DIFF[value]}>{value.charAt(0) + value.slice(1).toLowerCase()}</Badge>;
}
