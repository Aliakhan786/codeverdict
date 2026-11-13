package com.aaliya.codeverdict.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class DashboardDtos {

    private DashboardDtos() {
    }

    // ---------- Home
    public record Summary(String displayName, String level, int levelIndex, String nextLevel, int totalXp,
                          int xpIntoLevel, int xpForNextLevel, int currentStreak, int longestStreak,
                          boolean activeToday, int todayCount, int dailyGoal, WeekStats week,
                          Challenge todaysChallenge, List<ContinueCase> continueCases) {
    }

    public record WeekStats(int reviews, Integer avgScore, int bugsProven) {
    }

    public record Challenge(UUID problemId, String title, String topic, String topicLabel, String difficulty,
                            Integer bestScore, String reason) {
    }

    public record ContinueCase(UUID problemId, String title, String topic, String topicLabel, Integer bestScore,
                               String bestGrade, int attempts, Instant lastAttemptAt) {
    }

    // ---------- Progress
    public record Progress(List<WeeklyScore> weeklyScores, List<TopicStrength> topicStrength,
                           List<MistakeCount> topMistakes, List<HeatmapDay> heatmap, Counters counters) {
    }

    public record WeeklyScore(LocalDate week, double avgScore, int reviews) {
    }

    public record TopicStrength(String topic, String label, double avgScore, int reviews) {
    }

    public record MistakeCount(String tag, String label, int count, List<UUID> submissionIds) {
    }

    public record HeatmapDay(LocalDate date, int count) {
    }

    public record Counters(int totalCases, int bugsProven, int fixesVerified, int matchedOptimal, int aiFalseAlarmsCaught) {
    }

    // ---------- Personal bests
    public record PersonalBest(UUID problemId, String title, String topic, String topicLabel, String difficulty,
                               int bestScore, String grade, int attempts, UUID bestSubmissionId,
                               Integer inputSize, Double userMs, Double optimalMs, Double ratio,
                               boolean allHiddenPassed, String rating) {
    }
}
