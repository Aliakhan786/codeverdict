package com.aaliya.codeverdict.dashboard;

import com.aaliya.codeverdict.auth.AuthUser;
import com.aaliya.codeverdict.submission.HistoryDtos.HistoryRow;
import com.aaliya.codeverdict.submission.HistoryQueries;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/export")
@Tag(name = "Export")
public class ExportController {

    private final HistoryQueries history;

    public ExportController(HistoryQueries history) {
        this.history = history;
    }

    @GetMapping(value = "/history.csv", produces = "text/csv")
    @Operation(summary = "Download the full history as CSV")
    public ResponseEntity<byte[]> historyCsv(@AuthenticationPrincipal AuthUser me) {
        StringBuilder sb = new StringBuilder(
                "submitted_at,problem,topic,difficulty,attempt,status,score,grade,bugs_proven,review_ms,submission_id\n");
        for (HistoryRow r : history.all(me.id())) {
            sb.append(r.createdAt()).append(',')
                    .append(csv(r.problemTitle())).append(',')
                    .append(r.topic()).append(',')
                    .append(r.difficulty()).append(',')
                    .append(r.attemptNo()).append(',')
                    .append(r.status()).append(',')
                    .append(r.verdictScore() == null ? "" : r.verdictScore()).append(',')
                    .append(r.grade() == null ? "" : r.grade()).append(',')
                    .append(r.bugsProven()).append(',')
                    .append(r.durationMs() == null ? "" : r.durationMs()).append(',')
                    .append(r.submissionId()).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"codeverdict-history.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Quotes a value and neutralises spreadsheet formulas (=, +, -, @ at the start). */
    static String csv(String s) {
        if (s == null) {
            return "";
        }
        String v = s;
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
