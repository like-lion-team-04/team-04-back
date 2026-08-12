package com.likelion.firstbite.firstbiteserver.auth.dto;

import com.likelion.firstbite.firstbiteserver.member.domain.Member;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SignUpResponse(
        UUID userId,
        String name,
        String email,
        String phoneNumber,
        LocalDate birthDate,
        boolean marketingAgreed,
        Instant createdAt
) {
    public static SignUpResponse from(Member member, String maskedPhoneNumber) {
        return new SignUpResponse(
                member.getId(),
                member.getName(),
                member.getEmail(),
                maskedPhoneNumber,
                member.getBirthDate(),
                member.isMarketingAgreed(),
                member.getCreatedAt()
        );
    }
}
