import { cn } from "@/lib/utils";

const COLORS: Record<string, string> = {
  S: "from-gold to-amber-300 text-[#1a1406] border-gold",
  A: "from-acquitted/80 to-acquitted text-[#04220f] border-acquitted",
  B: "from-info/70 to-info text-[#06172e] border-info",
  C: "from-warn/70 to-warn text-[#2a1800] border-warn",
  D: "from-guilty/70 to-guilty text-white border-guilty",
};

export function GradeBadge({ grade, size = "md" }: { grade?: string | null; size?: "sm" | "md" | "lg" }) {
  if (!grade) return <span className="text-muted">-</span>;
  const dims = size === "sm" ? "h-6 w-6 text-xs" : size === "lg" ? "h-16 w-16 text-3xl" : "h-9 w-9 text-base";
  return (
    <span
      className={cn(
        "inline-grid place-items-center rounded-lg border bg-gradient-to-br font-serif-display font-bold shadow-sm",
        COLORS[grade] ?? COLORS.D,
        dims,
      )}
    >
      {grade}
    </span>
  );
}
