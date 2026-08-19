package com.likelion.firstbite.firstbiteserver.history.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoachingHistoryListResponse(List<Item> items) {
    public record Item(
            UUID recordId,
            String mealName,
            Instant completedAt,
            int completedStages,
            int totalStages,
            Integer sleepinessScore,
            String completionReason,
            int skippedStages,
            long totalSeconds,
            boolean personalizationApplied,
            List<MenuItem> menuItems,
            List<StageResult> stageResults
    ) {}

    public record MenuItem(UUID foodId, String name, BigDecimal servingMultiplier) {}
    public record StageResult(int stage, String result, long actualSeconds) {}
}
