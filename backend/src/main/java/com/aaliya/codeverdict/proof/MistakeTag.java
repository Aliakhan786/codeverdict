package com.aaliya.codeverdict.proof;

/** Fixed list of mistake types. Powers the "Top repeated mistakes" chart. */
public enum MistakeTag {
    MISSING_NULL_CHECK("Missing null check"),
    MISSING_EMPTY_CHECK("Missing empty-input check"),
    OFF_BY_ONE("Off-by-one"),
    INT_OVERFLOW("Integer overflow"),
    WRONG_CONDITION("Wrong condition"),
    INFINITE_LOOP("Infinite loop"),
    WRONG_RETURN("Wrong return value"),
    UNHANDLED_NEGATIVE("Unhandled negative numbers"),
    DUPLICATE_HANDLING("Duplicates not handled"),
    INEFFICIENT_ALGORITHM("Inefficient algorithm"),
    OTHER("Other");

    private final String label;

    MistakeTag(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static MistakeTag parse(String s) {
        try {
            return s == null ? OTHER : valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
