package com.aaliya.codeverdict.submission;

import com.aaliya.codeverdict.submission.HistoryDtos.AttemptSide;
import com.aaliya.codeverdict.submission.HistoryDtos.HistoryFilter;
import com.aaliya.codeverdict.submission.HistoryDtos.HistoryRow;
import com.aaliya.codeverdict.submission.HistoryDtos.Page;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** History table queries. Plain SQL: dynamic filters + paging are clearer here than JPA criteria. */
@Repository
public class HistoryQueries {

    private static final String BASE = """
            FROM submissions s
            JOIN problems p ON p.id = s.problem_id
            LEFT JOIN reviews r ON r.submission_id = s.id
            WHERE s.user_id = :uid
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final ZoneId zone;

    public HistoryQueries(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.zone = clock.getZone();
    }

    public Page<HistoryRow> page(UUID userId, HistoryFilter f, int page, int size) {
        MapSqlParameterSource p = new MapSqlParameterSource("uid", userId);
        StringBuilder where = new StringBuilder();
        if (f.problemId() != null) {
            where.append(" AND s.problem_id = :pid");
            p.addValue("pid", f.problemId());
        }
        if (f.topic() != null && !f.topic().isBlank()) {
            where.append(" AND p.topic = :topic");
            p.addValue("topic", f.topic());
        }
        if (f.difficulty() != null && !f.difficulty().isBlank()) {
            where.append(" AND p.difficulty = :difficulty");
            p.addValue("difficulty", f.difficulty());
        }
        if (f.minScore() != null) {
            where.append(" AND r.verdict_score >= :minScore");
            p.addValue("minScore", f.minScore());
        }
        if (f.maxScore() != null) {
            where.append(" AND r.verdict_score <= :maxScore");
            p.addValue("maxScore", f.maxScore());
        }
        if (f.from() != null) {
            where.append(" AND s.created_at >= :from");
            p.addValue("from", Timestamp.from(f.from().atStartOfDay(zone).toInstant()));
        }
        if (f.to() != null) {
            where.append(" AND s.created_at < :to");
            p.addValue("to", Timestamp.from(f.to().plusDays(1).atStartOfDay(zone).toInstant()));
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) " + BASE + where, p, Long.class);
        p.addValue("limit", size).addValue("offset", page * size);
        List<HistoryRow> rows = jdbc.query("""
                SELECT s.id, s.problem_id, p.title, p.topic, p.difficulty, s.attempt_no, s.status,
                       r.verdict_score, r.grade, r.duration_ms, s.created_at,
                       (SELECT COUNT(*) FROM charges c WHERE c.review_id = r.id AND c.status IN ('PROVEN','ACQUITTED')
                          AND c.category <> 'PERFORMANCE') AS bugs
                """ + BASE + where + " ORDER BY s.created_at DESC LIMIT :limit OFFSET :offset", p, HISTORY_ROW);
        long t = total == null ? 0 : total;
        return new Page<>(rows, page, size, t, (int) Math.ceil(t / (double) size));
    }

    /** All rows (for CSV export). */
    public List<HistoryRow> all(UUID userId) {
        return jdbc.query("""
                SELECT s.id, s.problem_id, p.title, p.topic, p.difficulty, s.attempt_no, s.status,
                       r.verdict_score, r.grade, r.duration_ms, s.created_at,
                       (SELECT COUNT(*) FROM charges c WHERE c.review_id = r.id AND c.status IN ('PROVEN','ACQUITTED')
                          AND c.category <> 'PERFORMANCE') AS bugs
                """ + BASE + " ORDER BY s.created_at DESC", new MapSqlParameterSource("uid", userId), HISTORY_ROW);
    }

    public Optional<AttemptSide> attempt(UUID userId, UUID submissionId) {
        List<AttemptSide> rows = jdbc.query("""
                SELECT s.id, s.attempt_no, s.code, s.created_at, s.problem_id,
                       r.verdict_score, r.grade, r.correctness_score, r.edge_case_score, r.performance_score, r.quality_score,
                       (SELECT COUNT(*) FROM charges c WHERE c.review_id = r.id AND c.status IN ('PROVEN','ACQUITTED')
                          AND c.category <> 'PERFORMANCE') AS bugs
                FROM submissions s LEFT JOIN reviews r ON r.submission_id = s.id
                WHERE s.id = :sid AND s.user_id = :uid
                """, new MapSqlParameterSource().addValue("sid", submissionId).addValue("uid", userId),
                (rs, i) -> new AttemptSide(rs.getObject("id", UUID.class), rs.getInt("attempt_no"), rs.getString("code"),
                        (Integer) rs.getObject("verdict_score"), rs.getString("grade"),
                        (Integer) rs.getObject("correctness_score"), (Integer) rs.getObject("edge_case_score"),
                        (Integer) rs.getObject("performance_score"), (Integer) rs.getObject("quality_score"),
                        rs.getInt("bugs"), rs.getTimestamp("created_at").toInstant()));
        return rows.stream().findFirst();
    }

    public Optional<UUID> problemOf(UUID submissionId) {
        List<UUID> ids = jdbc.query("SELECT problem_id FROM submissions WHERE id = :sid",
                new MapSqlParameterSource("sid", submissionId), (rs, i) -> rs.getObject(1, UUID.class));
        return ids.stream().findFirst();
    }

    private static final RowMapper<HistoryRow> HISTORY_ROW = (rs, i) -> new HistoryRow(
            rs.getObject("id", UUID.class),
            rs.getObject("problem_id", UUID.class),
            rs.getString("title"),
            rs.getString("topic"),
            rs.getString("difficulty"),
            rs.getInt("attempt_no"),
            rs.getString("status"),
            (Integer) rs.getObject("verdict_score"),
            rs.getString("grade"),
            rs.getInt("bugs"),
            (Long) rs.getObject("duration_ms"),
            rs.getTimestamp("created_at").toInstant());
}
