package com.aaliya.codeverdict.problem;

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

/**
 * The trusted "case file" of a problem: verified reference solution, brute force,
 * hidden-test inputs and input generator. Cached so every attempt is judged the same way.
 */
@Entity
@Table(name = "optimal_solutions")
public class OptimalSolution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "problem_id", nullable = false, unique = true)
    private UUID problemId;

    @Column(nullable = false, columnDefinition = "text")
    private String code;

    @Column(name = "brute_force_code", columnDefinition = "text")
    private String bruteForceCode;

    @Column(length = 40)
    private String complexity;

    @Column(name = "key_idea", columnDefinition = "text")
    private String keyIdea;

    @Column(name = "generator_code", columnDefinition = "text")
    private String generatorCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "test_inputs", columnDefinition = "jsonb")
    private TestInputSet testInputs;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "verification_note", columnDefinition = "text")
    private String verificationNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProblemId() { return problemId; }
    public void setProblemId(UUID problemId) { this.problemId = problemId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getBruteForceCode() { return bruteForceCode; }
    public void setBruteForceCode(String bruteForceCode) { this.bruteForceCode = bruteForceCode; }
    public String getComplexity() { return complexity; }
    public void setComplexity(String complexity) { this.complexity = complexity; }
    public String getKeyIdea() { return keyIdea; }
    public void setKeyIdea(String keyIdea) { this.keyIdea = keyIdea; }
    public String getGeneratorCode() { return generatorCode; }
    public void setGeneratorCode(String generatorCode) { this.generatorCode = generatorCode; }
    public TestInputSet getTestInputs() { return testInputs; }
    public void setTestInputs(TestInputSet testInputs) { this.testInputs = testInputs; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public String getVerificationNote() { return verificationNote; }
    public void setVerificationNote(String verificationNote) { this.verificationNote = verificationNote; }
    public Instant getCreatedAt() { return createdAt; }
}
