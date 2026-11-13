package com.aaliya.codeverdict.problem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProblemRepository extends JpaRepository<Problem, UUID> {

    Optional<Problem> findByIdAndUserId(UUID id, UUID userId);

    List<Problem> findByUserIdOrderByCreatedAtAsc(UUID userId);

    long countByUserId(UUID userId);
}
