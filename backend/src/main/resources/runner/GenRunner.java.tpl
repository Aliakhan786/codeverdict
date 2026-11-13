/** Runs the solution on inputs made by the AI-written generator (random but seeded). */
public final class GenRunner {
    public static void main(String[] argv) {
        CvSupport.captureUserOutput();
        run(Integer.parseInt(argv[0]), Integer.parseInt(argv[1]), Integer.parseInt(argv[2]), Long.parseLong(argv[3]));
        CvSupport.finish();
    }

    public static void run(int seedStart, int count, int maxN, long timeoutMs) {
        int timeouts = 0;
        for (int k = 0; k < count; k++) {
            if (timeouts >= 2) { CvSupport.emit("GEN", k, "SKIP"); continue; }
            int seed = seedStart + k;
            int n = 1 + Math.floorMod(seed * 7919, maxN);
            final Object[] args;
            try {
                args = Gen.generate(n, new java.util.Random(seed));
            } catch (Throwable t) {
                CvSupport.emit("GEN", k, "BADINPUT", CvSupport.b64(CvSupport.describe(t)));
                continue;
            }
            String inputText = CvSupport.serArgs(args); // before the call: solutions may modify arrays
            CvSupport.Outcome o = CvSupport.call(() -> CvCall.call(args), timeoutMs);
            switch (o.status) {
                case "OK" -> CvSupport.emit("GEN", k, "OK", CvSupport.b64(inputText), CvSupport.b64(CvSupport.ser(o.value)));
                case "ERR" -> CvSupport.emit("GEN", k, "ERR", CvSupport.b64(inputText), CvSupport.b64(CvSupport.describe(o.error)));
                default -> {
                    timeouts++;
                    CvSupport.emit("GEN", k, "TIMEOUT", CvSupport.b64(inputText), CvSupport.b64("Time limit exceeded"));
                }
            }
        }
    }
}
