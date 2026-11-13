package com.aaliya.codeverdict.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/** The verdict for one submission. */
@Entity
@Table(name = "reviews")
public class Review {

    public static final String KIND_FULL = "FULL";
    public static final String KIND_COMPILE_ERROR = "COMPILE_ERROR";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "submission_id", nullable = false, unique = true)
    private UUID submissionId;

    @Column(nullable = false, length = 20)
    private String kind;

    @Column(name = "verdict_score", nullable = false)
    private int verdictScore;

    @Column(nullable = false, length = 2)
    private String grade;

    @Column(name = "correctness_score")
    private Integer correctnessScore;

    @Column(name = "performance_score")
    private Integer performanceScore;

    @Column(name = "edge_case_score")
    private Integer edgeCaseScore;

    @Column(name = "quality_score")
    private Integer qualityScore;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "claimed_complexity", length = 40)
    private String claimedComplexity;

    @Column(name = "measured_complexity", length = 40)
    private String measuredComplexity;

    @Column(name = "complexity_measured", nullable = false)
    private boolean complexityMeasured;

    @Column(name = "optimal_complexity", length = 40)
    private String optimalComplexity;

    @Column(name = "key_idea", columnDefinition = "text")
    private String keyIdea;

    @Column(name = "hidden_tests_passed", nullable = false)
    private int hiddenTestsPassed;

    @Column(name = "hidden_tests_total", nullable = false)
    private int hiddenTestsTotal;

    @Column(name = "ai_available", nullable = false)
    private boolean aiAvailable;

    @Column(name = "reference_verified", nullable = false)
    private boolean referenceVerified;

    @Column(name = "ai_model", length = 60)
    private String aiModel;

    @Column(name = "ai_tokens_used", nullable = false)
    private int aiTokensUsed;

    @Column(name = "ai_calls", nullable = false)
    private int aiCalls;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "xp_earned", nullable = false)
    private int xpEarned;

    @Column(name = "new_level", length = 30)
    private String newLevel;

    @Column(name = "personal_best", nullable = false)
    private boolean personalBest;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private ReviewDetails details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getSubmissionId() { return submissionId; }
    public void setSubmissionId(UUID submissionId) { this.submissionId = submissionId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public int getVerdictScore() { return verdictScore; }
    public void setVerdictScore(int verdictScore) { this.verdictScore = verdictScore; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Integer getCorrectnessScore() { return correctnessScore; }
    public void setCorrectnessScore(Integer correctnessScore) { this.correctnessScore = correctnessScore; }
    public Integer getPerformanceScore() { return performanceScore; }
    public void setPerformanceScore(Integer performanceScore) { this.performanceScore = performanceScore; }
    public Integer getEdgeCaseScore() { return edgeCaseScore; }
    public void setEdgeCaseScore(Integer edgeCaseScore) { this.edgeCaseScore = edgeCaseScore; }
    public Integer getQualityScore() { return qualityScore; }
    public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getClaimedComplexity() { return claimedComplexity; }
    public void setClaimedComplexity(String claimedComplexity) { this.claimedComplexity = claimedComplexity; }
    public String getMeasuredComplexity() { return measuredComplexity; }
    public void setMeasuredComplexity(String measuredComplexity) { this.measuredComplexity = measuredComplexity; }
    public boolean isComplexityMeasured() { return complexityMeasured; }
    public void setComplexityMeasured(boolean complexityMeasured) { this.complexityMeasured = complexityMeasured; }
    public String getOptimalComplexity() { return optimalComplexity; }
    public void setOptimalComplexity(String optimalComplexity) { this.optimalComplexity = optimalComplexity; }
    public String getKeyIdea() { return keyIdea; }
    public void setKeyIdea(String keyIdea) { this.keyIdea = keyIdea; }
    public int getHiddenTestsPassed() { return hiddenTestsPassed; }
    public void setHiddenTestsPassed(int hiddenTestsPassed) { this.hiddenTestsPassed = hiddenTestsPassed; }
    public int getHiddenTestsTotal() { return hiddenTestsTotal; }
    public void setHiddenTestsTotal(int hiddenTestsTotal) { this.hiddenTestsTotal = hiddenTestsTotal; }
    public boolean isAiAvailable() { return aiAvailable; }
    public void setAiAvailable(boolean aiAvailable) { this.aiAvailable = aiAvailable; }
    public boolean isReferenceVerified() { return referenceVerified; }
    public void setReferenceVerified(boolean referenceVerified) { this.referenceVerified = referenceVerified; }
    public String getAiModel() { return aiModel; }
    public void setAiModel(String aiModel) { this.aiModel = aiModel; }
    public int getAiTokensUsed() { return aiTokensUsed; }
    public void setAiTokensUsed(int aiTokensUsed) { this.aiTokensUsed = aiTokensUsed; }
    public int getAiCalls() { return aiCalls; }
    public void setAiCalls(int aiCalls) { this.aiCalls = aiCalls; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public int getXpEarned() { return xpEarned; }
    public void setXpEarned(int xpEarned) { this.xpEarned = xpEarned; }
    public String getNewLevel() { return newLevel; }
    public void setNewLevel(String newLevel) { this.newLevel = newLevel; }
    public boolean isPersonalBest() { return personalBest; }
    public void setPersonalBest(boolean personalBest) { this.personalBest = personalBest; }
    public ReviewDetails getDetails() { return details; }
    public void setDetails(ReviewDetails details) { this.details = details; }
    public Instant getCreatedAt() { return createdAt; }
}
