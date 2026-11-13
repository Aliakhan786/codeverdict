package com.aaliya.codeverdict.submission;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class HistoryDtos {

    private HistoryDtos() {
    }

    public record SubmitRequest(@NotNull @Size(min = 1, max = 20000) String code) {
    }

    public record SubmitResponse(UUID submissionId, String status, int attemptNo) {
    }

    public record HistoryFilter(UUID problemId, String topic, String difficulty, Integer minScore, Integer maxScore,
                                LocalDate from, LocalDate to) {
    }

    public record HistoryRow(UUID submissionId, UUID problemId, String problemTitle, String topic, String difficulty,
                             int attemptNo, String status, Integer verdictScore, String grade, int bugsProven,
                             Long durationMs, Instant createdAt) {
    }

    public record Page<T>(List<T> items, int page, int size, long total, int totalPages) {
    }

    public record AttemptSide(UUID submissionId, int attemptNo, String code, Integer verdictScore, String grade,
                              Integer correctness, Integer edgeCases, Integer performance, Integer quality,
                              int bugsProven, Instant createdAt) {
    }

    public record CompareResponse(UUID problemId, String problemTitle, AttemptSide a, AttemptSide b, Integer scoreDelta) {
    }
}
