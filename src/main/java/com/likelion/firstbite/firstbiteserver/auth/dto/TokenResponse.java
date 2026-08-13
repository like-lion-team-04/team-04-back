package com.likelion.firstbite.firstbiteserver.auth.dto;

public record TokenResponse(String tokenType, String accessToken, long expiresIn) {
}
