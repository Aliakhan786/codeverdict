package com.aaliya.codeverdict.ai.dto;

/** A charge sent to the AI so it can write a proof test (or a fix) for it. */
public record ChargeForTest(String chargeId, String category, int lineStart, int lineEnd,
                            String title, String explanation, String failingOutput) {
}
