package com.likelion.firstbite.firstbiteserver.coaching.dto;

import java.time.Instant;

public record UpdateCoachingTimerRequest(String action, Integer expectedStage, Instant occurredAt) {}
