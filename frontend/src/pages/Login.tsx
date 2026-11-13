import { useState, type FormEvent } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { FlaskConical, Gavel, ShieldCheck, Timer } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "@/auth/AuthContext";
import { useAuthConfig } from "@/api/hooks";
import { ApiError } from "@/api/client";
import { Button } from "@/components/ui/button";
import { Input, Label } from "@/components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";

export default function LoginPage() {
  const { user, login, register } = useAuth();
  const { data: config } = useAuthConfig();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? "/";
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({ username: "", email: "", password: "", displayName: "" });

  if (user) return <Navigate to={from} replace />;

  const run = async (fn: () => Promise<unknown>) => {
    setBusy(true);
    try {
      await fn();
      navigate(from, { replace: true });
    } catch (e) {
      toast.error(e instanceof ApiError ? e.message : "Could not reach the server. Is the backend running?");
    } finally {
      setBusy(false);
    }
  };

  const onLogin = (e: FormEvent) => {
    e.preventDefault();
    run(() => login(form.username, form.password));
  };
  const onRegister = (e: FormEvent) => {
    e.preventDefault();
    run(() => register({ username: form.username, email: form.email, password: form.password, displayName: form.displayName || undefined }));
  };
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) => setForm({ ...form, [k]: e.target.value });

  return (
    <div className="grid min-h-full lg:grid-cols-2">
      {/* Pitch */}
      <div className="relative hidden flex-col justify-between overflow-hidden border-r border-line bg-surface/50 p-12 lg:flex">
        <div className="flex items-center gap-3">
          <div className="grid h-11 w-11 place-items-center rounded-xl bg-gold/15 text-gold">
            <Gavel className="h-6 w-6" />
          </div>
          <span className="font-serif-display text-2xl font-semibold">CodeVerdict</span>
        </div>
        <div>
          <motion.h1
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="font-serif-display text-4xl font-semibold leading-tight"
          >
            Most AI code reviewers guess.
            <br />
            <span className="text-gold">CodeVerdict proves.</span>
          </motion.h1>
          <p className="mt-4 max-w-md text-muted">
            Every bug comes with a failing test, every speed claim comes with a measurement, and every fix is re-tested.
          </p>
          <div className="mt-10 grid max-w-md gap-4">
            {[
              { icon: FlaskConical, t: "Proof, not opinions", d: "A charge is guilty only if its test fails on your code and passes on a verified reference." },
              { icon: Timer, t: "Real timings", d: "Your code vs the optimal solution at 1k, 10k and 100k inputs." },
              { icon: ShieldCheck, t: "Track your growth", d: "Scores, repeated mistakes, streaks and personal bests." },
            ].map(({ icon: Icon, t, d }) => (
              <div key={t} className="flex gap-3">
                <div className="mt-0.5 grid h-8 w-8 shrink-0 place-items-center rounded-lg bg-raised text-gold">
                  <Icon className="h-4 w-4" />
                </div>
                <div>
                  <div className="text-sm font-semibold">{t}</div>
                  <div className="text-sm text-muted">{d}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="text-xs text-muted">Java · Spring Boot · React · PostgreSQL · Docker · Gemini</div>
      </div>

      {/* Form */}
      <div className="flex items-center justify-center p-6">
        <div className="w-full max-w-sm">
          <div className="mb-8 lg:hidden">
            <div className="flex items-center gap-2 font-serif-display text-2xl font-semibold">
              <Gavel className="h-6 w-6 text-gold" /> CodeVerdict
            </div>
          </div>
          <h2 className="font-serif-display text-2xl font-semibold">Enter the courtroom</h2>
          <p className="mb-6 mt-1 text-sm text-muted">Log in to review your code.</p>
          <Tabs defaultValue="login">
            <TabsList className="mb-5 w-full">
              <TabsTrigger value="login" className="flex-1">Log in</TabsTrigger>
              {config?.registrationEnabled !== false && <TabsTrigger value="register" className="flex-1">Create account</TabsTrigger>}
            </TabsList>
            <TabsContent value="login">
              <form onSubmit={onLogin} className="space-y-4">
                <div>
                  <Label>Username or email</Label>
                  <Input autoFocus required value={form.username} onChange={set("username")} autoComplete="username" />
                </div>
                <div>
                  <Label>Password</Label>
                  <Input type="password" required value={form.password} onChange={set("password")} autoComplete="current-password" />
                </div>
                <Button variant="gold" className="w-full" disabled={busy}>
                  {busy ? "Opening the court..." : "Log in"}
                </Button>
              </form>
            </TabsContent>
            <TabsContent value="register">
              <form onSubmit={onRegister} className="space-y-4">
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <Label>Username</Label>
                    <Input required minLength={3} value={form.username} onChange={set("username")} />
                  </div>
                  <div>
                    <Label>Display name</Label>
                    <Input value={form.displayName} onChange={set("displayName")} placeholder="optional" />
                  </div>
                </div>
                <div>
                  <Label>Email</Label>
                  <Input type="email" required value={form.email} onChange={set("email")} />
                </div>
                <div>
                  <Label hint="8+ characters">Password</Label>
                  <Input type="password" required minLength={8} value={form.password} onChange={set("password")} autoComplete="new-password" />
                </div>
                <Button variant="gold" className="w-full" disabled={busy}>
                  {busy ? "Creating..." : "Create account"}
                </Button>
                <p className="text-center text-xs text-muted">10 classic practice problems are added to your account.</p>
              </form>
            </TabsContent>
          </Tabs>
          {config?.demoEnabled && config.demoUsername && config.demoPassword && (
            <div className="mt-6 border-t border-line pt-6">
              <Button variant="outline" className="w-full" disabled={busy} onClick={() => run(() => login(config.demoUsername!, config.demoPassword!))}>
                Try the demo account
              </Button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
