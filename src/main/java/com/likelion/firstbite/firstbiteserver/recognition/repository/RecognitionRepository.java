package com.likelion.firstbite.firstbiteserver.recognition.repository;

import com.likelion.firstbite.firstbiteserver.recognition.domain.Recognition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RecognitionRepository extends JpaRepository<Recognition, UUID> {
    Optional<Recognition> findFirstByMemberIdAndIdempotencyKeyAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            UUID memberId, UUID idempotencyKey, Instant cutoff);
}
