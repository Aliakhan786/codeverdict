import { useEffect, useRef } from "react";
import Editor, { type OnMount } from "@monaco-editor/react";
import { monaco } from "@/lib/monaco";
import { useAuth } from "@/auth/AuthContext";

export interface LineMarker {
  lineStart: number;
  lineEnd: number;
  message: string;
  severity: "error" | "warning" | "info";
}

export interface LineHighlight {
  lineStart: number;
  lineEnd: number;
  kind: "guilty" | "acquitted";
}

/**
 * Monaco (the editor inside VS Code) with Java highlighting and inline markers:
 * red squiggles on the exact lines of proven bugs; hover shows the explanation.
 */
export function CodeEditor({ value, onChange, readOnly = false, height = "100%", markers = [], highlights = [], onCtrlEnter, onCtrlS, revealLine }: {
  value: string;
  onChange?: (v: string) => void;
  readOnly?: boolean;
  height?: string | number;
  markers?: LineMarker[];
  highlights?: LineHighlight[];
  onCtrlEnter?: () => void;
  onCtrlS?: () => void;
  revealLine?: number;
}) {
  const { user } = useAuth();
  const editorRef = useRef<monaco.editor.IStandaloneCodeEditor | null>(null);
  const decorations = useRef<monaco.editor.IEditorDecorationsCollection | null>(null);
  const callbacks = useRef({ onCtrlEnter, onCtrlS });
  callbacks.current = { onCtrlEnter, onCtrlS };

  const onMount: OnMount = (editor, m) => {
    editorRef.current = editor;
    decorations.current = editor.createDecorationsCollection([]);
    editor.addCommand(m.KeyMod.CtrlCmd | m.KeyCode.Enter, () => callbacks.current.onCtrlEnter?.());
    editor.addCommand(m.KeyMod.CtrlCmd | m.KeyCode.KeyS, () => callbacks.current.onCtrlS?.());
    applyMarkers();
  };

  const applyMarkers = () => {
    const editor = editorRef.current;
    const model = editor?.getModel();
    if (!editor || !model) return;
    const lines = model.getLineCount();
    monaco.editor.setModelMarkers(
      model,
      "codeverdict",
      markers.map((mk) => ({
        startLineNumber: Math.min(Math.max(1, mk.lineStart), lines),
        endLineNumber: Math.min(Math.max(mk.lineStart, mk.lineEnd), lines),
        startColumn: 1,
        endColumn: model.getLineMaxColumn(Math.min(Math.max(mk.lineStart, mk.lineEnd), lines)),
        message: mk.message,
        severity:
          mk.severity === "error" ? monaco.MarkerSeverity.Error : mk.severity === "warning" ? monaco.MarkerSeverity.Warning : monaco.MarkerSeverity.Info,
      })),
    );
    decorations.current?.set(
      highlights.map((h) => ({
        range: new monaco.Range(Math.min(h.lineStart, lines), 1, Math.min(h.lineEnd, lines), 1),
        options: {
          isWholeLine: true,
          className: h.kind === "guilty" ? "cv-guilty-line" : "cv-acquitted-line",
          linesDecorationsClassName: h.kind === "guilty" ? "cv-guilty-glyph" : "cv-acquitted-glyph",
        },
      })),
    );
  };

  useEffect(applyMarkers, [markers, highlights, value]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (revealLine && editorRef.current) {
      editorRef.current.revealLineInCenter(revealLine);
      editorRef.current.setPosition({ lineNumber: revealLine, column: 1 });
    }
  }, [revealLine]);

  return (
    <Editor
      height={height}
      language="java"
      theme={user?.theme === "light" ? "codeverdict-light" : "codeverdict-dark"}
      value={value}
      onChange={(v) => onChange?.(v ?? "")}
      onMount={onMount}
      loading={<div className="grid h-full place-items-center text-sm text-muted">Loading editor...</div>}
      options={{
        readOnly,
        fontFamily: '"JetBrains Mono Variable", "JetBrains Mono", monospace',
        fontSize: 14,
        fontLigatures: true,
        minimap: { enabled: false },
        scrollBeyondLastLine: false,
        tabSize: 4,
        insertSpaces: true,
        autoIndent: "full",
        formatOnPaste: true,
        renderLineHighlight: "all",
        smoothScrolling: true,
        cursorBlinking: "smooth",
        padding: { top: 12, bottom: 12 },
        automaticLayout: true,
        glyphMargin: false,
        lineDecorationsWidth: 10,
        bracketPairColorization: { enabled: true },
      }}
    />
  );
}
