package com.aaliya.codeverdict.proof;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Parses the "CV|..." protocol lines printed by the harness programs. */
public final class HarnessOutput {

    /** One input's result. status: OK | ERR | TIMEOUT | SKIP | BADINPUT | MISSING */
    public record CaseResult(int index, String status, String value, String inputText, long nanos) {
        public boolean ok() {
            return "OK".equals(status);
        }
    }

    /** status: OK | TOO_SLOW | ERROR | SKIPPED */
    public record BenchLine(int n, String status, double medianMs, int runs, String error) {
    }

    private final List<String[]> lines = new ArrayList<>();
    private String userStdout = "";
    private boolean ended;

    public static HarnessOutput parse(String stdout) {
        HarnessOutput out = new HarnessOutput();
        for (String raw : stdout.split("\r?\n")) {
            if (!raw.startsWith("CV|")) {
                continue;
            }
            String[] parts = raw.split("\\|", -1);
            if (parts.length >= 2 && "STDOUT".equals(parts[1]) && parts.length >= 3) {
                out.userStdout = decode(parts[2]);
            } else if (parts.length >= 2 && "END".equals(parts[1])) {
                out.ended = true;
            } else {
                out.lines.add(parts);
            }
        }
        return out;
    }

    /** Results of CaseRunner (kind "CASE") or GenRunner (kind "GEN"), indexed 0..count-1. */
    public List<CaseResult> cases(String kind, int count) {
        Map<Integer, CaseResult> byIndex = new HashMap<>();
        for (String[] p : lines) {
            if (p.length < 4 || !kind.equals(p[1])) {
                continue;
            }
            int idx = Integer.parseInt(p[2]);
            String status = p[3];
            CaseResult r;
            if ("CASE".equals(kind)) {
                long nanos = p.length > 4 ? parseLong(p[4]) : -1;
                String value = p.length > 5 ? decode(p[5]) : (p.length > 4 && "BADINPUT".equals(status) ? decode(p[4]) : null);
                r = new CaseResult(idx, status, value, null, nanos);
            } else {
                String input = p.length > 4 ? decode(p[4]) : null;
                String value = p.length > 5 ? decode(p[5]) : null;
                if ("BADINPUT".equals(status)) {
                    value = input;
                    input = null;
                }
                r = new CaseResult(idx, status, value, input, -1);
            }
            byIndex.put(idx, r);
        }
        List<CaseResult> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add(byIndex.getOrDefault(i, new CaseResult(i, "MISSING", null, null, -1)));
        }
        return out;
    }

    public List<BenchLine> bench() {
        List<BenchLine> out = new ArrayList<>();
        for (String[] p : lines) {
            if (p.length < 4 || !"BENCH".equals(p[1])) {
                continue;
            }
            int n = Integer.parseInt(p[2]);
            String status = p[3];
            if ("OK".equals(status)) {
                double ms = parseLong(p[4]) / 1_000_000.0;
                out.add(new BenchLine(n, status, ms, Integer.parseInt(p[5]), null));
            } else {
                out.add(new BenchLine(n, status, -1, 0, p.length > 4 ? decode(p[4]) : null));
            }
        }
        return out;
    }

    public String userStdout() {
        return userStdout;
    }

    public boolean ended() {
        return ended;
    }

    private static long parseLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String decode(String b64) {
        try {
            return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return b64;
        }
    }
}
