package com.likelion.firstbite.firstbiteserver.coaching.dto;

import java.time.Instant;

public record CompleteCoachingSessionRequest(String reason, Instant endedAt) {}
