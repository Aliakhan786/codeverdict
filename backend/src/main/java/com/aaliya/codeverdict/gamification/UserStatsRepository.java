package com.aaliya.codeverdict.gamification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserStatsRepository extends JpaRepository<UserStats, UUID> {
    Optional<UserStats> findByUserId(UUID userId);
}
