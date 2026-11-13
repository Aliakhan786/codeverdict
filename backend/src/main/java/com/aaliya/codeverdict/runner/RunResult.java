package com.aaliya.codeverdict.runner;

/** Outcome of running one JVM process. */
public record RunResult(
        int exitCode,
        String stdout,
        String stderr,
        boolean timedOut,
        boolean outputTruncated,
        long durationMs) {

    public boolean ok() {
        return !timedOut && exitCode == 0;
    }
}
