package com.aaliya.codeverdict.gamification;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XpRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);

    @Test
    void reviewXpScalesWithScore() {
        assertEquals(10, XpRules.reviewXp(0, false));
        assertEquals(30, XpRules.reviewXp(100, false));
        assertEquals(40, XpRules.reviewXp(100, true));
    }

    @Test
    void firstEverReviewStartsStreakAtOne() {
        var s = XpRules.updateStreak(null, TODAY, 0, 0);
        assertEquals(1, s.current());
        assertTrue(s.extendedToday());
    }

    @Test
    void consecutiveDayExtendsStreak() {
        var s = XpRules.updateStreak(TODAY.minusDays(1), TODAY, 5, 5);
        assertEquals(6, s.current());
        assertEquals(6, s.longest());
    }

    @Test
    void sameDayDoesNotDoubleCount() {
        var s = XpRules.updateStreak(TODAY, TODAY, 3, 7);
        assertEquals(3, s.current());
        assertEquals(7, s.longest());
        assertFalse(s.extendedToday());
    }

    @Test
    void missedDayResetsButKeepsLongest() {
        var s = XpRules.updateStreak(TODAY.minusDays(2), TODAY, 9, 9);
        assertEquals(1, s.current());
        assertEquals(9, s.longest());
    }

    @Test
    void visibleStreakDiesAfterAMissedDay() {
        assertEquals(4, XpRules.visibleStreak(TODAY.minusDays(1), TODAY, 4));
        assertEquals(0, XpRules.visibleStreak(TODAY.minusDays(2), TODAY, 4));
    }

    @Test
    void levelsFollowXp() {
        assertEquals("Intern", Levels.forXp(0).name());
        assertEquals("Junior Dev", Levels.forXp(150).name());
        assertEquals("Principal", Levels.forXp(99999).name());
        assertNull(Levels.next(Levels.forXp(99999)));
    }
}
