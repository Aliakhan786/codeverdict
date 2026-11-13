package com.aaliya.codeverdict.submission;

import com.aaliya.codeverdict.proof.ProofEngine;
import com.aaliya.codeverdict.proof.ReviewOutcome;
import com.aaliya.codeverdict.proof.ReviewRequest;
import com.aaliya.codeverdict.proof.Stage;
import com.aaliya.codeverdict.review.ReviewPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Runs one review in the background: engine -> save -> VERDICT_READY.
 * The pool is small and bounded, so a burst of submissions queues instead of overloading the machine.
 */
@Service
public class ReviewJobService {

    private static final Logger log = LoggerFactory.getLogger(ReviewJobService.class);

    private final ThreadPoolTaskExecutor executor;
    private final SubmissionService submissions;
    private final ProofEngine engine;
    private final ReviewPersistenceService persistence;
    private final SubmissionEventBus events;

    public ReviewJobService(@Qualifier("reviewExecutor") ThreadPoolTaskExecutor executor, SubmissionService submissions,
                            ProofEngine engine, ReviewPersistenceService persistence, SubmissionEventBus events) {
        this.executor = executor;
        this.submissions = submissions;
        this.engine = engine;
        this.persistence = persistence;
        this.events = events;
    }

    public void start(UUID submissionId) {
        events.publish(submissionId, Stage.QUEUED, null);
        try {
            executor.execute(() -> process(submissionId));
        } catch (TaskRejectedException e) {
            fail(submissionId, "The server is busy. Please try again in a minute.");
        }
    }

    void process(UUID submissionId) {
        long start = System.currentTimeMillis();
        try {
            ReviewRequest request = submissions.startReview(submissionId);
            ReviewOutcome outcome = engine.review(request, (stage, detail) -> {
                submissions.updateStage(submissionId, stage);
                events.publish(submissionId, stage, detail);
            });
            persistence.save(submissionId, outcome);
            events.publish(submissionId, Stage.VERDICT_READY, outcome.summary());
            log.info("Review {} done in {} ms (score {}, {} AI calls, {} tokens)", submissionId,
                    System.currentTimeMillis() - start, outcome.scores().total(), outcome.aiCalls(), outcome.aiTokens());
        } catch (Exception e) {
            log.error("Review {} failed", submissionId, e);
            fail(submissionId, "Review failed: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }

    private void fail(UUID submissionId, String reason) {
        try {
            submissions.markFailed(submissionId, reason);
        } finally {
            events.publish(submissionId, Stage.FAILED, reason);
        }
    }
}
