package com.likelion.firstbite.firstbiteserver.sidemenu.service;

import com.likelion.firstbite.firstbiteserver.analysis.service.MealAnalysisService;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealItem;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import com.likelion.firstbite.firstbiteserver.sidemenu.dto.AddSideMenuRequest;
import com.likelion.firstbite.firstbiteserver.sidemenu.dto.AddSideMenuResponse;
import com.likelion.firstbite.firstbiteserver.sidemenu.dto.RemoveSideMenuResponse;
import com.likelion.firstbite.firstbiteserver.sidemenu.repository.SideMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MealSideMenuService {
    private static final Set<BigDecimal> ALLOWED_MULTIPLIERS = Set.of(
            new BigDecimal("0.5"), new BigDecimal("1.0"), new BigDecimal("1.5"), new BigDecimal("2.0"));
    private final MealRepository mealRepository;
    private final SideMenuRepository sideMenuRepository;
    private final MealAnalysisService analysisService;

    @Transactional
    public AddSideMenuResponse add(UUID memberId, UUID mealId, AddSideMenuRequest request) {
        if (request == null || request.sideMenuId() == null) {
            throw invalidRequest();
        }
        BigDecimal multiplier = request.effectiveServingMultiplier();
        if (ALLOWED_MULTIPLIERS.stream().noneMatch(value -> value.compareTo(multiplier) == 0)) {
            throw invalidRequest();
        }
        Meal meal = mealRepository.findByIdForUpdate(mealId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEAL_NOT_FOUND", "식사를 찾을 수 없습니다."));
        if (!meal.belongsTo(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "MEAL_FORBIDDEN", "다른 사용자의 식사에 접근할 수 없습니다.");
        }
        if (!meal.isSideMenuEditable()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "MEAL_NOT_EDITABLE",
                    "현재 상태에서는 식사를 수정할 수 없습니다.");
        }
        var sideMenu = sideMenuRepository.findByIdAndActiveTrue(request.sideMenuId())
                .filter(side -> side.getFood().isActive())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "SIDE_MENU_NOT_FOUND",
                        "활성 사이드 메뉴를 찾을 수 없습니다."));
        boolean duplicated = meal.getItems().stream().anyMatch(item ->
                item.getFood().getId().equals(sideMenu.getFood().getId()) ||
                        (item.getSideMenu() != null && item.getSideMenu().getId().equals(sideMenu.getId())));
        if (duplicated) {
            throw new BusinessException(HttpStatus.CONFLICT, "SIDE_MENU_ALREADY_ADDED", "이미 추가된 사이드 메뉴입니다.");
        }

        MealItem added = MealItem.fromSideMenu(sideMenu, multiplier);
        meal.addSideMenu(added);
        mealRepository.saveAndFlush(meal);
        var analysis = analysisService.recalculateAfterMealChange(memberId, meal);
        return new AddSideMenuResponse(meal.getId(),
                new AddSideMenuResponse.AddedItem(added.getId(), added.getFoodName()),
                new AddSideMenuResponse.AnalysisSummary(analysis.reliefRate()), meal.getCoachingPlanVersion());
    }

    @Transactional
    public RemoveSideMenuResponse remove(UUID memberId, UUID mealId, UUID sideMenuId) {
        Meal meal = mealRepository.findByIdForUpdate(mealId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEAL_NOT_FOUND", "식사를 찾을 수 없습니다."));
        if (!meal.belongsTo(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "MEAL_FORBIDDEN", "다른 사용자의 식사에 접근할 수 없습니다.");
        }
        if (!meal.isSideMenuEditable()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "MEAL_NOT_EDITABLE",
                    "현재 상태에서는 식사를 수정할 수 없습니다.");
        }
        if (!meal.removeSideMenu(sideMenuId)) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "SIDE_MENU_NOT_IN_MEAL",
                    "현재 식사에 추가된 사이드 메뉴가 아닙니다.");
        }

        mealRepository.saveAndFlush(meal);
        var analysis = analysisService.recalculateAfterMealChange(memberId, meal);
        return new RemoveSideMenuResponse(meal.getId(), sideMenuId,
                new RemoveSideMenuResponse.AnalysisSummary(analysis.reliefRate()), meal.getCoachingPlanVersion());
    }

    private BusinessException invalidRequest() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "SIDE_MENU_REQUEST_INVALID",
                "사이드 메뉴 ID 또는 인분 배수가 올바르지 않습니다.");
    }
}
