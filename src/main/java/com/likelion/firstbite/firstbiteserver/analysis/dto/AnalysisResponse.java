package com.likelion.firstbite.firstbiteserver.analysis.dto;

import com.likelion.firstbite.firstbiteserver.analysis.domain.MealAnalysis;

import java.math.BigDecimal;
import java.util.UUID;

public record AnalysisResponse(
        UUID analysisId,
        BigDecimal baselineGl,
        BigDecimal recommendedGl,
        BigDecimal reliefRate,
        BigDecimal personalCoefficient,
        BigDecimal estimatedItemRatio,
        String disclaimer
) {
    private static final String DISCLAIMER = "개인 혈당 예측이 아닌 상대 비교입니다.";

    public static AnalysisResponse from(MealAnalysis analysis) {
        return new AnalysisResponse(analysis.getId(), analysis.getBaselineGl(), analysis.getRecommendedGl(),
                analysis.getReliefRate(), analysis.getPersonalCoefficient(), analysis.getEstimatedItemRatio(), DISCLAIMER);
    }
}
