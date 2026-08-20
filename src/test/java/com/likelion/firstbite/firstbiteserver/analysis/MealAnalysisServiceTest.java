package com.likelion.firstbite.firstbiteserver.analysis;

import com.likelion.firstbite.firstbiteserver.analysis.domain.MealAnalysis;
import com.likelion.firstbite.firstbiteserver.analysis.dto.AnalysisResponse;
import com.likelion.firstbite.firstbiteserver.analysis.repository.MealAnalysisRepository;
import com.likelion.firstbite.firstbiteserver.analysis.service.MealAnalysisService;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.feedback.repository.PersonalizationProfileRepository;
import com.likelion.firstbite.firstbiteserver.food.domain.DataQuality;
import com.likelion.firstbite.firstbiteserver.food.domain.Food;
import com.likelion.firstbite.firstbiteserver.food.domain.FoodCategory;
import com.likelion.firstbite.firstbiteserver.food.domain.ServingUnit;
import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealItem;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealSource;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * 부담(GL) 분석 계산 로직 단위 테스트. 통합 테스트가 다루지 않는 계산 분기를 검증한다.
 * - 순서 가능한 프리로드(탄수화물 + 단백질/식이섬유)가 있을 때만 기본 감소율(0.07) 적용
 * - 프리로드가 없으면 감소율 0 → 추천 부담 == 기존 부담
 * - GL 계산식: gi * availableCarb * servingMultiplier / 100
 * - 영양정보 부족(음수/누락) 시 422 NUTRITION_DATA_INSUFFICIENT
 */
@ExtendWith(MockitoExtension.class)
class MealAnalysisServiceTest {
    @Mock MealRepository mealRepository;
    @Mock MealAnalysisRepository analysisRepository;
    @Mock PersonalizationProfileRepository personalizationProfileRepository;
    @InjectMocks MealAnalysisService service;

    private final UUID memberId = UUID.randomUUID();
    private final UUID mealId = UUID.randomUUID();
    private final UUID idempotencyKey = UUID.randomUUID();

    @Test
    void appliesDefaultReliefWhenMealHasOrderablePreload() {
        // gi=70, carb=50, fiber=2 → availableCarb=48, protein=5(프리로드 존재), mult=1
        Meal meal = mealWith(food(new BigDecimal("70"), new BigDecimal("50"), new BigDecimal("2"),
                new BigDecimal("5"), DataQuality.MEASURED), BigDecimal.ONE);
        stubCommon(meal);

        AnalysisResponse response = service.analyze(memberId, mealId, idempotencyKey, null);

        // baseline = 70 * 48 * 1 / 100 = 33.60
        assertThat(response.baselineGl()).isEqualByComparingTo("33.60");
        assertThat(response.reliefRate()).isEqualByComparingTo("0.0700");
        assertThat(response.personalCoefficient()).isEqualByComparingTo("1.0000");
        // recommended = 33.60 * (1 - 0.07) = 31.248 → 31.25
        assertThat(response.recommendedGl()).isEqualByComparingTo("31.25");
    }

    @Test
    void appliesNoReliefWhenMealHasNoProteinOrFiber() {
        // 탄수화물만 있고 단백질/식이섬유가 없으면 순서 조정 여지가 없어 감소율 0
        Meal meal = mealWith(food(new BigDecimal("70"), new BigDecimal("50"), BigDecimal.ZERO,
                BigDecimal.ZERO, DataQuality.MEASURED), BigDecimal.ONE);
        stubCommon(meal);

        AnalysisResponse response = service.analyze(memberId, mealId, idempotencyKey, null);

        // availableCarb = 50, baseline = 70 * 50 / 100 = 35.00
        assertThat(response.baselineGl()).isEqualByComparingTo("35.00");
        assertThat(response.reliefRate()).isEqualByComparingTo("0.0000");
        assertThat(response.recommendedGl()).isEqualByComparingTo("35.00");
    }

    @Test
    void scalesGlWithServingMultiplier() {
        Meal meal = mealWith(food(new BigDecimal("70"), new BigDecimal("50"), new BigDecimal("2"),
                new BigDecimal("5"), DataQuality.MEASURED), new BigDecimal("1.5"));
        stubCommon(meal);

        AnalysisResponse response = service.analyze(memberId, mealId, idempotencyKey, null);

        // baseline = 70 * 48 * 1.5 / 100 = 50.40
        assertThat(response.baselineGl()).isEqualByComparingTo("50.40");
    }

    @Test
    void rejectsMealWithInsufficientNutrition() {
        // 음수 GI → 영양정보 부족
        Meal meal = mealWith(food(new BigDecimal("-1"), new BigDecimal("50"), new BigDecimal("2"),
                new BigDecimal("5"), DataQuality.MEASURED), BigDecimal.ONE);
        stubCommon(meal);

        assertThatThrownBy(() -> service.analyze(memberId, mealId, idempotencyKey, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode()).isEqualTo("NUTRITION_DATA_INSUFFICIENT"));
    }

    @Test
    void rejectsAccessToAnotherMembersMeal() {
        Meal meal = mealOfMember(UUID.randomUUID());
        when(mealRepository.findByIdForUpdate(mealId)).thenReturn(Optional.of(meal));
        when(analysisRepository.findFirstByMemberIdAndIdempotencyKeyAndCreatedAtAfterOrderByCreatedAtDesc(
                any(), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analyze(memberId, mealId, idempotencyKey, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode()).isEqualTo("MEAL_FORBIDDEN"));
    }

    @Test
    void rejectsMissingIdempotencyKey() {
        assertThatThrownBy(() -> service.analyze(memberId, mealId, null, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception ->
                        assertThat(((BusinessException) exception).getCode()).isEqualTo("IDEMPOTENCY_KEY_REQUIRED"));
    }

    private void stubCommon(Meal meal) {
        when(analysisRepository.findFirstByMemberIdAndIdempotencyKeyAndCreatedAtAfterOrderByCreatedAtDesc(
                any(), any(), any())).thenReturn(Optional.empty());
        when(mealRepository.findByIdForUpdate(mealId)).thenReturn(Optional.of(meal));
        lenient().when(personalizationProfileRepository.findById(memberId)).thenReturn(Optional.empty());
        lenient().when(analysisRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Meal mealWith(Food food, BigDecimal multiplier) {
        Meal meal = Meal.draft(memberId, MealSource.MANUAL, null);
        meal.addItem(MealItem.from(food, multiplier));
        return meal;
    }

    private Meal mealOfMember(UUID owner) {
        Meal meal = Meal.draft(owner, MealSource.MANUAL, null);
        meal.addItem(MealItem.from(food(new BigDecimal("70"), new BigDecimal("50"), new BigDecimal("2"),
                new BigDecimal("5"), DataQuality.MEASURED), BigDecimal.ONE));
        return meal;
    }

    private Food food(BigDecimal gi, BigDecimal carb, BigDecimal fiber, BigDecimal protein, DataQuality quality) {
        Food food = Food.create(UUID.randomUUID(), "code-" + UUID.randomUUID(), "테스트음식", "테스트",
                FoodCategory.OTHER, "ㅌㅅ", "1인분", BigDecimal.ONE, ServingUnit.COUNT, gi, quality, quality);
        food.updateNutrition(carb, fiber, protein, BigDecimal.ZERO, new BigDecimal("100"));
        return food;
    }
}
