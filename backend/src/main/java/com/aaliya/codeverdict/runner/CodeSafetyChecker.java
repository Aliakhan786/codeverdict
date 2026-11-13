package com.aaliya.codeverdict.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static pre-check that runs before ANY code (user's or AI's) is executed.
 * It is the first layer of defence; the Docker sandbox is the second.
 *
 * Comments and string literals are blanked out first so that a word inside a
 * comment does not trigger a false alarm, and unicode escapes are rejected
 * because javac decodes them before parsing (a classic way to hide tokens).
 */
public final class CodeSafetyChecker {

    public enum Profile {
        /** User solutions, reference solutions, fixes, input generators. */
        SOLUTION,
        /** AI-written JUnit proof tests (may also import JUnit). */
        TEST
    }

    public record Violation(int line, String message) {
    }

    private record Rule(Pattern pattern, String message) {
    }

    private static final List<Rule> RULES = List.of(
            rule("\\bjava\\s*\\.\\s*(io|nio|net|rmi|sql|awt|beans|security|lang\\s*\\.\\s*(reflect|invoke|management|instrument|ref))\\b",
                    "File, network, reflection and system packages are not allowed"),
            rule("\\b(javax|jdk|sun)\\s*\\.", "Only java.util, java.util.stream and java.math are allowed"),
            rule("\\bcom\\s*\\.\\s*sun\\s*\\.", "Internal JDK packages are not allowed"),
            rule("\\b(Runtime|ProcessBuilder|ProcessHandle|Process)\\b", "Starting processes is not allowed"),
            rule("\\b(Thread|ThreadGroup|ThreadLocal|Executors?|ExecutorService|ForkJoinPool|CompletableFuture|Timer|VirtualThread)\\b",
                    "Threads are not allowed"),
            rule("\\bSystem\\s*\\.\\s*(exit|getenv|getProperty|getProperties|setProperty|clearProperty|setOut|setErr|setIn|load|loadLibrary|console|setSecurityManager|getSecurityManager|gc|runFinalization|mapLibraryName|inheritedChannel)\\b",
                    "This System call is not allowed"),
            rule("\\bClass\\s*\\.\\s*forName\\b", "Reflection is not allowed"),
            rule("\\.\\s*(getDeclared\\w*|getMethods?|getFields?|getConstructors?|newInstance|setAccessible|getClassLoader|getModule|invoke)\\s*\\(",
                    "Reflection is not allowed"),
            rule("\\b(ClassLoader|MethodHandles?|MethodType|VarHandle|Unsafe|ModuleLayer|ScriptEngine\\w*|SecurityManager)\\b",
                    "Reflection and class loading are not allowed"),
            rule("\\bnative\\b", "Native methods are not allowed"),
            rule("\\b(File|Files|FileReader|FileWriter|FileInputStream|FileOutputStream|FileChannel|FileSystems?|Paths?|RandomAccessFile|ObjectInputStream|ObjectOutputStream)\\b",
                    "File access is not allowed"),
            rule("\\b(Socket|ServerSocket|DatagramSocket|URL|URI|URLConnection|HttpURLConnection|HttpClient|InetAddress)\\b",
                    "Network access is not allowed"),
            rule("\\bpackage\\s+[\\w.]+\\s*;", "Remove the package line: write a plain class Solution")
    );

    private static final Pattern IMPORT = Pattern.compile("\\bimport\\s+(static\\s+)?([\\w.]+(\\s*\\.\\s*\\*)?)\\s*;");

    private static final List<String> ALLOWED_IMPORTS_SOLUTION = List.of("java.util.", "java.math.", "java.lang.");
    private static final List<String> ALLOWED_IMPORTS_TEST = List.of("java.util.", "java.math.", "java.lang.",
            "org.junit.jupiter.api.");

    public List<Violation> check(String source, Profile profile) {
        List<Violation> violations = new ArrayList<>();
        if (source == null || source.isBlank()) {
            violations.add(new Violation(1, "Code is empty"));
            return violations;
        }
        int unicode = source.indexOf("\\u");
        if (unicode >= 0) {
            violations.add(new Violation(lineOf(source, unicode), "Unicode escapes (\\uXXXX) are not allowed in code"));
        }
        String code = blankCommentsAndStrings(source);

        Matcher im = IMPORT.matcher(code);
        List<String> allowed = profile == Profile.TEST ? ALLOWED_IMPORTS_TEST : ALLOWED_IMPORTS_SOLUTION;
        while (im.find()) {
            String target = im.group(2).replaceAll("\\s+", "");
            boolean ok = allowed.stream().anyMatch(target::startsWith);
            if (!ok) {
                violations.add(new Violation(lineOf(code, im.start()), "Import not allowed: " + target));
            }
        }
        for (Rule r : RULES) {
            Matcher m = r.pattern().matcher(code);
            if (m.find()) {
                violations.add(new Violation(lineOf(code, m.start()), r.message() + " (found '" + m.group().trim() + "')"));
            }
        }
        return violations;
    }

    /** Replaces comments, string, text-block and char literals with spaces, keeping line breaks. */
    static String blankCommentsAndStrings(String src) {
        StringBuilder out = new StringBuilder(src.length());
        int i = 0;
        int n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
                while (i < n && src.charAt(i) != '\n') {
                    out.append(' ');
                    i++;
                }
            } else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
                while (i < n && !(src.charAt(i) == '*' && i + 1 < n && src.charAt(i + 1) == '/')) {
                    out.append(src.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < n) {
                    out.append("  ");
                    i += 2;
                }
            } else if (c == '"' && src.startsWith("\"\"\"", i)) {
                out.append("\"\"\"");
                i += 3;
                while (i < n && !src.startsWith("\"\"\"", i)) {
                    out.append(src.charAt(i) == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < n) {
                    out.append("\"\"\"");
                    i += 3;
                }
            } else if (c == '"' || c == '\'') {
                char quote = c;
                out.append(quote);
                i++;
                while (i < n && src.charAt(i) != quote && src.charAt(i) != '\n') {
                    if (src.charAt(i) == '\\' && i + 1 < n) {
                        out.append("  ");
                        i += 2;
                    } else {
                        out.append(' ');
                        i++;
                    }
                }
                if (i < n && src.charAt(i) == quote) {
                    out.append(quote);
                    i++;
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static int lineOf(String text, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static Rule rule(String regex, String message) {
        return new Rule(Pattern.compile(regex), message);
    }
}
