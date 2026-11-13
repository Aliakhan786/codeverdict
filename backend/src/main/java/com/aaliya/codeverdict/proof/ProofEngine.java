package com.aaliya.codeverdict.proof;

import com.aaliya.codeverdict.ai.AiClient;
import com.aaliya.codeverdict.ai.AiResponse;
import com.aaliya.codeverdict.ai.AiUnavailableException;
import com.aaliya.codeverdict.ai.PromptLoader;
import com.aaliya.codeverdict.ai.dto.BrokenTest;
import com.aaliya.codeverdict.ai.dto.ChargeDraft;
import com.aaliya.codeverdict.ai.dto.ChargeForTest;
import com.aaliya.codeverdict.ai.dto.ChargesResponse;
import com.aaliya.codeverdict.ai.dto.FixDraft;
import com.aaliya.codeverdict.ai.dto.FixesResponse;
import com.aaliya.codeverdict.ai.dto.ProblemContext;
import com.aaliya.codeverdict.ai.dto.ProofTestDraft;
import com.aaliya.codeverdict.ai.dto.ProofTestsResponse;
import com.aaliya.codeverdict.ai.dto.QualityResponse;
import com.aaliya.codeverdict.ai.dto.ReferenceDraft;
import com.aaliya.codeverdict.ai.dto.TestInputDraft;
import com.aaliya.codeverdict.ai.dto.TestInputsResponse;
import com.aaliya.codeverdict.benchmark.BenchPoint;
import com.aaliya.codeverdict.benchmark.ComplexityEstimator;
import com.aaliya.codeverdict.benchmark.PerformanceJudge;
import com.aaliya.codeverdict.proof.HarnessOutput.CaseResult;
import com.aaliya.codeverdict.proof.ReviewOutcome.ChargeResult;
import com.aaliya.codeverdict.proof.ReviewOutcome.HiddenTestResult;
import com.aaliya.codeverdict.review.ScoringCalculator;
import com.aaliya.codeverdict.runner.CodeRunner;
import com.aaliya.codeverdict.runner.CodeSafetyChecker;
import com.aaliya.codeverdict.runner.CodeSafetyChecker.Profile;
import com.aaliya.codeverdict.runner.CodeSafetyChecker.Violation;
import com.aaliya.codeverdict.runner.JavaSourceCompiler;
import com.aaliya.codeverdict.runner.JavaSourceCompiler.CompileError;
import com.aaliya.codeverdict.runner.JavaSourceCompiler.CompileResult;
import com.aaliya.codeverdict.runner.RunResult;
import com.aaliya.codeverdict.runner.RunSpec;
import com.aaliya.codeverdict.runner.Workspace;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Proof Engine: turns AI suspicions into proven facts.
 *
 * <ol>
 *   <li>Compile the user's code (safety check first). Compile error: stop, no AI cost.</li>
 *   <li>Get a trusted reference: AI optimal + brute-force solutions that must agree with each
 *       other (differential testing) and with the sample output. Cached per problem.</li>
 *   <li>AI lists charges (suspected bugs) with exact line numbers.</li>
 *   <li>AI writes one JUnit test per BUG / EDGE_CASE charge.</li>
 *   <li>Double-check rule: GUILTY only if the test fails on the user's code AND passes on the reference.</li>
 *   <li>Hidden tests: AI writes inputs only; expected outputs come from running the reference.</li>
 *   <li>For each proven charge, the AI's fix is compiled and re-tested: ACQUITTED if it passes.</li>
 *   <li>Benchmark user vs reference at 3 input sizes; estimate complexity from the log-log slope.</li>
 *   <li>Score.</li>
 * </ol>
 *
 * This class has no Spring dependency, so it can be unit-tested and reasoned about on its own.
 */
public final class ProofEngine {

    private static final Logger LOG = Logger.getLogger(ProofEngine.class.getName());
    private static final Pattern CLASS_DECL = Pattern.compile("(public\\s+|final\\s+|abstract\\s+)*class\\s+([A-Za-z_$][\\w$]*)");
    private static final Pattern PACKAGE_DECL = Pattern.compile("(?m)^\\s*package\\s+[\\w.]+\\s*;\\s*$");
    private static final int MAX_AI_INPUTS = 20;

    private final AiClient ai;
    private final CodeRunner runner;
    private final JavaSourceCompiler compiler;
    private final CodeSafetyChecker safety;
    private final HarnessFactory harness;
    private final JUnitReportParser reports;
    private final VerdictClassifier classifier;
    private final ComplexityEstimator complexity;
    private final PerformanceJudge performance;
    private final ScoringCalculator scoring;
    private final EngineSettings settings;
    private final ExecutorService aiPool = Executors.newVirtualThreadPerTaskExecutor();

    public ProofEngine(AiClient ai, CodeRunner runner, JavaSourceCompiler compiler, EngineSettings settings) {
        this.ai = ai;
        this.runner = runner;
        this.compiler = compiler;
        this.settings = settings;
        this.safety = new CodeSafetyChecker();
        this.harness = new HarnessFactory();
        this.reports = new JUnitReportParser();
        this.classifier = new VerdictClassifier();
        this.complexity = new ComplexityEstimator();
        this.performance = new PerformanceJudge();
        this.scoring = new ScoringCalculator();
    }

    public ReviewOutcome review(ReviewRequest request, ProgressListener listener) {
        try (Workspace ws = Workspace.create(settings.workDir(), settings.keepWorkspaces())) {
            return new Job(request, listener, ws).run();
        }
    }

    // =====================================================================================
    // Quick "Run" button: compile + sample input only, no AI.
    // =====================================================================================

    public record SampleRun(boolean compiled, List<CompileError> compileErrors, String output, String expected,
                            Boolean passed, String error, String stdout, long timeMs) {
    }

    public SampleRun runSample(String methodSignature, String sampleInput, String sampleOutput,
                               OutputComparator.Mode mode, String code) {
        try (Workspace ws = Workspace.create(settings.workDir(), settings.keepWorkspaces())) {
            MethodSignature sig = MethodSignature.parse(methodSignature);
            List<CompileError> blocked = toErrors(safety.check(code, Profile.SOLUTION));
            if (!blocked.isEmpty()) {
                return new SampleRun(false, blocked, null, sampleOutput, null, null, null, 0);
            }
            CompileResult userCompile = compileSolution(ws, "user", normalizeSolution(code), sig);
            if (!userCompile.success()) {
                return new SampleRun(false, explainCompileErrors(userCompile, sig), null, sampleOutput, null, null, null, 0);
            }
            if (sampleInput == null || sampleInput.isBlank()) {
                return new SampleRun(true, List.of(), null, sampleOutput, null,
                        "This problem has no sample input. Add one in the problem settings.", null, 0);
            }
            ws.write("src/harness/CvSupport.java", harness.support());
            compiler.compile(ws, List.of("src/harness/CvSupport.java"), "classes/harness", List.of(), false);
            HarnessFactory.CaseRunnerSource src = harness.caseRunner(List.of(sampleInput), false);
            ws.write("src/cases-sample/CaseRunner.java", src.source());
            CompileResult cr = compiler.compile(ws, List.of("src/cases-sample/CaseRunner.java"), "classes/cases-sample",
                    List.of("classes/harness", "classes/user"), false);
            if (!cr.success()) {
                return new SampleRun(true, List.of(), null, sampleOutput, null,
                        "The sample input is not a valid Java argument list: " + cr.firstMessages(1), null, 0);
            }
            RunResult rr = runner.run(ws, RunSpec.program(
                    List.of("classes/harness", "classes/cases-sample", "classes/user"), "CaseRunner",
                    List.of(String.valueOf(settings.caseTimeoutMs())),
                    Duration.ofMillis(settings.caseTimeoutMs() + 10_000)));
            HarnessOutput out = HarnessOutput.parse(rr.stdout());
            CaseResult c = out.cases("CASE", 1).get(0);
            long ms = c.nanos() > 0 ? c.nanos() / 1_000_000 : 0;
            if (!c.ok()) {
                String err = c.value() != null ? c.value() : rr.timedOut() ? "Time limit exceeded" : abbreviate(rr.stderr(), 500);
                return new SampleRun(true, List.of(), null, sampleOutput, false, err, out.userStdout(), ms);
            }
            Boolean passed = sampleOutput == null || sampleOutput.isBlank() ? null
                    : OutputComparator.same(sampleOutput, c.value(), mode);
            return new SampleRun(true, List.of(), c.value(), sampleOutput, passed, null, out.userStdout(), ms);
        }
    }

    // =====================================================================================
    // One review job
    // =====================================================================================

    private final class Job {
        private final ReviewRequest req;
        private final ProgressListener listener;
        private final Workspace ws;
        private final ProblemContext problem;
        private final long startedAt = System.currentTimeMillis();
        private final List<String> warnings = new ArrayList<>();
        private final AtomicInteger aiTokens = new AtomicInteger();
        private final AtomicInteger aiCalls = new AtomicInteger();
        private volatile boolean aiAvailable;
        private MethodSignature sig;
        private String userCode;

        // reference state
        private CaseFile caseFile;
        private boolean caseFileIsNew;
        private boolean referenceReady;
        private boolean generatorReady;
        private boolean hiddenCasesReady;
        private Set<Integer> validInputs = new HashSet<>();

        Job(ReviewRequest req, ProgressListener listener, Workspace ws) {
            this.req = req;
            this.listener = listener;
            this.ws = ws;
            this.problem = req.problem();
            this.aiAvailable = ai.isConfigured();
            if (!aiAvailable) {
                warnings.add("AI review unavailable (no Gemini API key configured) - partial verdict.");
            }
        }

        ReviewOutcome run() {
            // ---------------------------------------------------------------- 1. compile
            listener.onStage(Stage.COMPILING, null);
            sig = MethodSignature.parse(problem.methodSignature());
            userCode = normalizeSolution(req.userCode());
            List<CompileError> blocked = toErrors(safety.check(userCode, Profile.SOLUTION));
            if (!blocked.isEmpty()) {
                return compileErrorOutcome(blocked);
            }
            CompileResult userCompile = compileSolution(ws, "user", userCode, sig);
            if (!userCompile.success()) {
                return compileErrorOutcome(explainCompileErrors(userCompile, sig));
            }
            ws.write("src/harness/CvSupport.java", harness.support());
            CompileResult hc = compiler.compile(ws, List.of("src/harness/CvSupport.java"), "classes/harness", List.of(), false);
            if (!hc.success()) {
                throw new IllegalStateException("Harness did not compile: " + hc.firstMessages(3));
            }

            // ---------------------------------------------------------------- 2+3. reference & charges
            listener.onStage(Stage.READING_CODE, null);
            String numbered = PromptLoader.numberLines(userCode);
            CompletableFuture<ChargesResponse> chargesF = aiAsync(() -> ai.findCharges(problem, numbered));
            CompletableFuture<QualityResponse> qualityF = aiAsync(() -> ai.rateQuality(problem, numbered));

            prepareReference();
            List<ChargeWork> charges = toChargeWork(await(chargesF, "find charges"));

            // ---------------------------------------------------------------- 4. gather evidence
            listener.onStage(Stage.GATHERING_EVIDENCE, charges.isEmpty() ? "No charges filed" : charges.size() + " charge(s) filed");
            List<ChargeWork> testable = charges.stream().filter(c -> c.category.needsProofTest()).toList();
            if (!testable.isEmpty()) {
                gatherEvidence(testable);
            }

            // ---------------------------------------------------------------- 5. run evidence
            listener.onStage(Stage.RUNNING_TESTS, null);
            List<ChargeWork> withTests = testable.stream().filter(c -> c.testClass != null).toList();
            if (!withTests.isEmpty()) {
                runEvidence(withTests);
            }
            for (ChargeWork c : charges) {
                if (c.category == ChargeCategory.STYLE) {
                    c.status = ChargeStatus.NOTED;
                    c.statusReason = "Style advice (not something a test can prove)";
                }
            }

            // ---------------------------------------------------------------- 6. hidden tests
            listener.onStage(Stage.HIDDEN_TESTS, null);
            HiddenRun hidden = runHiddenTests();

            // ---------------------------------------------------------------- 7. fixes
            listener.onStage(Stage.VERIFYING_FIXES, null);
            List<ChargeWork> guilty = charges.stream().filter(c -> c.status == ChargeStatus.PROVEN && c.testClass != null).toList();
            if (!guilty.isEmpty()) {
                verifyFixes(guilty, hidden);
            }

            // ---------------------------------------------------------------- 8. benchmark
            listener.onStage(Stage.BENCHMARKING, null);
            List<BenchPoint> bench = runBenchmarks();

            // ---------------------------------------------------------------- 9. score
            listener.onStage(Stage.SCORING, null);
            QualityResponse quality = await(qualityF, "rate quality");
            List<BenchPoint> userPoints = bench.stream().filter(b -> "USER".equals(b.solution())).toList();
            List<BenchPoint> optPoints = bench.stream().filter(b -> "OPTIMAL".equals(b.solution())).toList();
            String claimed = quality == null ? null : quality.estimatedComplexity();
            ComplexityEstimator.Estimate est = complexity.estimate(userPoints, claimed);
            PerformanceJudge.Judgement perf = performance.judge(userPoints, optPoints);

            for (ChargeWork c : charges) {
                if (c.category == ChargeCategory.PERFORMANCE) {
                    if (perf.score() == null) {
                        c.status = ChargeStatus.ERROR;
                        c.statusReason = "Could not be measured (no benchmark available)";
                    } else if (perf.score() <= 10) {
                        c.status = ChargeStatus.PROVEN;
                        c.statusReason = perf.ratio() == null
                                ? "Measured: your code was too slow at n=" + perf.comparedN()
                                : String.format("Measured: %.1fx slower than the optimal solution at n=%d", perf.ratio(), perf.comparedN());
                        c.proofOutput = c.statusReason;
                    } else {
                        c.status = ChargeStatus.DISMISSED;
                        c.statusReason = "The benchmark shows your code is within 10x of the optimal solution";
                    }
                }
            }

            int provenBugs = (int) charges.stream().filter(c -> c.status.isGuilty() && c.category == ChargeCategory.BUG).count();
            int provenEdges = (int) charges.stream().filter(c -> c.status.isGuilty() && c.category == ChargeCategory.EDGE_CASE).count();
            int passed = (int) hidden.results.stream().filter(HiddenTestResult::passed).count();
            ScoringCalculator.Scores scores = scoring.score(new ScoringCalculator.Inputs(
                    passed, hidden.results.size(), provenBugs, provenEdges, chargesAvailable,
                    perf.score(), quality == null ? null : quality.total()));

            String optimalComplexity = caseFile == null ? null : caseFile.complexity();
            String measured = est.measured() ? est.label() : null;
            String summary = summary(hidden.results, charges, est, optimalComplexity, perf);

            List<ChargeResult> chargeResults = charges.stream().map(ChargeWork::toResult).toList();
            return new ReviewOutcome(ReviewOutcome.Kind.FULL, List.of(), chargeResults, hidden.results, bench, perf,
                    claimed, measured, est.measured(), est.measured() ? est.slope() : null,
                    optimalComplexity, caseFile == null ? null : caseFile.keyIdea(), quality, scores, summary,
                    aiAvailable, referenceReady && caseFile != null && caseFile.verified(),
                    caseFile == null ? null : caseFile.verificationNote(), List.copyOf(warnings), ai.modelName(),
                    aiTokens.get(), aiCalls.get(), elapsed(), caseFile, caseFileIsNew);
        }

        private boolean chargesAvailable = false;

        // ------------------------------------------------------------------ reference

        private void prepareReference() {
            CaseFile cached = req.cachedCaseFile();
            if (cached != null && cached.optimalCode() != null && (cached.verified() || !aiAvailable)) {
                if (compileSolution(ws, "ref", normalizeSolution(cached.optimalCode()), sig).success()) {
                    caseFile = cached;
                    referenceReady = true;
                    setupGeneratorAndCases(cached.generatorCode(), cached.inputs());
                    validInputs = allIndices(cached.inputs());
                    if (!cached.verified()) {
                        warnings.add("Reference solution is not verified; results may be less reliable.");
                    }
                    return;
                }
            }
            if (!aiAvailable) {
                warnings.add("No trusted reference available yet, so charges cannot be proven and hidden tests cannot run.");
                return;
            }
            try {
                List<TestInputDraft> inputs = cached != null && cached.inputs() != null && !cached.inputs().isEmpty() ? cached.inputs() : null;
                String generator = cached != null ? cached.generatorCode() : null;
                if (inputs == null) {
                    TestInputsResponse t = call(() -> ai.writeTestInputs(problem));
                    inputs = t.inputs() == null ? List.of() : t.inputs().stream()
                            .filter(i -> i.args() != null && !i.args().isBlank()).limit(MAX_AI_INPUTS).toList();
                    generator = t.generatorCode();
                }
                setupGeneratorAndCases(generator, inputs);

                CaseFile best = null;
                for (int attempt = 1; attempt <= 2 && hasTime(30_000); attempt++) {
                    final boolean retry = attempt == 2;
                    ReferenceDraft rd = call(() -> ai.writeReference(problem, retry));
                    String optimal = normalizeSolution(rd.optimalCode());
                    if (!safety.check(optimal, Profile.SOLUTION).isEmpty()
                            || !compileSolution(ws, "ref", optimal, sig).success()) {
                        LOG.info("Reference attempt " + attempt + " did not compile or failed the safety check");
                        continue;
                    }
                    String brute = rd.bruteForceCode() == null ? null : normalizeSolution(rd.bruteForceCode());
                    boolean bruteOk = brute != null && safety.check(brute, Profile.SOLUTION).isEmpty()
                            && compileSolution(ws, "brute", brute, sig).success();
                    Verification v = verifyReference(inputs, bruteOk);
                    List<TestInputDraft> kept = new ArrayList<>();
                    for (int i = 0; i < inputs.size(); i++) {
                        if (v.validInputs.contains(i)) {
                            kept.add(inputs.get(i));
                        }
                    }
                    best = new CaseFile(optimal, brute, rd.complexity(), rd.keyIdea(), generatorReady ? generator : null,
                            inputs, v.verified, v.note);
                    validInputs = v.validInputs;
                    referenceReady = true;
                    if (v.verified) {
                        // store only inputs that are valid for the problem
                        best = new CaseFile(optimal, brute, rd.complexity(), rd.keyIdea(), generatorReady ? generator : null,
                                kept, true, v.note);
                        if (kept.size() != inputs.size()) {
                            setupGeneratorAndCases(generatorReady ? generator : null, kept);
                            validInputs = allIndices(kept);
                        }
                        break;
                    }
                }
                if (best == null) {
                    warnings.add("Could not build a reference solution that compiles. Charges cannot be proven this time.");
                    referenceReady = false;
                } else {
                    caseFile = best;
                    caseFileIsNew = true;
                    if (!best.verified()) {
                        warnings.add("Reference solution could not be fully verified (" + best.verificationNote()
                                + "). Proofs and hidden tests may be less reliable.");
                    }
                }
            } catch (AiUnavailableException e) {
                markAiDown(e);
                if (cached != null && cached.optimalCode() != null
                        && compileSolution(ws, "ref", normalizeSolution(cached.optimalCode()), sig).success()) {
                    caseFile = cached;
                    referenceReady = true;
                    setupGeneratorAndCases(cached.generatorCode(), cached.inputs());
                    validInputs = allIndices(cached.inputs());
                }
            }
        }

        private void setupGeneratorAndCases(String generatorCode, List<TestInputDraft> inputs) {
            generatorReady = false;
            if (generatorCode != null && !generatorCode.isBlank()
                    && safety.check(generatorCode, Profile.SOLUTION).isEmpty()) {
                ws.write("src/gen/Gen.java", harness.gen(generatorCode));
                ws.write("src/gen/GenRunner.java", harness.genRunner());
                ws.write("src/gen/BenchRunner.java", harness.benchRunner());
                CompileResult gc = compiler.compile(ws, List.of("src/gen/Gen.java", "src/gen/GenRunner.java", "src/gen/BenchRunner.java"),
                        "classes/gen", List.of("classes/harness", "classes/user"), false);
                generatorReady = gc.success();
                if (!gc.success()) {
                    LOG.info("Input generator did not compile: " + gc.firstMessages(2));
                }
            }
            hiddenCasesReady = compileCases("hidden", inputs == null ? List.of() : inputs.stream().map(TestInputDraft::args).toList());
        }

        /** Compiles CaseRunner; AI inputs that do not compile are replaced by a harmless placeholder and marked invalid. */
        private boolean compileCases(String label, List<String> inputs) {
            List<String> current = new ArrayList<>(inputs);
            Set<Integer> broken = new HashSet<>();
            for (int round = 0; round < 3; round++) {
                HarnessFactory.CaseRunnerSource src = harness.caseRunner(current, generatorReady);
                String file = "src/cases-" + label + "/CaseRunner.java";
                ws.write(file, src.source());
                List<String> cp = new ArrayList<>(List.of("classes/harness", "classes/user"));
                if (generatorReady) {
                    cp.add("classes/gen");
                }
                CompileResult r = compiler.compile(ws, List.of(file), "classes/cases-" + label, cp, false);
                if (r.success()) {
                    if ("hidden".equals(label)) {
                        brokenHiddenInputs = broken;
                    }
                    return true;
                }
                if (!"hidden".equals(label)) {
                    return false; // only AI inputs may be dropped; a bad sample input is reported as such
                }
                boolean mapped = false;
                for (CompileError e : r.errors()) {
                    int idx = src.inputIndexForLine(e.line(), current.size());
                    if (idx >= 0 && !broken.contains(idx)) {
                        broken.add(idx);
                        current.set(idx, "/* invalid input removed */");
                        mapped = true;
                    }
                }
                if (!mapped) {
                    LOG.info("CaseRunner did not compile: " + r.firstMessages(3));
                    return false;
                }
            }
            return false;
        }

        private Set<Integer> brokenHiddenInputs = new HashSet<>();

        private record Verification(boolean verified, String note, Set<Integer> validInputs) {
        }

        private Verification verifyReference(List<TestInputDraft> inputs, boolean bruteOk) {
            List<String> notes = new ArrayList<>();
            Boolean samplePassed = null;
            if (problem.sampleInput() != null && !problem.sampleInput().isBlank()
                    && problem.sampleOutput() != null && !problem.sampleOutput().isBlank()) {
                if (compileCases("sample", List.of(problem.sampleInput()))) {
                    CasesRun s = runCases("sample", "ref", 1, false, 0, 0, 0);
                    CaseResult r = s.cases.get(0);
                    samplePassed = r.ok() && OutputComparator.same(problem.sampleOutput(), r.value(), req.compareMode());
                    notes.add(samplePassed ? "matches the sample output" : "does NOT match the sample output");
                } else {
                    notes.add("sample input is not valid Java, skipped");
                }
            }
            Set<Integer> valid = new HashSet<>();
            int agreements = 0;
            int disagreements = 0;
            if (hiddenCasesReady) {
                int n = inputs.size();
                int seeds = generatorReady ? settings.differentialSeeds() : 0;
                CasesRun refRun = runCases("hidden", "ref", n, generatorReady, 1, seeds, 8);
                for (int i = 0; i < n; i++) {
                    if (refRun.cases.get(i).ok() && !brokenHiddenInputs.contains(i)) {
                        valid.add(i);
                    }
                }
                if (bruteOk) {
                    CasesRun bruteRun = runCases("hidden", "brute", n, generatorReady, 1, seeds, 8);
                    for (int i : valid) {
                        CaseResult b = bruteRun.cases.get(i);
                        if (b.ok()) {
                            if (OutputComparator.same(refRun.cases.get(i).value(), b.value(), req.compareMode())) {
                                agreements++;
                            } else {
                                disagreements++;
                            }
                        }
                    }
                    for (int k = 0; k < refRun.gens.size(); k++) {
                        CaseResult a = refRun.gens.get(k);
                        CaseResult b = k < bruteRun.gens.size() ? bruteRun.gens.get(k) : null;
                        if (a.ok() && b != null && b.ok()) {
                            if (OutputComparator.same(a.value(), b.value(), req.compareMode())) {
                                agreements++;
                            } else {
                                disagreements++;
                            }
                        }
                    }
                    notes.add(disagreements == 0
                            ? "agrees with a brute-force solution on " + agreements + " inputs"
                            : "disagrees with the brute-force solution on " + disagreements + " inputs");
                } else {
                    notes.add("no brute-force solution to compare with");
                }
            }
            boolean verified = !Boolean.FALSE.equals(samplePassed) && disagreements == 0
                    && (agreements > 0 || Boolean.TRUE.equals(samplePassed));
            String note = notes.isEmpty() ? "nothing to verify against" : String.join("; ", notes);
            return new Verification(verified, note, valid);
        }

        // ------------------------------------------------------------------ charges & evidence

        private List<ChargeWork> toChargeWork(ChargesResponse response) {
            List<ChargeWork> list = new ArrayList<>();
            if (response == null || response.charges() == null) {
                return list;
            }
            chargesAvailable = true;
            int lineCount = userCode.split("\r?\n", -1).length;
            List<ChargeDraft> drafts = new ArrayList<>(response.charges());
            drafts.sort(Comparator.comparing(d -> Severity.parse(d.severity())));
            int i = 1;
            for (ChargeDraft d : drafts) {
                if (list.size() >= settings.maxCharges() || d.title() == null || d.title().isBlank()) {
                    continue;
                }
                ChargeWork c = new ChargeWork();
                c.key = "c" + i++;
                c.category = ChargeCategory.parse(d.category());
                c.severity = Severity.parse(d.severity());
                c.lineStart = clamp(d.lineStart(), 1, lineCount);
                c.lineEnd = clamp(Math.max(d.lineEnd(), c.lineStart), c.lineStart, lineCount);
                c.title = abbreviate(d.title(), 280);
                c.explanation = d.explanation() == null ? "" : d.explanation();
                c.mistakeTag = MistakeTag.parse(d.mistakeTag());
                if (c.category == ChargeCategory.PERFORMANCE) {
                    c.mistakeTag = MistakeTag.INEFFICIENT_ALGORITHM;
                }
                c.status = ChargeStatus.DISMISSED; // until proven
                c.statusReason = "Not proven";
                list.add(c);
            }
            return list;
        }

        private void gatherEvidence(List<ChargeWork> testable) {
            if (!aiAvailable || !hasTime(20_000)) {
                testable.forEach(c -> c.error("No evidence could be produced (AI unavailable or out of time)"));
                return;
            }
            List<ChargeForTest> asks = testable.stream().map(c -> c.forTest(null)).toList();
            ProofTestsResponse resp;
            try {
                resp = call(() -> ai.writeProofTests(problem, userCode, asks));
            } catch (AiUnavailableException e) {
                markAiDown(e);
                testable.forEach(c -> c.error("No evidence: AI unavailable"));
                return;
            }
            Map<String, String> byId = testsById(resp);
            List<BrokenTest> broken = new ArrayList<>();
            for (ChargeWork c : testable) {
                String code = byId.get(c.key);
                if (code == null) {
                    c.error("The AI did not write a proof test for this charge");
                    continue;
                }
                String err = compileProofTest(c, code);
                if (err != null) {
                    broken.add(new BrokenTest(c.key, c.pendingTestCode, err));
                }
            }
            if (!broken.isEmpty() && hasTime(20_000)) {
                try {
                    ProofTestsResponse repaired = call(() -> ai.repairProofTests(problem, userCode, broken));
                    Map<String, String> fixed = testsById(repaired);
                    for (BrokenTest b : broken) {
                        ChargeWork c = find(testable, b.chargeId());
                        String code = fixed.get(b.chargeId());
                        String err = code == null ? "no repaired test" : compileProofTest(c, code);
                        if (err != null) {
                            c.error("Proof test did not compile even after one repair");
                        }
                    }
                } catch (AiUnavailableException e) {
                    markAiDown(e);
                    broken.forEach(b -> find(testable, b.chargeId()).error("Proof test did not compile"));
                }
            } else {
                broken.forEach(b -> find(testable, b.chargeId()).error("Proof test did not compile"));
            }
        }

        /** @return null when compiled, otherwise the compiler errors */
        private String compileProofTest(ChargeWork c, String rawCode) {
            String className = "ProofTest_" + c.key;
            String code = renameClass(stripPackage(rawCode), className);
            c.pendingTestCode = code;
            List<Violation> v = safety.check(code, Profile.TEST);
            if (!v.isEmpty()) {
                return "Blocked by safety check: " + v.get(0).message();
            }
            String file = "src/tests/" + className + ".java";
            ws.write(file, code);
            CompileResult r = compiler.compile(ws, List.of(file), "classes/tests", List.of("classes/user"), true);
            if (!r.success()) {
                return r.firstMessages(5);
            }
            c.testClass = className;
            c.proofTestCode = code;
            return null;
        }

        private void runEvidence(List<ChargeWork> withTests) {
            List<String> classes = withTests.stream().map(c -> c.testClass).toList();
            Map<String, JUnitReportParser.ClassResult> onUser = runJUnit(classes, "user", "user");
            Map<String, JUnitReportParser.ClassResult> onRef = referenceReady ? runJUnit(classes, "ref", "ref") : Map.of();
            for (ChargeWork c : withTests) {
                JUnitReportParser.ClassResult u = onUser.get(c.testClass);
                JUnitReportParser.ClassResult r = onRef.get(c.testClass);
                VerdictClassifier.Decision d = classifier.classify(toRun(u), toRun(r), referenceReady);
                c.status = d.status();
                c.statusReason = d.reason();
                c.badEvidence = d.badEvidence();
                if (u != null && !u.passed()) {
                    c.proofOutput = u.firstFailure();
                    c.evidenceLine = u.solutionLine();
                } else if (u != null) {
                    c.proofOutput = "All assertions passed on your code.";
                }
                if (d.badEvidence() && r != null) {
                    c.proofOutput = (c.proofOutput == null ? "" : c.proofOutput + "\n")
                            + "On the reference: " + r.firstFailure();
                }
            }
        }

        private Map<String, JUnitReportParser.ClassResult> runJUnit(List<String> classes, String variant, String label) {
            String reportsDir = "out/reports/" + label;
            ws.deleteDir(reportsDir);
            List<String> args = new ArrayList<>(List.of(
                    "--disable-banner",
                    "--disable-ansi-colors",
                    "--details=none",
                    "--reports-dir=" + reportsDir,
                    "--config=junit.jupiter.execution.timeout.default=" + settings.testTimeout().toSeconds() + " s",
                    "--config=junit.jupiter.execution.timeout.thread.mode.default=SEPARATE_THREAD"));
            for (String c : classes) {
                args.add("--select-class=" + c);
            }
            Duration timeout = settings.testTimeout().multipliedBy(classes.size() + 1).plusSeconds(15);
            RunResult rr = runner.run(ws, RunSpec.junit(List.of("classes/tests", "classes/" + variant), args, timeout));
            Map<String, JUnitReportParser.ClassResult> parsed = new HashMap<>(reports.parse(ws.path(reportsDir)));
            boolean missing = classes.stream().anyMatch(c -> !parsed.containsKey(c));
            if (missing) {
                // The test JVM died or could not report (time limit, out of memory, ...). Missing classes count
                // as failed; the double-check rule still protects correct code because the reference run is the control.
                LOG.log(Level.INFO, "JUnit run on {0} did not report every class (exit {1}, timedOut {2})",
                        new Object[]{variant, rr.exitCode(), rr.timedOut()});
                String why = rr.timedOut() ? "Time limit exceeded (possible infinite loop)"
                        : "The test run crashed: " + crashLine(rr.stdout() + "\n" + rr.stderr());
                for (String c : classes) {
                    parsed.putIfAbsent(c, new JUnitReportParser.ClassResult(1, 1, rr.timedOut(), why, null));
                }
            }
            return parsed;
        }

        // ------------------------------------------------------------------ hidden tests

        private final class HiddenRun {
            final List<HiddenTestResult> results = new ArrayList<>();
            final List<String> expectedCases = new ArrayList<>();   // per AI input index (null if invalid)
            final List<String> expectedGens = new ArrayList<>();
            final Set<String> userPassedKeys = new HashSet<>();
            int caseCount;
            int genCount;
        }

        private HiddenRun runHiddenTests() {
            HiddenRun h = new HiddenRun();
            if (!referenceReady || !hiddenCasesReady || caseFile == null || !hasTime(10_000)) {
                if (referenceReady && !hasTime(10_000)) {
                    warnings.add("Skipped hidden tests: job time limit reached.");
                }
                return h;
            }
            List<TestInputDraft> inputs = caseFile.inputs() == null ? List.of() : caseFile.inputs();
            h.caseCount = inputs.size();
            h.genCount = generatorReady ? settings.randomHiddenTests() : 0;
            CasesRun ref = runCases("hidden", "ref", h.caseCount, generatorReady, 1000, h.genCount, 20);
            CasesRun user = runCases("hidden", "user", h.caseCount, generatorReady, 1000, h.genCount, 20);
            for (int i = 0; i < h.caseCount; i++) {
                CaseResult exp = ref.cases.get(i);
                boolean valid = exp.ok() && validInputs.contains(i) && !brokenHiddenInputs.contains(i);
                h.expectedCases.add(valid ? exp.value() : null);
                if (!valid) {
                    continue;
                }
                CaseResult act = user.cases.get(i);
                boolean ok = act.ok() && OutputComparator.same(exp.value(), act.value(), req.compareMode());
                TestInputDraft in = inputs.get(i);
                h.results.add(new HiddenTestResult(in.args(), exp.value(), act.ok() ? act.value() : null, ok,
                        in.edgeCase(), in.description(), act.ok() ? null : describeFailure(act)));
                if (ok) {
                    h.userPassedKeys.add("case" + i);
                }
            }
            for (int k = 0; k < h.genCount; k++) {
                CaseResult exp = k < ref.gens.size() ? ref.gens.get(k) : null;
                if (exp == null || !exp.ok()) {
                    h.expectedGens.add(null);
                    continue;
                }
                h.expectedGens.add(exp.value());
                CaseResult act = user.gens.get(k);
                boolean ok = act.ok() && OutputComparator.same(exp.value(), act.value(), req.compareMode());
                h.results.add(new HiddenTestResult(exp.inputText(), exp.value(), act.ok() ? act.value() : null, ok,
                        false, "Random input #" + (k + 1), act.ok() ? null : describeFailure(act)));
                if (ok) {
                    h.userPassedKeys.add("gen" + k);
                }
            }
            return h;
        }

        // ------------------------------------------------------------------ fixes

        private void verifyFixes(List<ChargeWork> guilty, HiddenRun hidden) {
            if (!aiAvailable || !hasTime(25_000)) {
                return;
            }
            List<ChargeForTest> asks = guilty.stream().map(c -> c.forTest(c.proofOutput)).toList();
            FixesResponse resp;
            try {
                resp = call(() -> ai.writeFixes(problem, userCode, asks));
            } catch (AiUnavailableException e) {
                markAiDown(e);
                return;
            }
            if (resp == null || resp.fixes() == null) {
                return;
            }
            for (FixDraft f : resp.fixes()) {
                ChargeWork c = find(guilty, f.chargeId());
                if (c == null || f.fixedCode() == null || !hasTime(8_000)) {
                    continue;
                }
                String code = normalizeSolution(f.fixedCode());
                c.suggestedFixCode = code;
                c.fixExplanation = f.fixExplanation();
                c.fixVerified = false;
                String variant = "fix-" + c.key;
                if (!safety.check(code, Profile.SOLUTION).isEmpty() || !compileSolution(ws, variant, code, sig).success()) {
                    continue;
                }
                JUnitReportParser.ClassResult proof = runJUnit(List.of(c.testClass), variant, variant).get(c.testClass);
                if (proof == null || !proof.passed()) {
                    continue;
                }
                boolean noRegressions = true;
                if (!hidden.results.isEmpty()) {
                    CasesRun fixRun = runCases("hidden", variant, hidden.caseCount, generatorReady, 1000, hidden.genCount, 20);
                    for (String key : hidden.userPassedKeys) {
                        boolean isCase = key.startsWith("case");
                        int idx = Integer.parseInt(key.substring(isCase ? 4 : 3));
                        String expected = isCase ? hidden.expectedCases.get(idx) : hidden.expectedGens.get(idx);
                        CaseResult got = isCase ? fixRun.cases.get(idx) : fixRun.gens.get(idx);
                        if (expected == null || !got.ok() || !OutputComparator.same(expected, got.value(), req.compareMode())) {
                            noRegressions = false;
                            break;
                        }
                    }
                }
                if (noRegressions) {
                    c.fixVerified = true;
                    c.status = ChargeStatus.ACQUITTED;
                    c.statusReason = "Fix verified: the proof test passes after the fix and no hidden test broke";
                }
            }
        }

        // ------------------------------------------------------------------ benchmark

        private List<BenchPoint> runBenchmarks() {
            List<BenchPoint> points = new ArrayList<>();
            if (!generatorReady) {
                return points;
            }
            if (!hasTime(15_000)) {
                warnings.add("Skipped benchmark: job time limit reached.");
                return points;
            }
            points.addAll(bench("user", "USER"));
            if (referenceReady && hasTime(10_000)) {
                points.addAll(bench("ref", "OPTIMAL"));
            }
            return points;
        }

        private List<BenchPoint> bench(String variant, String label) {
            List<String> args = new ArrayList<>(List.of(
                    String.valueOf(settings.benchLimitMs()),
                    String.valueOf(settings.benchWarmups()),
                    String.valueOf(settings.benchRuns())));
            settings.benchSizes().forEach(s -> args.add(String.valueOf(s)));
            long worst = settings.benchLimitMs() * (settings.benchWarmups() + settings.benchRuns()) * settings.benchSizes().size();
            Duration timeout = Duration.ofMillis(Math.min(worst + 10_000, Math.max(10_000, remainingMs() - 5_000)));
            RunResult rr = runner.run(ws, RunSpec.benchmark(List.of("classes/harness", "classes/gen", "classes/" + variant),
                    "BenchRunner", args, timeout));
            HarnessOutput out = HarnessOutput.parse(rr.stdout());
            List<BenchPoint> points = new ArrayList<>();
            Map<Integer, HarnessOutput.BenchLine> byN = new LinkedHashMap<>();
            out.bench().forEach(b -> byN.put(b.n(), b));
            for (int n : settings.benchSizes()) {
                HarnessOutput.BenchLine b = byN.get(n);
                if (b == null) {
                    points.add(new BenchPoint(label, n, null, 0, rr.timedOut() ? "TOO_SLOW" : "SKIPPED", null));
                } else {
                    points.add(new BenchPoint(label, n, "OK".equals(b.status()) ? b.medianMs() : null, b.runs(), b.status(), b.error()));
                }
            }
            return points;
        }

        // ------------------------------------------------------------------ helpers

        private record CasesRun(List<CaseResult> cases, List<CaseResult> gens) {
        }

        private CasesRun runCases(String label, String variant, int caseCount, boolean withGen,
                                  int seedStart, int genCount, int maxN) {
            List<String> cp = new ArrayList<>(List.of("classes/harness"));
            if (generatorReady) {
                cp.add("classes/gen");
            }
            cp.add("classes/cases-" + label);
            cp.add("classes/" + variant);
            List<String> args = new ArrayList<>(List.of(String.valueOf(settings.caseTimeoutMs())));
            if (withGen && genCount > 0) {
                args.addAll(List.of(String.valueOf(seedStart), String.valueOf(genCount), String.valueOf(maxN),
                        String.valueOf(settings.caseTimeoutMs())));
            }
            Duration timeout = Duration.ofMillis(4 * settings.caseTimeoutMs() + 20_000);
            RunResult rr = runner.run(ws, RunSpec.program(cp, "CaseRunner", args, timeout));
            HarnessOutput out = HarnessOutput.parse(rr.stdout());
            if (!out.ended()) {
                LOG.log(Level.FINE, "CaseRunner on {0} did not finish cleanly: {1}", new Object[]{variant, abbreviate(rr.stderr(), 300)});
            }
            return new CasesRun(out.cases("CASE", caseCount), withGen && genCount > 0 ? out.cases("GEN", genCount) : List.of());
        }

        private <T> CompletableFuture<T> aiAsync(Supplier<AiResponse<T>> task) {
            if (!aiAvailable) {
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.supplyAsync(() -> call(task), aiPool);
        }

        private <T> T await(CompletableFuture<T> f, String what) {
            try {
                return f.join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof AiUnavailableException ex) {
                    markAiDown(ex);
                    return null;
                }
                LOG.log(Level.WARNING, "AI task '" + what + "' failed", e.getCause());
                warnings.add("AI step '" + what + "' failed: " + e.getCause().getMessage());
                return null;
            }
        }

        private <T> T call(Supplier<AiResponse<T>> task) {
            AiResponse<T> r = task.get();
            if (!r.cached()) {
                aiCalls.incrementAndGet();
                aiTokens.addAndGet(r.tokens());
            }
            return r.value();
        }

        private synchronized void markAiDown(AiUnavailableException e) {
            if (aiAvailable) {
                warnings.add("AI review unavailable: " + e.getMessage() + " Partial verdict.");
            }
            aiAvailable = false;
        }

        private ReviewOutcome compileErrorOutcome(List<CompileError> errors) {
            String first = errors.isEmpty() ? "" : " Line " + errors.get(0).line() + ": " + errors.get(0).message();
            ScoringCalculator.Scores zero = new ScoringCalculator.Scores(0, 0, 0, 0, 0, "D", false);
            return new ReviewOutcome(ReviewOutcome.Kind.COMPILE_ERROR, errors, List.of(), List.of(), List.of(),
                    PerformanceJudge.Judgement.notMeasured(), null, null, false, null, null, null, null, zero,
                    abbreviate("Did not compile." + first, 500), ai.isConfigured(), false, null, List.copyOf(warnings),
                    ai.modelName(), 0, 0, elapsed(), null, false);
        }

        private String summary(List<HiddenTestResult> hidden, List<ChargeWork> charges, ComplexityEstimator.Estimate est,
                               String optimalComplexity, PerformanceJudge.Judgement perf) {
            List<String> parts = new ArrayList<>();
            if (!hidden.isEmpty()) {
                long ok = hidden.stream().filter(HiddenTestResult::passed).count();
                parts.add(ok == hidden.size() ? "Correct on all " + hidden.size() + " hidden tests"
                        : "Passed " + ok + "/" + hidden.size() + " hidden tests");
            }
            List<ChargeWork> proven = charges.stream().filter(c -> c.status.isGuilty() && c.category != ChargeCategory.PERFORMANCE).toList();
            if (chargesAvailable) {
                parts.add(proven.isEmpty() ? "no proven bugs"
                        : proven.size() + " proven bug" + (proven.size() > 1 ? "s" : "") + " (" + proven.get(0).title + ")");
            }
            if (perf.score() != null && perf.score() == 30) {
                parts.add("as fast as the optimal solution");
            } else if (est.label() != null && optimalComplexity != null) {
                parts.add((est.measured() ? "runs in " : "estimated ") + est.label() + " vs optimal " + optimalComplexity);
            } else if (perf.score() != null && perf.score() == 0) {
                parts.add("too slow on large inputs");
            }
            if (parts.isEmpty()) {
                return "Compiled successfully. Detailed review unavailable right now.";
            }
            String s = String.join(" · ", parts);
            return Character.toUpperCase(s.charAt(0)) + s.substring(1) + ".";
        }

        private boolean hasTime(long neededMs) {
            return remainingMs() > neededMs;
        }

        private long remainingMs() {
            return settings.jobTimeout().toMillis() - elapsed();
        }

        private long elapsed() {
            return System.currentTimeMillis() - startedAt;
        }
    }

    // =====================================================================================
    // Small mutable holder used while a charge moves through the pipeline
    // =====================================================================================

    private static final class ChargeWork {
        String key;
        ChargeCategory category;
        Severity severity;
        int lineStart;
        int lineEnd;
        String title;
        String explanation;
        MistakeTag mistakeTag;
        ChargeStatus status;
        String statusReason;
        boolean badEvidence;
        String testClass;
        String pendingTestCode;
        String proofTestCode;
        String proofOutput;
        Integer evidenceLine;
        String suggestedFixCode;
        String fixExplanation;
        Boolean fixVerified;

        void error(String reason) {
            status = ChargeStatus.ERROR;
            statusReason = reason;
        }

        ChargeForTest forTest(String failingOutput) {
            return new ChargeForTest(key, category.name(), lineStart, lineEnd, title, explanation, failingOutput);
        }

        ChargeResult toResult() {
            return new ChargeResult(key, category, severity, lineStart, lineEnd, title, explanation, mistakeTag, status,
                    statusReason, badEvidence, proofTestCode != null ? proofTestCode : pendingTestCode, proofOutput,
                    evidenceLine, suggestedFixCode, fixExplanation, fixVerified);
        }
    }

    // =====================================================================================
    // Static helpers
    // =====================================================================================

    /** Compiles Solution.java together with CvCall.java (which calls the method from the signature). */
    private CompileResult compileSolution(Workspace ws, String variant, String code, MethodSignature sig) {
        ws.write("src/" + variant + "/Solution.java", code);
        ws.write("src/" + variant + "/CvCall.java", harness.cvCall(sig));
        return compiler.compile(ws, List.of("src/" + variant + "/Solution.java", "src/" + variant + "/CvCall.java"),
                "classes/" + variant, List.of(), false);
    }

    /** Errors in CvCall.java mean the method does not match the problem's signature. */
    private static List<CompileError> explainCompileErrors(CompileResult r, MethodSignature sig) {
        List<CompileError> inSolution = r.errors().stream().filter(e -> "Solution.java".equals(e.file())).toList();
        if (!inSolution.isEmpty()) {
            return inSolution;
        }
        return List.of(new CompileError("Solution.java", 1, 1,
                "Your class Solution must have the method: " + sig + "  (compiler said: "
                        + (r.errors().isEmpty() ? "?" : r.errors().get(0).message()) + ")"));
    }

    private static List<CompileError> toErrors(List<Violation> violations) {
        List<CompileError> list = new ArrayList<>();
        for (Violation v : violations) {
            list.add(new CompileError("Solution.java", v.line(), 1, "Blocked for safety: " + v.message()));
        }
        return list;
    }

    static String normalizeSolution(String code) {
        if (code == null) {
            return "";
        }
        String c = code.replace("\r\n", "\n");
        // AI answers sometimes arrive wrapped in a markdown fence
        c = c.replaceAll("^\\s*```[a-zA-Z]*\\s*\n", "").replaceAll("\n?```\\s*$", "");
        return stripPackage(c);
    }

    private static String stripPackage(String code) {
        Matcher m = PACKAGE_DECL.matcher(code);
        return m.find() ? m.replaceFirst("") : code;
    }

    static String renameClass(String code, String className) {
        String c = code.replaceAll("^\\s*```[a-zA-Z]*\\s*\n", "").replaceAll("\n?```\\s*$", "");
        Matcher m = CLASS_DECL.matcher(c);
        if (m.find() && !m.group(2).equals(className)) {
            String old = m.group(2);
            c = c.substring(0, m.start(2)) + className + c.substring(m.end(2));
            c = c.replaceAll("\\b" + Pattern.quote(old) + "\\b(?=\\s*\\()", className); // constructors
        }
        return c;
    }

    private static Map<String, String> testsById(ProofTestsResponse r) {
        Map<String, String> map = new HashMap<>();
        if (r != null && r.tests() != null) {
            for (ProofTestDraft t : r.tests()) {
                if (t.chargeId() != null && t.testClassCode() != null) {
                    map.put(t.chargeId().trim(), t.testClassCode());
                }
            }
        }
        return map;
    }

    private static ChargeWork find(List<ChargeWork> list, String key) {
        if (key == null) {
            return null;
        }
        return list.stream().filter(c -> c.key.equals(key.trim())).findFirst().orElse(null);
    }

    private static VerdictClassifier.TestRun toRun(JUnitReportParser.ClassResult r) {
        return r == null ? null : new VerdictClassifier.TestRun(r.passed(), r.timedOut(), r.firstFailure());
    }

    private static Set<Integer> allIndices(List<?> list) {
        Set<Integer> s = new HashSet<>();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                s.add(i);
            }
        }
        return s;
    }

    private static String describeFailure(CaseResult r) {
        return switch (r.status()) {
            case "TIMEOUT" -> "Time limit exceeded";
            case "SKIP" -> "Skipped after repeated time-outs";
            case "MISSING" -> "No result (the program crashed or ran out of memory)";
            default -> r.value();
        };
    }

    private static final Pattern CRASH = Pattern.compile("(OutOfMemoryError|StackOverflowError|[A-Za-z.]*(Exception|Error))[^\\n]*");

    /** Finds the most telling line ("java.lang.OutOfMemoryError: Java heap space") in crash output. */
    private static String crashLine(String s) {
        if (s == null || s.isBlank()) {
            return "no error output";
        }
        Matcher oom = Pattern.compile("(OutOfMemoryError|StackOverflowError)[^\\n]*").matcher(s);
        if (oom.find()) {
            return abbreviate(oom.group().trim(), 300);
        }
        Matcher m = CRASH.matcher(s);
        return m.find() ? abbreviate(m.group().trim(), 300) : "unknown reason";
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static String abbreviate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}
