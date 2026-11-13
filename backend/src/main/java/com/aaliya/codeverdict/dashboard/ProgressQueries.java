package com.aaliya.codeverdict.dashboard;

import com.aaliya.codeverdict.dashboard.DashboardDtos.HeatmapDay;
import com.aaliya.codeverdict.dashboard.DashboardDtos.MistakeCount;
import com.aaliya.codeverdict.dashboard.DashboardDtos.PersonalBest;
import com.aaliya.codeverdict.dashboard.DashboardDtos.TopicStrength;
import com.aaliya.codeverdict.dashboard.DashboardDtos.WeeklyScore;
import com.aaliya.codeverdict.problem.Topic;
import com.aaliya.codeverdict.proof.MistakeTag;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The analytics SQL behind the dashboards (good interview material):
 * date_trunc for weekly averages, GROUP BY for top mistakes, ROW_NUMBER() window
 * function for the personal best per problem, LATERAL joins for the matching timings.
 * Times are bucketed in the app's time zone, not UTC.
 */
@Repository
public class ProgressQueries {

    private final NamedParameterJdbcTemplate jdbc;
    private final String zone;

    public ProgressQueries(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.zone = clock.getZone().getId();
    }

    private MapSqlParameterSource params(UUID userId) {
        return new MapSqlParameterSource().addValue("uid", userId).addValue("tz", zone);
    }

    public List<WeeklyScore> weeklyScores(UUID userId, int weeks) {
        return jdbc.query("""
                SELECT date_trunc('week', r.created_at AT TIME ZONE :tz)::date AS week,
                       AVG(r.verdict_score) AS avg_score, COUNT(*) AS reviews
                FROM reviews r JOIN submissions s ON s.id = r.submission_id
                WHERE s.user_id = :uid AND r.kind = 'FULL'
                  AND r.created_at >= now() - make_interval(weeks => :weeks)
                GROUP BY 1 ORDER BY 1
                """, params(userId).addValue("weeks", weeks),
                (rs, i) -> new WeeklyScore(rs.getObject("week", LocalDate.class),
                        Math.round(rs.getDouble("avg_score") * 10) / 10.0, rs.getInt("reviews")));
    }

    public List<TopicStrength> topicStrength(UUID userId) {
        return jdbc.query("""
                SELECT p.topic, AVG(r.verdict_score) AS avg_score, COUNT(*) AS reviews
                FROM reviews r
                JOIN submissions s ON s.id = r.submission_id
                JOIN problems p ON p.id = s.problem_id
                WHERE s.user_id = :uid AND r.kind = 'FULL'
                GROUP BY p.topic ORDER BY avg_score ASC
                """, params(userId),
                (rs, i) -> new TopicStrength(rs.getString("topic"), topicLabel(rs.getString("topic")),
                        Math.round(rs.getDouble("avg_score") * 10) / 10.0, rs.getInt("reviews")));
    }

    public List<MistakeCount> topMistakes(UUID userId, int limit) {
        return jdbc.query("""
                SELECT c.mistake_tag, COUNT(*) AS n, array_agg(DISTINCT s.id) AS submission_ids
                FROM charges c
                JOIN reviews r ON r.id = c.review_id
                JOIN submissions s ON s.id = r.submission_id
                WHERE s.user_id = :uid AND c.status IN ('PROVEN', 'ACQUITTED')
                GROUP BY c.mistake_tag
                ORDER BY n DESC
                LIMIT :limit
                """, params(userId).addValue("limit", limit),
                (rs, i) -> new MistakeCount(rs.getString("mistake_tag"), MistakeTag.parse(rs.getString("mistake_tag")).label(),
                        rs.getInt("n"), uuids(rs.getArray("submission_ids"))));
    }

    public List<HeatmapDay> heatmap(UUID userId, int days) {
        return jdbc.query("""
                SELECT (s.created_at AT TIME ZONE :tz)::date AS day, COUNT(*) AS n
                FROM submissions s
                WHERE s.user_id = :uid AND s.status = 'DONE'
                  AND s.created_at >= now() - make_interval(days => :days)
                GROUP BY 1 ORDER BY 1
                """, params(userId).addValue("days", days),
                (rs, i) -> new HeatmapDay(rs.getObject("day", LocalDate.class), rs.getInt("n")));
    }

    public Map<String, Object> counters(UUID userId) {
        return jdbc.queryForMap("""
                SELECT
                  (SELECT COUNT(*) FROM reviews r JOIN submissions s ON s.id = r.submission_id
                     WHERE s.user_id = :uid AND r.kind = 'FULL') AS total_cases,
                  (SELECT COUNT(*) FROM charges c JOIN reviews r ON r.id = c.review_id JOIN submissions s ON s.id = r.submission_id
                     WHERE s.user_id = :uid AND c.status IN ('PROVEN','ACQUITTED') AND c.category <> 'PERFORMANCE') AS bugs_proven,
                  (SELECT COUNT(*) FROM charges c JOIN reviews r ON r.id = c.review_id JOIN submissions s ON s.id = r.submission_id
                     WHERE s.user_id = :uid AND c.status = 'ACQUITTED') AS fixes_verified,
                  (SELECT COUNT(*) FROM charges c JOIN reviews r ON r.id = c.review_id JOIN submissions s ON s.id = r.submission_id
                     WHERE s.user_id = :uid AND c.status = 'DISMISSED' AND c.category IN ('BUG','EDGE_CASE')) AS false_alarms
                """, params(userId));
    }

    /** Best attempt per problem via ROW_NUMBER() OVER (PARTITION BY problem_id ORDER BY verdict_score DESC). */
    public List<PersonalBest> personalBests(UUID userId) {
        return jdbc.query("""
                WITH ranked AS (
                    SELECT s.problem_id, s.id AS submission_id, r.id AS review_id, r.verdict_score, r.grade,
                           r.hidden_tests_passed, r.hidden_tests_total,
                           ROW_NUMBER() OVER (PARTITION BY s.problem_id ORDER BY r.verdict_score DESC, s.created_at ASC) AS rn,
                           COUNT(*) OVER (PARTITION BY s.problem_id) AS attempts
                    FROM submissions s JOIN reviews r ON r.submission_id = s.id
                    WHERE s.user_id = :uid AND r.kind = 'FULL'
                )
                SELECT p.id, p.title, p.topic, p.difficulty, rk.verdict_score, rk.grade, rk.attempts, rk.submission_id,
                       rk.hidden_tests_passed, rk.hidden_tests_total,
                       ub.input_size, ub.median_ms AS user_ms, ob.median_ms AS optimal_ms
                FROM ranked rk
                JOIN problems p ON p.id = rk.problem_id
                LEFT JOIN LATERAL (
                    SELECT b.input_size, b.median_ms FROM benchmarks b
                    WHERE b.review_id = rk.review_id AND b.solution = 'USER' AND b.status = 'OK'
                    ORDER BY b.input_size DESC LIMIT 1) ub ON TRUE
                LEFT JOIN LATERAL (
                    SELECT b.median_ms FROM benchmarks b
                    WHERE b.review_id = rk.review_id AND b.solution = 'OPTIMAL' AND b.status = 'OK'
                      AND b.input_size = ub.input_size LIMIT 1) ob ON TRUE
                WHERE rk.rn = 1
                ORDER BY rk.verdict_score DESC, p.title
                """, params(userId), (rs, i) -> {
            Double userMs = (Double) rs.getObject("user_ms");
            Double optMs = (Double) rs.getObject("optimal_ms");
            Integer size = (Integer) rs.getObject("input_size");
            boolean allPassed = rs.getInt("hidden_tests_total") > 0
                    && rs.getInt("hidden_tests_passed") == rs.getInt("hidden_tests_total");
            Double ratio = userMs != null && optMs != null ? Math.max(userMs, 0.5) / Math.max(optMs, 0.5) : null;
            return new PersonalBest(rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("topic"),
                    topicLabel(rs.getString("topic")), rs.getString("difficulty"), rs.getInt("verdict_score"),
                    rs.getString("grade"), rs.getInt("attempts"), rs.getObject("submission_id", UUID.class),
                    size, userMs, optMs, ratio == null ? null : Math.round(ratio * 100) / 100.0, allPassed,
                    rating(ratio, allPassed, rs.getInt("verdict_score")));
        });
    }

    /** green = matches/beats optimal, yellow = close, red = far. */
    static String rating(Double ratio, boolean allPassed, int score) {
        if (ratio == null) {
            return score >= 85 ? "YELLOW" : "RED";
        }
        if (allPassed && ratio <= 1.5) {
            return "GREEN";
        }
        if (ratio <= 5 && score >= 60) {
            return "YELLOW";
        }
        return "RED";
    }

    private static String topicLabel(String topic) {
        try {
            return Topic.valueOf(topic).label();
        } catch (IllegalArgumentException | NullPointerException e) {
            return topic;
        }
    }

    private static List<UUID> uuids(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        Object[] values = (Object[]) array.getArray();
        List<UUID> list = new ArrayList<>();
        Arrays.stream(values).limit(20).forEach(v -> list.add(v instanceof UUID u ? u : UUID.fromString(v.toString())));
        return list;
    }
}
