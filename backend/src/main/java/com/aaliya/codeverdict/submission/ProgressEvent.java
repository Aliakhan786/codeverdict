package com.aaliya.codeverdict.submission;

import java.time.Instant;

/** One SSE message: {"stage":"RUNNING_TESTS","label":"Running the evidence","progress":52,...} */
public record ProgressEvent(String stage, String label, String detail, int progress, boolean terminal, Instant at) {
}
