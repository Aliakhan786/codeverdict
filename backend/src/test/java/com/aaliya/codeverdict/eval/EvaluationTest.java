package com.aaliya.codeverdict.eval;

import com.aaliya.codeverdict.ai.AiClient;
import com.aaliya.codeverdict.ai.GeminiClient;
import com.aaliya.codeverdict.ai.PromptLoader;
import com.aaliya.codeverdict.ai.dto.ProblemContext;
import com.aaliya.codeverdict.problem.StarterProblems;
import com.aaliya.codeverdict.proof.ChargeCategory;
import com.aaliya.codeverdict.proof.ChargeStatus;
import com.aaliya.codeverdict.proof.EngineSettings;
import com.aaliya.codeverdict.proof.ProgressListener;
import com.aaliya.codeverdict.proof.ProofEngine;
import com.aaliya.codeverdict.proof.ReviewOutcome;
import com.aaliya.codeverdict.proof.ReviewRequest;
import com.aaliya.codeverdict.runner.JavaSourceCompiler;
import com.aaliya.codeverdict.runner.LocalProcessRunner;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The evaluation from the roadmap (section 11): "Does proving charges remove false alarms
 * without losing real bugs?" Runs the REAL engine with the REAL Gemini model on 30 snippets
 * (20 with one known bug, 10 correct) and compares:
 *   Mode A - AI only:      every BUG / EDGE_CASE charge the AI raises
 *   Mode B - CodeVerdict:  only charges that were proven (GUILTY or ACQUITTED)
 *
 * Run it (takes ~15-30 min on the free tier because of rate limits):
 *   GEMINI_API_KEY=... mvn test -Dtest=EvaluationTest -Deval=true -Deval.runs=3
 * Results are written to ../docs/eval/results.md. Report the numbers you get - good or bad.
 */
@Tag("eval")
@EnabledIfSystemProperty(named = "eval", matches = "true")
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class EvaluationTest {

    private static final Path EVAL_DIR = Path.of("../docs/eval");

    record Snippet(String file, String problem, boolean hasBug, int bugLine, String bugType) {
    }

    record Counts(int reported, int correct, int falseAlarms, int bugsCaught) {
    }

    @Test
    void runEvaluation() throws Exception {
        int runs = Integer.getInteger("eval.runs", 1);
        long pauseMs = Long.getLong("eval.pause.ms", 15_000);
        List<Snippet> snippets = loadSnippets();

        Path jar = Path.of("target/runner-lib/junit-platform-console-standalone.jar");
        AiClient ai = new GeminiClient(new PromptLoader(), System.getenv("GEMINI_API_KEY"),
                System.getenv().getOrDefault("GEMINI_MODEL", "gemini-2.5-flash"),
                "https://generativelanguage.googleapis.com/v1beta", Duration.ofSeconds(90), 4, 0);
        EngineSettings settings = new EngineSettings(Path.of(System.getProperty("java.io.tmpdir"), "cv-eval"), false,
                Duration.ofSeconds(5), Duration.ofSeconds(240), 8, 2000, List.of(1000, 10000, 100000), 2000, 3, 5, 10, 50);
        ProofEngine engine = new ProofEngine(ai, new LocalProcessRunner(jar, 256, 65536),
                new JavaSourceCompiler(jar, Duration.ofSeconds(10)), settings);

        StringBuilder perSnippet = new StringBuilder();
        double[] sumA = new double[4];
        double[] sumB = new double[4];
        int testedCharges = 0;
        int badEvidence = 0;
        int guilty = 0;
        int acquitted = 0;
        long totalMs = 0;
        int reviews = 0;
        int tokens = 0;

        for (int run = 1; run <= runs; run++) {
            int[] a = new int[4];
            int[] b = new int[4];
            for (Snippet s : snippets) {
                StarterProblems.Starter p = StarterProblems.ALL.stream()
                        .filter(x -> x.title().equals(s.problem())).findFirst().orElseThrow();
                String code = Files.readString(EVAL_DIR.resolve("snippets").resolve(s.file()));
                ReviewOutcome o = engine.review(new ReviewRequest(
                        new ProblemContext(p.title(), p.statement(), p.topic().label(), p.signature(), p.sampleInput(), p.sampleOutput()),
                        code, p.mode(), null), ProgressListener.NONE);
                reviews++;
                totalMs += o.durationMs();
                tokens += o.aiTokens();

                List<ReviewOutcome.ChargeResult> correctness = o.charges().stream()
                        .filter(c -> c.category() == ChargeCategory.BUG || c.category() == ChargeCategory.EDGE_CASE).toList();
                List<ReviewOutcome.ChargeResult> proven = correctness.stream().filter(c -> c.status().isGuilty()).toList();
                Counts ca = count(s, correctness);
                Counts cb = count(s, proven);
                add(a, ca);
                add(b, cb);
                for (ReviewOutcome.ChargeResult c : correctness) {
                    if (c.proofTestCode() != null && c.status() != ChargeStatus.ERROR) {
                        testedCharges++;
                        if (c.badEvidence()) {
                            badEvidence++;
                        }
                    }
                    if (c.status().isGuilty()) {
                        guilty++;
                    }
                    if (c.status() == ChargeStatus.ACQUITTED) {
                        acquitted++;
                    }
                }
                perSnippet.append(String.format(Locale.ROOT, "| %d | %s | %s | %d | %d | %d | %d | %d | %.1f s |%n",
                        run, s.file(), s.hasBug() ? "line " + s.bugLine() : "correct",
                        ca.reported(), ca.falseAlarms(), cb.reported(), cb.falseAlarms(), o.scores().total(), o.durationMs() / 1000.0));
                System.out.printf("run %d %-45s A: %d reported / %d false | B: %d reported / %d false%n",
                        run, s.file(), ca.reported(), ca.falseAlarms(), cb.reported(), cb.falseAlarms());
                Thread.sleep(pauseMs); // stay under the free-tier requests-per-minute limit
            }
            for (int i = 0; i < 4; i++) {
                sumA[i] += a[i];
                sumB[i] += b[i];
            }
        }

        long buggy = snippets.stream().filter(Snippet::hasBug).count();
        String md = """
                # CodeVerdict evaluation results

                Date: %s · model: %s · runs: %d · snippets: %d (%d with one known bug, %d correct)

                Only BUG and EDGE_CASE charges count (correctness claims). A charge is "correct" when the snippet has a bug
                and the charge's line range is within 2 lines of the true bug line. Numbers are averages over the runs.

                | Metric | A: AI only | B: CodeVerdict (proven only) |
                |---|---|---|
                | Charges reported | %.1f | %.1f |
                | False alarms | %.1f | %.1f |
                | Bugs caught (out of %d) | %.1f | %.1f |
                | Precision | %s | %s |

                | Proof engine metric | Value |
                |---|---|
                | Bad-evidence rate (AI tests that also failed on the reference) | %s |
                | Fix success (ACQUITTED / GUILTY) | %s |
                | Average verdict time | %.1f s |
                | Average tokens per review | %d |

                ## Per snippet

                | Run | Snippet | Truth | A reported | A false | B reported | B false | Score | Time |
                |---|---|---|---|---|---|---|---|---|
                %s
                """.formatted(LocalDate.now(), ai.modelName(), runs, snippets.size(), buggy, snippets.size() - buggy,
                sumA[0] / runs, sumB[0] / runs, sumA[2] / runs, sumB[2] / runs, buggy, sumA[3] / runs, sumB[3] / runs,
                pct(sumA[1], sumA[0]), pct(sumB[1], sumB[0]), pct(badEvidence, testedCharges), pct(acquitted, guilty),
                totalMs / 1000.0 / Math.max(1, reviews), tokens / Math.max(1, reviews), perSnippet);
        Files.writeString(EVAL_DIR.resolve("results.md"), md);
        System.out.println(md);
    }

    private static Counts count(Snippet s, List<ReviewOutcome.ChargeResult> charges) {
        int correct = 0;
        for (ReviewOutcome.ChargeResult c : charges) {
            if (s.hasBug() && c.lineStart() - 2 <= s.bugLine() && s.bugLine() <= c.lineEnd() + 2) {
                correct++;
            }
        }
        int falseAlarms = charges.size() - correct;
        return new Counts(charges.size(), correct, falseAlarms, correct > 0 ? 1 : 0);
    }

    private static void add(int[] acc, Counts c) {
        acc[0] += c.reported();
        acc[1] += c.correct();
        acc[2] += c.falseAlarms();
        acc[3] += c.bugsCaught();
    }

    private static String pct(double num, double den) {
        return den == 0 ? "n/a" : String.format(Locale.ROOT, "%.0f%%", 100.0 * num / den);
    }

    private static List<Snippet> loadSnippets() throws Exception {
        List<Snippet> list = new ArrayList<>();
        List<String> lines = Files.readAllLines(EVAL_DIR.resolve("answers.csv"));
        for (String line : lines.subList(1, lines.size())) {
            String[] f = line.split(",", -1);
            list.add(new Snippet(f[0], f[1], Boolean.parseBoolean(f[2]), f[3].isBlank() ? -1 : Integer.parseInt(f[3]), f[4]));
        }
        return list;
    }
}
