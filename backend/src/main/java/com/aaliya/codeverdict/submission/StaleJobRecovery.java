package com.aaliya.codeverdict.submission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Jobs live in memory; if the server restarted mid-review, mark those submissions FAILED so the user can retry. */
@Component
public class StaleJobRecovery implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaleJobRecovery.class);

    private final SubmissionRepository submissions;
    private final SubmissionService service;

    public StaleJobRecovery(SubmissionRepository submissions, SubmissionService service) {
        this.submissions = submissions;
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Submission> stale = submissions.findByStatusIn(List.of(SubmissionStatus.QUEUED, SubmissionStatus.RUNNING));
        for (Submission s : stale) {
            service.markFailed(s.getId(), "The server restarted during this review. Please submit again.");
        }
        if (!stale.isEmpty()) {
            log.info("Marked {} interrupted review(s) as failed", stale.size());
        }
    }
}
