package com.likelion.firstbite.firstbiteserver.meal.domain;

import com.likelion.firstbite.firstbiteserver.food.domain.Food;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meal_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealItem {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "meal_id", nullable = false) private Meal meal;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "food_id", nullable = false) private Food food;
    @Column(name = "food_name", nullable = false, length = 100) private String foodName;
    @Column(name = "serving_multiplier", nullable = false, precision = 3, scale = 1) private BigDecimal servingMultiplier;
    @Column(nullable = false) private boolean estimated;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public static MealItem from(Food food, BigDecimal servingMultiplier) {
        MealItem item = new MealItem();
        item.id = UUID.randomUUID();
        item.food = food;
        item.foodName = food.getName();
        item.servingMultiplier = servingMultiplier;
        item.estimated = food.getNutritionDataQuality() != com.likelion.firstbite.firstbiteserver.food.domain.DataQuality.MEASURED
                || food.getGiDataQuality() != com.likelion.firstbite.firstbiteserver.food.domain.DataQuality.MEASURED;
        item.createdAt = Instant.now();
        return item;
    }

    void attachTo(Meal meal) {
        this.meal = meal;
    }
}
