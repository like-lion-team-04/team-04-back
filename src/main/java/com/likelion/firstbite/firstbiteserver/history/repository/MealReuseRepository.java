package com.likelion.firstbite.firstbiteserver.history.repository;

import com.likelion.firstbite.firstbiteserver.history.domain.MealReuse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface MealReuseRepository extends JpaRepository<MealReuse, UUID> {
    Optional<MealReuse> findFirstByMemberIdAndIdempotencyKeyAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID memberId, UUID idempotencyKey, Instant cutoff);
}
