package com.aaliya.codeverdict.ai.dto;

/** One suspected issue ("charge") as reported by the AI, before any proof. */
public record ChargeDraft(
        String category,     // BUG | EDGE_CASE | PERFORMANCE | STYLE
        String severity,     // HIGH | MEDIUM | LOW
        int lineStart,
        int lineEnd,
        String title,
        String explanation,
        String mistakeTag) {
}
