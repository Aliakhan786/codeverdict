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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Decorator that caches AI answers by hash(task + problem + code + inputs).
 * Re-submitting identical code costs zero API calls, which matters on the free tier.
 * Small in-memory LRU; reference solutions and test inputs are also persisted per
 * problem in the database (optimal_solutions), which is the long-lived cache.
 */
public final class CachingAiClient implements AiClient {

    private final AiClient delegate;
    private final Map<String, AiResponse<?>> cache;

    public CachingAiClient(AiClient delegate, int maxEntries) {
        this.delegate = delegate;
        this.cache = new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, AiResponse<?>> eldest) {
                return size() > maxEntries;
            }
        };
    }

    @Override
    public AiResponse<ChargesResponse> findCharges(ProblemContext p, String code) {
        return cached(key("charges", p, code), () -> delegate.findCharges(p, code));
    }

    @Override
    public AiResponse<ProofTestsResponse> writeProofTests(ProblemContext p, String code, List<ChargeForTest> charges) {
        return cached(key("tests", p, code, charges), () -> delegate.writeProofTests(p, code, charges));
    }

    @Override
    public AiResponse<ProofTestsResponse> repairProofTests(ProblemContext p, String code, List<BrokenTest> broken) {
        return cached(key("repair", p, code, broken), () -> delegate.repairProofTests(p, code, broken));
    }

    @Override
    public AiResponse<FixesResponse> writeFixes(ProblemContext p, String code, List<ChargeForTest> charges) {
        return cached(key("fixes", p, code, charges), () -> delegate.writeFixes(p, code, charges));
    }

    @Override
    public AiResponse<ReferenceDraft> writeReference(ProblemContext p, boolean retry) {
        if (retry) {
            return delegate.writeReference(p, true); // a retry must not return the cached (bad) answer
        }
        return cached(key("reference", p), () -> delegate.writeReference(p, false));
    }

    @Override
    public AiResponse<TestInputsResponse> writeTestInputs(ProblemContext p) {
        return cached(key("inputs", p), () -> delegate.writeTestInputs(p));
    }

    @Override
    public AiResponse<QualityResponse> rateQuality(ProblemContext p, String code) {
        return cached(key("quality", p, code), () -> delegate.rateQuality(p, code));
    }

    @Override
    public String modelName() {
        return delegate.modelName();
    }

    @Override
    public boolean isConfigured() {
        return delegate.isConfigured();
    }

    @SuppressWarnings("unchecked")
    private <T> AiResponse<T> cached(String key, Supplier<AiResponse<T>> call) {
        synchronized (cache) {
            AiResponse<?> hit = cache.get(key);
            if (hit != null) {
                return AiResponse.cachedCopy((AiResponse<T>) hit);
            }
        }
        AiResponse<T> fresh = call.get();
        synchronized (cache) {
            cache.put(key, fresh);
        }
        return fresh;
    }

    private String key(String task, ProblemContext p, Object... parts) {
        StringBuilder sb = new StringBuilder(task).append('\u0000')
                .append(delegate.modelName()).append('\u0000')
                .append(p);
        for (Object part : parts) {
            sb.append('\u0000').append(Objects.toString(part));
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
