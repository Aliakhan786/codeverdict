package com.aaliya.codeverdict.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChargeRepository extends JpaRepository<Charge, UUID> {
    List<Charge> findByReviewIdOrderByPositionAsc(UUID reviewId);
}
