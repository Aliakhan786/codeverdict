package com.aaliya.codeverdict.runner;

/**
 * Runs already-compiled code. Two implementations (Strategy pattern):
 * {@link LocalProcessRunner} for development and {@link DockerRunner}
 * for production, selected with {@code codeverdict.runner.mode=local|docker}.
 */
public interface CodeRunner {

    RunResult run(Workspace workspace, RunSpec spec);

    /** Short name shown in the health endpoint and logs. */
    String name();
}
