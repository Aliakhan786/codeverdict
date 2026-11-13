package com.aaliya.codeverdict.runner;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Compiles Java sources in-process with {@code javax.tools.JavaCompiler}.
 * Much faster than starting {@code javac} (no JVM start-up). Compiling never runs
 * user code ({@code -proc:none} disables annotation processors), so it is safe
 * to do outside the sandbox; only the run step needs isolation.
 */
public final class JavaSourceCompiler {

    public record CompileError(String file, long line, long column, String message) {
    }

    public record CompileResult(boolean success, List<CompileError> errors) {
        public String firstMessages(int max) {
            StringBuilder sb = new StringBuilder();
            errors.stream().limit(max).forEach(e ->
                    sb.append(e.file()).append(':').append(e.line()).append(": ").append(e.message()).append('\n'));
            return sb.toString().trim();
        }
    }

    private final JavaCompiler compiler;
    private final Path junitJar;
    private final Duration timeout;
    private final ExecutorService pool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "javac");
        t.setDaemon(true);
        return t;
    });

    public JavaSourceCompiler(Path junitJar, Duration timeout) {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No Java compiler found. Run the backend on a JDK (not a JRE).");
        }
        this.junitJar = junitJar;
        this.timeout = timeout;
    }

    /**
     * @param sources    source files relative to the workspace
     * @param outDir     output folder relative to the workspace
     * @param classpath  extra class folders relative to the workspace
     * @param withJUnit  put the JUnit API on the compile classpath (for proof tests)
     */
    public CompileResult compile(Workspace ws, List<String> sources, String outDir, List<String> classpath,
                                 boolean withJUnit) {
        Future<CompileResult> f = pool.submit(() -> doCompile(ws, sources, outDir, classpath, withJUnit));
        try {
            return f.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            f.cancel(true);
            return new CompileResult(false, List.of(new CompileError("", 0, 0,
                    "Compilation took longer than " + timeout.toSeconds() + " s")));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while compiling", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Compiler crashed", e.getCause());
        }
    }

    private CompileResult doCompile(Workspace ws, List<String> sources, String outDir, List<String> classpath,
                                    boolean withJUnit) throws IOException {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            List<File> files = new ArrayList<>();
            for (String s : sources) {
                files.add(ws.path(s).toFile());
            }
            ws.dir(outDir);
            List<String> cp = new ArrayList<>();
            for (String c : classpath) {
                cp.add(ws.path(c).toString());
            }
            if (withJUnit) {
                cp.add(junitJar.toAbsolutePath().toString());
            }
            List<String> options = new ArrayList<>(List.of(
                    "-d", ws.path(outDir).toString(),
                    "-proc:none",
                    "--release", String.valueOf(Runtime.version().feature()),
                    "-encoding", "UTF-8",
                    "-g",
                    "-nowarn",
                    "-Xlint:none",
                    "-Xmaxerrs", "20"));
            if (!cp.isEmpty()) {
                options.add("-cp");
                options.add(String.join(File.pathSeparator, cp));
            }
            Boolean ok = compiler.getTask(null, fm, diagnostics, options, null,
                    fm.getJavaFileObjectsFromFiles(files)).call();
            List<CompileError> errors = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    String file = d.getSource() == null ? "" : Path.of(d.getSource().getName()).getFileName().toString();
                    errors.add(new CompileError(file, d.getLineNumber(), d.getColumnNumber(), d.getMessage(Locale.ROOT)));
                }
            }
            return new CompileResult(Boolean.TRUE.equals(ok) && errors.isEmpty(), errors);
        }
    }
}
