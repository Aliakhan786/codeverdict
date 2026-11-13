package com.aaliya.codeverdict.problem;

import com.aaliya.codeverdict.ai.dto.TestInputDraft;

import java.util.List;

/** JSONB wrapper for the cached hidden-test inputs of a problem. */
public record TestInputSet(List<TestInputDraft> inputs) {
}
