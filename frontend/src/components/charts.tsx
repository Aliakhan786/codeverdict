import { Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, Tooltip, XAxis, YAxis } from "recharts";
import type { ProgressData } from "@/types/api";

const tooltipStyle = { background: "rgb(var(--surface))", border: "1px solid rgb(var(--line))", borderRadius: 8, fontSize: 12 };
const tick = { fill: "rgb(var(--muted))", fontSize: 11 };

export function WeeklyScoreChart({ data }: { data: ProgressData["weeklyScores"] }) {
  const rows = data.map((d) => ({ ...d, label: new Date(d.week).toLocaleDateString(undefined, { day: "numeric", month: "short" }) }));
  return (
    <ResponsiveContainer width="100%" height={240}>
      <AreaChart data={rows} margin={{ top: 8, right: 8, left: -16, bottom: 0 }}>
        <defs>
          <linearGradient id="gold" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="rgb(var(--gold))" stopOpacity={0.4} />
            <stop offset="100%" stopColor="rgb(var(--gold))" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" stroke="rgb(var(--line))" vertical={false} />
        <XAxis dataKey="label" tick={tick} axisLine={false} tickLine={false} />
        <YAxis domain={[0, 100]} tick={tick} axisLine={false} tickLine={false} />
        <Tooltip contentStyle={tooltipStyle} formatter={(v: number, _n, item) => [`${v} avg (${(item.payload as { reviews: number }).reviews} reviews)`, "Week"]} />
        <Area type="monotone" dataKey="avgScore" stroke="rgb(var(--gold))" strokeWidth={2.5} fill="url(#gold)" dot={{ r: 3, fill: "rgb(var(--gold))" }} />
      </AreaChart>
    </ResponsiveContainer>
  );
}

export function TopicStrength({ data }: { data: ProgressData["topicStrength"] }) {
  const color = (v: number) => (v >= 85 ? "rgb(var(--acquitted))" : v >= 70 ? "rgb(var(--gold))" : v >= 50 ? "rgb(var(--warn))" : "rgb(var(--guilty))");
  return (
    <ResponsiveContainer width="100%" height={Math.max(160, data.length * 36)}>
      <BarChart data={data} layout="vertical" margin={{ top: 0, right: 16, left: 8, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="rgb(var(--line))" horizontal={false} />
        <XAxis type="number" domain={[0, 100]} tick={tick} axisLine={false} tickLine={false} />
        <YAxis type="category" dataKey="label" width={104} tick={{ ...tick, fill: "rgb(var(--fg))" }} axisLine={false} tickLine={false} />
        <Tooltip contentStyle={tooltipStyle} cursor={{ fill: "rgb(var(--raised))" }} formatter={(v: number, _n, item) => [`${v} avg over ${(item.payload as { reviews: number }).reviews} reviews`, "Score"]} />
        <Bar dataKey="avgScore" radius={[0, 6, 6, 0]} barSize={18}>
          {data.map((d) => <Cell key={d.topic} fill={color(d.avgScore)} />)}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  );
}
