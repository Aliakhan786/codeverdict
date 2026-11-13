import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { Point } from "@/types/api";
import { formatMs, formatN } from "@/lib/utils";

/** Grouped bars: your time vs the optimal solution at each input size (log scale). */
export function SpeedChart({ user, optimal }: { user: Point[]; optimal: Point[] }) {
  const sizes = Array.from(new Set([...user, ...optimal].map((p) => p.n))).sort((a, b) => a - b);
  const data = sizes.map((n) => {
    const u = user.find((p) => p.n === n);
    const o = optimal.find((p) => p.n === n);
    return {
      n: `n = ${formatN(n)}`,
      you: u?.ms ?? (u?.status === "TOO_SLOW" ? 2000 : null),
      youStatus: u?.status,
      optimal: o?.ms ?? null,
    };
  });
  return (
    <ResponsiveContainer width="100%" height={240}>
      <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }} barGap={4}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgb(var(--line))" vertical={false} />
        <XAxis dataKey="n" tick={{ fill: "rgb(var(--muted))", fontSize: 12 }} axisLine={false} tickLine={false} />
        <YAxis
          scale="log"
          domain={[0.01, "auto"]}
          allowDataOverflow
          tickFormatter={(v: number) => formatMs(v)}
          tick={{ fill: "rgb(var(--muted))", fontSize: 11 }}
          axisLine={false}
          tickLine={false}
          width={64}
        />
        <Tooltip
          cursor={{ fill: "rgb(var(--raised))" }}
          contentStyle={{ background: "rgb(var(--surface))", border: "1px solid rgb(var(--line))", borderRadius: 8, fontSize: 12 }}
          formatter={(value: number, name: string, item) => {
            const payload = (item as { payload?: { youStatus?: string } }).payload;
            if (name === "You" && payload?.youStatus === "TOO_SLOW") return ["over 2 s (too slow)", name];
            return [formatMs(value), name];
          }}
        />
        <Legend wrapperStyle={{ fontSize: 12 }} />
        <Bar dataKey="you" name="You" fill="rgb(var(--gold))" radius={[4, 4, 0, 0]} />
        <Bar dataKey="optimal" name="Optimal" fill="rgb(var(--acquitted))" radius={[4, 4, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
