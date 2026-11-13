package com.aaliya.codeverdict.runner;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Development runner: starts a plain {@code java} process on this machine with
 * a memory cap and a hard timeout. Safety relies on {@link CodeSafetyChecker}
 * running first; use {@link DockerRunner} when strangers can submit code.
 */
public final class LocalProcessRunner implements CodeRunner {

    private final Path javaExecutable;
    private final Path junitJar;
    private final int memoryMb;
    private final int maxOutputBytes;

    public LocalProcessRunner(Path junitJar, int memoryMb, int maxOutputBytes) {
        this.javaExecutable = currentJava();
        this.junitJar = junitJar;
        this.memoryMb = memoryMb;
        this.maxOutputBytes = maxOutputBytes;
    }

    /** Uses the same JDK that runs the backend, so it works without PATH setup (also on Windows). */
    static Path currentJava() {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        return Path.of(System.getProperty("java.home"), "bin", windows ? "java.exe" : "java");
    }

    @Override
    public RunResult run(Workspace workspace, RunSpec spec) {
        List<String> cmd = new ArrayList<>();
        cmd.add(javaExecutable.toString());
        cmd.addAll(JvmFlags.forSpec(spec, memoryMb));
        List<String> cp = new ArrayList<>();
        if (spec.withJUnit()) {
            cp.add(junitJar.toAbsolutePath().toString());
        }
        for (String rel : spec.classpath()) {
            cp.add(workspace.path(rel).toString());
        }
        cmd.add("-cp");
        cmd.add(String.join(File.pathSeparator, cp));
        cmd.add(spec.mainClass());
        cmd.addAll(spec.args());
        return ProcessSupport.execute(cmd, workspace.root(), spec.timeout(), maxOutputBytes,
                Map.of("JAVA_TOOL_OPTIONS", ""), null);
    }

    @Override
    public String name() {
        return "local";
    }
}
