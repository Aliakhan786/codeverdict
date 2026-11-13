package com.aaliya.codeverdict.benchmark;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BenchmarkRepository extends JpaRepository<Benchmark, UUID> {
    List<Benchmark> findByReviewIdOrderBySolutionAscInputSizeAsc(UUID reviewId);
}
