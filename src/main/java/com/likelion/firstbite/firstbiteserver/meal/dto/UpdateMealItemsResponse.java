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

    public record Item(UUID mealItemId, UUID foodId, String name, BigDecimal servingMultiplier,
                       BigDecimal carbohydrateG, BigDecimal fiberG, BigDecimal proteinG, BigDecimal fatG,
                       BigDecimal calorieKcal, BigDecimal gi, boolean estimated) {
        static Item from(MealItem item) {
            var food = item.getFood();
            var multiplier = item.getServingMultiplier();
            return new Item(item.getId(), food.getId(), item.getFoodName(), multiplier,
                    food.getCarbG().multiply(multiplier), food.getFiberG().multiply(multiplier),
                    food.getProteinG().multiply(multiplier), food.getFatG().multiply(multiplier),
                    food.getCalorieKcal().multiply(multiplier), food.getGi(), item.isEstimated());
        }
    }
}
