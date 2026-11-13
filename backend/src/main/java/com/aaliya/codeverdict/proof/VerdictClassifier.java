package com.aaliya.codeverdict.proof;

/**
 * The double-check rule, the heart of CodeVerdict:
 * a charge is GUILTY only if its proof test FAILS on the user's code
 * AND PASSES on a trusted reference solution. A badly written AI test
 * (one that also fails on correct code) can therefore never convict.
 */
public final class VerdictClassifier {

    public record Decision(ChargeStatus status, boolean badEvidence, String reason) {
    }

    /** Test outcome on one solution; {@code null} when there was no result at all. */
    public record TestRun(boolean passed, boolean timedOut, String failure) {
    }

    public Decision classify(TestRun onUser, TestRun onReference, boolean referenceAvailable) {
        if (onUser == null) {
            return new Decision(ChargeStatus.ERROR, false, "The proof test produced no result");
        }
        if (onUser.passed()) {
            return new Decision(ChargeStatus.DISMISSED, false, "The proof test passes on your code, so the AI was wrong");
        }
        if (!referenceAvailable || onReference == null) {
            return new Decision(ChargeStatus.ERROR, false, "No trusted reference to double-check the evidence");
        }
        if (onReference.passed()) {
            return new Decision(ChargeStatus.PROVEN, false, onUser.timedOut()
                    ? "Your code exceeded the time limit while the reference finished"
                    : "Fails on your code, passes on the trusted reference");
        }
        return new Decision(ChargeStatus.DISMISSED, true, "Bad evidence: the test also fails on the trusted reference");
    }
}
