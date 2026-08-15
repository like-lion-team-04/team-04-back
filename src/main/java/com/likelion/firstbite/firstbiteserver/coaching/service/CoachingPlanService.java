package com.likelion.firstbite.firstbiteserver.coaching.service;

import com.likelion.firstbite.firstbiteserver.analysis.repository.MealAnalysisRepository;
import com.likelion.firstbite.firstbiteserver.coaching.dto.CoachingPlanResponse;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.food.domain.Food;
import com.likelion.firstbite.firstbiteserver.food.domain.FoodCategory;
import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealItem;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealStatus;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoachingPlanService {
    private static final int STAGE_INTERVAL_SECONDS = 300;
    private static final BigDecimal PROTEIN_STAGE_MIN_G = new BigDecimal("5");
    private static final BigDecimal FIBER_STAGE_MIN_G = new BigDecimal("2");
    private final MealRepository mealRepository;
    private final MealAnalysisRepository analysisRepository;

    @Transactional(readOnly = true)
    public CoachingPlanResponse getPlan(UUID memberId, UUID mealId) {
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEAL_NOT_FOUND", "식사를 찾을 수 없습니다."));
        if (!meal.belongsTo(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "MEAL_FORBIDDEN", "다른 사용자의 식사에 접근할 수 없습니다.");
        }
        if (meal.getStatus() != MealStatus.ANALYZED
                || analysisRepository.findFirstByMealIdOrderByCreatedAtDesc(mealId).isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, "ANALYSIS_REQUIRED", "부담 분석을 먼저 완료해 주세요.");
        }
        if (meal.getItems().isEmpty()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "COACHING_PLAN_UNAVAILABLE",
                    "코칭 단계를 구성할 식사 항목이 없습니다.");
        }

        List<MealItem> proteinItems = new ArrayList<>();
        List<MealItem> fiberItems = new ArrayList<>();
        List<MealItem> carbohydrateItems = new ArrayList<>();
        for (MealItem item : meal.getItems()) {
            switch (classify(item.getFood())) {
                case PROTEIN -> proteinItems.add(item);
                case FIBER -> fiberItems.add(item);
                case CARBOHYDRATE -> carbohydrateItems.add(item);
            }
        }

        List<StageDraft> drafts = new ArrayList<>();
        addIfPresent(drafts, "단백질 음식부터", proteinItems);
        addIfPresent(drafts, "채소·식이섬유 반찬", fiberItems);
        addIfPresent(drafts, "밥·면", carbohydrateItems);
        if (drafts.isEmpty()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "COACHING_PLAN_UNAVAILABLE",
                    "현재 메뉴로 코칭 단계를 구성할 수 없습니다.");
        }

        List<CoachingPlanResponse.Stage> stages = new ArrayList<>();
        for (int index = 0; index < drafts.size(); index++) {
            StageDraft draft = drafts.get(index);
            Integer seconds = index == drafts.size() - 1 ? null : STAGE_INTERVAL_SECONDS;
            stages.add(new CoachingPlanResponse.Stage(index + 1, draft.title(), draft.itemIds(), seconds));
        }
        int version = meal.getCoachingPlanVersion();
        UUID planId = UUID.nameUUIDFromBytes((mealId + ":" + version).getBytes(StandardCharsets.UTF_8));
        return new CoachingPlanResponse(planId, version, CoachingPlanResponse.RuleType.PROTEIN_FIRST,
                stages, CoachingPlanResponse.GuideTone.NON_RESTRICTIVE);
    }

    private StageType classify(Food food) {
        BigDecimal protein = food.getProteinG();
        BigDecimal fiber = food.getFiberG();
        BigDecimal availableCarb = food.getAvailableCarbG();
        if (isStapleCarbohydrate(food.getSearchCategory())) {
            return StageType.CARBOHYDRATE;
        }
        if (protein.compareTo(PROTEIN_STAGE_MIN_G) >= 0
                && protein.compareTo(fiber) >= 0) {
            return StageType.PROTEIN;
        }
        if (availableCarb.compareTo(protein.multiply(new BigDecimal("2"))) > 0
                && fiber.compareTo(FIBER_STAGE_MIN_G) < 0) {
            return StageType.CARBOHYDRATE;
        }
        if (fiber.compareTo(FIBER_STAGE_MIN_G) >= 0) {
            return StageType.FIBER;
        }
        return availableCarb.signum() > 0 ? StageType.CARBOHYDRATE : StageType.PROTEIN;
    }

    private boolean isStapleCarbohydrate(FoodCategory category) {
        return category == FoodCategory.RICE || category == FoodCategory.NOODLE
                || category == FoodCategory.BUNSIK || category == FoodCategory.RICE_BOWL
                || category == FoodCategory.BREAD;
    }

    private void addIfPresent(List<StageDraft> drafts, String title, List<MealItem> items) {
        if (!items.isEmpty()) {
            drafts.add(new StageDraft(title, items.stream().map(MealItem::getId).toList()));
        }
    }

    private enum StageType { PROTEIN, FIBER, CARBOHYDRATE }
    private record StageDraft(String title, List<UUID> itemIds) {}
}
