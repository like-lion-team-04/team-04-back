package com.likelion.firstbite.firstbiteserver.coaching.dto;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingRecord;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingSessionStatus;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingStageRecord;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public record CompleteCoachingSessionResponse(
        UUID recordId,
        UUID sessionId,
        CoachingSessionStatus status,
        Summary summary,
        List<Stage> stages,
        List<Item> items
) {
    public static CompleteCoachingSessionResponse from(CoachingRecord record, CoachingPlanResponse plan,
                                                       List<CoachingStageRecord> stageRecords) {
        Map<Integer, CoachingStageRecord> recordsByStage = stageRecords.stream()
                .collect(Collectors.toMap(CoachingStageRecord::getStage, Function.identity()));
        List<Stage> stages = plan.stages().stream().map(stage -> {
            CoachingStageRecord stageRecord = recordsByStage.get(stage.stage());
            return new Stage(stage.stage(), stage.title(), stage.recommendedSeconds(),
                    stageRecord == null ? null : stageRecord.getActualSeconds(),
                    stageRecord == null ? "NOT_STARTED" : stageRecord.getResult().name());
        }).toList();
        List<Item> items = plan.recommendedOrder().stream().map(item -> new Item(
                item.order(), item.stage(), item.foodId(), item.name(), item.imageUrl(),
                item.servingMultiplier(), item.gi(), item.giDataQuality())).toList();
        int adherenceRate = record.getTotalStages() == 0 ? 0
                : (int) Math.round(record.getCompletedStages() * 100.0 / record.getTotalStages());
        return new CompleteCoachingSessionResponse(record.getId(), record.getSessionId(),
                CoachingSessionStatus.COMPLETED,
                new Summary(record.getCompletedStages(), record.getSkippedStages(), record.getTotalStages(),
                        record.getTotalSeconds(), adherenceRate), stages, items);
    }

    public record Summary(int completedStages, int skippedStages, int totalStages,
                          long totalSeconds, int adherenceRate) {}
    public record Stage(int stage, String title, Integer recommendedSeconds,
                        Long actualSeconds, String result) {}
    public record Item(int order, int stage, UUID foodId, String name, String imageUrl,
                       BigDecimal servingMultiplier, BigDecimal gi, String giDataQuality) {}
}
