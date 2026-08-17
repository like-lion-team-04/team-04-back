package com.likelion.firstbite.firstbiteserver.sidemenu.dto;

import com.likelion.firstbite.firstbiteserver.sidemenu.domain.NutrientFocus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SideMenuRecommendationsResponse(List<Item> items) {
    public record Item(
            UUID sideMenuId,
            UUID foodId,
            String name,
            NutrientFocus nutrientFocus,
            String reason,
            BigDecimal expectedReliefDelta,
            int estimatedPrice,
            String description,
            String imageUrl,
            Nutrition nutrition,
            List<Reason> reasons,
            ExpectedEffects expectedEffects
    ) {}
    public record Nutrition(String servingDescription, BigDecimal servingAmount, String servingUnit,
                            BigDecimal carbohydrateG, BigDecimal fiberG, BigDecimal proteinG,
                            BigDecimal fatG, BigDecimal sodiumMg, BigDecimal calorieKcal,
                            BigDecimal gi, String giDataQuality) {}
    public record Reason(String title, String description) {}
    public record ExpectedEffects(BigDecimal reliefRateDelta, BigDecimal fiberDeltaG,
                                  BigDecimal proteinDeltaG, boolean estimated) {}
}
