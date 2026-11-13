package com.aaliya.codeverdict.ai.dto;

import java.util.List;

/**
 * Hidden-test inputs plus a generator method:
 * {@code static Object[] generate(int n, Random r)} used for random tests and benchmarks.
 */
public record TestInputsResponse(List<TestInputDraft> inputs, String generatorCode) {
}
