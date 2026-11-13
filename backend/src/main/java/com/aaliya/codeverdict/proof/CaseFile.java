package com.aaliya.codeverdict.proof;

import com.aaliya.codeverdict.ai.dto.TestInputDraft;

import java.util.List;

/**
 * Everything trusted about one problem, cached in optimal_solutions so later
 * attempts are compared against the SAME reference and the SAME inputs.
 */
public record CaseFile(
        String optimalCode,
        String bruteForceCode,
        String complexity,
        String keyIdea,
        String generatorCode,
        List<TestInputDraft> inputs,
        boolean verified,
        String verificationNote) {
}
