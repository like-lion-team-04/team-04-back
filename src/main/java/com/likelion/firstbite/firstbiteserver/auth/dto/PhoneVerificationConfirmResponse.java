package com.likelion.firstbite.firstbiteserver.auth.dto;

public record PhoneVerificationConfirmResponse(String verificationToken, int expiresIn) {}
