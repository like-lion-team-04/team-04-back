package com.likelion.firstbite.firstbiteserver.feedback.dto;

import java.time.Instant;
import java.util.UUID;

public record FeedbackStatusResponse(
        Status status,
        UUID recordId,
        String question,
        Scale scale,
        Integer sleepinessScore,
        Instant answeredAt,
        Instant expiresAt
) {
    public enum Status { PENDING, ANSWERED, EXPIRED, NONE }
    public record Scale(int min, int max) {}

    public static FeedbackStatusResponse none() {
        return new FeedbackStatusResponse(Status.NONE, null, null, null, null, null, null);
    }
}
