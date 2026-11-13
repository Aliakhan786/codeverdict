import { DiffEditor } from "@monaco-editor/react";
import "@/lib/monaco";
import { useAuth } from "@/auth/AuthContext";

/** Side-by-side diff (fix preview, attempt comparison). */
export function DiffViewer({ original, modified, height = 420, leftLabel, rightLabel }: {
  original: string; modified: string; height?: number | string; leftLabel?: string; rightLabel?: string;
}) {
  const { user } = useAuth();
  return (
    <div className="overflow-hidden rounded-xl border border-line">
      {(leftLabel || rightLabel) && (
        <div className="grid grid-cols-2 border-b border-line bg-raised text-xs text-muted">
          <div className="px-3 py-2">{leftLabel}</div>
          <div className="border-l border-line px-3 py-2">{rightLabel}</div>
        </div>
      )}
      <DiffEditor
        height={height}
        language="java"
        theme={user?.theme === "light" ? "codeverdict-light" : "codeverdict-dark"}
        original={original}
        modified={modified}
        options={{
          readOnly: true,
          renderSideBySide: true,
          minimap: { enabled: false },
          fontFamily: '"JetBrains Mono Variable", monospace',
          fontSize: 13,
          scrollBeyondLastLine: false,
          automaticLayout: true,
        }}
      />
    </div>
  );
}
