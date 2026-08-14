package com.likelion.firstbite.firstbiteserver.sidemenu.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AddSideMenuResponse(
        UUID mealId,
        AddedItem addedItem,
        AnalysisSummary analysis,
        int coachingPlanVersion
) {
    public record AddedItem(UUID mealItemId, String name) {}
    public record AnalysisSummary(BigDecimal reliefRate) {}
}
