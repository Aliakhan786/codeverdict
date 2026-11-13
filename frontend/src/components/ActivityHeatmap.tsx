import { useMemo } from "react";
import { Tip } from "@/components/ui/tooltip";
import { cn } from "@/lib/utils";

/** GitHub-style calendar of the last ~26 weeks. */
export function ActivityHeatmap({ days, weeks = 26 }: { days: { date: string; count: number }[]; weeks?: number }) {
  const { columns, total, activeDays } = useMemo(() => {
    const counts = new Map(days.map((d) => [d.date, d.count]));
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const start = new Date(today);
    start.setDate(start.getDate() - (weeks * 7 - 1) - ((today.getDay() + 6) % 7 === 6 ? 0 : 0));
    // align to Monday
    start.setDate(start.getDate() - ((start.getDay() + 6) % 7));
    const cols: { date: string; count: number; future: boolean }[][] = [];
    const cursor = new Date(start);
    let t = 0;
    let a = 0;
    while (cursor <= today || cols.length === 0 || cols[cols.length - 1].length < 7) {
      if (cols.length === 0 || cols[cols.length - 1].length === 7) cols.push([]);
      const key = `${cursor.getFullYear()}-${String(cursor.getMonth() + 1).padStart(2, "0")}-${String(cursor.getDate()).padStart(2, "0")}`;
      const c = counts.get(key) ?? 0;
      t += c;
      if (c > 0) a++;
      cols[cols.length - 1].push({ date: key, count: c, future: cursor > today });
      cursor.setDate(cursor.getDate() + 1);
      if (cols.length > weeks + 2) break;
    }
    return { columns: cols, total: t, activeDays: a };
  }, [days, weeks]);

  const level = (c: number) => (c === 0 ? "bg-raised" : c === 1 ? "bg-gold/30" : c === 2 ? "bg-gold/55" : c <= 4 ? "bg-gold/80" : "bg-gold");

  return (
    <div>
      <div className="flex gap-[3px] overflow-x-auto pb-1">
        {columns.map((col, i) => (
          <div key={i} className="flex flex-col gap-[3px]">
            {col.map((d) => (
              <Tip key={d.date} content={`${d.count} review${d.count === 1 ? "" : "s"} on ${new Date(d.date).toDateString()}`}>
                <div className={cn("h-3.5 w-3.5 rounded-[3px]", d.future ? "opacity-0" : level(d.count))} />
              </Tip>
            ))}
          </div>
        ))}
      </div>
      <div className="mt-3 flex items-center justify-between text-xs text-muted">
        <span>{total} reviews on {activeDays} days</span>
        <span className="flex items-center gap-1">
          less {[0, 1, 2, 3, 5].map((c) => <span key={c} className={cn("h-3 w-3 rounded-[3px]", level(c))} />)} more
        </span>
      </div>
    </div>
  );
}
