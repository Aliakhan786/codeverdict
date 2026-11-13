/// <reference types="vitest" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

// In development, /api is proxied to the Spring Boot backend so cookies stay same-origin.
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { "@": path.resolve(__dirname, "src") },
  },
  server: {
    port: 5174,
    proxy: {
      "/api": { target: process.env.VITE_PROXY_TARGET ?? "http://localhost:18081", changeOrigin: true },
      "/swagger-ui": { target: process.env.VITE_PROXY_TARGET ?? "http://localhost:18081", changeOrigin: true },
      "/v3/api-docs": { target: process.env.VITE_PROXY_TARGET ?? "http://localhost:18081", changeOrigin: true },
    },
  },
  build: {
    chunkSizeWarningLimit: 4000,
    rollupOptions: {
      output: {
        manualChunks: {
          monaco: ["@monaco-editor/react", "monaco-editor/esm/vs/editor/editor.api.js"],
          charts: ["recharts"],
        },
      },
    },
  },
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    css: false,
  },
});
