package com.aaliya.codeverdict.dashboard;

import com.aaliya.codeverdict.auth.AuthDtos.MeResponse;
import com.aaliya.codeverdict.auth.AuthService;
import com.aaliya.codeverdict.dashboard.DashboardDtos.Challenge;
import com.aaliya.codeverdict.dashboard.DashboardDtos.ContinueCase;
import com.aaliya.codeverdict.dashboard.DashboardDtos.Counters;
import com.aaliya.codeverdict.dashboard.DashboardDtos.PersonalBest;
import com.aaliya.codeverdict.dashboard.DashboardDtos.Progress;
import com.aaliya.codeverdict.dashboard.DashboardDtos.Summary;
import com.aaliya.codeverdict.dashboard.DashboardDtos.TopicStrength;
import com.aaliya.codeverdict.dashboard.DashboardDtos.WeekStats;
import com.aaliya.codeverdict.problem.ProblemDtos.ProblemResponse;
import com.aaliya.codeverdict.problem.ProblemService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class DashboardService {

    private final AuthService auth;
    private final ProblemService problems;
    private final ProgressQueries queries;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public DashboardService(AuthService auth, ProblemService problems, ProgressQueries queries,
                            NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.auth = auth;
        this.problems = problems;
        this.queries = queries;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Summary summary(UUID userId) {
        MeResponse me = auth.me(userId);
        LocalDate today = LocalDate.now(clock);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        MapSqlParameterSource p = new MapSqlParameterSource()
                .addValue("uid", userId)
                .addValue("today", Timestamp.from(today.atStartOfDay(clock.getZone()).toInstant()))
                .addValue("week", Timestamp.from(weekStart.atStartOfDay(clock.getZone()).toInstant()));
        Map<String, Object> w = jdbc.queryForMap("""
                SELECT
                  (SELECT COUNT(*) FROM reviews r JOIN submissions s ON s.id = r.submission_id
                     WHERE s.user_id = :uid AND r.kind = 'FULL' AND r.created_at >= :week) AS reviews,
                  (SELECT AVG(r.verdict_score) FROM reviews r JOIN submissions s ON s.id = r.submission_id
                     WHERE s.user_id = :uid AND r.kind = 'FULL' AND r.created_at >= :week) AS avg_score,
                  (SELECT COUNT(*) FROM charges c JOIN reviews r ON r.id = c.review_id JOIN submissions s ON s.id = r.submission_id
                     WHERE s.user_id = :uid AND c.status IN ('PROVEN','ACQUITTED') AND c.category <> 'PERFORMANCE'
                       AND r.created_at >= :week) AS bugs,
                  (SELECT COUNT(*) FROM submissions s WHERE s.user_id = :uid AND s.status = 'DONE'
                       AND s.created_at >= :today) AS today_count
                """, p);
        Number avg = (Number) w.get("avg_score");
        WeekStats week = new WeekStats(((Number) w.get("reviews")).intValue(),
                avg == null ? null : (int) Math.round(avg.doubleValue()), ((Number) w.get("bugs")).intValue());
        int todayCount = ((Number) w.get("today_count")).intValue();

        List<ProblemResponse> all = problems.list(userId, null, null);
        List<ContinueCase> continueCases = all.stream()
                .filter(pr -> pr.lastAttemptAt() != null)
                .sorted(Comparator.comparing(ProblemResponse::lastAttemptAt).reversed())
                .limit(3)
                .map(pr -> new ContinueCase(pr.id(), pr.title(), pr.topic().name(), pr.topicLabel(), pr.bestScore(),
                        pr.bestGrade(), pr.attempts(), pr.lastAttemptAt()))
                .toList();

        var s = me.stats();
        return new Summary(me.displayName(), s.level(), s.levelIndex(), s.nextLevel(), s.totalXp(), s.xpIntoLevel(),
                s.xpForNextLevel(), s.currentStreak(), s.longestStreak(), todayCount > 0, todayCount, me.dailyGoal(),
                week, todaysChallenge(userId, all, today), continueCases);
    }

    /**
     * Today's challenge: a problem from the weakest topic that is not mastered yet (best score < 95).
     * Stable for the whole day (seeded by date) so it does not change on every refresh.
     */
    private Challenge todaysChallenge(UUID userId, List<ProblemResponse> all, LocalDate today) {
        if (all.isEmpty()) {
            return null;
        }
        List<TopicStrength> strengths = queries.topicStrength(userId); // weakest first
        for (TopicStrength t : strengths) {
            Optional<ProblemResponse> pick = all.stream()
                    .filter(pr -> pr.topic().name().equals(t.topic()))
                    .filter(pr -> pr.bestScore() == null || pr.bestScore() < 95)
                    .min(Comparator.comparing((ProblemResponse pr) -> pr.bestScore() == null ? -1 : pr.bestScore()));
            if (pick.isPresent()) {
                ProblemResponse pr = pick.get();
                return new Challenge(pr.id(), pr.title(), pr.topic().name(), pr.topicLabel(), pr.difficulty().name(),
                        pr.bestScore(), "Your weakest topic is " + t.label() + " (avg " + Math.round(t.avgScore()) + ")");
            }
        }
        List<ProblemResponse> fresh = all.stream().filter(pr -> pr.attempts() == 0)
                .sorted(Comparator.comparing(ProblemResponse::difficulty).thenComparing(ProblemResponse::title)).toList();
        List<ProblemResponse> pool = fresh.isEmpty()
                ? all.stream().filter(pr -> pr.bestScore() == null || pr.bestScore() < 95).toList() : fresh;
        if (pool.isEmpty()) {
            pool = all;
        }
        ProblemResponse pr = pool.get((int) Math.floorMod(today.toEpochDay(), (long) pool.size()));
        return new Challenge(pr.id(), pr.title(), pr.topic().name(), pr.topicLabel(), pr.difficulty().name(),
                pr.bestScore(), fresh.isEmpty() ? "Push this one to a perfect score" : "A new case you have not tried yet");
    }

    @Transactional(readOnly = true)
    public Progress progress(UUID userId) {
        Map<String, Object> c = queries.counters(userId);
        List<PersonalBest> bests = queries.personalBests(userId);
        int matched = (int) bests.stream().filter(b -> "GREEN".equals(b.rating())).count();
        Counters counters = new Counters(num(c, "total_cases"), num(c, "bugs_proven"), num(c, "fixes_verified"),
                matched, num(c, "false_alarms"));
        return new Progress(queries.weeklyScores(userId, 12), queries.topicStrength(userId),
                queries.topMistakes(userId, 8), queries.heatmap(userId, 365), counters);
    }

    @Transactional(readOnly = true)
    public List<PersonalBest> personalBests(UUID userId) {
        return queries.personalBests(userId);
    }

    private static int num(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? 0 : ((Number) v).intValue();
    }
}
