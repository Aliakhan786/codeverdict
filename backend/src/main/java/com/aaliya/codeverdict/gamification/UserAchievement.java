package com.aaliya.codeverdict.gamification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_achievements")
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "achievement_id", nullable = false)
    private UUID achievementId;

    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserAchievement() {
    }

    public UserAchievement(UUID userId, UUID achievementId, UUID submissionId) {
        this.userId = userId;
        this.achievementId = achievementId;
        this.submissionId = submissionId;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        if (unlockedAt == null) {
            unlockedAt = createdAt;
        }
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getAchievementId() { return achievementId; }
    public UUID getSubmissionId() { return submissionId; }
    public Instant getUnlockedAt() { return unlockedAt; }
}
