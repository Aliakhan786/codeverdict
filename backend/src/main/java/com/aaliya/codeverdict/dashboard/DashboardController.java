package com.aaliya.codeverdict.dashboard;

import com.aaliya.codeverdict.auth.AuthUser;
import com.aaliya.codeverdict.dashboard.DashboardDtos.PersonalBest;
import com.aaliya.codeverdict.dashboard.DashboardDtos.Progress;
import com.aaliya.codeverdict.dashboard.DashboardDtos.Summary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/summary")
    @Operation(summary = "Home page: level, XP, streak, this week, today's challenge, continue cards")
    public Summary summary(@AuthenticationPrincipal AuthUser me) {
        return dashboard.summary(me.id());
    }

    @GetMapping("/progress")
    @Operation(summary = "Weekly scores, topic strength, top mistakes, heatmap, counters")
    public Progress progress(@AuthenticationPrincipal AuthUser me) {
        return dashboard.progress(me.id());
    }

    @GetMapping("/personal-bests")
    @Operation(summary = "Best attempt per problem vs the optimal solution")
    public List<PersonalBest> personalBests(@AuthenticationPrincipal AuthUser me) {
        return dashboard.personalBests(me.id());
    }
}
