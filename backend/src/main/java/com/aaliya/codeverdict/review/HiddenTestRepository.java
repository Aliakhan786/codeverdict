package com.aaliya.codeverdict.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HiddenTestRepository extends JpaRepository<HiddenTest, UUID> {
    List<HiddenTest> findByReviewIdOrderByPositionAsc(UUID reviewId);
}
