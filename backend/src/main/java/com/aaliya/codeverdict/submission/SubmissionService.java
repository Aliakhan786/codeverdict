package com.aaliya.codeverdict.submission;

import com.aaliya.codeverdict.common.ApiException;
import com.aaliya.codeverdict.config.CodeVerdictProperties;
import com.aaliya.codeverdict.problem.Problem;
import com.aaliya.codeverdict.problem.ProblemService;
import com.aaliya.codeverdict.problem.OptimalSolutionRepository;
import com.aaliya.codeverdict.ai.dto.ProblemContext;
import com.aaliya.codeverdict.proof.CaseFile;
import com.aaliya.codeverdict.proof.ReviewRequest;
import com.aaliya.codeverdict.proof.Stage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SubmissionService {

    private final SubmissionRepository submissions;
    private final ProblemService problems;
    private final OptimalSolutionRepository optimalSolutions;
    private final CodeVerdictProperties props;
    private final Clock clock;

    public SubmissionService(SubmissionRepository submissions, ProblemService problems,
                             OptimalSolutionRepository optimalSolutions, CodeVerdictProperties props, Clock clock) {
        this.submissions = submissions;
        this.problems = problems;
        this.optimalSolutions = optimalSolutions;
        this.props = props;
        this.clock = clock;
    }

    /** Saves the attempt as QUEUED. The caller starts the background job after this transaction commits. */
    @Transactional
    public Submission create(UUID userId, UUID problemId, String code) {
        Problem problem = problems.owned(userId, problemId);
        if (code == null || code.isBlank()) {
            throw ApiException.badRequest("Code is empty");
        }
        if (code.length() > props.limits().maxCodeChars()) {
            throw ApiException.badRequest("Code is longer than " + props.limits().maxCodeChars() + " characters");
        }
        if (submissions.existsByUserIdAndStatusIn(userId, List.of(SubmissionStatus.QUEUED, SubmissionStatus.RUNNING))) {
            throw new ApiException(HttpStatus.CONFLICT, "REVIEW_IN_PROGRESS",
                    "A review is already running. Wait for its verdict first.");
        }
        long lastHour = submissions.countByUserIdAndCreatedAtAfter(userId, Instant.now(clock).minus(Duration.ofHours(1)));
        if (lastHour >= props.review().maxPerHour()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
                    "Hourly review limit reached (" + props.review().maxPerHour() + "). This protects the free AI quota.");
        }
        Submission s = new Submission();
        s.setProblemId(problem.getId());
        s.setUserId(userId);
        s.setAttemptNo(submissions.maxAttemptNo(problem.getId()) + 1);
        s.setCode(code);
        s.setStatus(SubmissionStatus.QUEUED);
        s.setCurrentStage(Stage.QUEUED.name());
        return submissions.save(s);
    }

    /** Marks the submission RUNNING and collects everything the engine needs. */
    @Transactional
    public ReviewRequest startReview(UUID submissionId) {
        Submission s = submissions.findById(submissionId).orElseThrow(() -> ApiException.notFound("Submission"));
        Problem p = problems.owned(s.getUserId(), s.getProblemId());
        s.setStatus(SubmissionStatus.RUNNING);
        s.setCurrentStage(Stage.COMPILING.name());
        CaseFile cached = optimalSolutions.findByProblemId(p.getId())
                .map(o -> new CaseFile(o.getCode(), o.getBruteForceCode(), o.getComplexity(), o.getKeyIdea(),
                        o.getGeneratorCode(), o.getTestInputs() == null ? null : o.getTestInputs().inputs(),
                        o.isVerified(), o.getVerificationNote()))
                .orElse(null);
        ProblemContext ctx = new ProblemContext(p.getTitle(), p.getStatement(), p.getTopic().label(),
                p.getMethodSignature(), p.getSampleInput(), p.getSampleOutput());
        return new ReviewRequest(ctx, s.getCode(), p.getCompareMode(), cached);
    }

    @Transactional
    public void updateStage(UUID submissionId, Stage stage) {
        submissions.updateStage(submissionId, stage.name(), SubmissionStatus.RUNNING);
    }

    @Transactional
    public void markFailed(UUID submissionId, String reason) {
        submissions.findById(submissionId).ifPresent(s -> {
            s.setStatus(SubmissionStatus.FAILED);
            s.setCurrentStage(Stage.FAILED.name());
            s.setFailureReason(reason == null ? "Unknown error" : reason.length() > 1000 ? reason.substring(0, 1000) : reason);
            s.setCompletedAt(Instant.now(clock));
        });
    }

    @Transactional(readOnly = true)
    public ProgressEvent currentProgress(UUID userId, UUID submissionId) {
        Submission s = submissions.findByIdAndUserId(submissionId, userId)
                .orElseThrow(() -> ApiException.notFound("Submission"));
        Stage stage = switch (s.getStatus()) {
            case DONE -> Stage.VERDICT_READY;
            case FAILED -> Stage.FAILED;
            default -> parseStage(s.getCurrentStage());
        };
        return new ProgressEvent(stage.name(), stage.label(),
                s.getStatus() == SubmissionStatus.FAILED ? s.getFailureReason() : null,
                stage.progress(), stage.terminal(), Instant.now(clock));
    }

    private static Stage parseStage(String s) {
        try {
            return s == null ? Stage.QUEUED : Stage.valueOf(s);
        } catch (IllegalArgumentException e) {
            return Stage.QUEUED;
        }
    }
}
