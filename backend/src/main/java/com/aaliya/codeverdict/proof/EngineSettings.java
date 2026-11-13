package com.aaliya.codeverdict.proof;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/** Limits and sizes used by the Proof Engine (bound from application.yml). */
public record EngineSettings(
        Path workDir,
        boolean keepWorkspaces,
        Duration testTimeout,
        Duration jobTimeout,
        int maxCharges,
        long caseTimeoutMs,
        List<Integer> benchSizes,
        long benchLimitMs,
        int benchWarmups,
        int benchRuns,
        int randomHiddenTests,
        int differentialSeeds) {
}
