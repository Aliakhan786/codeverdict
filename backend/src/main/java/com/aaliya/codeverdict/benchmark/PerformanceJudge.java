package com.aaliya.codeverdict.benchmark;

import java.util.List;
import java.util.Optional;

/**
 * Turns benchmark points into the performance part of the verdict (0-30):
 * 30 within 2x of the reference at the largest size both finished,
 * 20 within 10x, 10 if it finishes, 0 if it was too slow or crashed.
 * Times under 0.5 ms are treated as 0.5 ms so timer noise cannot fake a 3x gap.
 */
public final class PerformanceJudge {

    public record Judgement(Integer score, Integer comparedN, Double userMs, Double optimalMs, Double ratio) {
        public static Judgement notMeasured() {
            return new Judgement(null, null, null, null, null);
        }
    }

    private static final double FLOOR_MS = 0.5;

    public Judgement judge(List<BenchPoint> user, List<BenchPoint> optimal) {
        if (user == null || user.isEmpty() || optimal == null || optimal.isEmpty()) {
            return Judgement.notMeasured();
        }
        Optional<BenchPoint> largestRef = optimal.stream().filter(BenchPoint::ok)
                .max((a, b) -> Integer.compare(a.n(), b.n()));
        if (largestRef.isEmpty()) {
            return Judgement.notMeasured();
        }
        int n = largestRef.get().n();
        double refMs = largestRef.get().medianMs();
        boolean userFailedBefore = user.stream().anyMatch(p -> p.n() <= n
                && ("TOO_SLOW".equals(p.status()) || "ERROR".equals(p.status())));
        Optional<BenchPoint> userAt = user.stream().filter(p -> p.n() == n && p.ok()).findFirst();
        if (userFailedBefore || userAt.isEmpty()) {
            return new Judgement(0, n, null, refMs, null);
        }
        double userMs = userAt.get().medianMs();
        double ratio = Math.max(userMs, FLOOR_MS) / Math.max(refMs, FLOOR_MS);
        int score = ratio <= 2.0 ? 30 : ratio <= 10.0 ? 20 : 10;
        return new Judgement(score, n, userMs, refMs, ratio);
    }
}
