package com.aaliya.codeverdict.proof;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Writes the small trusted Java programs that call the user's Solution:
 * <ul>
 *   <li>{@code CvCall}     - calls the method from the problem signature (also checks the signature)</li>
 *   <li>{@code CaseRunner} - runs a fixed list of inputs (sample input, AI test inputs)</li>
 *   <li>{@code GenRunner}  - runs seeded random inputs from the AI generator</li>
 *   <li>{@code BenchRunner}- times the solution at several input sizes</li>
 * </ul>
 * Inputs are Java argument lists, e.g. {@code new int[]{2,7,11,15}, 9}.
 */
public final class HarnessFactory {

    public String support() {
        return template("CvSupport.java.tpl");
    }

    public String genRunner() {
        return template("GenRunner.java.tpl");
    }

    public String benchRunner() {
        return template("BenchRunner.java.tpl");
    }

    /** The one place that knows how to call the user's method with an Object[] of arguments. */
    public String cvCall(MethodSignature sig) {
        StringBuilder call = new StringBuilder("s.").append(sig.name()).append('(');
        for (int i = 0; i < sig.params().size(); i++) {
            if (i > 0) {
                call.append(", ");
            }
            call.append(MethodSignature.castExpr(sig.params().get(i).type(), "a[" + i + "]"));
        }
        call.append(')');
        String body = sig.isVoid()
                ? "        " + call + ";\n        return a.length == 1 ? a[0] : a;\n"
                : "        " + sig.returnType() + " result = " + call + ";\n        return result;\n";
        return """
                import java.util.*;
                import java.math.*;

                @SuppressWarnings({"unchecked", "rawtypes"})
                public final class CvCall {
                    private CvCall() {}

                    public static Object call(Object[] a) throws Exception {
                        if (a.length != %d) {
                            throw new IllegalArgumentException("Expected %d argument(s) but got " + a.length);
                        }
                        Solution s = new Solution();
                %s    }
                }
                """.formatted(sig.params().size(), sig.params().size(), body);
    }

    /**
     * One method per input, each on ONE line, so a compile error in an AI-written
     * input can be mapped back to the input and that input dropped.
     * With a generator it also runs seeded random inputs (argv: timeout seedStart count maxN timeout).
     */
    public CaseRunnerSource caseRunner(List<String> inputs, boolean withGenerator) {
        StringBuilder sb = new StringBuilder();
        sb.append("import java.util.*;\n");                                 // line 1
        sb.append("import java.math.*;\n");                                 // line 2
        sb.append("@SuppressWarnings({\"unchecked\", \"rawtypes\"})\n");    // line 3
        sb.append("public final class CaseRunner {\n");                     // line 4
        int firstInputLine = 5;
        for (int i = 0; i < inputs.size(); i++) {
            String args = inputs.get(i).replace('\n', ' ').replace('\r', ' ').trim();
            sb.append("    static Object[] in").append(i).append("() { return new Object[]{ ").append(args).append(" }; }\n");
        }
        sb.append("""
                    static Object[] input(int i) {
                        switch (i) {
                """);
        for (int i = 0; i < inputs.size(); i++) {
            sb.append("            case ").append(i).append(": return in").append(i).append("();\n");
        }
        sb.append("""
                            default: throw new IllegalArgumentException("no input " + i);
                        }
                    }

                    public static void main(String[] argv) {
                        CvSupport.captureUserOutput();
                        long timeoutMs = Long.parseLong(argv[0]);
                        int count = %d;
                        int timeouts = 0;
                        for (int i = 0; i < count; i++) {
                            if (timeouts >= 2) { CvSupport.emit("CASE", i, "SKIP"); continue; }
                            final Object[] args;
                            try {
                                args = input(i);
                            } catch (Throwable t) {
                                CvSupport.emit("CASE", i, "BADINPUT", CvSupport.b64(CvSupport.describe(t)));
                                continue;
                            }
                            CvSupport.Outcome o = CvSupport.call(() -> CvCall.call(args), timeoutMs);
                            switch (o.status) {
                                case "OK" -> CvSupport.emit("CASE", i, "OK", o.nanos, CvSupport.b64(CvSupport.ser(o.value)));
                                case "ERR" -> CvSupport.emit("CASE", i, "ERR", o.nanos, CvSupport.b64(CvSupport.describe(o.error)));
                                default -> {
                                    timeouts++;
                                    CvSupport.emit("CASE", i, "TIMEOUT", -1, CvSupport.b64("Time limit exceeded (" + timeoutMs + " ms)"));
                                }
                            }
                        }
                        if (argv.length >= 5) {
                            %s
                        }
                        CvSupport.finish();
                    }
                }
                """.formatted(inputs.size(), withGenerator
                ? "GenRunner.run(Integer.parseInt(argv[1]), Integer.parseInt(argv[2]), Integer.parseInt(argv[3]), Long.parseLong(argv[4]));"
                : "// no input generator for this problem"));
        return new CaseRunnerSource(sb.toString(), firstInputLine);
    }

    /** Wraps the AI generator code (which must define static Object[] generate(int n, Random r)). */
    public String gen(String generatorCode) {
        return """
                import java.util.*;
                import java.math.*;

                @SuppressWarnings({"unchecked", "rawtypes"})
                public final class Gen {
                    private Gen() {}

                %s
                }
                """.formatted(generatorCode);
    }

    /** @param firstInputLine line number of {@code in0()}; input i is on line firstInputLine + i */
    public record CaseRunnerSource(String source, int firstInputLine) {
        public int inputIndexForLine(long line, int inputCount) {
            long idx = line - firstInputLine;
            return idx >= 0 && idx < inputCount ? (int) idx : -1;
        }
    }

    private static String template(String name) {
        try (InputStream in = HarnessFactory.class.getResourceAsStream("/runner/" + name)) {
            if (in == null) {
                throw new IllegalStateException("Missing harness template " + name);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
