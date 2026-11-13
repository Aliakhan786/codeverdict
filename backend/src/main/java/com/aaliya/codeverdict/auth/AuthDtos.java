package com.aaliya.codeverdict.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 30) @Pattern(regexp = "^[A-Za-z0-9_.-]+$",
                    message = "only letters, digits, _ . -") String username,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Size(max = 100) String displayName) {
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record StatsDto(int totalXp, String level, int levelIndex, String nextLevel, int xpIntoLevel,
                           int xpForNextLevel, int currentStreak, int longestStreak) {
    }

    public record MeResponse(UUID id, String username, String email, String displayName, String role, String theme,
                             boolean soundEnabled, int dailyGoal, boolean readOnly, StatsDto stats) {
    }

    /** token is returned for API tools (Swagger/Postman); the browser uses the httpOnly cookie instead. */
    public record AuthResponse(String token, MeResponse user) {
    }

    public record AuthConfig(boolean registrationEnabled, boolean demoEnabled, String demoUsername, String demoPassword) {
    }
}
