package com.likelion.firstbite.firstbiteserver.history.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoachingHistoryDetailResponse(
        UUID recordId, UUID mealId, String mealType, Instant completedAt, String completionReason,
        Summary summary, List<Item> items, List<RecommendedOrderItem> recommendedOrder, List<Stage> stages,
        Feedback feedback, boolean personalizationApplied) {
    public record Summary(int completedStages, int skippedStages, int totalStages, long totalSeconds) {}
    public record Item(UUID foodId, String name, String imageUrl, BigDecimal servingMultiplier) {}
    public record RecommendedOrderItem(int order, int stage, UUID foodId, String name,
                                       String imageUrl, BigDecimal servingMultiplier) {}
    public record Stage(int stage, String title, Integer recommendedSeconds,
                        String result, Long actualSeconds) {}
    public record Feedback(UUID feedbackId, String status, Integer sleepinessScore,
                           String sleepinessLabel, Instant answeredAt, boolean personalizationUpdated) {}
}
