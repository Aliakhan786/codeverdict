/**
 * Benchmark harness. For each input size: build the input with a FIXED seed,
 * do warm-up runs (lets the JIT compile hot code), then time several runs with
 * System.nanoTime and report the median (robust against GC pauses / noise).
 * Input generation is never inside the timed part.
 */
public final class BenchRunner {
    public static void main(String[] argv) {
        CvSupport.captureUserOutput();
        long limitMs = Long.parseLong(argv[0]);
        int warmups = Integer.parseInt(argv[1]);
        int runs = Integer.parseInt(argv[2]);
        long sink = 0;
        boolean stop = false;
        for (int i = 3; i < argv.length; i++) {
            int n = Integer.parseInt(argv[i]);
            if (stop) { CvSupport.emit("BENCH", n, "SKIPPED"); continue; }
            long[] times = new long[runs];
            String status = "OK";
            String error = "";
            for (int r = 0; r < warmups + runs; r++) {
                final Object[] args;
                try {
                    args = Gen.generate(n, new java.util.Random(42));
                } catch (Throwable t) {
                    status = "ERROR"; error = "Input generator failed: " + CvSupport.describe(t); break;
                }
                CvSupport.Outcome o = CvSupport.call(() -> CvCall.call(args), limitMs);
                if ("TIMEOUT".equals(o.status)) { status = "TOO_SLOW"; error = "Over " + limitMs + " ms"; break; }
                if ("ERR".equals(o.status)) { status = "ERROR"; error = CvSupport.describe(o.error); break; }
                sink += System.identityHashCode(o.value); // keep the result alive
                if (r >= warmups) times[r - warmups] = o.nanos;
            }
            if ("OK".equals(status)) {
                java.util.Arrays.sort(times);
                CvSupport.emit("BENCH", n, "OK", times[runs / 2], runs);
            } else {
                CvSupport.emit("BENCH", n, status, CvSupport.b64(error));
                stop = true; // bigger inputs would only be slower
            }
        }
        CvSupport.emit("SINK", sink);
        CvSupport.finish();
    }
}
