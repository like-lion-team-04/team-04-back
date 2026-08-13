package com.likelion.firstbite.firstbiteserver.member.dto;

import com.likelion.firstbite.firstbiteserver.member.domain.MemberStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AccountMeResponse(
        UUID userId,
        String name,
        String email,
        String phoneNumber,
        LocalDate birthDate,
        boolean marketingAgreed,
        MemberStatus status,
        PersonalizationSummary personalization,
        Instant createdAt
) {
    public record PersonalizationSummary(boolean enabled, long feedbackCount) {
    }
}
