package com.aaliya.codeverdict.proof;

import com.aaliya.codeverdict.ai.dto.ProblemContext;
import com.aaliya.codeverdict.runner.JavaSourceCompiler;
import com.aaliya.codeverdict.runner.LocalProcessRunner;
import com.aaliya.codeverdict.support.FakeAiClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the REAL Proof Engine (real javac, real JVM processes, real JUnit) with a scripted AI.
 * This is the test that shows the double-check rule works end to end.
 */
class ProofEngineIntegrationTest {

    private static ProofEngine engine;

    private static final ProblemContext PROBLEM = new ProblemContext("Maximum Subarray",
            "Return the largest sum of a contiguous non-empty subarray. 1 <= n <= 10^5.", "DP",
            FakeAiClient.SIGNATURE, "new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}", "6");

    @BeforeAll
    static void setUp() {
        Path jar = Path.of(System.getProperty("junit.console.jar", "target/runner-lib/junit-platform-console-standalone.jar"));
        assumeTrue(Files.exists(jar), "JUnit console jar missing - run 'mvn generate-resources' first");
        EngineSettings settings = new EngineSettings(Path.of(System.getProperty("java.io.tmpdir"), "cv-test-jobs"), false,
                Duration.ofSeconds(5), Duration.ofSeconds(180), 8, 2000, List.of(1000, 10000, 100000), 2000, 3, 5, 10, 50);
        engine = new ProofEngine(new FakeAiClient(), new LocalProcessRunner(jar, 256, 65536),
                new JavaSourceCompiler(jar, Duration.ofSeconds(10)), settings);
    }

    @Test
    void buggyCodeGetsProvenChargeDismissedChargesAndVerifiedFix() {
        ReviewOutcome o = engine.review(new ReviewRequest(PROBLEM, FakeAiClient.BUGGY_USER_CODE,
                OutputComparator.Mode.EXACT, null), ProgressListener.NONE);

        assertEquals(ReviewOutcome.Kind.FULL, o.kind());
        assertTrue(o.referenceVerified(), "reference must be verified against brute force: " + o.referenceNote());
        Map<String, ReviewOutcome.ChargeResult> byTitle = o.charges().stream()
                .collect(Collectors.toMap(ReviewOutcome.ChargeResult::title, Function.identity()));

        // true bug: proven, then the AI fix is re-tested and acquitted
        var realBug = byTitle.get("Wrong answer when all numbers are negative");
        assertEquals(ChargeStatus.ACQUITTED, realBug.status());
        assertTrue(realBug.fixVerified());
        assertTrue(realBug.proofOutput().contains("expected: <-1> but was: <0>"));

        // AI guessed wrong: the test passes on the user's code
        assertEquals(ChargeStatus.DISMISSED, byTitle.get("Crash on a single element").status());

        // AI wrote a wrong test (expects 7 for [1,2,3]): fails on the reference too -> bad evidence
        var bad = byTitle.get("Sum is off by one");
        assertEquals(ChargeStatus.DISMISSED, bad.status());
        assertTrue(bad.badEvidence());

        // performance charge is judged by measurement: O(n^2) is too slow at n = 100,000
        assertEquals(ChargeStatus.PROVEN, byTitle.get("Quadratic nested loops").status());

        // invalid AI inputs (empty array, broken syntax) were dropped; random inputs were added
        assertEquals(5 + 10, o.hiddenTests().size());
        assertFalse(o.hiddenTests().stream().allMatch(ReviewOutcome.HiddenTestResult::passed));
        assertNotNull(o.caseFile());
        assertEquals(5, o.caseFile().inputs().size());
        assertTrue(o.scores().total() < 70);
    }

    @Test
    void correctCodeIsCleanAndFast() {
        ReviewOutcome o = engine.review(new ReviewRequest(PROBLEM, FakeAiClient.CORRECT_USER_CODE,
                OutputComparator.Mode.EXACT, null), ProgressListener.NONE);
        assertTrue(o.hiddenTests().stream().allMatch(ReviewOutcome.HiddenTestResult::passed));
        assertTrue(o.charges().stream().noneMatch(c -> c.status().isGuilty()));
        assertEquals(40, o.scores().correctness());
        assertEquals(30, o.scores().performance());
    }

    @Test
    void compileErrorStopsEarlyWithLineNumber() {
        ReviewOutcome o = engine.review(new ReviewRequest(PROBLEM, "class Solution {\n  public int maxSubArray(int[] nums) {\n    return 0\n  }\n}",
                OutputComparator.Mode.EXACT, null), ProgressListener.NONE);
        assertEquals(ReviewOutcome.Kind.COMPILE_ERROR, o.kind());
        assertEquals(3, o.compileErrors().get(0).line());
        assertEquals(0, o.aiCalls());
    }

    @Test
    void infiniteLoopIsStoppedAndProven() {
        String loop = """
                class Solution {
                    public int maxSubArray(int[] nums) {
                        int best = nums[0], cur = 0, i = 0;
                        while (i < nums.length) {
                            cur = Math.max(nums[i], cur + nums[i]);
                            best = Math.max(best, cur);
                            if (nums[i] < 0) continue;
                            i++;
                        }
                        return best;
                    }
                }
                """;
        ReviewOutcome o = engine.review(new ReviewRequest(PROBLEM, loop, OutputComparator.Mode.EXACT, null),
                ProgressListener.NONE);
        var c = o.charges().stream().filter(x -> x.title().startsWith("Wrong answer")).findFirst().orElseThrow();
        assertEquals(ChargeStatus.PROVEN, c.status());
        assertTrue(c.proofOutput().contains("Time limit"));
    }

    @Test
    void dangerousCodeIsBlockedBeforeRunning() {
        ReviewOutcome o = engine.review(new ReviewRequest(PROBLEM,
                "class Solution { public int maxSubArray(int[] nums) { System.exit(0); return 0; } }",
                OutputComparator.Mode.EXACT, null), ProgressListener.NONE);
        assertEquals(ReviewOutcome.Kind.COMPILE_ERROR, o.kind());
        assertTrue(o.compileErrors().get(0).message().startsWith("Blocked for safety"));
    }

    @Test
    void sampleRunReportsOutputAndPass() {
        var r = engine.runSample(FakeAiClient.SIGNATURE, "new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}", "6",
                OutputComparator.Mode.EXACT, FakeAiClient.CORRECT_USER_CODE);
        assertTrue(r.compiled());
        assertEquals("6", r.output());
        assertEquals(Boolean.TRUE, r.passed());
    }
}
