package com.aaliya.codeverdict.review;

import com.aaliya.codeverdict.ai.dto.QualityResponse;
import com.aaliya.codeverdict.gamification.AchievementDto;
import com.aaliya.codeverdict.problem.Difficulty;
import com.aaliya.codeverdict.problem.Topic;
import com.aaliya.codeverdict.proof.ChargeCategory;
import com.aaliya.codeverdict.proof.ChargeStatus;
import com.aaliya.codeverdict.proof.MistakeTag;
import com.aaliya.codeverdict.proof.Severity;
import com.aaliya.codeverdict.submission.SubmissionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** JSON shapes of the Verdict page. */
public final class VerdictDtos {

    private VerdictDtos() {
    }

    public record VerdictResponse(
            UUID submissionId,
            UUID problemId,
            String problemTitle,
            Topic topic,
            String topicLabel,
            Difficulty difficulty,
            String methodSignature,
            int attemptNo,
            String code,
            SubmissionStatus status,
            String stage,
            String failureReason,
            Instant createdAt,
            Instant completedAt,
            PreviousAttempt previousAttempt,
            ReviewDto review) {
    }

    public record PreviousAttempt(UUID submissionId, int attemptNo, int verdictScore) {
    }

    public record ReviewDto(
            String kind,
            int verdictScore,
            String grade,
            String summary,
            ScoresDto scores,
            List<ChargeDto> charges,
            HiddenTestsDto hiddenTests,
            PerformanceDto performance,
            QualityResponse quality,
            List<ReviewDetails.CompileErrorItem> compileErrors,
            List<String> warnings,
            boolean aiAvailable,
            boolean referenceVerified,
            String referenceNote,
            String aiModel,
            int aiTokensUsed,
            int aiCalls,
            long durationMs,
            int xpEarned,
            String newLevel,
            boolean personalBest,
            List<AchievementDto> newBadges) {
    }

    public record ScoresDto(Integer correctness, Integer edgeCases, Integer performance, Integer quality, boolean partial) {
    }

    public record ChargeDto(
            UUID id,
            ChargeCategory category,
            Severity severity,
            int lineStart,
            int lineEnd,
            String title,
            String explanation,
            ChargeStatus status,
            String statusReason,
            boolean badEvidence,
            String proofTestCode,
            String proofOutput,
            Integer evidenceLine,
            String suggestedFixCode,
            String fixExplanation,
            Boolean fixVerified,
            MistakeTag mistakeTag,
            String mistakeLabel) {
    }

    public record HiddenTestsDto(int passed, int total, List<HiddenTestDto> cases) {
    }

    public record HiddenTestDto(String input, String expected, String actual, boolean passed, boolean edgeCase,
                                String description, String error) {
    }

    public record PointDto(int n, Double ms, String status) {
    }

    public record PerformanceDto(
            String claimedComplexity,
            String measuredComplexity,
            boolean complexityMeasured,
            Double slope,
            String optimalComplexity,
            String keyIdea,
            Integer comparedN,
            Double ratio,
            List<PointDto> user,
            List<PointDto> optimal) {
    }
}
