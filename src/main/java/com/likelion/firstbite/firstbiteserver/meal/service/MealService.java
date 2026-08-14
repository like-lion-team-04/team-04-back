package com.likelion.firstbite.firstbiteserver.meal.service;

import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.food.domain.Food;
import com.likelion.firstbite.firstbiteserver.food.repository.FoodRepository;
import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealItem;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealStatus;
import com.likelion.firstbite.firstbiteserver.meal.dto.CreateMealRequest;
import com.likelion.firstbite.firstbiteserver.meal.dto.CreateMealResponse;
import com.likelion.firstbite.firstbiteserver.meal.dto.UpdateMealItemsRequest;
import com.likelion.firstbite.firstbiteserver.meal.dto.UpdateMealItemsResponse;
import jakarta.persistence.EntityManager;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MealService {
    private static final Set<BigDecimal> ALLOWED_MULTIPLIERS = Set.of(
            new BigDecimal("0.5"), new BigDecimal("1.0"), new BigDecimal("1.5"), new BigDecimal("2.0"));
    private final MealRepository mealRepository;
    private final FoodRepository foodRepository;
    private final EntityManager entityManager;

    @Transactional
    public CreateMealResponse createDraft(UUID memberId, CreateMealRequest request) {
        validate(request);
        List<UUID> foodIds = request.items().stream().map(CreateMealRequest.Item::foodId).distinct().toList();
        List<Food> foods = foodRepository.findAllById(foodIds);
        if (foods.size() != foodIds.size() || foods.stream().anyMatch(food -> !food.isActive())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "FOOD_NOT_FOUND", "존재하지 않는 음식이 포함되어 있습니다.");
        }

        var foodById = foods.stream().collect(java.util.stream.Collectors.toMap(Food::getId, food -> food));
        Meal meal = Meal.draft(memberId, request.source(), request.recognitionId());
        request.items().forEach(item -> meal.addItem(MealItem.from(foodById.get(item.foodId()), item.servingMultiplier())));
        return CreateMealResponse.from(mealRepository.save(meal));
    }

    @Transactional
    public UpdateMealItemsResponse replaceItems(UUID memberId, UUID mealId, UpdateMealItemsRequest request) {
        Meal meal = mealRepository.findByIdForUpdate(mealId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEAL_NOT_FOUND", "식사를 찾을 수 없습니다."));
        if (!meal.belongsTo(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "MEAL_FORBIDDEN", "다른 사용자의 식사는 수정할 수 없습니다.");
        }
        if (meal.getStatus() != MealStatus.DRAFT) {
            throw new BusinessException(HttpStatus.CONFLICT, "MEAL_ALREADY_CONFIRMED", "이미 확정된 식사입니다.");
        }
        validateItems(request == null ? null : request.items());

        List<UUID> foodIds = request.items().stream().map(UpdateMealItemsRequest.Item::foodId).distinct().toList();
        var foodById = loadActiveFoods(foodIds);

        meal.clearItems();
        entityManager.flush();
        request.items().forEach(item -> meal.addItem(MealItem.from(foodById.get(item.foodId()), item.servingMultiplier())));
        return UpdateMealItemsResponse.from(mealRepository.save(meal));
    }

    private void validate(CreateMealRequest request) {
        if (request == null || request.source() == null || request.items() == null) {
            throw invalidItems();
        }
        validateItems(request.items().stream()
                .map(item -> item == null ? null : new UpdateMealItemsRequest.Item(item.foodId(), item.servingMultiplier()))
                .toList());
        boolean recognitionMismatch = request.source() == com.likelion.firstbite.firstbiteserver.meal.domain.MealSource.IMAGE
                ? request.recognitionId() == null : request.recognitionId() != null;
        if (recognitionMismatch) {
            throw invalidItems();
        }
    }

    private void validateItems(List<UpdateMealItemsRequest.Item> items) {
        if (items == null || items.isEmpty() || items.size() > 20
                || items.stream().anyMatch(item -> item == null || item.foodId() == null || item.servingMultiplier() == null)
                || items.stream().anyMatch(item -> ALLOWED_MULTIPLIERS.stream()
                .noneMatch(allowed -> allowed.compareTo(item.servingMultiplier()) == 0))
                || items.stream().map(UpdateMealItemsRequest.Item::foodId).distinct().count() != items.size()) {
            throw invalidItems();
        }
    }

    private java.util.Map<UUID, Food> loadActiveFoods(List<UUID> foodIds) {
        List<Food> foods = foodRepository.findAllById(foodIds);
        if (foods.size() != foodIds.size() || foods.stream().anyMatch(food -> !food.isActive())) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "FOOD_NOT_FOUND", "존재하지 않는 음식이 포함되어 있습니다.");
        }
        return foods.stream().collect(java.util.stream.Collectors.toMap(Food::getId, food -> food));
    }

    private BusinessException invalidItems() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "MEAL_ITEMS_INVALID", "식사 항목 또는 인분 배수가 올바르지 않습니다.");
    }
}
