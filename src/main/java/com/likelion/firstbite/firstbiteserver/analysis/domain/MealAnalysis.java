package com.likelion.firstbite.firstbiteserver.analysis.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meal_analyses", indexes = {
        @Index(name = "idx_meal_analyses_meal", columnList = "meal_id,created_at"),
        @Index(name = "idx_meal_analyses_idempotency", columnList = "member_id,idempotency_key,created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealAnalysis {
    @Id private UUID id;
    @Column(name = "meal_id", nullable = false) private UUID mealId;
    @Column(name = "member_id", nullable = false) private UUID memberId;
    @Column(name = "idempotency_key", nullable = false) private UUID idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Column(name = "baseline_gl", nullable = false, precision = 10, scale = 2) private BigDecimal baselineGl;
    @Column(name = "recommended_gl", nullable = false, precision = 10, scale = 2) private BigDecimal recommendedGl;
    @Column(name = "relief_rate", nullable = false, precision = 6, scale = 4) private BigDecimal reliefRate;
    @Column(name = "personal_coefficient", nullable = false, precision = 6, scale = 4) private BigDecimal personalCoefficient;
    @Column(name = "estimated_item_ratio", nullable = false, precision = 6, scale = 4) private BigDecimal estimatedItemRatio;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public static MealAnalysis create(UUID mealId, UUID memberId, UUID idempotencyKey, String requestHash,
                                      BigDecimal baselineGl, BigDecimal recommendedGl, BigDecimal reliefRate,
                                      BigDecimal personalCoefficient, BigDecimal estimatedItemRatio) {
        MealAnalysis analysis = new MealAnalysis();
        analysis.id = UUID.randomUUID();
        analysis.mealId = mealId;
        analysis.memberId = memberId;
        analysis.idempotencyKey = idempotencyKey;
        analysis.requestHash = requestHash;
        analysis.baselineGl = baselineGl;
        analysis.recommendedGl = recommendedGl;
        analysis.reliefRate = reliefRate;
        analysis.personalCoefficient = personalCoefficient;
        analysis.estimatedItemRatio = estimatedItemRatio;
        analysis.createdAt = Instant.now();
        return analysis;
    }
}
