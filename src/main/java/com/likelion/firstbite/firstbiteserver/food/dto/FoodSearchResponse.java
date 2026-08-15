package com.likelion.firstbite.firstbiteserver.food.dto;

import com.likelion.firstbite.firstbiteserver.food.domain.Food;
import com.likelion.firstbite.firstbiteserver.food.domain.FoodCategory;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FoodSearchResponse(List<Item> items) {
    public record Item(UUID foodId, String name, FoodCategory category, BigDecimal defaultServing,
                       BigDecimal carbohydrateG, BigDecimal fiberG, BigDecimal proteinG, BigDecimal fatG,
                       BigDecimal calorieKcal, BigDecimal gi, String dataQuality) {
        public static Item from(Food food) {
            String quality = food.getGiDataQuality().name().equals("MEASURED") ? "MEASURED" : "ESTIMATED";
            return new Item(food.getId(), food.getName(), food.getSearchCategory(), BigDecimal.ONE,
                    food.getCarbG(), food.getFiberG(), food.getProteinG(), food.getFatG(),
                    food.getCalorieKcal(), food.getGi(), quality);
        }
    }
}
