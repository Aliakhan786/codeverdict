package com.aaliya.codeverdict.benchmark;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplexityEstimatorTest {

    private final ComplexityEstimator est = new ComplexityEstimator();

    private static BenchPoint p(int n, Double ms) {
        return new BenchPoint("USER", n, ms, 5, ms == null ? "TOO_SLOW" : "OK", null);
    }

    @Test
    void linearGrowthIsON() {
        var e = est.estimate(List.of(p(1000, 0.5), p(10000, 5.0), p(100000, 50.0)), "O(n)");
        assertTrue(e.measured());
        assertEquals("O(n)", e.label());
        assertEquals(1.0, e.slope(), 0.01);
    }

    @Test
    void quadraticGrowthIsON2EvenWhenLargestTimesOut() {
        var e = est.estimate(List.of(p(1000, 0.4), p(10000, 40.0), p(100000, null)), "O(n^2)");
        assertTrue(e.measured());
        assertEquals("O(n²)", e.label());
    }

    @Test
    void aiEstimateBreaksTieBetweenNAndNLogN() {
        var e = est.estimate(List.of(p(1000, 1.0), p(10000, 13.0), p(100000, 160.0)), "O(n log n)");
        assertEquals("O(n log n)", e.label());
    }

    @Test
    void tooFastToMeasureFallsBackToEstimate() {
        var e = est.estimate(List.of(p(1000, 0.01), p(10000, 0.05), p(100000, 0.2)), "O(n)");
        assertFalse(e.measured());
        assertEquals("O(n)", e.label());
    }

    @Test
    void inconsistentSlopesAreNotTrusted() {
        var e = est.estimate(List.of(p(1000, 1.0), p(10000, 1.2), p(100000, 300.0)), "O(n)");
        assertFalse(e.measured());
    }
}
