import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Trusted helper compiled next to the user's Solution. Talks to the backend
 * through lines on stdout that start with "CV|". The user's own prints are
 * captured separately so they can never corrupt this protocol.
 */
public final class CvSupport {
    private CvSupport() {}

    private static final int MAX_CAPTURE = 16 * 1024;
    private static java.io.PrintStream realOut = System.out;
    private static final java.io.ByteArrayOutputStream CAPTURED = new java.io.ByteArrayOutputStream();

    public static void captureUserOutput() {
        realOut = System.out;
        System.setOut(new java.io.PrintStream(new java.io.OutputStream() {
            @Override public void write(int b) {
                synchronized (CAPTURED) { if (CAPTURED.size() < MAX_CAPTURE) CAPTURED.write(b); }
            }
            @Override public void write(byte[] b, int off, int len) {
                synchronized (CAPTURED) {
                    int room = MAX_CAPTURE - CAPTURED.size();
                    if (room > 0) CAPTURED.write(b, off, Math.min(room, len));
                }
            }
        }, true, StandardCharsets.UTF_8));
    }

    public static String capturedOutput() {
        synchronized (CAPTURED) { return CAPTURED.toString(StandardCharsets.UTF_8); }
    }

    public static void emit(Object... parts) {
        StringBuilder sb = new StringBuilder("CV");
        for (Object p : parts) sb.append('|').append(p);
        realOut.println(sb);
        realOut.flush();
    }

    public static String b64(String s) {
        return Base64.getEncoder().encodeToString((s == null ? "" : s).getBytes(StandardCharsets.UTF_8));
    }

    /** Stable text form of any value: arrays and lists as [a, b], maps sorted by key. */
    public static String ser(Object o) {
        if (o == null) return "null";
        if (o instanceof String s) return s;
        if (o instanceof Double d) return num(d);
        if (o instanceof Float f) return num(f.doubleValue());
        if (o instanceof int[] a) return Arrays.toString(a);
        if (o instanceof long[] a) return Arrays.toString(a);
        if (o instanceof char[] a) return Arrays.toString(a);
        if (o instanceof boolean[] a) return Arrays.toString(a);
        if (o instanceof byte[] a) return Arrays.toString(a);
        if (o instanceof short[] a) return Arrays.toString(a);
        if (o instanceof double[] a) {
            StringJoiner j = new StringJoiner(", ", "[", "]");
            for (double d : a) j.add(num(d));
            return j.toString();
        }
        if (o instanceof float[] a) {
            StringJoiner j = new StringJoiner(", ", "[", "]");
            for (float d : a) j.add(num(d));
            return j.toString();
        }
        if (o instanceof Object[] a) {
            StringJoiner j = new StringJoiner(", ", "[", "]");
            for (Object x : a) j.add(ser(x));
            return j.toString();
        }
        if (o instanceof Collection<?> c) {
            StringJoiner j = new StringJoiner(", ", "[", "]");
            for (Object x : c) j.add(ser(x));
            return j.toString();
        }
        if (o instanceof Map<?, ?> m) {
            List<String> entries = new ArrayList<>();
            for (Map.Entry<?, ?> e : m.entrySet()) entries.add(ser(e.getKey()) + "=" + ser(e.getValue()));
            Collections.sort(entries);
            return "{" + String.join(", ", entries) + "}";
        }
        return String.valueOf(o);
    }

    /** Arguments shown as they would be passed: a, b, c */
    public static String serArgs(Object[] args) {
        StringJoiner j = new StringJoiner(", ");
        for (Object a : args) j.add(ser(a));
        return j.toString();
    }

    private static String num(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) return String.valueOf(d);
        String s = String.format(Locale.ROOT, "%.5f", d);
        s = s.replaceAll("0+$", "");
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s.equals("-0") ? "0" : s;
    }

    public static final class Outcome {
        public final String status; // OK | ERR | TIMEOUT
        public final Object value;
        public final Throwable error;
        public final long nanos;
        Outcome(String status, Object value, Throwable error, long nanos) {
            this.status = status; this.value = value; this.error = error; this.nanos = nanos;
        }
    }

    /** Runs one call in its own thread (large stack for recursion) with a time limit. */
    public static Outcome call(Callable<Object> task, long timeoutMs) {
        final Object[] box = new Object[1];
        final Throwable[] err = new Throwable[1];
        final long[] took = new long[1];
        Thread t = new Thread(null, () -> {
            long start = System.nanoTime();
            try {
                box[0] = task.call();
            } catch (Throwable e) {
                err[0] = e;
            }
            took[0] = System.nanoTime() - start;
        }, "cv-call", 128L * 1024 * 1024);
        t.setDaemon(true);
        t.start();
        try {
            t.join(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (t.isAlive()) return new Outcome("TIMEOUT", null, null, -1);
        if (err[0] != null) return new Outcome("ERR", null, unwrap(err[0]), took[0]);
        return new Outcome("OK", box[0], null, took[0]);
    }

    private static Throwable unwrap(Throwable t) {
        if (t instanceof java.lang.reflect.InvocationTargetException && t.getCause() != null) return t.getCause();
        return t;
    }

    /** "ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0 (line 13)" */
    public static String describe(Throwable t) {
        StringBuilder sb = new StringBuilder(t.getClass().getSimpleName());
        if (t.getMessage() != null && !(t instanceof StackOverflowError)) {
            String msg = t.getMessage();
            sb.append(": ").append(msg.length() > 300 ? msg.substring(0, 300) + "..." : msg);
        }
        for (StackTraceElement e : t.getStackTrace()) {
            if ("Solution.java".equals(e.getFileName())) {
                sb.append(" (line ").append(e.getLineNumber()).append(')');
                break;
            }
        }
        return sb.toString();
    }

    public static void finish() {
        emit("STDOUT", b64(capturedOutput()));
        emit("END");
        realOut.flush();
        Runtime.getRuntime().halt(0); // stops any runaway daemon thread immediately
    }
}
