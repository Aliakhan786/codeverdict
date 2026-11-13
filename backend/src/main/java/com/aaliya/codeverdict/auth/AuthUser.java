package com.aaliya.codeverdict.auth;

import java.util.UUID;

/** The logged-in user as seen by controllers: {@code @AuthenticationPrincipal AuthUser me}. */
public record AuthUser(UUID id, String username, String role) {

    public boolean isDemo() {
        return User.ROLE_DEMO.equals(role);
    }
}
