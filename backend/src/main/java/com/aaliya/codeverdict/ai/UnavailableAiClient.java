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

/** Used when no API key is configured: every call fails fast, the pipeline gives a partial verdict. */
public final class UnavailableAiClient implements AiClient {

    private final String reason;

    public UnavailableAiClient(String reason) {
        this.reason = reason;
    }

    private <T> T fail() {
        throw new AiUnavailableException(reason);
    }

    @Override
    public AiResponse<ChargesResponse> findCharges(ProblemContext p, String c) {
        return fail();
    }

    @Override
    public AiResponse<ProofTestsResponse> writeProofTests(ProblemContext p, String c, List<ChargeForTest> ch) {
        return fail();
    }

    @Override
    public AiResponse<ProofTestsResponse> repairProofTests(ProblemContext p, String c, List<BrokenTest> b) {
        return fail();
    }

    @Override
    public AiResponse<FixesResponse> writeFixes(ProblemContext p, String c, List<ChargeForTest> ch) {
        return fail();
    }

    @Override
    public AiResponse<ReferenceDraft> writeReference(ProblemContext p, boolean retry) {
        return fail();
    }

    @Override
    public AiResponse<TestInputsResponse> writeTestInputs(ProblemContext p) {
        return fail();
    }

    @Override
    public AiResponse<QualityResponse> rateQuality(ProblemContext p, String c) {
        return fail();
    }

    @Override
    public String modelName() {
        return "none";
    }

    @Override
    public boolean isConfigured() {
        return false;
    }
}
