package com.likelion.firstbite.firstbiteserver.auth.dto;

public record PhoneVerificationSendResponse(int expiresIn, int resendAfter) {}
