package com.likelion.firstbite.firstbiteserver.feedback.dto;

import java.util.UUID;

public record SubmitFeedbackResponse(UUID feedbackId, UUID recordId, Integer sleepinessScore,
                                     long feedbackCount, boolean personalizationUpdated) {}
