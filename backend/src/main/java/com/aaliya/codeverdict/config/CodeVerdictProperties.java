package com.aaliya.codeverdict.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/** Typed view of the "codeverdict.*" settings in application.yml. */
@ConfigurationProperties(prefix = "codeverdict")
public record CodeVerdictProperties(
        String timezone,
        Jwt jwt,
        Cors cors,
        Auth auth,
        Demo demo,
        Ai ai,
        Runner runner,
        Review review,
        Limits limits) {

    public record Jwt(String secret, Duration ttl, boolean cookieSecure) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Auth(boolean registrationEnabled) {
    }

    public record Demo(boolean enabled, String username, String password, boolean readOnly) {
    }

    public record Ai(String apiKey, String model, String baseUrl, Duration timeout, int maxAttempts,
                     Integer thinkingBudget, int cacheSize) {
    }

    public record Runner(String mode, String junitJar, String workDir, boolean keepWorkspaces, String dockerImage,
                         int memoryMb, int cpus, int pidsLimit, int maxOutputKb, Duration compileTimeout,
                         Duration testTimeout, Duration caseTimeout, Duration jobTimeout, int maxCharges,
                         List<Integer> benchSizes, Duration benchLimit, int benchWarmups, int benchRuns,
                         int randomHiddenTests, int differentialSeeds) {
    }

    public record Review(int maxPerHour, int workerThreads, int queueCapacity) {
    }

    public record Limits(int maxCodeChars, int maxStatementChars) {
    }
}
