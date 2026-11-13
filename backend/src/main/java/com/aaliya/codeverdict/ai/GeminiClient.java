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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Calls the Gemini REST API ({@code models/{model}:generateContent}) in JSON mode:
 * every prompt has a response schema, so the answer is always parseable JSON.
 *
 * Reliability: retries HTTP 429/500/503 with exponential backoff (2 s, 4 s, ...),
 * and retries once if the JSON does not parse.
 */
public final class GeminiClient implements AiClient {

    private static final Logger LOG = Logger.getLogger(GeminiClient.class.getName());

    private static final String SYSTEM = """
            You are the analysis engine of CodeVerdict, a Java code review tool for DSA practice.
            Every claim you make will be checked by compiling and running real code, so be precise.
            If you are not sure about something, leave it out. Answer ONLY with JSON matching the schema.
            """;

    private final HttpClient http;
    private final ObjectMapper mapper;
    private final PromptLoader prompts;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration timeout;
    private final int maxAttempts;
    private final Integer thinkingBudget;

    public GeminiClient(PromptLoader prompts, String apiKey, String model, String baseUrl,
                        Duration timeout, int maxAttempts, Integer thinkingBudget) {
        this.prompts = prompts;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.timeout = timeout;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.thinkingBudget = thinkingBudget;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // ---------------------------------------------------------------- the six tasks

    @Override
    public AiResponse<ChargesResponse> findCharges(ProblemContext p, String numberedCode) {
        return generate("find_charges", vars(p, Map.of("code", numberedCode)), ChargesResponse.class, 0.2);
    }

    @Override
    public AiResponse<ProofTestsResponse> writeProofTests(ProblemContext p, String code, List<ChargeForTest> charges) {
        return generate("proof_test", vars(p, Map.of(
                "code", PromptLoader.numberLines(code),
                "charges", toJson(charges))), ProofTestsResponse.class, 0.2);
    }

    @Override
    public AiResponse<ProofTestsResponse> repairProofTests(ProblemContext p, String code, List<BrokenTest> broken) {
        return generate("repair_test", vars(p, Map.of(
                "code", PromptLoader.numberLines(code),
                "broken", toJson(broken))), ProofTestsResponse.class, 0.1);
    }

    @Override
    public AiResponse<FixesResponse> writeFixes(ProblemContext p, String code, List<ChargeForTest> provenCharges) {
        return generate("fix", vars(p, Map.of(
                "code", PromptLoader.numberLines(code),
                "charges", toJson(provenCharges))), FixesResponse.class, 0.2);
    }

    @Override
    public AiResponse<ReferenceDraft> writeReference(ProblemContext p, boolean retry) {
        Map<String, String> extra = Map.of("retryNote", retry
                ? "A previous attempt produced solutions that disagreed with each other or with the sample. Be extra careful."
                : "");
        return generate("reference", vars(p, extra), ReferenceDraft.class, retry ? 0.5 : 0.2);
    }

    @Override
    public AiResponse<TestInputsResponse> writeTestInputs(ProblemContext p) {
        return generate("inputs", vars(p, Map.of()), TestInputsResponse.class, 0.4);
    }

    @Override
    public AiResponse<QualityResponse> rateQuality(ProblemContext p, String numberedCode) {
        return generate("quality", vars(p, Map.of("code", numberedCode)), QualityResponse.class, 0.2);
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    // ---------------------------------------------------------------- plumbing

    private <T> AiResponse<T> generate(String task, Map<String, String> vars, Class<T> type, double temperature) {
        String prompt = prompts.render(task, vars);
        String body = requestBody(prompt, prompts.schema(task), temperature);
        long start = System.currentTimeMillis();
        int tokens = 0;
        for (int parseAttempt = 1; parseAttempt <= 2; parseAttempt++) {
            JsonNode response = postWithRetry(body, task);
            tokens += response.path("usageMetadata").path("totalTokenCount").asInt(0);
            String text = extractText(response);
            try {
                T value = mapper.readValue(text, type);
                return new AiResponse<>(value, tokens, System.currentTimeMillis() - start, false);
            } catch (JsonProcessingException e) {
                LOG.log(Level.WARNING, "Gemini returned invalid JSON for {0} (attempt {1})", new Object[]{task, parseAttempt});
            }
        }
        throw new AiUnavailableException("AI answer for '" + task + "' was not valid JSON twice");
    }

    private JsonNode postWithRetry(String body, String task) {
        URI uri = URI.create(baseUrl + "/models/" + model + ":generateContent");
        AiUnavailableException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest req = HttpRequest.newBuilder(uri)
                        .timeout(timeout)
                        .header("Content-Type", "application/json")
                        .header("x-goog-api-key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                int code = res.statusCode();
                if (code == 200) {
                    return mapper.readTree(res.body());
                }
                String msg = "Gemini HTTP " + code + " for " + task + ": " + abbreviate(res.body());
                if (code == 429 || code == 500 || code == 503 || code == 504) {
                    last = new AiUnavailableException(code == 429 ? "AI rate limit reached (free tier). Try again in a minute." : msg);
                    LOG.log(Level.WARNING, "{0} - retrying (attempt {1}/{2})", new Object[]{msg, attempt, maxAttempts});
                    sleep(2000L * (1L << (attempt - 1)));
                    continue;
                }
                throw new AiUnavailableException(msg);
            } catch (IOException e) {
                last = new AiUnavailableException("Could not reach Gemini: " + e.getMessage(), e);
                sleep(2000L * (1L << (attempt - 1)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AiUnavailableException("Interrupted while calling Gemini", e);
            }
        }
        throw last != null ? last : new AiUnavailableException("Gemini call failed");
    }

    private String requestBody(String prompt, String schemaJson, double temperature) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.putObject("systemInstruction").putArray("parts").addObject().put("text", SYSTEM);
            ArrayNode contents = root.putArray("contents");
            ObjectNode user = contents.addObject();
            user.put("role", "user");
            user.putArray("parts").addObject().put("text", prompt);
            ObjectNode gen = root.putObject("generationConfig");
            gen.put("temperature", temperature);
            gen.put("responseMimeType", "application/json");
            gen.set("responseSchema", mapper.readTree(schemaJson));
            if (thinkingBudget != null && thinkingBudget >= 0) {
                gen.putObject("thinkingConfig").put("thinkingBudget", thinkingBudget);
            }
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Bad response schema file", e);
        }
    }

    private static String extractText(JsonNode response) {
        JsonNode candidate = response.path("candidates").path(0);
        String finish = candidate.path("finishReason").asText("");
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : candidate.path("content").path("parts")) {
            if (!part.path("thought").asBoolean(false)) {
                sb.append(part.path("text").asText(""));
            }
        }
        if (sb.isEmpty()) {
            throw new AiUnavailableException("Gemini returned no content (finishReason=" + finish + ")");
        }
        return sb.toString();
    }

    private Map<String, String> vars(ProblemContext p, Map<String, String> extra) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put("title", p.title());
        v.put("statement", p.statement());
        v.put("topic", p.topic());
        v.put("signature", p.methodSignature());
        v.put("sampleInput", p.sampleInput() == null ? "(none)" : p.sampleInput());
        v.put("sampleOutput", p.sampleOutput() == null ? "(none)" : p.sampleOutput());
        v.putAll(extra);
        return v;
    }

    private String toJson(Object o) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String abbreviate(String s) {
        return s == null ? "" : s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
