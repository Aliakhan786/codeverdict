package com.aaliya.codeverdict.problem;

import com.aaliya.codeverdict.proof.OutputComparator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request/response shapes for the problems API. */
public final class ProblemDtos {

    private ProblemDtos() {
    }

    public record ProblemRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 10000) String statement,
            @NotNull Topic topic,
            @NotNull Difficulty difficulty,
            @NotBlank @Size(max = 300) String methodSignature,
            @Size(max = 5000) String sampleInput,
            @Size(max = 5000) String sampleOutput,
            OutputComparator.Mode compareMode) {
    }

    public record ProblemResponse(
            UUID id,
            String title,
            String statement,
            Topic topic,
            String topicLabel,
            Difficulty difficulty,
            String methodSignature,
            String sampleInput,
            String sampleOutput,
            OutputComparator.Mode compareMode,
            String starterCode,
            int attempts,
            Integer bestScore,
            String bestGrade,
            Instant lastAttemptAt,
            Instant createdAt) {
    }

    public record RunRequest(@NotNull @Size(max = 20000) String code) {
    }

    public record CompileErrorDto(long line, long column, String message) {
    }

    public record RunResponse(
            boolean compiled,
            List<CompileErrorDto> compileErrors,
            String output,
            String expected,
            Boolean passed,
            String error,
            String stdout,
            long timeMs) {
    }

    public record OptimalResponse(String code, String complexity, String keyIdea, boolean verified, String verificationNote) {
    }

    public record TopicOption(Topic value, String label) {
    }
}
