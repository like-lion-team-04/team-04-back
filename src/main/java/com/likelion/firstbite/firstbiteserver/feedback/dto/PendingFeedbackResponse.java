package com.likelion.firstbite.firstbiteserver.feedback.dto;

import java.time.Instant;
import java.util.UUID;

public record PendingFeedbackResponse(boolean pending, UUID recordId, String question, Scale scale, Instant expiresAt) {
    public static PendingFeedbackResponse none() { return new PendingFeedbackResponse(false, null, null, null, null); }
    public record Scale(int min, int max) {}
}
