package com.likelion.firstbite.firstbiteserver.feedback.service;

import com.likelion.firstbite.firstbiteserver.feedback.domain.*;
import com.likelion.firstbite.firstbiteserver.feedback.dto.PersonalizationResponse;
import com.likelion.firstbite.firstbiteserver.feedback.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonalizationService {
    public static final int ACTIVATION_COUNT = 3;
    private static final BigDecimal MIN = new BigDecimal("0.80");
    private static final BigDecimal MAX = new BigDecimal("1.20");
    private final CoachingFeedbackRepository feedbackRepository;
    private final PersonalizationProfileRepository profileRepository;

    @Transactional
    public PersonalizationProfile refresh(UUID memberId, Instant now) {
        int count = Math.toIntExact(feedbackRepository.countByMemberIdAndSkippedFalse(memberId));
        PersonalizationProfile profile = profileRepository.findById(memberId)
                .orElseGet(() -> PersonalizationProfile.initial(memberId, now));
        if (count < ACTIVATION_COUNT) {
            profile.update(count, null, PersonalizationDirection.STANDARD, now);
        } else {
            BigDecimal average = BigDecimal.valueOf(feedbackRepository.averageValidScore(memberId).orElse(3.0));
            BigDecimal coefficient = BigDecimal.ONE.add(new BigDecimal("3").subtract(average)
                    .multiply(new BigDecimal("0.10"))).setScale(2, RoundingMode.HALF_UP)
                    .max(MIN).min(MAX);
            profile.update(count, coefficient, direction(coefficient), now);
        }
        return profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public PersonalizationResponse get(UUID memberId) {
        long count = feedbackRepository.countByMemberIdAndSkippedFalse(memberId);
        var profile = profileRepository.findById(memberId).orElse(null);
        boolean enabled = count >= ACTIVATION_COUNT;
        BigDecimal coefficient = enabled && profile != null ? profile.getCoefficient() : null;
        PersonalizationDirection direction = enabled && profile != null
                ? profile.getDirection() : PersonalizationDirection.STANDARD;
        return new PersonalizationResponse(enabled, count, coefficient, direction.name(), message(enabled, direction));
    }

    private PersonalizationDirection direction(BigDecimal coefficient) {
        if (coefficient.compareTo(new BigDecimal("0.99")) < 0) return PersonalizationDirection.GENTLER;
        if (coefficient.compareTo(new BigDecimal("1.01")) > 0) return PersonalizationDirection.STRONGER;
        return PersonalizationDirection.STANDARD;
    }

    private String message(boolean enabled, PersonalizationDirection direction) {
        if (!enabled) return "아직 데이터가 부족해요. 피드백 3회부터 맞춤 안내가 시작돼요.";
        return switch (direction) {
            case GENTLER -> "일반 기준보다 약간 완만하게 안내 중이에요.";
            case STRONGER -> "일반 기준보다 조금 더 적극적으로 안내 중이에요.";
            case STANDARD -> "현재 일반 기준으로 안내 중이에요.";
        };
    }
}
