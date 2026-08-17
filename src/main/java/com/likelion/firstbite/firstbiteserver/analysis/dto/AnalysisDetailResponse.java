package com.likelion.firstbite.firstbiteserver.analysis.dto;

import com.likelion.firstbite.firstbiteserver.analysis.domain.MealAnalysis;
import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record AnalysisDetailResponse(
        UUID analysisId,
        Burden baseline,
        Burden recommended,
        BigDecimal reliefRate,
        BurdenLevel baselineLevel,
        BurdenLevel recommendedLevel,
        List<ChartPoint> baselineChart,
        List<ChartPoint> recommendedChart,
        List<ItemContribution> itemContributions,
        List<ReductionFactor> reductionFactors,
        ComparisonConditions comparisonConditions,
        DataQuality dataQuality,
        List<EvidenceSummary> sources,
        String chartUnit,
        String disclaimer
) {
    private static final List<Integer> MINUTES = List.of(0, 30, 60, 90, 120);
    private static final List<BigDecimal> BASE_CURVE = List.of(
            value("0"), value("0.50"), value("1.00"), value("0.70"), value("0.20"));
    private static final String DISCLAIMER =
            "설명을 위한 상대적 비교이며 개인 혈당 수치나 실제 혈당 변화를 예측하지 않습니다.";

    public static AnalysisDetailResponse from(MealAnalysis analysis, Meal meal) {
        BigDecimal curveRatio = analysis.getBaselineGl().signum() == 0
                ? BigDecimal.ONE
                : analysis.getRecommendedGl().divide(analysis.getBaselineGl(), 4, RoundingMode.HALF_UP);
        List<BigDecimal> recommendedCurve = BASE_CURVE.stream()
                .map(point -> point.multiply(curveRatio).setScale(2, RoundingMode.HALF_UP))
                .toList();
        List<ItemContribution> contributions = meal.getItems().stream()
                .map(item -> contribution(item, analysis.getReliefRate()))
                .sorted(Comparator.comparing(ItemContribution::baselineGl).reversed())
                .toList();
        return new AnalysisDetailResponse(
                analysis.getId(),
                new Burden(analysis.getBaselineGl(), BASE_CURVE),
                new Burden(analysis.getRecommendedGl(), recommendedCurve),
                analysis.getReliefRate(),
                BurdenLevel.from(analysis.getBaselineGl()),
                BurdenLevel.from(analysis.getRecommendedGl()),
                chart(BASE_CURVE),
                chart(recommendedCurve),
                contributions,
                reductionFactors(meal, analysis),
                new ComparisonConditions("PROTEIN_FIRST", 5, 15,
                        analysis.getPersonalCoefficient().compareTo(BigDecimal.ONE) != 0,
                        analysis.getPersonalCoefficient()),
                DataQuality.from(analysis.getEstimatedItemRatio()),
                List.of(
                        new EvidenceSummary("MFDS_NUTRITION_DB", "식품의약품안전처 식품영양성분 DB"),
                        new EvidenceSummary("KOREAN_GI_STUDY", "한국인 다소비 식품 혈당지수 실측 연구")
                ),
                "RELATIVE",
                DISCLAIMER
        );
    }

    private static ItemContribution contribution(MealItem item, BigDecimal reliefRate) {
        BigDecimal baselineGl = item.getFood().getGi()
                .multiply(item.getFood().getAvailableCarbG())
                .multiply(item.getServingMultiplier())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal recommendedGl = baselineGl.multiply(BigDecimal.ONE.subtract(reliefRate))
                .setScale(2, RoundingMode.HALF_UP);
        String quality = item.getFood().getGiDataQuality().name().equals("MEASURED")
                ? "MEASURED" : "ESTIMATED";
        return new ItemContribution(item.getId(), item.getFood().getId(), item.getFoodName(),
                item.getServingMultiplier(), item.getFood().getGi(), quality, baselineGl, recommendedGl);
    }

    private static List<ReductionFactor> reductionFactors(Meal meal, MealAnalysis analysis) {
        java.util.ArrayList<ReductionFactor> factors = new java.util.ArrayList<>();
        boolean hasProtein = meal.getItems().stream().anyMatch(item -> item.getFood().getProteinG().signum() > 0);
        boolean hasFiber = meal.getItems().stream().anyMatch(item -> item.getFood().getFiberG().signum() > 0);
        if (analysis.getReliefRate().signum() > 0 && hasProtein) {
            factors.add(new ReductionFactor("PROTEIN_FIRST", "단백질을 먼저 섭취하는 순서를 반영했습니다."));
        }
        if (analysis.getReliefRate().signum() > 0 && hasFiber) {
            factors.add(new ReductionFactor("FIBER_PRELOAD", "식이섬유가 포함된 항목을 탄수화물보다 먼저 배치했습니다."));
        }
        if (analysis.getPersonalCoefficient().compareTo(BigDecimal.ONE) != 0) {
            factors.add(new ReductionFactor("PERSONALIZATION", "누적 피드백에 따른 개인화 계수를 반영했습니다."));
        }
        return List.copyOf(factors);
    }

    private static List<ChartPoint> chart(List<BigDecimal> curve) {
        return java.util.stream.IntStream.range(0, MINUTES.size())
                .mapToObj(index -> new ChartPoint(MINUTES.get(index), curve.get(index)))
                .toList();
    }

    private static BigDecimal value(String value) { return new BigDecimal(value); }

    public record Burden(BigDecimal gl, List<BigDecimal> curve) {}
    public record ChartPoint(int minute, BigDecimal value) {}
    public record ItemContribution(UUID mealItemId, UUID foodId, String name, BigDecimal servingMultiplier,
                                   BigDecimal gi, String dataQuality, BigDecimal baselineGl,
                                   BigDecimal recommendedGl) {}
    public record ReductionFactor(String type, String description) {}
    public record ComparisonConditions(String orderRule, int stageIntervalMinutes,
                                       int totalRecommendedMinutes, boolean personalizationApplied,
                                       BigDecimal personalCoefficient) {}
    public record EvidenceSummary(String evidenceId, String title) {}

    public enum BurdenLevel {
        LOW, MEDIUM, HIGH;

        static BurdenLevel from(BigDecimal gl) {
            if (gl.compareTo(new BigDecimal("10")) <= 0) return LOW;
            if (gl.compareTo(new BigDecimal("20")) <= 0) return MEDIUM;
            return HIGH;
        }
    }

    public enum DataQuality {
        MEASURED, MIXED, ESTIMATED;

        static DataQuality from(BigDecimal estimatedItemRatio) {
            if (estimatedItemRatio.signum() == 0) return MEASURED;
            if (estimatedItemRatio.compareTo(BigDecimal.ONE) == 0) return ESTIMATED;
            return MIXED;
        }
    }
}
