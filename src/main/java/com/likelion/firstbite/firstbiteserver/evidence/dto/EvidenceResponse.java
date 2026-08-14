package com.likelion.firstbite.firstbiteserver.evidence.dto;

import java.util.List;

public record EvidenceResponse(List<Source> sources, Calculation calculation, String medicalDisclaimer) {
    public record Source(String evidenceId, String type, String title, List<String> authors,
                         int year, String url, String usage) {}
    public record Calculation(String glFormula, String availableCarbohydrateFormula) {}
}
