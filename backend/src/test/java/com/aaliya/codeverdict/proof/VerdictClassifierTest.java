package com.aaliya.codeverdict.proof;

import com.aaliya.codeverdict.proof.VerdictClassifier.TestRun;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The double-check rule. */
class VerdictClassifierTest {

    private final VerdictClassifier classifier = new VerdictClassifier();
    private static final TestRun PASS = new TestRun(true, false, null);
    private static final TestRun FAIL = new TestRun(false, false, "expected 1 but was 0");
    private static final TestRun TIMEOUT = new TestRun(false, true, "timed out");

    @Test
    void failsOnUserPassesOnReferenceIsGuilty() {
        var d = classifier.classify(FAIL, PASS, true);
        assertEquals(ChargeStatus.PROVEN, d.status());
        assertFalse(d.badEvidence());
    }

    @Test
    void passesOnUserIsDismissed() {
        assertEquals(ChargeStatus.DISMISSED, classifier.classify(PASS, PASS, true).status());
    }

    @Test
    void failsOnBothIsBadEvidenceAndDismissed() {
        var d = classifier.classify(FAIL, FAIL, true);
        assertEquals(ChargeStatus.DISMISSED, d.status());
        assertTrue(d.badEvidence());
    }

    @Test
    void timeoutOnUserOnlyIsGuilty() {
        assertEquals(ChargeStatus.PROVEN, classifier.classify(TIMEOUT, PASS, true).status());
    }

    @Test
    void noReferenceMeansNoConviction() {
        assertEquals(ChargeStatus.ERROR, classifier.classify(FAIL, null, false).status());
    }

    @Test
    void noResultIsError() {
        assertEquals(ChargeStatus.ERROR, classifier.classify(null, PASS, true).status());
    }
}
