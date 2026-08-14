package com.likelion.firstbite.firstbiteserver.coaching.dto;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingRecord;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingSessionStatus;

import java.util.UUID;

public record CompleteCoachingSessionResponse(
        UUID recordId,
        UUID sessionId,
        CoachingSessionStatus status,
        Summary summary
) {
    public static CompleteCoachingSessionResponse from(CoachingRecord record) {
        return new CompleteCoachingSessionResponse(record.getId(), record.getSessionId(),
                CoachingSessionStatus.COMPLETED,
                new Summary(record.getCompletedStages(), record.getSkippedStages(), record.getTotalSeconds()));
    }

    public record Summary(int completedStages, int skippedStages, long totalSeconds) {}
}
