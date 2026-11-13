package com.aaliya.codeverdict.common;

import com.aaliya.codeverdict.ai.AiClient;
import com.aaliya.codeverdict.runner.CodeRunner;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health")
public class HealthController {

    private final JdbcTemplate jdbc;
    private final CodeRunner runner;
    private final AiClient ai;

    public HealthController(JdbcTemplate jdbc, CodeRunner runner, AiClient ai) {
        this.jdbc = jdbc;
        this.runner = runner;
        this.ai = ai;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        boolean dbUp;
        try {
            Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
            dbUp = one != null && one == 1;
        } catch (Exception e) {
            dbUp = false;
        }
        body.put("status", dbUp ? "ok" : "degraded");
        body.put("database", dbUp ? "up" : "down");
        body.put("runner", runner.name());
        body.put("ai", ai.isConfigured() ? ai.modelName() : "not configured");
        return body;
    }
}
