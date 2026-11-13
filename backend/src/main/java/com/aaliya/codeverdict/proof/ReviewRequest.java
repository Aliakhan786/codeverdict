package com.aaliya.codeverdict.proof;

import com.aaliya.codeverdict.ai.dto.ProblemContext;

/** Input of one review. {@code cachedCaseFile} is null the first time a problem is reviewed. */
public record ReviewRequest(
        ProblemContext problem,
        String userCode,
        OutputComparator.Mode compareMode,
        CaseFile cachedCaseFile) {
}
