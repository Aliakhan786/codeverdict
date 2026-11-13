package com.aaliya.codeverdict.submission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    Optional<Submission> findByIdAndUserId(UUID id, UUID userId);

    long countByUserIdAndCreatedAtAfter(UUID userId, Instant after);

    boolean existsByUserIdAndStatusIn(UUID userId, Collection<SubmissionStatus> statuses);

    List<Submission> findByStatusIn(Collection<SubmissionStatus> statuses);

    @Query("select coalesce(max(s.attemptNo), 0) from Submission s where s.problemId = :problemId")
    int maxAttemptNo(@Param("problemId") UUID problemId);

    @Modifying
    @Query("update Submission s set s.currentStage = :stage, s.status = :status where s.id = :id")
    int updateStage(@Param("id") UUID id, @Param("stage") String stage, @Param("status") SubmissionStatus status);
}
