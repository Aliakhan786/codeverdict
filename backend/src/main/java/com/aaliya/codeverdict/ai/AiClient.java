package com.aaliya.codeverdict.ai;

import com.aaliya.codeverdict.ai.dto.BrokenTest;
import com.aaliya.codeverdict.ai.dto.ChargeForTest;
import com.aaliya.codeverdict.ai.dto.ChargesResponse;
import com.aaliya.codeverdict.ai.dto.FixesResponse;
import com.aaliya.codeverdict.ai.dto.ProblemContext;
import com.aaliya.codeverdict.ai.dto.ProofTestsResponse;
import com.aaliya.codeverdict.ai.dto.QualityResponse;
import com.aaliya.codeverdict.ai.dto.ReferenceDraft;
import com.aaliya.codeverdict.ai.dto.TestInputsResponse;

import java.util.List;

/**
 * Everything CodeVerdict asks an AI model. Small, focused tasks, each answered
 * in strict JSON. The pipeline only depends on this interface, so Gemini can be
 * swapped for another model (Groq, OpenAI, a local model) without touching it.
 */
public interface AiClient {

    /** Task 1: list suspected issues ("charges"). Code is sent with line numbers. */
    AiResponse<ChargesResponse> findCharges(ProblemContext problem, String numberedCode);

    /** Task 2: one JUnit 5 test class per charge that FAILS if the bug is real. */
    AiResponse<ProofTestsResponse> writeProofTests(ProblemContext problem, String code, List<ChargeForTest> charges);

    /** Task 2b: repair proof tests that did not compile (called at most once). */
    AiResponse<ProofTestsResponse> repairProofTests(ProblemContext problem, String code, List<BrokenTest> broken);

    /** Task 3: a corrected full Solution for each proven charge. */
    AiResponse<FixesResponse> writeFixes(ProblemContext problem, String code, List<ChargeForTest> provenCharges);

    /** Task 4: optimal + brute-force reference solutions. */
    AiResponse<ReferenceDraft> writeReference(ProblemContext problem, boolean retry);

    /** Task 5: hidden-test inputs (no outputs!) and a seeded input generator. */
    AiResponse<TestInputsResponse> writeTestInputs(ProblemContext problem);

    /** Task 6: code quality rubric. */
    AiResponse<QualityResponse> rateQuality(ProblemContext problem, String numberedCode);

    String modelName();

    boolean isConfigured();
}
