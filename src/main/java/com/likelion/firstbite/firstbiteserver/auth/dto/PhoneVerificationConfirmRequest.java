package com.likelion.firstbite.firstbiteserver.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PhoneVerificationConfirmRequest(
        @NotBlank(message = "휴대폰 번호는 필수입니다.") String phoneNumber,
        @NotBlank(message = "인증번호는 필수입니다.")
        @Pattern(regexp = "\\d{6}", message = "인증번호는 6자리 숫자여야 합니다.") String code
) {}
