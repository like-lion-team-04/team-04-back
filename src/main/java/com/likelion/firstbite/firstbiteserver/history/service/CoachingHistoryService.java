package com.likelion.firstbite.firstbiteserver.history.service;

import com.likelion.firstbite.firstbiteserver.analysis.domain.MealAnalysis;
import com.likelion.firstbite.firstbiteserver.analysis.repository.MealAnalysisRepository;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingRecord;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingStageRecord;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingRecordRepository;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingStageRecordRepository;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.history.dto.CoachingHistoryListResponse;
import com.likelion.firstbite.firstbiteserver.history.dto.HistoryPageMeta;
import com.likelion.firstbite.firstbiteserver.feedback.domain.CoachingFeedback;
import com.likelion.firstbite.firstbiteserver.feedback.repository.CoachingFeedbackRepository;
import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachingHistoryService {
    static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private final CoachingRecordRepository recordRepository;
    private final MealRepository mealRepository;
    private final CoachingFeedbackRepository feedbackRepository;
    private final CoachingStageRecordRepository stageRecordRepository;
    private final MealAnalysisRepository analysisRepository;

    @Transactional(readOnly = true)
    public Result getHistory(UUID memberId, LocalDate from, LocalDate to, int page, int size) {
        validate(from, to, page, size);
        Instant fromInstant = from == null ? null : from.atStartOfDay(SERVICE_ZONE).toInstant();
        Instant toExclusive = to == null ? null : to.plusDays(1).atStartOfDay(SERVICE_ZONE).toInstant();
        var records = recordRepository.findHistory(memberId, fromInstant, toExclusive,
                PageRequest.of(page - 1, size));
        var mealIds = records.getContent().stream().map(CoachingRecord::getMealId).distinct().toList();
        Map<UUID, Meal> meals = mealRepository.findAllByIdIn(mealIds).stream()
                .collect(Collectors.toMap(Meal::getId, Function.identity()));
        Map<UUID, CoachingFeedback> feedbacks = feedbackRepository.findAllByRecordIdIn(
                        records.getContent().stream().map(CoachingRecord::getId).toList()).stream()
                .collect(Collectors.toMap(CoachingFeedback::getRecordId, Function.identity()));
        var sessionIds = records.getContent().stream().map(CoachingRecord::getSessionId).distinct().toList();
        List<CoachingStageRecord> stageRecords = sessionIds.isEmpty() ? List.of()
                : stageRecordRepository.findAllBySessionIdInOrderBySessionIdAscStageAsc(sessionIds);
        Map<UUID, List<CoachingStageRecord>> stagesBySession = stageRecords.stream()
                .collect(Collectors.groupingBy(CoachingStageRecord::getSessionId));
        List<MealAnalysis> analyses = mealIds.isEmpty() ? List.of()
                : analysisRepository.findAllByMealIdInOrderByCreatedAtDesc(mealIds);
        Map<UUID, MealAnalysis> latestAnalysisByMeal = new HashMap<>();
        analyses.forEach(analysis -> latestAnalysisByMeal.putIfAbsent(analysis.getMealId(), analysis));

        var items = records.getContent().stream().map(record -> {
            Meal meal = meals.get(record.getMealId());
            String mealName = meal == null ? "삭제된 식사" : summarizeMeal(meal);
            CoachingFeedback feedback = feedbacks.get(record.getId());
            Integer score = feedback == null || feedback.isSkipped() ? null : feedback.getSleepinessScore();
            List<CoachingHistoryListResponse.MenuItem> menuItems = meal == null ? List.of() : meal.getItems().stream()
                    .map(item -> new CoachingHistoryListResponse.MenuItem(item.getFood().getId(),
                            item.getFoodName(), item.getServingMultiplier()))
                    .toList();
            List<CoachingHistoryListResponse.StageResult> stageResults = stagesBySession
                    .getOrDefault(record.getSessionId(), List.of()).stream()
                    .map(stage -> new CoachingHistoryListResponse.StageResult(stage.getStage(),
                            stage.getResult().name(), stage.getActualSeconds()))
                    .toList();
            MealAnalysis analysis = latestAnalysisByMeal.get(record.getMealId());
            boolean personalizationApplied = analysis != null
                    && analysis.getPersonalCoefficient().compareTo(BigDecimal.ONE) != 0;
            return new CoachingHistoryListResponse.Item(record.getId(), mealName, record.getCompletedAt(),
                    record.getCompletedStages(), record.getTotalStages(), score, record.getReason().name(),
                    record.getSkippedStages(), record.getTotalSeconds(), personalizationApplied,
                    menuItems, stageResults);
        }).toList();
        return new Result(new CoachingHistoryListResponse(items),
                new HistoryPageMeta(page, size, records.getTotalElements(), records.getTotalPages()));
    }

    static String summarizeMeal(Meal meal) {
        if (meal.getItems().isEmpty()) return "메뉴 없음";
        String first = meal.getItems().get(0).getFoodName();
        int remaining = meal.getItems().size() - 1;
        return remaining == 0 ? first : first + " 외 " + remaining + "개";
    }

    private void validate(LocalDate from, LocalDate to, int page, int size) {
        if (page < 1 || size < 1 || size > 50 || from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "HISTORY_QUERY_INVALID",
                    "조회 기간 또는 페이지 조건이 올바르지 않습니다.");
        }
    }

    public record Result(CoachingHistoryListResponse data, HistoryPageMeta meta) {}
}
