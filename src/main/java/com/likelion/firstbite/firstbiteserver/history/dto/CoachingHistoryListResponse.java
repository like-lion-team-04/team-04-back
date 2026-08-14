package com.likelion.firstbite.firstbiteserver.history.dto;

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
            Integer sleepinessScore
    ) {}
}
