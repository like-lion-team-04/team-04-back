package com.likelion.firstbite.firstbiteserver.auth.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PhoneVerificationConfirmRequest(
        @NotNull(message = "인증 요청 ID는 필수입니다.") UUID requestId
) {}
