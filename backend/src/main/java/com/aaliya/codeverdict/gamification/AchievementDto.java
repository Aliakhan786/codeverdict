package com.aaliya.codeverdict.gamification;

import java.time.Instant;

public record AchievementDto(String code, String name, String description, String icon, int xpReward,
                             boolean unlocked, Instant unlockedAt) {
}
