package com.aaliya.codeverdict.review;

/**
 * Verdict score (0-100):
 * <pre>
 * Correctness  40  = 40 x hidden tests passed / total            (measured)
 * Edge cases   10  = 10 - 3 per proven edge-case charge (min 0)   (measured)
 * Performance  30  = vs reference at the largest input size        (measured)
 * Code quality 20  = AI rubric: naming, readability, structure, duplication (AI-rated)
 * </pre>
 * 80% of the score is measured, not AI opinion. If a part could not be computed
 * (e.g. the AI was down), the score is scaled over the parts that were.
 */
public final class ScoringCalculator {

    public record Inputs(
            int hiddenPassed,
            int hiddenTotal,
            int provenBugs,
            int provenEdgeCases,
            boolean chargesAvailable,
            Integer performanceScore,
            Integer qualityScore) {
    }

    public record Scores(Integer correctness, Integer edgeCases, Integer performance, Integer quality,
                         int total, String grade, boolean partial) {
    }

    public static final int MAX_CORRECTNESS = 40;
    public static final int MAX_EDGE = 10;
    public static final int MAX_PERFORMANCE = 30;
    public static final int MAX_QUALITY = 20;

    public Scores score(Inputs in) {
        Integer correctness;
        if (in.hiddenTotal() > 0) {
            correctness = (int) Math.round(MAX_CORRECTNESS * (double) in.hiddenPassed() / in.hiddenTotal());
        } else if (in.chargesAvailable()) {
            correctness = Math.max(0, MAX_CORRECTNESS - 10 * in.provenBugs());
        } else {
            correctness = null;
        }
        Integer edge = in.chargesAvailable() ? Math.max(0, MAX_EDGE - 3 * in.provenEdgeCases()) : null;
        Integer perf = in.performanceScore();
        Integer quality = in.qualityScore() == null ? null : Math.max(0, Math.min(MAX_QUALITY, in.qualityScore()));

        int got = 0;
        int max = 0;
        if (correctness != null) { got += correctness; max += MAX_CORRECTNESS; }
        if (edge != null) { got += edge; max += MAX_EDGE; }
        if (perf != null) { got += perf; max += MAX_PERFORMANCE; }
        if (quality != null) { got += quality; max += MAX_QUALITY; }
        int total = max == 0 ? 0 : (int) Math.round(100.0 * got / max);
        return new Scores(correctness, edge, perf, quality, total, grade(total), max < 100);
    }

    public static String grade(int score) {
        if (score >= 95) return "S";
        if (score >= 85) return "A";
        if (score >= 70) return "B";
        if (score >= 50) return "C";
        return "D";
    }
}
