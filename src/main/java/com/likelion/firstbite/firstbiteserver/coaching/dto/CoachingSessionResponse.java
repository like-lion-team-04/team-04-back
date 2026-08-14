package com.likelion.firstbite.firstbiteserver.coaching.dto;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingSession;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingSessionStatus;

import java.time.Instant;
import java.util.UUID;

public record CoachingSessionResponse(
        UUID sessionId,
        CoachingSessionStatus status,
        int currentStage,
        Instant startedAt,
        Instant stageEndsAt
) {
    public static CoachingSessionResponse from(CoachingSession session) {
        return new CoachingSessionResponse(session.getId(), session.getStatus(), session.getCurrentStage(),
                session.getStartedAt(), session.getStageEndsAt());
    }
}
