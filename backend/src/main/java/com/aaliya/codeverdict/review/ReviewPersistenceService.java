package com.aaliya.codeverdict.review;

import com.aaliya.codeverdict.benchmark.Benchmark;
import com.aaliya.codeverdict.benchmark.BenchmarkRepository;
import com.aaliya.codeverdict.benchmark.PerformanceJudge;
import com.aaliya.codeverdict.common.ApiException;
import com.aaliya.codeverdict.gamification.GamificationService;
import com.aaliya.codeverdict.problem.OptimalSolution;
import com.aaliya.codeverdict.problem.OptimalSolutionRepository;
import com.aaliya.codeverdict.problem.Problem;
import com.aaliya.codeverdict.problem.ProblemRepository;
import com.aaliya.codeverdict.problem.TestInputSet;
import com.aaliya.codeverdict.proof.CaseFile;
import com.aaliya.codeverdict.proof.ReviewOutcome;
import com.aaliya.codeverdict.proof.Stage;
import com.aaliya.codeverdict.submission.Submission;
import com.aaliya.codeverdict.submission.SubmissionRepository;
import com.aaliya.codeverdict.submission.SubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Saves a finished review (verdict, charges, tests, timings, case file, XP) in ONE transaction. */
@Service
public class ReviewPersistenceService {

    private final SubmissionRepository submissions;
    private final ProblemRepository problems;
    private final ReviewRepository reviews;
    private final ChargeRepository charges;
    private final HiddenTestRepository hiddenTests;
    private final BenchmarkRepository benchmarks;
    private final OptimalSolutionRepository optimalSolutions;
    private final GamificationService gamification;

    public ReviewPersistenceService(SubmissionRepository submissions, ProblemRepository problems,
                                    ReviewRepository reviews, ChargeRepository charges,
                                    HiddenTestRepository hiddenTests, BenchmarkRepository benchmarks,
                                    OptimalSolutionRepository optimalSolutions, GamificationService gamification) {
        this.submissions = submissions;
        this.problems = problems;
        this.reviews = reviews;
        this.charges = charges;
        this.hiddenTests = hiddenTests;
        this.benchmarks = benchmarks;
        this.optimalSolutions = optimalSolutions;
        this.gamification = gamification;
    }

    @Transactional
    public Review save(UUID submissionId, ReviewOutcome o) {
        Submission sub = submissions.findById(submissionId).orElseThrow(() -> ApiException.notFound("Submission"));
        Problem problem = problems.findById(sub.getProblemId()).orElseThrow(() -> ApiException.notFound("Problem"));

        Review r = new Review();
        r.setSubmissionId(submissionId);
        boolean full = o.kind() == ReviewOutcome.Kind.FULL;
        r.setKind(full ? Review.KIND_FULL : Review.KIND_COMPILE_ERROR);
        r.setVerdictScore(o.scores().total());
        r.setGrade(o.scores().grade());
        r.setCorrectnessScore(o.scores().correctness());
        r.setEdgeCaseScore(o.scores().edgeCases());
        r.setPerformanceScore(o.scores().performance());
        r.setQualityScore(o.scores().quality());
        r.setSummary(o.summary());
        r.setClaimedComplexity(trim(o.claimedComplexity(), 40));
        r.setMeasuredComplexity(trim(o.measuredComplexity(), 40));
        r.setComplexityMeasured(o.complexityMeasured());
        r.setOptimalComplexity(trim(o.optimalComplexity(), 40));
        r.setKeyIdea(o.keyIdea());
        r.setHiddenTestsTotal(o.hiddenTests().size());
        r.setHiddenTestsPassed((int) o.hiddenTests().stream().filter(ReviewOutcome.HiddenTestResult::passed).count());
        r.setAiAvailable(o.aiAvailable());
        r.setReferenceVerified(o.referenceVerified());
        r.setAiModel(trim(o.aiModel(), 60));
        r.setAiTokensUsed(o.aiTokens());
        r.setAiCalls(o.aiCalls());
        r.setDurationMs(o.durationMs());
        PerformanceJudge.Judgement perf = o.performance();
        r.setDetails(new ReviewDetails(
                o.compileErrors().stream().map(e -> new ReviewDetails.CompileErrorItem(e.line(), e.column(), e.message())).toList(),
                o.quality(), o.warnings(), o.referenceNote(), o.complexitySlope(),
                perf == null ? null : perf.comparedN(), perf == null ? null : perf.userMs(),
                perf == null ? null : perf.optimalMs(), perf == null ? null : perf.ratio(),
                o.scores().partial()));
        r = reviews.saveAndFlush(r);

        List<Charge> savedCharges = new ArrayList<>();
        int pos = 0;
        for (ReviewOutcome.ChargeResult c : o.charges()) {
            Charge ch = new Charge();
            ch.setReviewId(r.getId());
            ch.setPosition(pos++);
            ch.setCategory(c.category());
            ch.setSeverity(c.severity());
            ch.setLineStart(c.lineStart());
            ch.setLineEnd(c.lineEnd());
            ch.setTitle(trim(c.title(), 300));
            ch.setExplanation(c.explanation());
            ch.setStatus(c.status());
            ch.setStatusReason(c.statusReason());
            ch.setBadEvidence(c.badEvidence());
            ch.setProofTestCode(c.proofTestCode());
            ch.setProofOutput(c.proofOutput());
            ch.setEvidenceLine(c.evidenceLine());
            ch.setSuggestedFixCode(c.suggestedFixCode());
            ch.setFixExplanation(c.fixExplanation());
            ch.setFixVerified(c.fixVerified());
            ch.setMistakeTag(c.mistakeTag());
            savedCharges.add(ch);
        }
        savedCharges = charges.saveAllAndFlush(savedCharges);

        List<HiddenTest> savedTests = new ArrayList<>();
        pos = 0;
        for (ReviewOutcome.HiddenTestResult h : o.hiddenTests()) {
            HiddenTest t = new HiddenTest();
            t.setReviewId(r.getId());
            t.setPosition(pos++);
            t.setInput(h.input() == null ? "" : h.input());
            t.setExpectedOutput(h.expected());
            t.setActualOutput(h.actual());
            t.setPassed(h.passed());
            t.setEdgeCase(h.edgeCase());
            t.setDescription(trim(h.description(), 200));
            t.setError(h.error());
            savedTests.add(t);
        }
        savedTests = hiddenTests.saveAllAndFlush(savedTests);

        final UUID reviewId = r.getId();
        benchmarks.saveAllAndFlush(o.benchmarks().stream().map(b -> new Benchmark(reviewId, b)).toList());

        if (o.caseFileIsNew() && o.caseFile() != null) {
            saveCaseFile(problem.getId(), o.caseFile());
        }

        if (full) {
            GamificationService.Result g = gamification.onReviewCompleted(sub, r, savedCharges, savedTests,
                    problem.getTopic().name());
            r.setXpEarned(g.xpEarned());
            r.setNewLevel(g.newLevel());
            r.setPersonalBest(g.personalBest());
        }

        sub.setStatus(SubmissionStatus.DONE);
        sub.setCurrentStage(Stage.VERDICT_READY.name());
        sub.setCompletedAt(Instant.now());
        return r;
    }

    private void saveCaseFile(UUID problemId, CaseFile cf) {
        OptimalSolution o = optimalSolutions.findByProblemId(problemId).orElseGet(OptimalSolution::new);
        o.setProblemId(problemId);
        o.setCode(cf.optimalCode());
        o.setBruteForceCode(cf.bruteForceCode());
        o.setComplexity(trim(cf.complexity(), 40));
        o.setKeyIdea(cf.keyIdea());
        o.setGeneratorCode(cf.generatorCode());
        o.setTestInputs(new TestInputSet(cf.inputs()));
        o.setVerified(cf.verified());
        o.setVerificationNote(cf.verificationNote());
        optimalSolutions.save(o);
    }

    private static String trim(String s, int max) {
        return s == null ? null : s.length() > max ? s.substring(0, max) : s;
    }
}
