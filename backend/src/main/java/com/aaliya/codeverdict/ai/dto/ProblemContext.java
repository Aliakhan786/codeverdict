package com.aaliya.codeverdict.ai.dto;

/** The problem as the AI sees it. Never contains user data beyond the problem and code. */
public record ProblemContext(
        String title,
        String statement,
        String topic,
        String methodSignature,
        String sampleInput,
        String sampleOutput) {
}
