package com.aaliya.codeverdict.benchmark;

/** One timing: who (USER/OPTIMAL), input size n, median ms, status OK/TOO_SLOW/ERROR/SKIPPED. */
public record BenchPoint(String solution, int n, Double medianMs, int runs, String status, String error) {
    public boolean ok() {
        return "OK".equals(status) && medianMs != null;
    }
}
