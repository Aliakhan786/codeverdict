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

/** Full XP history: every point has a reason. */
@Entity
@Table(name = "xp_events")
public class XpEvent {

    public enum Source { REVIEW, FIX, BADGE, STREAK }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Source source;

    @Column(nullable = false)
    private int points;

    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(length = 200)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected XpEvent() {
    }

    public XpEvent(UUID userId, Source source, int points, UUID submissionId, String description) {
        this.userId = userId;
        this.source = source;
        this.points = points;
        this.submissionId = submissionId;
        this.description = description;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public Source getSource() { return source; }
    public int getPoints() { return points; }
    public UUID getSubmissionId() { return submissionId; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
