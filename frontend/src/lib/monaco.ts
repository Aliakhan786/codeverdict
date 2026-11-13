// Use the bundled monaco-editor instead of loading it from a CDN (works offline, no third-party script).
// Only the core editor features + the Java grammar are bundled (not all 80 languages).
import { loader } from "@monaco-editor/react";
import "monaco-editor/esm/vs/editor/editor.all.js";
import "monaco-editor/esm/vs/basic-languages/java/java.contribution.js";
import * as monaco from "monaco-editor/esm/vs/editor/editor.api.js";
import EditorWorker from "monaco-editor/esm/vs/editor/editor.worker?worker";

self.MonacoEnvironment = {
  getWorker() {
    return new EditorWorker();
  },
};

monaco.editor.defineTheme("codeverdict-dark", {
  base: "vs-dark",
  inherit: true,
  rules: [
    { token: "keyword", foreground: "e8b84a" },
    { token: "comment", foreground: "6b7a99", fontStyle: "italic" },
    { token: "number", foreground: "7cc4fa" },
    { token: "string", foreground: "8bd5a0" },
  ],
  colors: {
    "editor.background": "#0d1528",
    "editor.lineHighlightBackground": "#16213b",
    "editorLineNumber.foreground": "#4a5878",
    "editorLineNumber.activeForeground": "#e8b84a",
    "editorGutter.background": "#0d1528",
    "editor.selectionBackground": "#2a3b64",
  },
});

monaco.editor.defineTheme("codeverdict-light", {
  base: "vs",
  inherit: true,
  rules: [{ token: "keyword", foreground: "a46a00" }],
  colors: { "editor.background": "#ffffff" },
});

loader.config({ monaco });

export { monaco };
