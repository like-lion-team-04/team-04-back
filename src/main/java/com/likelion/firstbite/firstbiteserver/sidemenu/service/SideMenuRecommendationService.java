package com.likelion.firstbite.firstbiteserver.sidemenu.service;

import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import com.likelion.firstbite.firstbiteserver.sidemenu.domain.NutrientFocus;
import com.likelion.firstbite.firstbiteserver.sidemenu.domain.SideMenu;
import com.likelion.firstbite.firstbiteserver.sidemenu.dto.SideMenuRecommendationsResponse;
import com.likelion.firstbite.firstbiteserver.sidemenu.repository.SideMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SideMenuRecommendationService {
    // 의료 권장량이 아닌, 식사 순서 코칭 단계를 구성하기 위한 내부 기준이다.
    static final BigDecimal PROTEIN_COACHING_TARGET_G = new BigDecimal("15");
    static final BigDecimal FIBER_COACHING_TARGET_G = new BigDecimal("5");
    private final MealRepository mealRepository;
    private final SideMenuRepository sideMenuRepository;

    @Transactional(readOnly = true)
    public SideMenuRecommendationsResponse recommend(UUID memberId, UUID mealId, int limit) {
        if (limit < 1 || limit > 3) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SIDE_MENU_LIMIT_INVALID", "limit은 1부터 3까지 가능합니다.");
        }
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEAL_NOT_FOUND", "식사를 찾을 수 없습니다."));
        if (!meal.belongsTo(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "MEAL_FORBIDDEN", "다른 사용자의 식사에 접근할 수 없습니다.");
        }
        if (meal.getItems().isEmpty()) {
            throw notNeeded("추천할 수 있는 식사 항목이 없습니다.");
        }

        BigDecimal protein = meal.getItems().stream()
                .map(item -> item.getFood().getProteinG().multiply(item.getServingMultiplier()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal fiber = meal.getItems().stream()
                .map(item -> item.getFood().getFiberG().multiply(item.getServingMultiplier()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean proteinNeeded = protein.compareTo(PROTEIN_COACHING_TARGET_G) < 0;
        boolean fiberNeeded = fiber.compareTo(FIBER_COACHING_TARGET_G) < 0;
        if (!proteinNeeded && !fiberNeeded) {
            throw notNeeded("현재 식사는 단백질과 식이섬유 보완이 필요하지 않습니다.");
        }

        Set<UUID> existingFoodIds = meal.getItems().stream()
                .map(item -> item.getFood().getId()).collect(Collectors.toSet());
        BigDecimal proteinDeficitRate = deficitRate(protein, PROTEIN_COACHING_TARGET_G);
        BigDecimal fiberDeficitRate = deficitRate(fiber, FIBER_COACHING_TARGET_G);

        var items = sideMenuRepository.findAllByActiveTrue().stream()
                .filter(side -> side.getFood().isActive())
                .filter(side -> !existingFoodIds.contains(side.getFood().getId()))
                .filter(side -> side.getNutrientFocus() == NutrientFocus.PROTEIN ? proteinNeeded : fiberNeeded)
                .sorted(Comparator
                        .comparing((SideMenu side) -> focusDeficit(side, proteinDeficitRate, fiberDeficitRate)).reversed()
                        .thenComparing(this::focusAmount, Comparator.reverseOrder())
                        .thenComparing(side -> side.getFood().getName()))
                .limit(limit)
                .map(this::toResponse)
                .toList();
        if (items.isEmpty()) {
            throw notNeeded("현재 추가할 수 있는 사이드 메뉴 후보가 없습니다.");
        }
        return new SideMenuRecommendationsResponse(items);
    }

    private BigDecimal deficitRate(BigDecimal current, BigDecimal target) {
        return target.subtract(current).max(BigDecimal.ZERO).divide(target, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal focusDeficit(SideMenu side, BigDecimal proteinDeficit, BigDecimal fiberDeficit) {
        return side.getNutrientFocus() == NutrientFocus.PROTEIN ? proteinDeficit : fiberDeficit;
    }

    private BigDecimal focusAmount(SideMenu side) {
        return side.getNutrientFocus() == NutrientFocus.PROTEIN
                ? side.getFood().getProteinG() : side.getFood().getFiberG();
    }

    private SideMenuRecommendationsResponse.Item toResponse(SideMenu side) {
        BigDecimal amount = focusAmount(side).stripTrailingZeros();
        String nutrient = side.getNutrientFocus() == NutrientFocus.PROTEIN ? "단백질" : "식이섬유";
        BigDecimal delta = side.getNutrientFocus().reliefDelta();
        return new SideMenuRecommendationsResponse.Item(side.getId(), side.getFood().getName(),
                side.getNutrientFocus(), nutrient + " " + amount.toPlainString() + "g을 보완해요.",
                delta, side.getEstimatedPrice());
    }

    private BusinessException notNeeded(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "SIDE_MENU_NOT_NEEDED", message);
    }
}
