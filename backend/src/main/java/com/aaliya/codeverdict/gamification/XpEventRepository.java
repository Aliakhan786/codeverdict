package com.aaliya.codeverdict.gamification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface XpEventRepository extends JpaRepository<XpEvent, UUID> {
    List<XpEvent> findBySubmissionId(UUID submissionId);
}
