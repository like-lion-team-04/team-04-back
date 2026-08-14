package com.likelion.firstbite.firstbiteserver.history.service;

import com.likelion.firstbite.firstbiteserver.analysis.repository.MealAnalysisRepository;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingRecord;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingRecordRepository;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingStageRecordRepository;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.history.dto.CoachingHistoryDetailResponse;
import com.likelion.firstbite.firstbiteserver.feedback.repository.CoachingFeedbackRepository;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoachingHistoryDetailService {
    private final CoachingRecordRepository recordRepository;
    private final CoachingStageRecordRepository stageRecordRepository;
    private final MealRepository mealRepository;
    private final MealAnalysisRepository analysisRepository;
    private final CoachingFeedbackRepository feedbackRepository;

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
        var items = meal.getItems().stream().map(item -> new CoachingHistoryDetailResponse.Item(
                item.getFood().getId(), item.getFoodName(), item.getServingMultiplier())).toList();
        var stages = stageRecordRepository.findAllBySessionIdOrderByStageAsc(record.getSessionId()).stream()
                .map(stage -> new CoachingHistoryDetailResponse.Stage(stage.getStage(),
                        stage.getResult().name(), stage.getActualSeconds())).toList();
        boolean personalized = analysisRepository.findFirstByMealIdOrderByCreatedAtDesc(record.getMealId())
                .map(analysis -> analysis.getPersonalCoefficient().compareTo(BigDecimal.ONE) != 0).orElse(false);
        var feedback = feedbackRepository.findByRecordId(recordId)
                .map(value -> new CoachingHistoryDetailResponse.Feedback(
                        value.isSkipped() ? null : value.getSleepinessScore())).orElse(null);
        return new CoachingHistoryDetailResponse(record.getId(), record.getCompletedAt(), items, stages, feedback, personalized);
    }
}
