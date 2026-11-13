package com.aaliya.codeverdict.benchmark;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PerformanceJudgeTest {

    private final PerformanceJudge judge = new PerformanceJudge();

    private static BenchPoint u(int n, Double ms, String status) {
        return new BenchPoint("USER", n, ms, 5, status, null);
    }

    private static BenchPoint o(int n, double ms) {
        return new BenchPoint("OPTIMAL", n, ms, 5, "OK", null);
    }

    @Test
    void withinTwoTimesGetsFullMarks() {
        var j = judge.judge(List.of(u(100000, 3.0, "OK")), List.of(o(100000, 2.0)));
        assertEquals(30, j.score());
    }

    @Test
    void withinTenTimesGetsTwenty() {
        assertEquals(20, judge.judge(List.of(u(100000, 15.0, "OK")), List.of(o(100000, 2.0))).score());
    }

    @Test
    void finishingButSlowGetsTen() {
        assertEquals(10, judge.judge(List.of(u(100000, 500.0, "OK")), List.of(o(100000, 2.0))).score());
    }

    @Test
    void tooSlowGetsZero() {
        var j = judge.judge(List.of(u(10000, 40.0, "OK"), u(100000, null, "TOO_SLOW")), List.of(o(10000, 0.3), o(100000, 2.0)));
        assertEquals(0, j.score());
    }

    @Test
    void subMillisecondNoiseIsNotPunished() {
        // 0.3 ms vs 0.1 ms is 3x on paper but both are below the 0.5 ms noise floor
        assertEquals(30, judge.judge(List.of(u(100000, 0.3, "OK")), List.of(o(100000, 0.1))).score());
    }

    @Test
    void noReferenceMeansNotMeasured() {
        assertNull(judge.judge(List.of(u(100000, 3.0, "OK")), List.of()).score());
    }
}
