package com.aaliya.codeverdict.problem;

import com.aaliya.codeverdict.common.ApiException;
import com.aaliya.codeverdict.problem.ProblemDtos.CompileErrorDto;
import com.aaliya.codeverdict.problem.ProblemDtos.OptimalResponse;
import com.aaliya.codeverdict.problem.ProblemDtos.ProblemRequest;
import com.aaliya.codeverdict.problem.ProblemDtos.ProblemResponse;
import com.aaliya.codeverdict.problem.ProblemDtos.RunResponse;
import com.aaliya.codeverdict.proof.MethodSignature;
import com.aaliya.codeverdict.proof.OutputComparator;
import com.aaliya.codeverdict.proof.ProofEngine;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Semaphore;

@Service
public class ProblemService {

    private final ProblemRepository problems;
    private final OptimalSolutionRepository optimalSolutions;
    private final NamedParameterJdbcTemplate jdbc;
    private final ProofEngine engine;
    /** "Run" compiles and starts a JVM; allow only a few at once so the server stays responsive. */
    private final Semaphore runSlots = new Semaphore(3);

    public ProblemService(ProblemRepository problems, OptimalSolutionRepository optimalSolutions,
                          NamedParameterJdbcTemplate jdbc, ProofEngine engine) {
        this.problems = problems;
        this.optimalSolutions = optimalSolutions;
        this.jdbc = jdbc;
        this.engine = engine;
    }

    private record Stats(int attempts, Integer bestScore, String bestGrade, java.time.Instant lastAttemptAt) {
    }

    @Transactional(readOnly = true)
    public List<ProblemResponse> list(UUID userId, Topic topic, Difficulty difficulty) {
        Map<UUID, Stats> stats = statsByProblem(userId);
        return problems.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .filter(p -> topic == null || p.getTopic() == topic)
                .filter(p -> difficulty == null || p.getDifficulty() == difficulty)
                .map(p -> toResponse(p, stats.get(p.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProblemResponse get(UUID userId, UUID id) {
        Problem p = owned(userId, id);
        return toResponse(p, statsByProblem(userId).get(id));
    }

    @Transactional
    public ProblemResponse create(UUID userId, ProblemRequest req) {
        validateSignature(req.methodSignature());
        Problem p = new Problem();
        p.setUserId(userId);
        apply(p, req);
        return toResponse(problems.save(p), null);
    }

    @Transactional
    public ProblemResponse update(UUID userId, UUID id, ProblemRequest req) {
        validateSignature(req.methodSignature());
        Problem p = owned(userId, id);
        boolean contractChanged = !Objects.equals(p.getStatement(), req.statement())
                || !Objects.equals(p.getMethodSignature(), req.methodSignature().trim())
                || !Objects.equals(p.getSampleInput(), blankToNull(req.sampleInput()))
                || !Objects.equals(p.getSampleOutput(), blankToNull(req.sampleOutput()));
        apply(p, req);
        if (contractChanged) {
            // the cached reference/inputs were built for the old problem: rebuild on the next review
            optimalSolutions.deleteByProblemId(id);
        }
        return toResponse(p, statsByProblem(userId).get(id));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        problems.delete(owned(userId, id));
    }

    public RunResponse run(UUID userId, UUID id, String code) {
        Problem p = owned(userId, id);
        if (!runSlots.tryAcquire()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "BUSY", "Too many runs at once, try again in a second");
        }
        try {
            ProofEngine.SampleRun r = engine.runSample(p.getMethodSignature(), p.getSampleInput(), p.getSampleOutput(),
                    p.getCompareMode(), code);
            return new RunResponse(r.compiled(),
                    r.compileErrors().stream().map(e -> new CompileErrorDto(e.line(), e.column(), e.message())).toList(),
                    r.output(), r.expected(), r.passed(), r.error(), r.stdout(), r.timeMs());
        } finally {
            runSlots.release();
        }
    }

    @Transactional(readOnly = true)
    public OptimalResponse optimal(UUID userId, UUID id) {
        owned(userId, id);
        Integer done = jdbc.queryForObject(
                "SELECT COUNT(*) FROM submissions WHERE problem_id = :pid AND user_id = :uid AND status = 'DONE'",
                new MapSqlParameterSource().addValue("pid", id).addValue("uid", userId), Integer.class);
        if (done == null || done == 0) {
            throw new ApiException(HttpStatus.FORBIDDEN, "TRY_FIRST", "Submit at least one attempt before viewing the optimal solution");
        }
        OptimalSolution o = optimalSolutions.findByProblemId(id)
                .orElseThrow(() -> ApiException.notFound("Optimal solution (it is created during your first review)"));
        return new OptimalResponse(o.getCode(), o.getComplexity(), o.getKeyIdea(), o.isVerified(), o.getVerificationNote());
    }

    public Problem owned(UUID userId, UUID id) {
        return problems.findByIdAndUserId(id, userId).orElseThrow(() -> ApiException.notFound("Problem"));
    }

    // ------------------------------------------------------------------------------------------

    private Map<UUID, Stats> statsByProblem(UUID userId) {
        String sql = """
                SELECT s.problem_id,
                       COUNT(*)                     AS attempts,
                       MAX(r.verdict_score)         AS best_score,
                       MAX(s.created_at)            AS last_attempt,
                       (SELECT r2.grade FROM reviews r2 JOIN submissions s2 ON s2.id = r2.submission_id
                         WHERE s2.problem_id = s.problem_id AND r2.kind = 'FULL'
                         ORDER BY r2.verdict_score DESC LIMIT 1) AS best_grade
                FROM submissions s
                LEFT JOIN reviews r ON r.submission_id = s.id AND r.kind = 'FULL'
                WHERE s.user_id = :uid
                GROUP BY s.problem_id
                """;
        Map<UUID, Stats> map = new HashMap<>();
        jdbc.query(sql, new MapSqlParameterSource("uid", userId), rs -> {
            Timestamp last = rs.getTimestamp("last_attempt");
            int best = rs.getInt("best_score");
            Integer bestScore = rs.wasNull() ? null : best;
            map.put(rs.getObject("problem_id", UUID.class), new Stats(rs.getInt("attempts"), bestScore,
                    rs.getString("best_grade"), last == null ? null : last.toInstant()));
        });
        return map;
    }

    private static void apply(Problem p, ProblemRequest req) {
        p.setTitle(req.title().trim());
        p.setStatement(req.statement());
        p.setTopic(req.topic());
        p.setDifficulty(req.difficulty());
        p.setMethodSignature(req.methodSignature().trim());
        p.setSampleInput(blankToNull(req.sampleInput()));
        p.setSampleOutput(blankToNull(req.sampleOutput()));
        p.setCompareMode(req.compareMode() == null ? OutputComparator.Mode.EXACT : req.compareMode());
    }

    private static void validateSignature(String signature) {
        try {
            MethodSignature.parse(signature);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    static ProblemResponse toResponse(Problem p, Stats s) {
        return new ProblemResponse(p.getId(), p.getTitle(), p.getStatement(), p.getTopic(), p.getTopic().label(),
                p.getDifficulty(), p.getMethodSignature(), p.getSampleInput(), p.getSampleOutput(), p.getCompareMode(),
                StarterCode.of(p.getMethodSignature()),
                s == null ? 0 : s.attempts(), s == null ? null : s.bestScore(), s == null ? null : s.bestGrade(),
                s == null ? null : s.lastAttemptAt(), p.getCreatedAt());
    }
}
