package com.aaliya.codeverdict.proof;

/** Courtroom statuses of a charge (suspected issue). */
public enum ChargeStatus {
    /** Guilty: the proof test fails on the user's code and passes on the trusted reference. */
    PROVEN,
    /** The AI suspected it but the evidence did not confirm it. */
    DISMISSED,
    /** Proven, and the suggested fix makes the proof test (and all hidden tests) pass. */
    ACQUITTED,
    /** The evidence could not be produced (test did not compile even after one repair). */
    ERROR,
    /** Style remark: advisory only, never "proven". */
    NOTED;

    public boolean isGuilty() {
        return this == PROVEN || this == ACQUITTED;
    }
}
