package com.aaliya.codeverdict.gamification;

import com.aaliya.codeverdict.proof.ChargeCategory;
import com.aaliya.codeverdict.review.Charge;
import com.aaliya.codeverdict.review.HiddenTest;
import com.aaliya.codeverdict.review.Review;
import com.aaliya.codeverdict.submission.Submission;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * XP, levels, streaks and badges. Runs inside the same transaction that saves a review,
 * so a verdict and its rewards are stored together or not at all.
 */
@Service
public class GamificationService {

    public record Result(int xpEarned, String newLevel, boolean personalBest, Integer previousBest,
                         List<AchievementDto> newBadges) {
    }

    private final UserStatsRepository statsRepo;
    private final XpEventRepository xpEvents;
    private final AchievementRepository achievements;
    private final UserAchievementRepository userAchievements;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public GamificationService(UserStatsRepository statsRepo, XpEventRepository xpEvents,
                               AchievementRepository achievements, UserAchievementRepository userAchievements,
                               NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.statsRepo = statsRepo;
        this.xpEvents = xpEvents;
        this.achievements = achievements;
        this.userAchievements = userAchievements;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** Call after the review and its charges/tests were saved AND flushed. */
    @Transactional
    public Result onReviewCompleted(Submission submission, Review review, List<Charge> charges,
                                    List<HiddenTest> hiddenTests, String topic) {
        UUID userId = submission.getUserId();
        UserStats stats = statsRepo.findByUserId(userId).orElseGet(() -> {
            UserStats s = new UserStats();
            s.setUserId(userId);
            return statsRepo.save(s);
        });
        String levelBefore = Levels.forXp(stats.getTotalXp()).name();
        int score = review.getVerdictScore();
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("uid", userId)
                .addValue("pid", submission.getProblemId())
                .addValue("sid", submission.getId())
                .addValue("attempt", submission.getAttemptNo())
                .addValue("topic", topic);

        Integer previousBest = jdbc.queryForObject("""
                SELECT MAX(r.verdict_score) FROM reviews r JOIN submissions s ON s.id = r.submission_id
                WHERE s.problem_id = :pid AND s.id <> :sid AND r.kind = 'FULL'
                """, p, Integer.class);
        boolean personalBest = previousBest != null && score > previousBest;

        int earned = 0;
        // 1) review XP
        int reviewXp = XpRules.reviewXp(score, personalBest);
        earned += addXp(userId, XpEvent.Source.REVIEW, reviewXp, submission.getId(),
                "Review scored " + score + (personalBest ? " (personal best!)" : ""));

        // 2) bonus for fixing mistakes that were proven on the previous attempt
        List<String> previousTags = jdbc.queryForList("""
                SELECT DISTINCT c.mistake_tag FROM charges c
                JOIN reviews r ON r.id = c.review_id JOIN submissions s ON s.id = r.submission_id
                WHERE s.problem_id = :pid AND c.status IN ('PROVEN', 'ACQUITTED')
                  AND s.attempt_no = (SELECT MAX(s2.attempt_no) FROM submissions s2 JOIN reviews r2 ON r2.submission_id = s2.id
                                      WHERE s2.problem_id = :pid AND s2.attempt_no < :attempt AND r2.kind = 'FULL')
                """, p, String.class);
        Set<String> nowProven = charges.stream().filter(c -> c.getStatus().isGuilty())
                .map(c -> c.getMistakeTag().name()).collect(Collectors.toSet());
        long fixed = previousTags.stream().filter(t -> !nowProven.contains(t)).count();
        if (fixed > 0) {
            earned += addXp(userId, XpEvent.Source.FIX, XpRules.fixBonus((int) fixed), submission.getId(),
                    "Fixed " + fixed + " mistake(s) from your last attempt");
        }

        // 3) streak
        LocalDate today = LocalDate.now(clock);
        XpRules.StreakUpdate streak = XpRules.updateStreak(stats.getLastActiveDate(), today,
                stats.getCurrentStreak(), stats.getLongestStreak());
        stats.setCurrentStreak(streak.current());
        stats.setLongestStreak(streak.longest());
        stats.setLastActiveDate(today);
        if (streak.extendedToday() && streak.current() > 1) {
            earned += addXp(userId, XpEvent.Source.STREAK, XpRules.streakBonus(streak.current()), submission.getId(),
                    streak.current() + "-day streak");
        }

        // 4) badges
        List<AchievementDto> newBadges = checkBadges(submission, review, charges, hiddenTests, p, streak.current(), today);
        for (AchievementDto b : newBadges) {
            earned += addXp(userId, XpEvent.Source.BADGE, b.xpReward(), submission.getId(), "Badge: " + b.name());
        }

        stats.setTotalXp(stats.getTotalXp() + earned);
        String levelAfter = Levels.forXp(stats.getTotalXp()).name();
        stats.setLevel(levelAfter);
        statsRepo.save(stats);
        return new Result(earned, levelAfter.equals(levelBefore) ? null : levelAfter, personalBest, previousBest, newBadges);
    }

    @Transactional(readOnly = true)
    public List<AchievementDto> catalogue(UUID userId) {
        Map<UUID, UserAchievement> unlocked = userAchievements.findByUserId(userId).stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, Function.identity()));
        return achievements.findAllByOrderBySortOrderAsc().stream().map(a -> {
            UserAchievement ua = unlocked.get(a.getId());
            return new AchievementDto(a.getCode(), a.getName(), a.getDescription(), a.getIcon(), a.getXpReward(),
                    ua != null, ua == null ? null : ua.getUnlockedAt());
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<AchievementDto> unlockedBy(UUID submissionId) {
        Map<UUID, Achievement> all = achievements.findAll().stream()
                .collect(Collectors.toMap(Achievement::getId, Function.identity()));
        List<AchievementDto> list = new ArrayList<>();
        for (UserAchievement ua : userAchievements.findBySubmissionId(submissionId)) {
            Achievement a = all.get(ua.getAchievementId());
            if (a != null) {
                list.add(new AchievementDto(a.getCode(), a.getName(), a.getDescription(), a.getIcon(), a.getXpReward(),
                        true, ua.getUnlockedAt()));
            }
        }
        return list;
    }

    // ------------------------------------------------------------------------------------------

    private List<AchievementDto> checkBadges(Submission submission, Review review, List<Charge> charges,
                                             List<HiddenTest> hiddenTests, MapSqlParameterSource p,
                                             int streak, LocalDate today) {
        UUID userId = submission.getUserId();
        Set<UUID> have = new HashSet<>();
        userAchievements.findByUserId(userId).forEach(ua -> have.add(ua.getAchievementId()));
        Integer fullReviews = jdbc.queryForObject("""
                SELECT COUNT(*) FROM reviews r JOIN submissions s ON s.id = r.submission_id
                WHERE s.user_id = :uid AND r.kind = 'FULL'
                """, p, Integer.class);
        Integer firstScore = jdbc.query("""
                SELECT r.verdict_score FROM reviews r JOIN submissions s ON s.id = r.submission_id
                WHERE s.problem_id = :pid AND r.kind = 'FULL' AND s.id <> :sid
                ORDER BY s.attempt_no ASC LIMIT 1
                """, p, rs -> rs.next() ? rs.getInt(1) : null);
        Map<String, Object> topicStats = jdbc.queryForMap("""
                SELECT COUNT(*) AS n, COALESCE(AVG(r.verdict_score), 0) AS avg
                FROM reviews r JOIN submissions s ON s.id = r.submission_id JOIN problems pr ON pr.id = s.problem_id
                WHERE s.user_id = :uid AND pr.topic = :topic AND r.kind = 'FULL'
                """, p);
        long topicCount = ((Number) topicStats.get("n")).longValue();
        double topicAvg = ((Number) topicStats.get("avg")).doubleValue();

        boolean anyGuilty = charges.stream().anyMatch(c -> c.getStatus().isGuilty());
        boolean edgeGuilty = charges.stream().anyMatch(c -> c.getStatus().isGuilty() && c.getCategory() == ChargeCategory.EDGE_CASE);
        List<HiddenTest> edgeTests = hiddenTests.stream().filter(HiddenTest::isEdgeCase).toList();
        int score = review.getVerdictScore();
        int total = fullReviews == null ? 0 : fullReviews;

        List<String> earnedCodes = new ArrayList<>();
        if (total >= 1) earnedCodes.add("FIRST_VERDICT");
        if (score >= 90 && !anyGuilty) earnedCodes.add("CLEAN_RECORD");
        if (review.getPerformanceScore() != null && review.getPerformanceScore() == 30) earnedCodes.add("SPEED_DEMON");
        if (firstScore != null && score - firstScore >= 30) earnedCodes.add("COMEBACK");
        if (edgeTests.size() >= 3 && edgeTests.stream().allMatch(HiddenTest::isPassed) && !edgeGuilty) earnedCodes.add("EDGE_LORD");
        if (streak >= 7) earnedCodes.add("STREAK_7");
        if (streak >= 30) earnedCodes.add("STREAK_30");
        if (topicCount >= 3 && topicAvg >= 85) earnedCodes.add("TOPIC_MASTER");
        if (total >= 10) earnedCodes.add("CASELOAD_10");
        if (total >= 50) earnedCodes.add("CASELOAD_50");

        List<AchievementDto> unlocked = new ArrayList<>();
        for (Achievement a : achievements.findAllByOrderBySortOrderAsc()) {
            if (earnedCodes.contains(a.getCode()) && !have.contains(a.getId())) {
                UserAchievement ua = userAchievements.save(new UserAchievement(userId, a.getId(), submission.getId()));
                unlocked.add(new AchievementDto(a.getCode(), a.getName(), a.getDescription(), a.getIcon(),
                        a.getXpReward(), true, ua.getUnlockedAt()));
            }
        }
        return unlocked;
    }

    private int addXp(UUID userId, XpEvent.Source source, int points, UUID submissionId, String description) {
        xpEvents.save(new XpEvent(userId, source, points, submissionId, description));
        return points;
    }
}
