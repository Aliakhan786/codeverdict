import type { ReactNode } from "react";
import { Gavel } from "lucide-react";

/** Every page uses this instead of looking broken when there is no data yet. */
export function EmptyState({ title, message, action, icon }: { title: string; message: string; action?: ReactNode; icon?: ReactNode }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-line px-6 py-14 text-center">
      <div className="mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-raised text-gold">{icon ?? <Gavel className="h-6 w-6" />}</div>
      <h3 className="font-serif-display text-lg font-semibold">{title}</h3>
      <p className="mt-1 max-w-sm text-sm text-muted">{message}</p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}
