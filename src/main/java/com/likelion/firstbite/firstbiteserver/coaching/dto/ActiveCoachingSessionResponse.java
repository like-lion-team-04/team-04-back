package com.likelion.firstbite.firstbiteserver.coaching.dto;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingSession;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingSessionStatus;

import java.time.Instant;
import java.util.UUID;

public record ActiveCoachingSessionResponse(boolean active, Session session) {
    public static ActiveCoachingSessionResponse none() {
        return new ActiveCoachingSessionResponse(false, null);
    }

    public static ActiveCoachingSessionResponse from(CoachingSession session, Instant now) {
        return new ActiveCoachingSessionResponse(true, new Session(session.getId(), session.getMealId(),
                session.getStatus(), session.getCurrentStage(), session.getTotalStages(), session.getStartedAt(),
                session.getStageEndsAt(), session.getPausedAt(), session.remainingSeconds(now)));
    }

    public record Session(
            UUID sessionId,
            UUID mealId,
            CoachingSessionStatus status,
            int currentStage,
            int totalStages,
            Instant startedAt,
            Instant stageEndsAt,
            Instant pausedAt,
            long remainingSeconds
    ) {}
}
