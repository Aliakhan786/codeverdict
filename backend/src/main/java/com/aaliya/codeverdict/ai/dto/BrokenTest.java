package com.aaliya.codeverdict.ai.dto;

/** A proof test that did not compile, sent back once for repair. */
public record BrokenTest(String chargeId, String testClassCode, String compilerErrors) {
}
