package com.likelion.firstbite.firstbiteserver.analysis.dto;

import com.likelion.firstbite.firstbiteserver.analysis.domain.MealAnalysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

public record AnalysisDetailResponse(
        UUID analysisId,
        Burden baseline,
        Burden recommended,
        BigDecimal reliefRate,
        DataQuality dataQuality,
        List<EvidenceSummary> sources,
        String disclaimer
) {
    private static final List<BigDecimal> BASE_CURVE = List.of(
            value("0"), value("0.50"), value("1.00"), value("0.70"), value("0.20"));
    private static final String DISCLAIMER = "개인 혈당 예측이 아닌 상대 비교입니다.";

    public static AnalysisDetailResponse from(MealAnalysis analysis) {
        BigDecimal curveRatio = analysis.getBaselineGl().signum() == 0
                ? BigDecimal.ONE
                : analysis.getRecommendedGl().divide(analysis.getBaselineGl(), 4, RoundingMode.HALF_UP);
        List<BigDecimal> recommendedCurve = BASE_CURVE.stream()
                .map(point -> point.multiply(curveRatio).setScale(2, RoundingMode.HALF_UP))
                .toList();
        return new AnalysisDetailResponse(
                analysis.getId(),
                new Burden(analysis.getBaselineGl(), BASE_CURVE),
                new Burden(analysis.getRecommendedGl(), recommendedCurve),
                analysis.getReliefRate(),
                DataQuality.from(analysis.getEstimatedItemRatio()),
                List.of(
                        new EvidenceSummary("MFDS_NUTRITION_DB", "식약처 식품영양성분 DB"),
                        new EvidenceSummary("KOREAN_GI_STUDY", "한국인 대상 식품 혈당지수 실측 연구")
                ),
                DISCLAIMER
        );
    }

    private static BigDecimal value(String value) {
        return new BigDecimal(value);
    }

    public record Burden(BigDecimal gl, List<BigDecimal> curve) {}

    public record EvidenceSummary(String evidenceId, String title) {}

    public enum DataQuality {
        MEASURED, MIXED, ESTIMATED;

        static DataQuality from(BigDecimal estimatedItemRatio) {
            if (estimatedItemRatio.signum() == 0) return MEASURED;
            if (estimatedItemRatio.compareTo(BigDecimal.ONE) == 0) return ESTIMATED;
            return MIXED;
        }
    }
}
