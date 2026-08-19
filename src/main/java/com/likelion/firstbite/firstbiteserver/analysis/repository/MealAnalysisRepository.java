package com.likelion.firstbite.firstbiteserver.analysis.repository;

import com.likelion.firstbite.firstbiteserver.analysis.domain.MealAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MealAnalysisRepository extends JpaRepository<MealAnalysis, UUID> {
    Optional<MealAnalysis> findFirstByMemberIdAndIdempotencyKeyAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID memberId, UUID idempotencyKey, Instant cutoff);

    Optional<MealAnalysis> findFirstByMealIdOrderByCreatedAtDesc(UUID mealId);
    List<MealAnalysis> findAllByMealIdInOrderByCreatedAtDesc(Collection<UUID> mealIds);
    Optional<MealAnalysis> findByIdAndMemberId(UUID id, UUID memberId);
}
