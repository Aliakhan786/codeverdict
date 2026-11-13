package com.aaliya.codeverdict.problem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OptimalSolutionRepository extends JpaRepository<OptimalSolution, UUID> {

    Optional<OptimalSolution> findByProblemId(UUID problemId);

    @Modifying
    @Query("delete from OptimalSolution o where o.problemId = :problemId")
    void deleteByProblemId(@Param("problemId") UUID problemId);
}
