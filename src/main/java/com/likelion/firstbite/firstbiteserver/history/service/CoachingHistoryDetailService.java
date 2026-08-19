package com.likelion.firstbite.firstbiteserver.history.service;

import com.likelion.firstbite.firstbiteserver.analysis.repository.MealAnalysisRepository;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingStageRecord;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingRecord;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingRecordRepository;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingStageRecordRepository;
import com.likelion.firstbite.firstbiteserver.coaching.service.CoachingPlanService;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.history.dto.CoachingHistoryDetailResponse;
import com.likelion.firstbite.firstbiteserver.feedback.repository.CoachingFeedbackRepository;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachingHistoryDetailService {
    private final CoachingRecordRepository recordRepository;
    private final CoachingStageRecordRepository stageRecordRepository;
    private final MealRepository mealRepository;
    private final MealAnalysisRepository analysisRepository;
    private final CoachingFeedbackRepository feedbackRepository;
    private final CoachingPlanService coachingPlanService;

    @Transactional(readOnly = true)
    public CoachingHistoryDetailResponse getDetail(UUID memberId, UUID recordId) {
        CoachingRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "COACHING_RECORD_NOT_FOUND", "코칭 기록을 찾을 수 없습니다."));
        if (!record.getMemberId().equals(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "HISTORY_FORBIDDEN", "본인의 기록만 조회할 수 있습니다.");
        }
        var meal = mealRepository.findAllByIdIn(java.util.List.of(record.getMealId())).stream().findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "MEAL_NOT_FOUND", "식사를 찾을 수 없습니다."));
        var plan = coachingPlanService.getPlan(memberId, record.getMealId());
        var items = meal.getItems().stream().map(item -> new CoachingHistoryDetailResponse.Item(
                item.getFood().getId(), item.getFoodName(), item.getFood().getImageUrl(),
                item.getServingMultiplier())).toList();
        var recommendedOrder = plan.recommendedOrder().stream()
                .map(item -> new CoachingHistoryDetailResponse.RecommendedOrderItem(item.order(), item.stage(),
                        item.foodId(), item.name(), item.imageUrl(), item.servingMultiplier()))
                .toList();
        Map<Integer, CoachingStageRecord> stageRecords = stageRecordRepository
                .findAllBySessionIdOrderByStageAsc(record.getSessionId()).stream()
                .collect(Collectors.toMap(CoachingStageRecord::getStage, Function.identity()));
        var stages = plan.stages().stream().map(stage -> {
            CoachingStageRecord execution = stageRecords.get(stage.stage());
            return new CoachingHistoryDetailResponse.Stage(stage.stage(), stage.title(),
                    stage.recommendedSeconds(), execution == null ? "NOT_STARTED" : execution.getResult().name(),
                    execution == null ? null : execution.getActualSeconds());
        }).toList();
        boolean personalized = analysisRepository.findFirstByMealIdOrderByCreatedAtDesc(record.getMealId())
                .map(analysis -> analysis.getPersonalCoefficient().compareTo(BigDecimal.ONE) != 0).orElse(false);
        var feedback = feedbackRepository.findByRecordId(recordId)
                .map(value -> new CoachingHistoryDetailResponse.Feedback(value.getId(),
                        value.isSkipped() ? "SKIPPED" : "ANSWERED",
                        value.isSkipped() ? null : value.getSleepinessScore(),
                        value.isSkipped() ? null : sleepinessLabel(value.getSleepinessScore()),
                        value.getAnsweredAt(), value.isPersonalizationUpdated()))
                .orElseGet(() -> new CoachingHistoryDetailResponse.Feedback(
                        null, "PENDING", null, null, null, false));
        var summary = new CoachingHistoryDetailResponse.Summary(record.getCompletedStages(),
                record.getSkippedStages(), record.getTotalStages(), record.getTotalSeconds());
        return new CoachingHistoryDetailResponse(record.getId(), record.getMealId(), mealType(record),
                record.getCompletedAt(), record.getReason().name(), summary, items, recommendedOrder,
                stages, feedback, personalized);
    }

    private String mealType(CoachingRecord record) {
        int hour = record.getCompletedAt().atZone(ZoneId.of("Asia/Seoul")).getHour();
        if (hour >= 5 && hour < 10) return "BREAKFAST";
        if (hour >= 10 && hour < 15) return "LUNCH";
        if (hour >= 15 && hour < 21) return "DINNER";
        return "SNACK";
    }

    private String sleepinessLabel(Integer score) {
        if (score == null) return null;
        return switch (score) {
            case 1 -> "너무 졸렸어요";
            case 2 -> "꽤 졸렸어요";
            case 3 -> "졸렸어요";
            case 4 -> "별로 졸리지 않았어요";
            case 5 -> "안 졸렸어요";
            default -> null;
        };
    }
}
