package com.aaliya.codeverdict.proof;

@FunctionalInterface
public interface ProgressListener {
    void onStage(Stage stage, String detail);

    ProgressListener NONE = (s, d) -> { };
}
