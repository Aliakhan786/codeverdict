package com.aaliya.codeverdict.benchmark;

import java.util.ArrayList;
import java.util.List;

/**
 * Estimates time complexity from measurements: if time grows like n^k then
 * log(time) = k*log(n) + c, so k is the slope of log(time) vs log(n).
 * slope ~1 means O(n), ~2 means O(n^2).
 *
 * We only say "measured" when the evidence is clear: at least two sizes that
 * took >= 0.25 ms (below that, timer noise dominates; each point is already a median of 5 warmed-up runs) and, with three sizes,
 * both slopes agree. Otherwise the AI estimate is shown, labelled "estimated".
 * O(n) and O(n log n) differ by only ~0.1 in slope, so the measurement alone
 * cannot separate them; the AI estimate is used as a tie-breaker inside that band.
 */
public final class ComplexityEstimator {

    public record Estimate(String label, double slope, boolean measured) {
    }

    private static final double NOISE_FLOOR_MS = 0.25;
    private static final double MAX_SLOPE_DISAGREEMENT = 0.7;

    public Estimate estimate(List<BenchPoint> points, String aiEstimate) {
        List<BenchPoint> usable = new ArrayList<>();
        for (BenchPoint p : points) {
            if (p.ok() && p.medianMs() >= NOISE_FLOOR_MS) {
                usable.add(p);
            }
        }
        usable.sort((a, b) -> Integer.compare(a.n(), b.n()));
        if (usable.size() < 2) {
            return new Estimate(aiEstimate, Double.NaN, false);
        }
        List<Double> slopes = new ArrayList<>();
        for (int i = 1; i < usable.size(); i++) {
            BenchPoint a = usable.get(i - 1);
            BenchPoint b = usable.get(i);
            slopes.add(Math.log(b.medianMs() / a.medianMs()) / Math.log((double) b.n() / a.n()));
        }
        double min = slopes.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = slopes.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        if (max - min > MAX_SLOPE_DISAGREEMENT) {
            return new Estimate(aiEstimate, Double.NaN, false);
        }
        double slope = slopes.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return new Estimate(label(slope, aiEstimate), slope, true);
    }

    static String label(double slope, String aiEstimate) {
        if (slope < 0.5) {
            return "O(1) / O(log n)";
        }
        if (slope < 1.45) {
            String ai = aiEstimate == null ? "" : aiEstimate.replace(" ", "").toLowerCase();
            return ai.contains("nlogn") ? "O(n log n)" : "O(n)";
        }
        if (slope < 2.5) {
            return "O(n²)";
        }
        return "O(n³) or worse";
    }
}
