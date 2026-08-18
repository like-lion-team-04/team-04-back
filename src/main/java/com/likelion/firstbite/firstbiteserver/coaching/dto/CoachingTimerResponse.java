package com.likelion.firstbite.firstbiteserver.coaching.dto;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingSession;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingSessionStatus;

import java.time.Instant;
import java.util.UUID;

public record CoachingTimerResponse(
        UUID sessionId,
        CoachingSessionStatus status,
        int currentStage,
        Instant stageEndsAt,
        Instant pausedAt,
        long remainingSeconds
) {
    public static CoachingTimerResponse from(CoachingSession session, Instant now) {
        return new CoachingTimerResponse(session.getId(), session.getStatus(), session.getCurrentStage(),
                session.getStageEndsAt(), session.getPausedAt(), session.remainingSeconds(now));
    }
}
