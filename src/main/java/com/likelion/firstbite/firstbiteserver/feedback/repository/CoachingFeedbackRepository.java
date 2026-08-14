package com.likelion.firstbite.firstbiteserver.feedback.repository;

import com.likelion.firstbite.firstbiteserver.feedback.domain.CoachingFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface CoachingFeedbackRepository extends JpaRepository<CoachingFeedback, UUID> {
    Optional<CoachingFeedback> findByRecordId(UUID recordId);
    List<CoachingFeedback> findAllByRecordIdIn(Collection<UUID> recordIds);
    Optional<CoachingFeedback> findFirstByMemberIdAndIdempotencyKeyAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID memberId, UUID key, Instant cutoff);
    long countByMemberIdAndSkippedFalse(UUID memberId);

    @Query("select avg(f.sleepinessScore) from CoachingFeedback f where f.memberId = :memberId and f.skipped = false")
    Optional<Double> averageValidScore(@Param("memberId") UUID memberId);
}
