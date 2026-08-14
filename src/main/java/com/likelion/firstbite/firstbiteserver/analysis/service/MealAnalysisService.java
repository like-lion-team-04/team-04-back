package com.likelion.firstbite.firstbiteserver.analysis.service;

import com.likelion.firstbite.firstbiteserver.analysis.domain.MealAnalysis;
import com.likelion.firstbite.firstbiteserver.analysis.dto.AnalysisResponse;
import com.likelion.firstbite.firstbiteserver.analysis.dto.AnalysisDetailResponse;
import com.likelion.firstbite.firstbiteserver.analysis.dto.CreateAnalysisRequest;
import com.likelion.firstbite.firstbiteserver.analysis.repository.MealAnalysisRepository;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealItem;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MealAnalysisService {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal DEFAULT_RELIEF_RATE = new BigDecimal("0.0700");
    private static final BigDecimal MAX_RELIEF_RATE = new BigDecimal("0.3000");
    private static final BigDecimal NEUTRAL_PERSONAL_COEFFICIENT = new BigDecimal("1.0000");
    private final MealRepository mealRepository;
    private final MealAnalysisRepository analysisRepository;

    @Transactional
    public AnalysisResponse analyze(UUID memberId, UUID mealId, UUID idempotencyKey,
                                    CreateAnalysisRequest request) {
        if (idempotencyKey == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key가 필요합니다.");
        }
        boolean usePersonalization = request == null || request.personalizationEnabled();
        String requestHash = hash(mealId + ":" + usePersonalization);
        var existing = analysisRepository
                .findFirstByMemberIdAndIdempotencyKeyAndCreatedAtAfterOrderByCreatedAtDesc(
                        memberId, idempotencyKey, Instant.now().minus(Duration.ofHours(24)));
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new BusinessException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT",
                        "동일한 Idempotency-Key를 다른 요청에 사용할 수 없습니다.");
            }
            return AnalysisResponse.from(existing.get());
        }

        Meal meal = mealRepository.findByIdForUpdate(mealId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEAL_NOT_FOUND", "식사를 찾을 수 없습니다."));
        if (!meal.belongsTo(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "MEAL_FORBIDDEN", "다른 사용자의 식사에 접근할 수 없습니다.");
        }
        if (meal.getItems().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "MEAL_EMPTY", "분석할 메뉴가 없습니다.");
        }
        MealAnalysis analysis = calculateAndSave(meal, memberId, idempotencyKey, requestHash);
        meal.markAnalyzed();
        return AnalysisResponse.from(analysis);
    }

    @Transactional
    public AnalysisResponse recalculateAfterMealChange(UUID memberId, Meal meal) {
        if (!meal.belongsTo(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "MEAL_FORBIDDEN", "다른 사용자의 식사에 접근할 수 없습니다.");
        }
        UUID recalculationKey = UUID.randomUUID();
        MealAnalysis analysis = calculateAndSave(meal, memberId, recalculationKey,
                hash(meal.getId() + ":side-menu:" + recalculationKey));
        meal.markAnalyzed();
        return AnalysisResponse.from(analysis);
    }

    private MealAnalysis calculateAndSave(Meal meal, UUID memberId, UUID idempotencyKey, String requestHash) {
        validateNutrition(meal);
        BigDecimal baselineGl = meal.getItems().stream().map(this::calculateItemGl)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal reliefRate = calculateReliefRate(meal);
        BigDecimal recommendedGl = baselineGl.multiply(BigDecimal.ONE.subtract(reliefRate))
                .setScale(2, RoundingMode.HALF_UP);
        long estimatedCount = meal.getItems().stream().filter(MealItem::isEstimated).count();
        BigDecimal estimatedRatio = BigDecimal.valueOf(estimatedCount)
                .divide(BigDecimal.valueOf(meal.getItems().size()), 4, RoundingMode.HALF_UP);
        return analysisRepository.save(MealAnalysis.create(meal.getId(), memberId, idempotencyKey,
                requestHash, baselineGl, recommendedGl, reliefRate, NEUTRAL_PERSONAL_COEFFICIENT, estimatedRatio));
    }

    private BigDecimal calculateReliefRate(Meal meal) {
        BigDecimal base = hasOrderablePreload(meal) ? DEFAULT_RELIEF_RATE : BigDecimal.ZERO;
        BigDecimal sideDelta = meal.getItems().stream()
                .filter(item -> item.getSideMenu() != null)
                .map(item -> item.getSideMenu().getNutrientFocus().reliefDelta().multiply(item.getServingMultiplier()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return base.add(sideDelta).min(MAX_RELIEF_RATE).setScale(4, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public AnalysisDetailResponse getLatest(UUID memberId, UUID mealId) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEAL_NOT_FOUND", "식사를 찾을 수 없습니다."));
        if (!meal.belongsTo(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "MEAL_FORBIDDEN", "다른 사용자의 식사에 접근할 수 없습니다.");
        }
        MealAnalysis analysis = analysisRepository.findFirstByMealIdOrderByCreatedAtDesc(mealId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ANALYSIS_NOT_FOUND",
                        "부담 분석 결과를 찾을 수 없습니다."));
        return AnalysisDetailResponse.from(analysis);
    }

    private BigDecimal calculateItemGl(MealItem item) {
        return item.getFood().getGi()
                .multiply(item.getFood().getAvailableCarbG())
                .multiply(item.getServingMultiplier())
                .divide(ONE_HUNDRED, 6, RoundingMode.HALF_UP);
    }

    private boolean hasOrderablePreload(Meal meal) {
        boolean hasCarbohydrate = meal.getItems().stream()
                .anyMatch(item -> item.getFood().getAvailableCarbG().signum() > 0);
        boolean hasProteinOrFiber = meal.getItems().stream().anyMatch(item ->
                item.getFood().getProteinG().signum() > 0 || item.getFood().getFiberG().signum() > 0);
        return hasCarbohydrate && hasProteinOrFiber;
    }

    private void validateNutrition(Meal meal) {
        boolean invalid = meal.getItems().stream().anyMatch(item -> item.getFood().getGi() == null
                || item.getFood().getAvailableCarbG() == null
                || item.getFood().getProteinG() == null
                || item.getFood().getFiberG() == null
                || item.getFood().getGi().signum() < 0
                || item.getFood().getAvailableCarbG().signum() < 0);
        if (invalid) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "NUTRITION_DATA_INSUFFICIENT",
                    "GL 계산에 필요한 영양정보가 부족합니다.");
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
