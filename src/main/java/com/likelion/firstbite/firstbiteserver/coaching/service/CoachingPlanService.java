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
import java.util.concurrent.atomic.AtomicInteger;

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
        addIfPresent(drafts, "단백질 음식부터", StageType.PROTEIN, proteinItems);
        addIfPresent(drafts, "채소·식이섬유 반찬", StageType.FIBER, fiberItems);
        addIfPresent(drafts, "밥·면", StageType.CARBOHYDRATE, carbohydrateItems);
        if (drafts.isEmpty()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "COACHING_PLAN_UNAVAILABLE",
                    "현재 메뉴로 코칭 단계를 구성할 수 없습니다.");
        }

        List<CoachingPlanResponse.Stage> stages = new ArrayList<>();
        for (int index = 0; index < drafts.size(); index++) {
            StageDraft draft = drafts.get(index);
            Integer seconds = index == drafts.size() - 1 ? null : STAGE_INTERVAL_SECONDS;
            List<CoachingPlanResponse.Item> items = draft.items().stream().map(this::toItem).toList();
            stages.add(new CoachingPlanResponse.Stage(index + 1, draft.title(),
                    draft.items().stream().map(MealItem::getId).toList(), seconds,
                    summary(draft.type(), draft.items()), guide(draft.type()), items));
        }
        AtomicInteger order = new AtomicInteger(1);
        List<CoachingPlanResponse.RecommendedOrderItem> recommendedOrder = stages.stream()
                .flatMap(stage -> stage.items().stream().map(item -> new CoachingPlanResponse.RecommendedOrderItem(
                        order.getAndIncrement(), stage.stage(), item.mealItemId(), item.foodId(), item.name(),
                        item.imageUrl(), item.servingMultiplier(), item.gi(), item.giDataQuality())))
                .toList();
        int version = meal.getCoachingPlanVersion();
        UUID planId = UUID.nameUUIDFromBytes((mealId + ":" + version).getBytes(StandardCharsets.UTF_8));
        return new CoachingPlanResponse(planId, version, CoachingPlanResponse.RuleType.PROTEIN_FIRST,
                stages, recommendedOrder, CoachingPlanResponse.GuideTone.NON_RESTRICTIVE);
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

    private void addIfPresent(List<StageDraft> drafts, String title, StageType type, List<MealItem> items) {
        if (!items.isEmpty()) {
            drafts.add(new StageDraft(title, type, List.copyOf(items)));
        }
    }

    private CoachingPlanResponse.Item toItem(MealItem item) {
        Food food = item.getFood();
        BigDecimal multiplier = item.getServingMultiplier();
        return new CoachingPlanResponse.Item(item.getId(), food.getId(), item.getFoodName(), food.getImageUrl(),
                food.getServingDescription(), multiplier, food.getCarbG().multiply(multiplier),
                food.getFiberG().multiply(multiplier), food.getProteinG().multiply(multiplier),
                food.getFatG().multiply(multiplier), food.getCalorieKcal().multiply(multiplier),
                food.getGi(), food.getGiDataQuality().name(), item.isEstimated());
    }

    private CoachingPlanResponse.StageSummary summary(StageType type, List<MealItem> items) {
        BigDecimal nutrient = items.stream().map(item -> switch (type) {
                    case PROTEIN -> item.getFood().getProteinG().multiply(item.getServingMultiplier());
                    case FIBER -> item.getFood().getFiberG().multiply(item.getServingMultiplier());
                    case CARBOHYDRATE -> item.getFood().getAvailableCarbG().multiply(item.getServingMultiplier());
                }).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal calories = items.stream().map(item -> item.getFood().getCalorieKcal()
                        .multiply(item.getServingMultiplier())).reduce(BigDecimal.ZERO, BigDecimal::add);
        int price = items.stream().filter(item -> item.getSideMenu() != null)
                .mapToInt(item -> item.getSideMenu().getEstimatedPrice()).sum();
        String nutrientName = switch (type) {
            case PROTEIN -> "PROTEIN";
            case FIBER -> "FIBER";
            case CARBOHYDRATE -> "AVAILABLE_CARBOHYDRATE";
        };
        return new CoachingPlanResponse.StageSummary(nutrientName, nutrient, calories, price == 0 ? null : price);
    }

    private String guide(StageType type) {
        return switch (type) {
            case PROTEIN -> "단백질 음식부터 천천히 드셔보세요.";
            case FIBER -> "채소와 식이섬유 반찬을 이어서 드셔보세요.";
            case CARBOHYDRATE -> "밥·면 음식은 마지막에 편하게 드세요.";
        };
    }

    private enum StageType { PROTEIN, FIBER, CARBOHYDRATE }
    private record StageDraft(String title, StageType type, List<MealItem> items) {}
}
