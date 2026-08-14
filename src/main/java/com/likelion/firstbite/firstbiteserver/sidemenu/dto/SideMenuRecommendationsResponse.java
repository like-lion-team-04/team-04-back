package com.likelion.firstbite.firstbiteserver.sidemenu.dto;

import com.likelion.firstbite.firstbiteserver.sidemenu.domain.NutrientFocus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SideMenuRecommendationsResponse(List<Item> items) {
    public record Item(
            UUID sideMenuId,
            String name,
            NutrientFocus nutrientFocus,
            String reason,
            BigDecimal expectedReliefDelta,
            int estimatedPrice
    ) {}
}
