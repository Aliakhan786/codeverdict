package com.aaliya.codeverdict.runner;

import java.time.Duration;
import java.util.List;

/**
 * What to run inside a workspace.
 *
 * @param classpath   class folders relative to the workspace (e.g. "classes/user")
 * @param mainClass   fully qualified main class
 * @param args        program arguments
 * @param timeout     hard wall-clock limit; the process is killed after it
 * @param withJUnit   add the JUnit console launcher jar to the classpath
 * @param fastStart   tune the JVM for quick start (tests) instead of peak speed (benchmarks)
 */
public record RunSpec(
        List<String> classpath,
        String mainClass,
        List<String> args,
        Duration timeout,
        boolean withJUnit,
        boolean fastStart) {

    public static RunSpec program(List<String> classpath, String mainClass, List<String> args, Duration timeout) {
        return new RunSpec(classpath, mainClass, args, timeout, false, true);
    }

    public static RunSpec benchmark(List<String> classpath, String mainClass, List<String> args, Duration timeout) {
        return new RunSpec(classpath, mainClass, args, timeout, false, false);
    }

    public static RunSpec junit(List<String> classpath, List<String> launcherArgs, Duration timeout) {
        return new RunSpec(classpath, "org.junit.platform.console.ConsoleLauncher", launcherArgs, timeout, true, true);
    }
}
