package com.likelion.firstbite.firstbiteserver.coaching.repository;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CoachingRecordRepository extends JpaRepository<CoachingRecord, UUID> {
    Optional<CoachingRecord> findFirstByMemberIdAndIdempotencyKeyAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID memberId, UUID idempotencyKey, Instant cutoff);
}
