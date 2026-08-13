package com.likelion.firstbite.firstbiteserver.auth.dto;

import java.util.UUID;

public record PhoneVerificationSendResponse(
        UUID requestId, String recipientNumber, String messageText, String smsUri, int expiresIn
) {}
