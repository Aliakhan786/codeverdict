import * as T from "@radix-ui/react-tabs";
import { cn } from "@/lib/utils";
import type { ComponentProps } from "react";

export const Tabs = T.Root;
export const TabsContent = T.Content;

export function TabsList({ className, ...p }: ComponentProps<typeof T.List>) {
  return <T.List className={cn("inline-flex gap-1 rounded-lg border border-line bg-bg p-1", className)} {...p} />;
}

export function TabsTrigger({ className, ...p }: ComponentProps<typeof T.Trigger>) {
  return (
    <T.Trigger
      className={cn(
        "rounded-md px-3 py-1.5 text-xs font-medium text-muted transition data-[state=active]:bg-raised data-[state=active]:text-fg",
        className,
      )}
      {...p}
    />
  );
}
