package com.likelion.firstbite.firstbiteserver.evidence.dto;

import java.util.List;
import java.math.BigDecimal;

public record EvidenceResponse(List<Source> sources, Calculation calculation, String medicalDisclaimer) {
    public record Source(String evidenceId, String type, String title, List<String> authors,
                         int year, String url, String usage, String description) {}
    public record Calculation(String glFormula, String availableCarbohydrateFormula,
                              String reductionCoefficientFormula, BigDecimal reductionCap,
                              String orderFeasibilityRule, String intervalCoefficientRule,
                              String personalCoefficientRule, StageIntervalRule stageIntervalRule) {}
    public record StageIntervalRule(int defaultMinutes, int minimumMinutes, int maximumMinutes,
                                    String description) {}
}
