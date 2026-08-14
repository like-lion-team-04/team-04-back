package com.likelion.firstbite.firstbiteserver.meal.dto;

import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateMealItemsResponse(UUID mealId, List<Item> items) {
    public static UpdateMealItemsResponse from(Meal meal) {
        return new UpdateMealItemsResponse(meal.getId(), meal.getItems().stream().map(Item::from).toList());
    }

    public record Item(UUID mealItemId, UUID foodId, String name, BigDecimal servingMultiplier) {
        static Item from(MealItem item) {
            return new Item(item.getId(), item.getFood().getId(), item.getFoodName(), item.getServingMultiplier());
        }
    }
}
