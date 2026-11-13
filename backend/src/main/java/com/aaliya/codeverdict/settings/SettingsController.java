package com.aaliya.codeverdict.settings;

import com.aaliya.codeverdict.ai.AiClient;
import com.aaliya.codeverdict.auth.AuthDtos.MeResponse;
import com.aaliya.codeverdict.auth.AuthService;
import com.aaliya.codeverdict.auth.AuthUser;
import com.aaliya.codeverdict.auth.User;
import com.aaliya.codeverdict.auth.UserRepository;
import com.aaliya.codeverdict.common.ApiException;
import com.aaliya.codeverdict.config.CodeVerdictProperties;
import com.aaliya.codeverdict.runner.CodeRunner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@Tag(name = "Settings")
public class SettingsController {

    public record SettingsRequest(
            @NotBlank @Size(max = 100) String displayName,
            @Pattern(regexp = "dark|light") String theme,
            boolean soundEnabled,
            @Min(1) @Max(20) int dailyGoal) {
    }

    public record SystemStatus(boolean aiConfigured, String aiModel, String runner, int maxReviewsPerHour,
                               String timezone) {
    }

    private final UserRepository users;
    private final AuthService auth;
    private final AiClient ai;
    private final CodeRunner runner;
    private final CodeVerdictProperties props;

    public SettingsController(UserRepository users, AuthService auth, AiClient ai, CodeRunner runner,
                              CodeVerdictProperties props) {
        this.users = users;
        this.auth = auth;
        this.ai = ai;
        this.runner = runner;
        this.props = props;
    }

    @PutMapping
    @Transactional
    @Operation(summary = "Update profile, theme, sound and daily goal")
    public MeResponse update(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody SettingsRequest req) {
        User u = users.findById(me.id()).orElseThrow(() -> ApiException.notFound("User"));
        u.setDisplayName(req.displayName().trim());
        if (req.theme() != null) {
            u.setTheme(req.theme());
        }
        u.setSoundEnabled(req.soundEnabled());
        u.setDailyGoal(req.dailyGoal());
        return auth.toMe(u);
    }

    @GetMapping("/status")
    @Operation(summary = "Gemini key status, runner mode, limits (the key itself is never returned)")
    public SystemStatus status() {
        return new SystemStatus(ai.isConfigured(), ai.isConfigured() ? ai.modelName() : null, runner.name(),
                props.review().maxPerHour(), props.timezone());
    }
}
