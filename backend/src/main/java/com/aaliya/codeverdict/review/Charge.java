package com.aaliya.codeverdict.review;

import com.aaliya.codeverdict.proof.ChargeCategory;
import com.aaliya.codeverdict.proof.ChargeStatus;
import com.aaliya.codeverdict.proof.MistakeTag;
import com.aaliya.codeverdict.proof.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** A suspected issue and its evidence. */
@Entity
@Table(name = "charges")
public class Charge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Column(name = "line_start", nullable = false)
    private int lineStart;

    @Column(name = "line_end", nullable = false)
    private int lineEnd;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "text")
    private String explanation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChargeStatus status;

    @Column(name = "status_reason", columnDefinition = "text")
    private String statusReason;

    @Column(name = "bad_evidence", nullable = false)
    private boolean badEvidence;

    @Column(name = "proof_test_code", columnDefinition = "text")
    private String proofTestCode;

    @Column(name = "proof_output", columnDefinition = "text")
    private String proofOutput;

    @Column(name = "evidence_line")
    private Integer evidenceLine;

    @Column(name = "suggested_fix_code", columnDefinition = "text")
    private String suggestedFixCode;

    @Column(name = "fix_explanation", columnDefinition = "text")
    private String fixExplanation;

    @Column(name = "fix_verified")
    private Boolean fixVerified;

    @Enumerated(EnumType.STRING)
    @Column(name = "mistake_tag", nullable = false, length = 40)
    private MistakeTag mistakeTag;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getReviewId() { return reviewId; }
    public void setReviewId(UUID reviewId) { this.reviewId = reviewId; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public ChargeCategory getCategory() { return category; }
    public void setCategory(ChargeCategory category) { this.category = category; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public int getLineStart() { return lineStart; }
    public void setLineStart(int lineStart) { this.lineStart = lineStart; }
    public int getLineEnd() { return lineEnd; }
    public void setLineEnd(int lineEnd) { this.lineEnd = lineEnd; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public ChargeStatus getStatus() { return status; }
    public void setStatus(ChargeStatus status) { this.status = status; }
    public String getStatusReason() { return statusReason; }
    public void setStatusReason(String statusReason) { this.statusReason = statusReason; }
    public boolean isBadEvidence() { return badEvidence; }
    public void setBadEvidence(boolean badEvidence) { this.badEvidence = badEvidence; }
    public String getProofTestCode() { return proofTestCode; }
    public void setProofTestCode(String proofTestCode) { this.proofTestCode = proofTestCode; }
    public String getProofOutput() { return proofOutput; }
    public void setProofOutput(String proofOutput) { this.proofOutput = proofOutput; }
    public Integer getEvidenceLine() { return evidenceLine; }
    public void setEvidenceLine(Integer evidenceLine) { this.evidenceLine = evidenceLine; }
    public String getSuggestedFixCode() { return suggestedFixCode; }
    public void setSuggestedFixCode(String suggestedFixCode) { this.suggestedFixCode = suggestedFixCode; }
    public String getFixExplanation() { return fixExplanation; }
    public void setFixExplanation(String fixExplanation) { this.fixExplanation = fixExplanation; }
    public Boolean getFixVerified() { return fixVerified; }
    public void setFixVerified(Boolean fixVerified) { this.fixVerified = fixVerified; }
    public MistakeTag getMistakeTag() { return mistakeTag; }
    public void setMistakeTag(MistakeTag mistakeTag) { this.mistakeTag = mistakeTag; }
}
