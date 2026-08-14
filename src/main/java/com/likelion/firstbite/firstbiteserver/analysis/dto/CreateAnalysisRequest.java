package com.likelion.firstbite.firstbiteserver.analysis.dto;

public record CreateAnalysisRequest(Boolean usePersonalization) {
    public boolean personalizationEnabled() {
        return usePersonalization == null || usePersonalization;
    }
}
