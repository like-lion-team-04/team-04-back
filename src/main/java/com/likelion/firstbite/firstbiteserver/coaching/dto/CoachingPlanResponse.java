package com.likelion.firstbite.firstbiteserver.coaching.dto;

import java.util.List;
import java.util.UUID;

public record CoachingPlanResponse(
        UUID planId,
        int version,
        RuleType ruleType,
        List<Stage> stages,
        GuideTone guideTone
) {
    public enum RuleType { PROTEIN_FIRST }
    public enum GuideTone { NON_RESTRICTIVE }

    public record Stage(int stage, String title, List<UUID> itemIds, Integer recommendedSeconds) {}
}
