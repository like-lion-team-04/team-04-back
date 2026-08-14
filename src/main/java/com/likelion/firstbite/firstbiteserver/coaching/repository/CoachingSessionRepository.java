package com.likelion.firstbite.firstbiteserver.coaching.repository;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingSession;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CoachingSessionRepository extends JpaRepository<CoachingSession, UUID> {
    Optional<CoachingSession> findFirstByMemberIdAndIdempotencyKeyAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID memberId, UUID idempotencyKey, Instant cutoff);

    boolean existsByMemberIdAndStatus(UUID memberId, CoachingSessionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from CoachingSession session where session.id = :id")
    Optional<CoachingSession> findByIdForUpdate(@Param("id") UUID id);
}
