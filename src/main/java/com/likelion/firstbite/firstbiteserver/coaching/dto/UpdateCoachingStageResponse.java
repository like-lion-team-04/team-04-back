package com.likelion.firstbite.firstbiteserver.coaching.dto;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingSessionStatus;
import com.likelion.firstbite.firstbiteserver.coaching.domain.StageResult;

import java.time.Instant;
import java.util.UUID;

public record UpdateCoachingStageResponse(
        UUID sessionId,
        int currentStage,
        CoachingSessionStatus status,
        PreviousStage previousStage,
        Instant stageEndsAt
) {
    public record PreviousStage(int stage, StageResult result, long actualSeconds) {}
}
