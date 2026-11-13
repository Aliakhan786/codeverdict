package com.aaliya.codeverdict.review;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoringCalculatorTest {

    private final ScoringCalculator calc = new ScoringCalculator();

    @Test
    void perfectSolutionScoresHundred() {
        var s = calc.score(new ScoringCalculator.Inputs(16, 16, 0, 0, true, 30, 20));
        assertEquals(100, s.total());
        assertEquals("S", s.grade());
        assertFalse(s.partial());
    }

    @Test
    void roadmapExample() {
        // 14/16 hidden tests -> 35, one proven edge case -> 7, O(n^2) within 10x -> 20, quality 15
        var s = calc.score(new ScoringCalculator.Inputs(14, 16, 0, 1, true, 20, 15));
        assertEquals(35, s.correctness());
        assertEquals(7, s.edgeCases());
        assertEquals(77, s.total());
        assertEquals("B", s.grade());
    }

    @Test
    void edgeCaseScoreNeverNegative() {
        var s = calc.score(new ScoringCalculator.Inputs(0, 10, 0, 5, true, 0, 0));
        assertEquals(0, s.edgeCases());
        assertEquals(0, s.total());
        assertEquals("D", s.grade());
    }

    @Test
    void missingPartsAreScaledNotPunished() {
        // AI down: no charges, no quality. Only correctness (40) and performance (30) measured.
        var s = calc.score(new ScoringCalculator.Inputs(10, 10, 0, 0, false, 30, null));
        assertNull(s.quality());
        assertNull(s.edgeCases());
        assertEquals(100, s.total());
        assertTrue(s.partial());
    }

    @Test
    void gradeBoundaries() {
        assertEquals("S", ScoringCalculator.grade(95));
        assertEquals("A", ScoringCalculator.grade(94));
        assertEquals("A", ScoringCalculator.grade(85));
        assertEquals("B", ScoringCalculator.grade(70));
        assertEquals("C", ScoringCalculator.grade(50));
        assertEquals("D", ScoringCalculator.grade(49));
    }
}
