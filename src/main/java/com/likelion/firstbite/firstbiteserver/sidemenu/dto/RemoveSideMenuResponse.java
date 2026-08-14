package com.likelion.firstbite.firstbiteserver.sidemenu.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RemoveSideMenuResponse(
        UUID mealId,
        UUID removedSideMenuId,
        AnalysisSummary analysis,
        int coachingPlanVersion
) {
    public record AnalysisSummary(BigDecimal reliefRate) {}
}
