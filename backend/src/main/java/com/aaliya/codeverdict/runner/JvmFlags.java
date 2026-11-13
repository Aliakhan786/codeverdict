package com.aaliya.codeverdict.runner;

import java.util.ArrayList;
import java.util.List;

/** JVM options shared by both runners. */
final class JvmFlags {

    private JvmFlags() {
    }

    static List<String> forSpec(RunSpec spec, int memoryMb) {
        List<String> flags = new ArrayList<>();
        flags.add("-Xmx" + memoryMb + "m");
        flags.add("-Xss8m");
        flags.add("-XX:+UseSerialGC");
        flags.add("-XX:-UsePerfData");
        flags.add("-Dfile.encoding=UTF-8");
        if (spec.fastStart()) {
            // Tests are short: skip the optimising JIT, start faster.
            flags.add("-XX:TieredStopAtLevel=1");
            flags.add("-Xshare:auto");
        }
        return flags;
    }
}
