package com.aaliya.codeverdict.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One hidden test: AI-written input, expected output from the verified reference. */
@Entity
@Table(name = "hidden_tests")
public class HiddenTest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, columnDefinition = "text")
    private String input;

    @Column(name = "expected_output", columnDefinition = "text")
    private String expectedOutput;

    @Column(name = "actual_output", columnDefinition = "text")
    private String actualOutput;

    @Column(nullable = false)
    private boolean passed;

    @Column(name = "is_edge_case", nullable = false)
    private boolean edgeCase;

    @Column(length = 200)
    private String description;

    @Column(columnDefinition = "text")
    private String error;

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
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    public String getActualOutput() { return actualOutput; }
    public void setActualOutput(String actualOutput) { this.actualOutput = actualOutput; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public boolean isEdgeCase() { return edgeCase; }
    public void setEdgeCase(boolean edgeCase) { this.edgeCase = edgeCase; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
