package com.likelion.firstbite.firstbiteserver.meal.dto;

import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealItem;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateMealResponse(UUID mealId, MealStatus status, List<Item> items) {
    public static CreateMealResponse from(Meal meal) {
        return new CreateMealResponse(meal.getId(), meal.getStatus(), meal.getItems().stream().map(Item::from).toList());
    }

    public record Item(UUID mealItemId, UUID foodId, String name, BigDecimal servingMultiplier, boolean estimated) {
        static Item from(MealItem item) {
            return new Item(item.getId(), item.getFood().getId(), item.getFoodName(), item.getServingMultiplier(), item.isEstimated());
        }
    }
}
