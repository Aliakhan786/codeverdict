package com.aaliya.codeverdict.problem;

public enum Topic {
    ARRAYS("Arrays"),
    STRINGS("Strings"),
    HASHMAP("HashMap"),
    TWO_POINTERS("Two Pointers"),
    SLIDING_WINDOW("Sliding Window"),
    RECURSION("Recursion"),
    DP("DP"),
    TREES("Trees"),
    GRAPHS("Graphs"),
    SORTING("Sorting");

    private final String label;

    Topic(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
