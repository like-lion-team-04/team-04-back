package com.likelion.firstbite.firstbiteserver.history.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoachingHistoryDetailResponse(
        UUID recordId, Instant completedAt, List<Item> items, List<Stage> stages,
        Feedback feedback, boolean personalizationApplied) {
    public record Item(UUID foodId, String name, BigDecimal servingMultiplier) {}
    public record Stage(int stage, String result, long actualSeconds) {}
    public record Feedback(Integer sleepinessScore) {}
}
