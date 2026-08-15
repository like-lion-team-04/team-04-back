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
    private static final String DISCLAIMER =
            "설명을 위한 상대적 비교이며 개인 혈당 수치나 실제 혈당 변화를 예측하지 않습니다.";

    public static AnalysisResponse from(MealAnalysis analysis) {
        return new AnalysisResponse(analysis.getId(), analysis.getBaselineGl(), analysis.getRecommendedGl(),
                analysis.getReliefRate(), analysis.getPersonalCoefficient(), analysis.getEstimatedItemRatio(), DISCLAIMER);
    }
}
