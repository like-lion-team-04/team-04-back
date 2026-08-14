package com.likelion.firstbite.firstbiteserver.food.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "foods", indexes = {
        @Index(name = "idx_foods_name", columnList = "name"),
        @Index(name = "idx_foods_initials", columnList = "initials"),
        @Index(name = "idx_foods_category_active", columnList = "search_category,is_active")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Food {
    @Id private UUID id;
    @Column(name = "food_code", nullable = false, unique = true, length = 80) private String foodCode;
    @Column(nullable = false, length = 100) private String name;
    @Column(name = "original_category", nullable = false, length = 40) private String originalCategory;
    @Enumerated(EnumType.STRING) @Column(name = "search_category", nullable = false, length = 20) private FoodCategory searchCategory;
    @Column(nullable = false, length = 50) private String initials;
    @Column(name = "serving_description", nullable = false, length = 150) private String servingDescription;
    @Column(name = "serving_amount", nullable = false, precision = 10, scale = 2) private BigDecimal servingAmount;
    @Enumerated(EnumType.STRING) @Column(name = "serving_unit", nullable = false, length = 10) private ServingUnit servingUnit;
    @Column(name = "carb_g", nullable = false, precision = 10, scale = 2) private BigDecimal carbG;
    @Column(name = "fiber_g", nullable = false, precision = 10, scale = 2) private BigDecimal fiberG;
    @Column(name = "protein_g", nullable = false, precision = 10, scale = 2) private BigDecimal proteinG;
    @Column(name = "fat_g", nullable = false, precision = 10, scale = 2) private BigDecimal fatG;
    @Column(name = "available_carb_g", nullable = false, precision = 10, scale = 2) private BigDecimal availableCarbG;
    @Column(nullable = false, precision = 7, scale = 2) private BigDecimal gi;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal gl;
    @Column(name = "calorie_kcal", nullable = false, precision = 10, scale = 2) private BigDecimal calorieKcal;
    @Enumerated(EnumType.STRING) @Column(name = "nutrition_data_quality", nullable = false, length = 20) private DataQuality nutritionDataQuality;
    @Enumerated(EnumType.STRING) @Column(name = "gi_data_quality", nullable = false, length = 20) private DataQuality giDataQuality;
    @Column(name = "is_active", nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public static Food create(UUID id, String foodCode, String name, String originalCategory,
                              FoodCategory searchCategory, String initials, String servingDescription,
                              BigDecimal servingAmount, ServingUnit servingUnit, BigDecimal gi,
                              DataQuality nutritionQuality, DataQuality giQuality) {
        Food food = new Food();
        food.id = id;
        food.foodCode = foodCode;
        food.name = name;
        food.originalCategory = originalCategory;
        food.searchCategory = searchCategory;
        food.initials = initials;
        food.servingDescription = servingDescription;
        food.servingAmount = servingAmount;
        food.servingUnit = servingUnit;
        food.carbG = BigDecimal.ZERO;
        food.fiberG = BigDecimal.ZERO;
        food.proteinG = BigDecimal.ZERO;
        food.fatG = BigDecimal.ZERO;
        food.availableCarbG = BigDecimal.ZERO;
        food.gi = gi;
        food.gl = BigDecimal.ZERO;
        food.calorieKcal = BigDecimal.ZERO;
        food.nutritionDataQuality = nutritionQuality;
        food.giDataQuality = giQuality;
        food.active = true;
        food.createdAt = Instant.now();
        food.updatedAt = food.createdAt;
        return food;
    }
}
