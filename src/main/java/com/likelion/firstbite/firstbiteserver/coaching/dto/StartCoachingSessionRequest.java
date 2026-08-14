package com.likelion.firstbite.firstbiteserver.coaching.dto;

import java.util.UUID;

public record StartCoachingSessionRequest(UUID mealId, Integer planVersion) {}
