package com.aaliya.codeverdict.review;

import com.aaliya.codeverdict.benchmark.Benchmark;
import com.aaliya.codeverdict.benchmark.BenchmarkRepository;
import com.aaliya.codeverdict.common.ApiException;
import com.aaliya.codeverdict.gamification.GamificationService;
import com.aaliya.codeverdict.problem.Problem;
import com.aaliya.codeverdict.problem.ProblemRepository;
import com.aaliya.codeverdict.review.VerdictDtos.ChargeDto;
import com.aaliya.codeverdict.review.VerdictDtos.HiddenTestDto;
import com.aaliya.codeverdict.review.VerdictDtos.HiddenTestsDto;
import com.aaliya.codeverdict.review.VerdictDtos.PerformanceDto;
import com.aaliya.codeverdict.review.VerdictDtos.PointDto;
import com.aaliya.codeverdict.review.VerdictDtos.PreviousAttempt;
import com.aaliya.codeverdict.review.VerdictDtos.ReviewDto;
import com.aaliya.codeverdict.review.VerdictDtos.ScoresDto;
import com.aaliya.codeverdict.review.VerdictDtos.VerdictResponse;
import com.aaliya.codeverdict.submission.Submission;
import com.aaliya.codeverdict.submission.SubmissionRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Builds the Verdict page JSON from the stored review. */
@Service
public class VerdictService {

    private final SubmissionRepository submissions;
    private final ProblemRepository problems;
    private final ReviewRepository reviews;
    private final ChargeRepository charges;
    private final HiddenTestRepository hiddenTests;
    private final BenchmarkRepository benchmarks;
    private final GamificationService gamification;
    private final NamedParameterJdbcTemplate jdbc;

    public VerdictService(SubmissionRepository submissions, ProblemRepository problems, ReviewRepository reviews,
                          ChargeRepository charges, HiddenTestRepository hiddenTests, BenchmarkRepository benchmarks,
                          GamificationService gamification, NamedParameterJdbcTemplate jdbc) {
        this.submissions = submissions;
        this.problems = problems;
        this.reviews = reviews;
        this.charges = charges;
        this.hiddenTests = hiddenTests;
        this.benchmarks = benchmarks;
        this.gamification = gamification;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public VerdictResponse get(UUID userId, UUID submissionId) {
        Submission s = submissions.findByIdAndUserId(submissionId, userId)
                .orElseThrow(() -> ApiException.notFound("Submission"));
        Problem p = problems.findById(s.getProblemId()).orElseThrow(() -> ApiException.notFound("Problem"));
        ReviewDto review = reviews.findBySubmissionId(s.getId()).map(r -> toDto(r, s.getId())).orElse(null);
        return new VerdictResponse(s.getId(), p.getId(), p.getTitle(), p.getTopic(), p.getTopic().label(),
                p.getDifficulty(), p.getMethodSignature(), s.getAttemptNo(), s.getCode(), s.getStatus(),
                s.getCurrentStage(), s.getFailureReason(), s.getCreatedAt(), s.getCompletedAt(),
                previousAttempt(s), review);
    }

    private PreviousAttempt previousAttempt(Submission s) {
        List<PreviousAttempt> rows = jdbc.query("""
                SELECT s.id, s.attempt_no, r.verdict_score FROM submissions s JOIN reviews r ON r.submission_id = s.id
                WHERE s.problem_id = :pid AND s.attempt_no < :attempt AND r.kind = 'FULL'
                ORDER BY s.attempt_no DESC LIMIT 1
                """, new MapSqlParameterSource().addValue("pid", s.getProblemId()).addValue("attempt", s.getAttemptNo()),
                (rs, i) -> new PreviousAttempt(rs.getObject("id", UUID.class), rs.getInt("attempt_no"),
                        rs.getInt("verdict_score")));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private ReviewDto toDto(Review r, UUID submissionId) {
        List<ChargeDto> chargeDtos = charges.findByReviewIdOrderByPositionAsc(r.getId()).stream()
                .map(c -> new ChargeDto(c.getId(), c.getCategory(), c.getSeverity(), c.getLineStart(), c.getLineEnd(),
                        c.getTitle(), c.getExplanation(), c.getStatus(), c.getStatusReason(), c.isBadEvidence(),
                        c.getProofTestCode(), c.getProofOutput(), c.getEvidenceLine(), c.getSuggestedFixCode(),
                        c.getFixExplanation(), c.getFixVerified(), c.getMistakeTag(), c.getMistakeTag().label()))
                .toList();
        List<HiddenTestDto> tests = hiddenTests.findByReviewIdOrderByPositionAsc(r.getId()).stream()
                .map(t -> new HiddenTestDto(t.getInput(), t.getExpectedOutput(), t.getActualOutput(), t.isPassed(),
                        t.isEdgeCase(), t.getDescription(), t.getError()))
                .toList();
        List<Benchmark> bench = benchmarks.findByReviewIdOrderBySolutionAscInputSizeAsc(r.getId());
        ReviewDetails d = r.getDetails();
        PerformanceDto perf = new PerformanceDto(r.getClaimedComplexity(), r.getMeasuredComplexity(),
                r.isComplexityMeasured(), d == null ? null : d.complexitySlope(), r.getOptimalComplexity(),
                r.getKeyIdea(), d == null ? null : d.comparedN(), d == null ? null : d.performanceRatio(),
                points(bench, "USER"), points(bench, "OPTIMAL"));
        return new ReviewDto(r.getKind(), r.getVerdictScore(), r.getGrade(), r.getSummary(),
                new ScoresDto(r.getCorrectnessScore(), r.getEdgeCaseScore(), r.getPerformanceScore(), r.getQualityScore(),
                        d != null && d.scorePartial()),
                chargeDtos, new HiddenTestsDto(r.getHiddenTestsPassed(), r.getHiddenTestsTotal(), tests), perf,
                d == null ? null : d.quality(),
                d == null || d.compileErrors() == null ? List.of() : d.compileErrors(),
                d == null || d.warnings() == null ? List.of() : d.warnings(),
                r.isAiAvailable(), r.isReferenceVerified(), d == null ? null : d.referenceNote(),
                r.getAiModel(), r.getAiTokensUsed(), r.getAiCalls(), r.getDurationMs(), r.getXpEarned(),
                r.getNewLevel(), r.isPersonalBest(), gamification.unlockedBy(submissionId));
    }

    private static List<PointDto> points(List<Benchmark> bench, String solution) {
        return bench.stream().filter(b -> solution.equals(b.getSolution()))
                .map(b -> new PointDto(b.getInputSize(), b.getMedianMs(), b.getStatus())).toList();
    }
}
