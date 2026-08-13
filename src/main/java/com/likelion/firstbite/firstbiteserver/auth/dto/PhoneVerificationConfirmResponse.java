package com.likelion.firstbite.firstbiteserver.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PhoneVerificationConfirmResponse(String status, String verificationToken, Integer expiresIn) {
    public static PhoneVerificationConfirmResponse verified(String token) {
        return new PhoneVerificationConfirmResponse("VERIFIED", token, 600);
    }

    public static PhoneVerificationConfirmResponse pending() {
        return new PhoneVerificationConfirmResponse("PENDING", null, null);
    }
}
