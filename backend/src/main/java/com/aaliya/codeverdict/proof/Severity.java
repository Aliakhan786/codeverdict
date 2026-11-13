package com.aaliya.codeverdict.proof;

public enum Severity {
    HIGH, MEDIUM, LOW;

    public static Severity parse(String s) {
        try {
            return s == null ? MEDIUM : valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEDIUM;
        }
    }
}
