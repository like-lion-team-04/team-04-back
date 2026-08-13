package com.likelion.firstbite.firstbiteserver.auth.dto;

import java.util.UUID;

public record LoginResponse(UUID userId, String name, String tokenType, String accessToken, long expiresIn) {}
