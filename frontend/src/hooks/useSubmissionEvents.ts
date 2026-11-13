import { useEffect, useRef, useState } from "react";
import { eventsUrl } from "@/api/client";
import type { ProgressEvent } from "@/types/api";

/**
 * Follows a running review over Server-Sent Events. EventSource sends the auth cookie
 * (withCredentials) and reconnects by itself; the server replays past stages on connect.
 */
export function useSubmissionEvents(submissionId: string | undefined, enabled: boolean, onTerminal: (e: ProgressEvent) => void) {
  const [events, setEvents] = useState<ProgressEvent[]>([]);
  const [connectionLost, setConnectionLost] = useState(false);
  const terminalRef = useRef(onTerminal);
  terminalRef.current = onTerminal;

  useEffect(() => {
    if (!submissionId || !enabled) return;
    setEvents([]);
    const es = new EventSource(eventsUrl(submissionId), { withCredentials: true });
    let done = false;
    es.addEventListener("progress", (msg) => {
      const e = JSON.parse((msg as MessageEvent).data) as ProgressEvent;
      setConnectionLost(false);
      setEvents((prev) => (prev.some((p) => p.stage === e.stage) ? prev : [...prev, e]));
      if (e.terminal && !done) {
        done = true;
        es.close();
        terminalRef.current(e);
      }
    });
    es.onerror = () => {
      if (!done) setConnectionLost(true);
    };
    return () => es.close();
  }, [submissionId, enabled]);

  return { events, current: events[events.length - 1], connectionLost };
}
