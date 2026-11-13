package com.aaliya.codeverdict.proof;

import com.aaliya.codeverdict.ai.dto.QualityResponse;
import com.aaliya.codeverdict.benchmark.BenchPoint;
import com.aaliya.codeverdict.benchmark.PerformanceJudge;
import com.aaliya.codeverdict.review.ScoringCalculator;
import com.aaliya.codeverdict.runner.JavaSourceCompiler.CompileError;

import java.util.List;

/** Everything the Proof Engine found. Pure data: the Spring layer saves it to PostgreSQL. */
public record ReviewOutcome(
        Kind kind,
        List<CompileError> compileErrors,
        List<ChargeResult> charges,
        List<HiddenTestResult> hiddenTests,
        List<BenchPoint> benchmarks,
        PerformanceJudge.Judgement performance,
        String claimedComplexity,
        String measuredComplexity,
        boolean complexityMeasured,
        Double complexitySlope,
        String optimalComplexity,
        String keyIdea,
        QualityResponse quality,
        ScoringCalculator.Scores scores,
        String summary,
        boolean aiAvailable,
        boolean referenceVerified,
        String referenceNote,
        List<String> warnings,
        String aiModel,
        int aiTokens,
        int aiCalls,
        long durationMs,
        CaseFile caseFile,
        boolean caseFileIsNew) {

    public enum Kind { FULL, COMPILE_ERROR }

    public record ChargeResult(
            String key,
            ChargeCategory category,
            Severity severity,
            int lineStart,
            int lineEnd,
            String title,
            String explanation,
            MistakeTag mistakeTag,
            ChargeStatus status,
            String statusReason,
            boolean badEvidence,
            String proofTestCode,
            String proofOutput,
            Integer evidenceLine,
            String suggestedFixCode,
            String fixExplanation,
            Boolean fixVerified) {
    }

    public record HiddenTestResult(
            String input,
            String expected,
            String actual,
            boolean passed,
            boolean edgeCase,
            String description,
            String error) {
    }
}
