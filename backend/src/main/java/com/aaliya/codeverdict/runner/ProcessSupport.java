package com.aaliya.codeverdict.runner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Starts a process, caps its output and kills it on timeout. Shared by both runners. */
final class ProcessSupport {

    private ProcessSupport() {
    }

    static RunResult execute(List<String> command, Path workDir, Duration timeout, int maxOutputBytes,
                             Map<String, String> env, Runnable onTimeout) {
        long start = System.nanoTime();
        ProcessBuilder pb = new ProcessBuilder(command).directory(workDir.toFile());
        if (env != null) {
            Map<String, String> pbEnv = pb.environment();
            // Keep only what a JVM needs to start; never leak secrets (API keys, DB passwords) to user code.
            Map<String, String> keep = new java.util.HashMap<>();
            for (String key : List.of("PATH", "SystemRoot", "SYSTEMROOT", "TEMP", "TMP", "WINDIR", "windir", "HOME", "LANG")) {
                if (pbEnv.containsKey(key)) {
                    keep.put(key, pbEnv.get(key));
                }
            }
            pbEnv.clear();
            pbEnv.putAll(keep);
            pbEnv.putAll(env);
        }
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not start process: " + command.get(0), e);
        }
        try { // the program gets no stdin
            process.getOutputStream().close();
        } catch (IOException ignored) {
            // nothing to do
        }

        CappedReader out = new CappedReader(process.getInputStream(), maxOutputBytes);
        CappedReader err = new CappedReader(process.getErrorStream(), maxOutputBytes);
        Thread tOut = Thread.ofVirtual().start(out);
        Thread tErr = Thread.ofVirtual().start(err);

        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            finished = false;
        }
        if (!finished) {
            if (onTimeout != null) {
                onTimeout.run();
            }
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            try {
                process.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        try {
            tOut.join(2000);
            tErr.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        int exit = finished ? process.exitValue() : -1;
        return new RunResult(exit, out.text(), err.text(), !finished, out.truncated() || err.truncated(), ms);
    }

    /** Reads a stream fully (so the process never blocks) but keeps only the first N bytes. */
    private static final class CappedReader implements Runnable {
        private final InputStream in;
        private final int max;
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        private final AtomicBoolean truncated = new AtomicBoolean(false);

        CappedReader(InputStream in, int max) {
            this.in = in;
            this.max = max;
        }

        @Override
        public void run() {
            byte[] chunk = new byte[8192];
            try (in) {
                int n;
                while ((n = in.read(chunk)) != -1) {
                    synchronized (buf) {
                        int room = max - buf.size();
                        if (room > 0) {
                            buf.write(chunk, 0, Math.min(room, n));
                        }
                        if (n > room) {
                            truncated.set(true);
                        }
                    }
                }
            } catch (IOException ignored) {
                // stream closed because the process was killed
            }
        }

        String text() {
            synchronized (buf) {
                return buf.toString(StandardCharsets.UTF_8);
            }
        }

        boolean truncated() {
            return truncated.get();
        }
    }
}
