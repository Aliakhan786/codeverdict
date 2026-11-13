package com.aaliya.codeverdict.benchmark;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** One timing: 3 sizes x 2 solutions (USER / OPTIMAL) per review. */
@Entity
@Table(name = "benchmarks")
public class Benchmark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "review_id", nullable = false)
    private UUID reviewId;

    @Column(nullable = false, length = 10)
    private String solution;

    @Column(name = "input_size", nullable = false)
    private int inputSize;

    @Column(name = "median_ms")
    private Double medianMs;

    @Column(nullable = false)
    private int runs;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(columnDefinition = "text")
    private String error;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Benchmark() {
    }

    public Benchmark(UUID reviewId, BenchPoint p) {
        this.reviewId = reviewId;
        this.solution = p.solution();
        this.inputSize = p.n();
        this.medianMs = p.medianMs();
        this.runs = p.runs();
        this.status = p.status();
        this.error = p.error();
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getReviewId() { return reviewId; }
    public String getSolution() { return solution; }
    public int getInputSize() { return inputSize; }
    public Double getMedianMs() { return medianMs; }
    public int getRuns() { return runs; }
    public String getStatus() { return status; }
    public String getError() { return error; }
}
