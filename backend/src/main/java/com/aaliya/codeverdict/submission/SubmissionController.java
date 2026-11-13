package com.aaliya.codeverdict.submission;

import com.aaliya.codeverdict.auth.AuthUser;
import com.aaliya.codeverdict.common.ApiException;
import com.aaliya.codeverdict.problem.ProblemService;
import com.aaliya.codeverdict.review.VerdictDtos.VerdictResponse;
import com.aaliya.codeverdict.review.VerdictService;
import com.aaliya.codeverdict.submission.HistoryDtos.AttemptSide;
import com.aaliya.codeverdict.submission.HistoryDtos.CompareResponse;
import com.aaliya.codeverdict.submission.HistoryDtos.HistoryFilter;
import com.aaliya.codeverdict.submission.HistoryDtos.HistoryRow;
import com.aaliya.codeverdict.submission.HistoryDtos.Page;
import com.aaliya.codeverdict.submission.HistoryDtos.SubmitRequest;
import com.aaliya.codeverdict.submission.HistoryDtos.SubmitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Submissions")
public class SubmissionController {

    private final SubmissionService submissions;
    private final ReviewJobService jobs;
    private final SubmissionEventBus events;
    private final VerdictService verdicts;
    private final HistoryQueries history;
    private final ProblemService problems;

    public SubmissionController(SubmissionService submissions, ReviewJobService jobs, SubmissionEventBus events,
                                VerdictService verdicts, HistoryQueries history, ProblemService problems) {
        this.submissions = submissions;
        this.jobs = jobs;
        this.events = events;
        this.verdicts = verdicts;
        this.history = history;
        this.problems = problems;
    }

    @PostMapping("/problems/{problemId}/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Submit code for a full verdict. Returns at once; follow progress on /submissions/{id}/events")
    public SubmitResponse submit(@AuthenticationPrincipal AuthUser me, @PathVariable UUID problemId,
                                 @Valid @RequestBody SubmitRequest req) {
        Submission s = submissions.create(me.id(), problemId, req.code()); // committed here
        jobs.start(s.getId());                                               // then the background job starts
        return new SubmitResponse(s.getId(), s.getStatus().name(), s.getAttemptNo());
    }

    @GetMapping(path = "/submissions/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Server-Sent Events stream of review stages (event name: progress)")
    public SseEmitter events(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        ProgressEvent current = submissions.currentProgress(me.id(), id);
        return events.subscribe(id, current);
    }

    @GetMapping("/submissions/{id}")
    @Operation(summary = "Submission + full verdict (Verdict page data)")
    public VerdictResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        return verdicts.get(me.id(), id);
    }

    @GetMapping("/submissions")
    @Operation(summary = "History (paged, newest first)")
    public Page<HistoryRow> list(@AuthenticationPrincipal AuthUser me,
                                 @RequestParam(required = false) UUID problemId,
                                 @RequestParam(required = false) String topic,
                                 @RequestParam(required = false) String difficulty,
                                 @RequestParam(required = false) Integer minScore,
                                 @RequestParam(required = false) Integer maxScore,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.max(1, Math.min(100, size));
        return history.page(me.id(), new HistoryFilter(problemId, topic, difficulty, minScore, maxScore, from, to),
                Math.max(0, page), safeSize);
    }

    @GetMapping("/submissions/compare")
    @Operation(summary = "Two attempts side by side")
    public CompareResponse compare(@AuthenticationPrincipal AuthUser me, @RequestParam UUID a, @RequestParam UUID b) {
        AttemptSide left = history.attempt(me.id(), a).orElseThrow(() -> ApiException.notFound("Attempt a"));
        AttemptSide right = history.attempt(me.id(), b).orElseThrow(() -> ApiException.notFound("Attempt b"));
        UUID problemId = history.problemOf(a).orElseThrow(() -> ApiException.notFound("Attempt a"));
        String title = problems.get(me.id(), problemId).title();
        Integer delta = left.verdictScore() != null && right.verdictScore() != null
                ? right.verdictScore() - left.verdictScore() : null;
        return new CompareResponse(problemId, title, left, right, delta);
    }
}
