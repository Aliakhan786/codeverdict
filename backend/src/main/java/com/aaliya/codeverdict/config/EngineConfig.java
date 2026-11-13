package com.aaliya.codeverdict.config;

import com.aaliya.codeverdict.ai.AiClient;
import com.aaliya.codeverdict.ai.CachingAiClient;
import com.aaliya.codeverdict.ai.GeminiClient;
import com.aaliya.codeverdict.ai.PromptLoader;
import com.aaliya.codeverdict.ai.UnavailableAiClient;
import com.aaliya.codeverdict.proof.EngineSettings;
import com.aaliya.codeverdict.proof.ProofEngine;
import com.aaliya.codeverdict.runner.CodeRunner;
import com.aaliya.codeverdict.runner.DockerRunner;
import com.aaliya.codeverdict.runner.JavaSourceCompiler;
import com.aaliya.codeverdict.runner.LocalProcessRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Wires the framework-free Proof Engine into Spring.
 * The runner is chosen by codeverdict.runner.mode (Strategy pattern):
 * "local" for development, "docker" for a public deployment.
 */
@Configuration
public class EngineConfig {

    private static final Logger log = LoggerFactory.getLogger(EngineConfig.class);

    @Bean
    public Path junitConsoleJar(CodeVerdictProperties props) {
        String configured = props.runner().junitJar();
        List<Path> candidates = configured != null && !configured.isBlank()
                ? List.of(Path.of(configured))
                : List.of(Path.of("target/runner-lib/junit-platform-console-standalone.jar"),
                          Path.of("backend/target/runner-lib/junit-platform-console-standalone.jar"),
                          Path.of("runner-lib/junit-platform-console-standalone.jar"));
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                log.info("Proof Engine uses JUnit launcher at {}", p.toAbsolutePath());
                return p.toAbsolutePath();
            }
        }
        throw new IllegalStateException("JUnit console launcher jar not found (looked at " + candidates + "). "
                + "Run 'mvn generate-resources' (or 'mvn spring-boot:run') once in backend/, or set JUNIT_JAR.");
    }

    @Bean
    public CodeRunner codeRunner(CodeVerdictProperties props, Path junitConsoleJar) {
        CodeVerdictProperties.Runner r = props.runner();
        int maxOutput = r.maxOutputKb() * 1024;
        if ("docker".equalsIgnoreCase(r.mode())) {
            log.info("Code runner: Docker sandbox ({}; {} MB; {} CPU; no network)", r.dockerImage(), r.memoryMb(), r.cpus());
            return new DockerRunner(r.dockerImage(), junitConsoleJar, r.memoryMb(), r.cpus(), r.pidsLimit(), maxOutput);
        }
        log.info("Code runner: local processes ({} MB heap). Use RUNNER_MODE=docker for untrusted users.", r.memoryMb());
        return new LocalProcessRunner(junitConsoleJar, r.memoryMb(), maxOutput);
    }

    @Bean
    public JavaSourceCompiler javaSourceCompiler(CodeVerdictProperties props, Path junitConsoleJar) {
        return new JavaSourceCompiler(junitConsoleJar, props.runner().compileTimeout());
    }

    @Bean
    public AiClient aiClient(CodeVerdictProperties props) {
        CodeVerdictProperties.Ai ai = props.ai();
        if (ai.apiKey() == null || ai.apiKey().isBlank()) {
            log.warn("GEMINI_API_KEY is not set: reviews will be partial (compile, hidden tests from cache, benchmark).");
            return new UnavailableAiClient("No Gemini API key configured");
        }
        Integer thinking = ai.thinkingBudget() != null && ai.thinkingBudget() >= 0 ? ai.thinkingBudget() : null;
        GeminiClient gemini = new GeminiClient(new PromptLoader(), ai.apiKey(), ai.model(), ai.baseUrl(),
                ai.timeout(), ai.maxAttempts(), thinking);
        log.info("AI: Gemini model {}", ai.model());
        return new CachingAiClient(gemini, ai.cacheSize());
    }

    @Bean
    public EngineSettings engineSettings(CodeVerdictProperties props) {
        CodeVerdictProperties.Runner r = props.runner();
        Path workDir = r.workDir() == null || r.workDir().isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "codeverdict-jobs")
                : Path.of(r.workDir());
        return new EngineSettings(workDir, r.keepWorkspaces(), r.testTimeout(), r.jobTimeout(), r.maxCharges(),
                r.caseTimeout().toMillis(), r.benchSizes(), r.benchLimit().toMillis(), r.benchWarmups(), r.benchRuns(),
                r.randomHiddenTests(), r.differentialSeeds());
    }

    @Bean
    public ProofEngine proofEngine(AiClient aiClient, CodeRunner codeRunner, JavaSourceCompiler compiler,
                                   EngineSettings settings) {
        return new ProofEngine(aiClient, codeRunner, compiler, settings);
    }
}
