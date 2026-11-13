package com.aaliya.codeverdict.ai.dto;

/** AI-rated code quality (each 0-5) plus one tip per area and a complexity estimate. */
public record QualityResponse(
        int naming,
        int readability,
        int structure,
        int duplication,
        String namingTip,
        String readabilityTip,
        String structureTip,
        String duplicationTip,
        String estimatedComplexity) {

    public int total() {
        return clamp(naming) + clamp(readability) + clamp(structure) + clamp(duplication);
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(5, v));
    }
}
