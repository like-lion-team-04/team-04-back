package com.likelion.firstbite.firstbiteserver.feedback.dto;

import java.time.Instant;

public record SubmitFeedbackRequest(Integer sleepinessScore, Boolean skipped, Instant answeredAt) {}
