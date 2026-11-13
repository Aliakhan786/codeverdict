package com.aaliya.codeverdict.ai.dto;

/** One hidden-test input written as a Java argument list, e.g. "new int[]{1,2}, 3". */
public record TestInputDraft(String args, boolean edgeCase, String description) {
}
