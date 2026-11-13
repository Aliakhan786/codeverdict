package com.aaliya.codeverdict.proof;

public enum ChargeCategory {
    BUG, EDGE_CASE, PERFORMANCE, STYLE;

    public static ChargeCategory parse(String s) {
        try {
            return s == null ? BUG : valueOf(s.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            return BUG;
        }
    }

    /** Only these get a JUnit proof test; PERFORMANCE is judged by the benchmark. */
    public boolean needsProofTest() {
        return this == BUG || this == EDGE_CASE;
    }
}
