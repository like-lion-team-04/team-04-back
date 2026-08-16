package com.likelion.firstbite.firstbiteserver.recognition.repository;

import com.likelion.firstbite.firstbiteserver.recognition.domain.Recognition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RecognitionRepository extends JpaRepository<Recognition, UUID> {
    @Query("select r from Recognition r join fetch r.image where r.id = :id")
    Optional<Recognition> findByIdWithImage(@Param("id") UUID id);

    Optional<Recognition> findFirstByMemberIdAndIdempotencyKeyAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            UUID memberId, UUID idempotencyKey, Instant cutoff);
}
