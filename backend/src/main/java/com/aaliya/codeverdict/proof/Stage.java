package com.aaliya.codeverdict.proof;

/** Live progress stages streamed to the Verdict page over Server-Sent Events. */
public enum Stage {
    QUEUED(0, "Waiting for a free courtroom"),
    COMPILING(8, "Compiling your code"),
    READING_CODE(20, "Reading the code for suspected bugs"),
    GATHERING_EVIDENCE(38, "Writing a proof test for every charge"),
    RUNNING_TESTS(52, "Running the evidence"),
    HIDDEN_TESTS(64, "Running hidden tests"),
    VERIFYING_FIXES(76, "Verifying suggested fixes"),
    BENCHMARKING(88, "Timing your code against the optimal solution"),
    SCORING(96, "Deliberating the verdict"),
    VERDICT_READY(100, "Verdict ready"),
    FAILED(100, "Review failed");

    private final int progress;
    private final String label;

    Stage(int progress, String label) {
        this.progress = progress;
        this.label = label;
    }

    public int progress() {
        return progress;
    }

    public String label() {
        return label;
    }

    public boolean terminal() {
        return this == VERDICT_READY || this == FAILED;
    }
}
