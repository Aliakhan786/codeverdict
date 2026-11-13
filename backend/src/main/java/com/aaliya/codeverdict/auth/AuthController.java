package com.aaliya.codeverdict.auth;

import com.aaliya.codeverdict.auth.AuthDtos.AuthConfig;
import com.aaliya.codeverdict.auth.AuthDtos.AuthResponse;
import com.aaliya.codeverdict.auth.AuthDtos.LoginRequest;
import com.aaliya.codeverdict.auth.AuthDtos.MeResponse;
import com.aaliya.codeverdict.auth.AuthDtos.RegisterRequest;
import com.aaliya.codeverdict.config.CodeVerdictProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService auth;
    private final JwtService jwt;
    private final CodeVerdictProperties props;

    public AuthController(AuthService auth, JwtService jwt, CodeVerdictProperties props) {
        this.auth = auth;
        this.jwt = jwt;
        this.props = props;
    }

    @PostMapping("/register")
    @Operation(summary = "Create an account (can be disabled with REGISTRATION_ENABLED=false)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        User u = auth.register(req);
        return withCookie(HttpStatus.CREATED, u);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with username (or email) and password; returns a JWT and sets the cv_token cookie")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        User u = auth.login(req.username(), req.password());
        return withCookie(HttpStatus.OK, u);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @GetMapping("/me")
    @Operation(summary = "Current user, settings and XP/level/streak")
    public MeResponse me(@AuthenticationPrincipal AuthUser me) {
        return auth.me(me.id());
    }

    @GetMapping("/config")
    @Operation(summary = "What the login page should offer (sign-up, demo account)")
    public AuthConfig config() {
        CodeVerdictProperties.Demo demo = props.demo();
        return new AuthConfig(props.auth().registrationEnabled(), demo.enabled(),
                demo.enabled() ? demo.username() : null, demo.enabled() ? demo.password() : null);
    }

    private ResponseEntity<AuthResponse> withCookie(HttpStatus status, User u) {
        String token = jwt.issue(u);
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie(token, jwt.ttl()).toString())
                .body(new AuthResponse(token, auth.toMe(u)));
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(JwtAuthFilter.COOKIE, value)
                .httpOnly(true)
                .secure(props.jwt().cookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
