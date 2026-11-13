package com.aaliya.codeverdict.api;

import com.aaliya.codeverdict.ai.AiClient;
import com.aaliya.codeverdict.support.FakeAiClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full stack: real Spring Boot app + real PostgreSQL (Testcontainers) + real Proof Engine,
 * with a scripted AI so the test never calls Gemini. Skipped automatically if Docker is not running.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "codeverdict.demo.enabled=false",
        "codeverdict.runner.mode=local",
        "codeverdict.review.max-per-hour=100"
})
class ApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @TestConfiguration
    static class FakeAi {
        @Bean
        @Primary
        AiClient fakeAiClient() {
            return new FakeAiClient();
        }
    }

    @Autowired
    TestRestTemplate rest;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void registerSubmitAndReadTheVerdict() throws Exception {
        // 1. register -> token
        ResponseEntity<String> reg = rest.postForEntity("/api/v1/auth/register",
                Map.of("username", "aaliya", "email", "aaliya@example.com", "password", "secret123"), String.class);
        assertEquals(201, reg.getStatusCode().value(), reg.getBody());
        String token = json.readTree(reg.getBody()).get("token").asText();
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);

        // 2. starter problems were seeded
        JsonNode problems = get("/api/v1/problems", h);
        assertEquals(10, problems.size());
        String problemId = null;
        for (JsonNode p : problems) {
            if ("Maximum Subarray".equals(p.get("title").asText())) {
                problemId = p.get("id").asText();
            }
        }

        // 3. quick run on the sample input
        JsonNode run = post("/api/v1/problems/" + problemId + "/run", Map.of("code", FakeAiClient.CORRECT_USER_CODE), h);
        assertEquals("6", run.get("output").asText());

        // 4. submit buggy code -> 202 + id, then poll until the background job finishes
        JsonNode submitted = post("/api/v1/problems/" + problemId + "/submissions",
                Map.of("code", FakeAiClient.BUGGY_USER_CODE), h);
        String submissionId = submitted.get("submissionId").asText();
        JsonNode verdict = null;
        for (int i = 0; i < 120; i++) {
            verdict = get("/api/v1/submissions/" + submissionId, h);
            String status = verdict.get("status").asText();
            if ("DONE".equals(status) || "FAILED".equals(status)) {
                break;
            }
            Thread.sleep(1000);
        }
        assertEquals("DONE", verdict.get("status").asText(), verdict.toString());
        JsonNode review = verdict.get("review");
        assertEquals("FULL", review.get("kind").asText());
        assertTrue(review.get("xpEarned").asInt() > 0);
        boolean acquitted = false;
        for (JsonNode c : review.get("charges")) {
            if ("ACQUITTED".equals(c.get("status").asText())) {
                acquitted = true;
            }
        }
        assertTrue(acquitted, "the real bug should be proven and its fix verified");

        // 5. dashboards read the stored data
        assertEquals(1, get("/api/v1/dashboard/progress", h).get("counters").get("totalCases").asInt());
        assertEquals(1, get("/api/v1/dashboard/personal-bests", h).size());
        assertTrue(get("/api/v1/dashboard/summary", h).get("totalXp").asInt() > 0);
        boolean firstVerdict = false;
        for (JsonNode a : get("/api/v1/achievements", h)) {
            if ("FIRST_VERDICT".equals(a.get("code").asText()) && a.get("unlocked").asBoolean()) {
                firstVerdict = true;
            }
        }
        assertTrue(firstVerdict);

        // 6. history + CSV export
        assertEquals(1, get("/api/v1/submissions", h).get("total").asInt());
        ResponseEntity<String> csv = rest.exchange("/api/v1/export/history.csv", HttpMethod.GET, new HttpEntity<>(h), String.class);
        assertTrue(csv.getBody().contains("Maximum Subarray"));
    }

    @Test
    void protectedEndpointsNeedLogin() {
        ResponseEntity<String> r = rest.getForEntity("/api/v1/problems", String.class);
        assertEquals(401, r.getStatusCode().value());
        assertEquals(200, rest.getForEntity("/api/v1/health", String.class).getStatusCode().value());
    }

    private JsonNode get(String url, HttpHeaders h) throws Exception {
        ResponseEntity<String> r = rest.exchange(url, HttpMethod.GET, new HttpEntity<>(h), String.class);
        assertEquals(200, r.getStatusCode().value(), url + " -> " + r.getBody());
        return json.readTree(r.getBody());
    }

    private JsonNode post(String url, Object body, HttpHeaders h) throws Exception {
        ResponseEntity<String> r = rest.exchange(url, HttpMethod.POST, new HttpEntity<>(body, h), String.class);
        assertTrue(r.getStatusCode().is2xxSuccessful(), url + " -> " + r.getBody());
        return json.readTree(r.getBody());
    }
}
