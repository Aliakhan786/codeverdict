import { useEffect, useState } from "react";
import { Download, KeyRound, Moon, Server, Sun, Volume2 } from "lucide-react";
import { toast } from "sonner";
import { useSystemStatus, useUpdateSettings } from "@/api/hooks";
import { exportUrl, ApiError } from "@/api/client";
import { useAuth } from "@/auth/AuthContext";
import { PageHeader } from "@/components/PageHeader";
import { Card, CardBody, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { gavelSound } from "@/lib/feedback";
import { cn } from "@/lib/utils";

export default function SettingsPage() {
  const { user } = useAuth();
  const { data: status } = useSystemStatus();
  const update = useUpdateSettings();
  const [form, setForm] = useState({ displayName: "", theme: "dark" as "dark" | "light", soundEnabled: false, dailyGoal: 2 });

  useEffect(() => {
    if (user) setForm({ displayName: user.displayName, theme: user.theme, soundEnabled: user.soundEnabled, dailyGoal: user.dailyGoal });
  }, [user]);

  const save = async (next = form) => {
    try {
      await update.mutateAsync(next);
      toast.success("Settings saved");
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Could not save");
    }
  };

  if (!user) return null;
  return (
    <div className="max-w-3xl space-y-6">
      <PageHeader eyebrow="Settings" title="Chambers" />
      <Card>
        <CardHeader title="Profile & practice" />
        <CardBody className="grid gap-4 sm:grid-cols-2">
          <div>
            <Label>Display name</Label>
            <Input value={form.displayName} onChange={(e) => setForm({ ...form, displayName: e.target.value })} />
          </div>
          <div>
            <Label hint="reviews per day">Daily goal</Label>
            <Input type="number" min={1} max={20} value={form.dailyGoal} onChange={(e) => setForm({ ...form, dailyGoal: Number(e.target.value) })} />
          </div>
          <div className="sm:col-span-2">
            <Button variant="gold" onClick={() => save()} disabled={update.isPending || user.readOnly}>Save</Button>
          </div>
        </CardBody>
      </Card>
      <Card>
        <CardHeader title="Look & feel" />
        <CardBody className="space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3"><Moon className="h-4 w-4 text-gold" /><span className="text-sm">Theme</span></div>
            <div className="flex gap-1 rounded-lg border border-line p-1">
              {(["dark", "light"] as const).map((t) => (
                <button key={t} disabled={user.readOnly}
                  onClick={() => { const next = { ...form, theme: t }; setForm(next); save(next); }}
                  className={cn("flex items-center gap-1.5 rounded-md px-3 py-1.5 text-xs", form.theme === t ? "bg-raised text-fg" : "text-muted")}>
                  {t === "dark" ? <Moon className="h-3.5 w-3.5" /> : <Sun className="h-3.5 w-3.5" />} {t}
                </button>
              ))}
            </div>
          </div>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3"><Volume2 className="h-4 w-4 text-gold" /><span className="text-sm">Gavel sound on verdict</span></div>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" className="h-4 w-4 accent-[rgb(var(--gold))]" checked={form.soundEnabled} disabled={user.readOnly}
                onChange={(e) => { const next = { ...form, soundEnabled: e.target.checked }; setForm(next); save(next); if (e.target.checked) gavelSound(); }} />
              {form.soundEnabled ? "On" : "Off"}
            </label>
          </div>
        </CardBody>
      </Card>
      <Card>
        <CardHeader title="System" subtitle="Read-only status of this server" icon={<Server className="h-4 w-4" />} />
        <CardBody className="grid gap-3 text-sm sm:grid-cols-2">
          <div className="flex items-center gap-2">
            <KeyRound className="h-4 w-4 text-muted" /> Gemini API key:
            <span className={status?.aiConfigured ? "text-acquitted" : "text-guilty"}>
              {status ? (status.aiConfigured ? `configured (${status.aiModel})` : "missing - set GEMINI_API_KEY in .env") : "..."}
            </span>
          </div>
          <div>Code runner: <span className="font-mono">{status?.runner}</span></div>
          <div>Review limit: {status?.maxReviewsPerHour} per hour</div>
          <div>Time zone: {status?.timezone}</div>
        </CardBody>
      </Card>
      <Card>
        <CardHeader title="Your data" />
        <CardBody>
          <a href={exportUrl()} download>
            <Button variant="outline"><Download className="h-4 w-4" /> Export history as CSV</Button>
          </a>
        </CardBody>
      </Card>
    </div>
  );
}
