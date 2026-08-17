package com.likelion.firstbite.firstbiteserver.coaching.dto;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

public record CoachingPlanResponse(
        UUID planId,
        int version,
        RuleType ruleType,
        List<Stage> stages,
        List<RecommendedOrderItem> recommendedOrder,
        GuideTone guideTone
) {
    public enum RuleType { PROTEIN_FIRST }
    public enum GuideTone { NON_RESTRICTIVE }

    public record Stage(int stage, String title, List<UUID> itemIds, Integer recommendedSeconds,
                        StageSummary summary, String guide, List<Item> items) {}
    public record StageSummary(String nutrientName, BigDecimal nutrientAmountG,
                               BigDecimal calorieKcal, Integer estimatedPrice) {}
    public record Item(UUID mealItemId, UUID foodId, String name, String imageUrl,
                       String servingDescription, BigDecimal servingMultiplier,
                       BigDecimal carbohydrateG, BigDecimal fiberG, BigDecimal proteinG,
                       BigDecimal fatG, BigDecimal calorieKcal, BigDecimal gi,
                       String giDataQuality, boolean estimated) {}
    public record RecommendedOrderItem(int order, int stage, UUID mealItemId, UUID foodId,
                                       String name, String imageUrl, BigDecimal servingMultiplier,
                                       BigDecimal gi, String giDataQuality) {}
}
