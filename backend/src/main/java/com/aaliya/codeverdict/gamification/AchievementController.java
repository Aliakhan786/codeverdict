package com.aaliya.codeverdict.gamification;

import com.aaliya.codeverdict.auth.AuthUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/achievements")
@Tag(name = "Achievements")
public class AchievementController {

    private final GamificationService gamification;

    public AchievementController(GamificationService gamification) {
        this.gamification = gamification;
    }

    @GetMapping
    public List<AchievementDto> all(@AuthenticationPrincipal AuthUser me) {
        return gamification.catalogue(me.id());
    }
}
