package com.aaliya.codeverdict.gamification;

import java.time.LocalDate;

/** Pure XP and streak rules (unit-tested, no database). */
public final class XpRules {

    private XpRules() {
    }

    /** Base XP for a full review: 10 + score / 5 (so 10..30), +10 for beating your previous best. */
    public static int reviewXp(int score, boolean beatPreviousBest) {
        return 10 + Math.max(0, Math.min(100, score)) / 5 + (beatPreviousBest ? 10 : 0);
    }

    /** Bonus for each mistake proven on the previous attempt and gone now (max 3 counted). */
    public static int fixBonus(int mistakesFixed) {
        return 15 * Math.min(3, Math.max(0, mistakesFixed));
    }

    /** Streak bonus when the streak grows: 5 + streak length, capped at 15. */
    public static int streakBonus(int newStreak) {
        return Math.min(15, 5 + newStreak);
    }

    public record StreakUpdate(int current, int longest, boolean extendedToday) {
    }

    /** Same day: unchanged. Yesterday: +1. Anything older (or first time): restart at 1. */
    public static StreakUpdate updateStreak(LocalDate lastActive, LocalDate today, int current, int longest) {
        if (lastActive != null && lastActive.equals(today)) {
            return new StreakUpdate(Math.max(current, 1), Math.max(longest, Math.max(current, 1)), false);
        }
        int next = lastActive != null && lastActive.plusDays(1).equals(today) ? current + 1 : 1;
        return new StreakUpdate(next, Math.max(longest, next), true);
    }

    /** A streak shown on the home page is only alive if you practised today or yesterday. */
    public static int visibleStreak(LocalDate lastActive, LocalDate today, int current) {
        if (lastActive == null) {
            return 0;
        }
        return lastActive.equals(today) || lastActive.plusDays(1).equals(today) ? current : 0;
    }
}
