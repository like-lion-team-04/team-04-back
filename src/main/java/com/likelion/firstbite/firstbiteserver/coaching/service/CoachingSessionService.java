package com.likelion.firstbite.firstbiteserver.coaching.service;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingSession;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingSessionStatus;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingStageRecord;
import com.likelion.firstbite.firstbiteserver.coaching.domain.ProgressAction;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CompletionReason;
import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingRecord;
import com.likelion.firstbite.firstbiteserver.coaching.domain.StageResult;
import com.likelion.firstbite.firstbiteserver.coaching.domain.TimerAction;
import com.likelion.firstbite.firstbiteserver.coaching.dto.CoachingSessionResponse;
import com.likelion.firstbite.firstbiteserver.coaching.dto.StartCoachingSessionRequest;
import com.likelion.firstbite.firstbiteserver.coaching.dto.UpdateCoachingStageRequest;
import com.likelion.firstbite.firstbiteserver.coaching.dto.UpdateCoachingStageResponse;
import com.likelion.firstbite.firstbiteserver.coaching.dto.CompleteCoachingSessionRequest;
import com.likelion.firstbite.firstbiteserver.coaching.dto.CompleteCoachingSessionResponse;
import com.likelion.firstbite.firstbiteserver.coaching.dto.ActiveCoachingSessionResponse;
import com.likelion.firstbite.firstbiteserver.coaching.dto.CoachingTimerResponse;
import com.likelion.firstbite.firstbiteserver.coaching.dto.UpdateCoachingTimerRequest;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingSessionRepository;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingStageRecordRepository;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingRecordRepository;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealStatus;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoachingSessionService {
    private static final List<CoachingSessionStatus> ACTIVE_STATUSES =
            List.of(CoachingSessionStatus.IN_PROGRESS, CoachingSessionStatus.PAUSED);
    private final CoachingSessionRepository sessionRepository;
    private final MealRepository mealRepository;
    private final CoachingPlanService coachingPlanService;
    private final CoachingStageRecordRepository stageRecordRepository;
    private final CoachingRecordRepository coachingRecordRepository;

    @Transactional(readOnly = true)
    public ActiveCoachingSessionResponse getActive(UUID memberId) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        return sessionRepository.findFirstByMemberIdAndStatusInOrderByUpdatedAtDesc(memberId, ACTIVE_STATUSES)
                .map(session -> ActiveCoachingSessionResponse.from(session, now))
                .orElseGet(ActiveCoachingSessionResponse::none);
    }

    @Transactional
    public CoachingSessionResponse start(UUID memberId, UUID idempotencyKey, StartCoachingSessionRequest request) {
        if (idempotencyKey == null || request == null || request.mealId() == null || request.planVersion() == null
                || request.planVersion() < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "COACHING_SESSION_REQUEST_INVALID",
                    "식사 ID, 계획 버전과 Idempotency-Key가 필요합니다.");
        }
        String requestHash = hash(request.mealId() + ":" + request.planVersion());
        var existing = sessionRepository
                .findFirstByMemberIdAndIdempotencyKeyAndCreatedAtAfterOrderByCreatedAtDesc(
                        memberId, idempotencyKey, Instant.now().minus(Duration.ofHours(24)));
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new BusinessException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT",
                        "동일한 Idempotency-Key를 다른 요청에 사용할 수 없습니다.");
            }
            return CoachingSessionResponse.from(existing.get());
        }

        var meal = mealRepository.findByIdForUpdate(request.mealId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEAL_NOT_FOUND", "식사를 찾을 수 없습니다."));
        if (!meal.belongsTo(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "MEAL_FORBIDDEN", "다른 사용자의 식사에 접근할 수 없습니다.");
        }
        if (meal.getStatus() != MealStatus.ANALYZED) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "MEAL_NOT_READY", "분석 완료된 식사만 시작할 수 있습니다.");
        }
        if (meal.getCoachingPlanVersion() != request.planVersion()) {
            throw new BusinessException(HttpStatus.CONFLICT, "COACHING_PLAN_CHANGED",
                    "코칭 계획이 변경되었습니다. 최신 계획을 다시 확인해 주세요.");
        }
        if (sessionRepository.existsByMemberIdAndStatusIn(memberId, ACTIVE_STATUSES)) {
            throw new BusinessException(HttpStatus.CONFLICT, "COACHING_ALREADY_ACTIVE", "이미 진행 중인 코칭 세션이 있습니다.");
        }

        var plan = coachingPlanService.getPlan(memberId, request.mealId());
        Integer firstStageSeconds = plan.stages().get(0).recommendedSeconds();
        // PostgreSQL timestamp 정밀도와 맞춰 멱등 재응답의 시각 문자열도 동일하게 유지한다.
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        CoachingSession session = sessionRepository.save(CoachingSession.start(request.mealId(), memberId,
                request.planVersion(), plan.stages().size(), firstStageSeconds, idempotencyKey, requestHash, now));
        return CoachingSessionResponse.from(session);
    }

    @Transactional
    public UpdateCoachingStageResponse updateStage(UUID memberId, UUID sessionId,
                                                   UpdateCoachingStageRequest request) {
        ProgressAction action = request == null ? null : ProgressAction.parse(request.action());
        if (action == null || request.expectedStage() == null || request.expectedStage() < 1
                || request.occurredAt() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "COACHING_ACTION_INVALID",
                    "동작, 현재 단계와 동작 시각이 올바르지 않습니다.");
        }
        CoachingSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COACHING_SESSION_NOT_FOUND",
                        "코칭 세션을 찾을 수 없습니다."));
        if (!session.belongsTo(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "COACHING_SESSION_FORBIDDEN",
                    "다른 사용자의 코칭 세션에 접근할 수 없습니다.");
        }
        if (session.getStatus() != CoachingSessionStatus.IN_PROGRESS) {
            throw new BusinessException(HttpStatus.GONE, "COACHING_SESSION_ENDED", "이미 종료된 코칭 세션입니다.");
        }
        if (session.getCurrentStage() != request.expectedStage()) {
            throw new BusinessException(HttpStatus.CONFLICT, "COACHING_STAGE_CONFLICT",
                    "서버의 현재 단계와 요청 단계가 일치하지 않습니다.");
        }
        if (session.isLastStage()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "COACHING_ACTION_INVALID",
                    "마지막 단계에서는 세션 종료를 요청해 주세요.");
        }

        Instant receivedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        CoachingSession.StageAdvance advance = session.advance(action, request.occurredAt(), receivedAt);
        stageRecordRepository.save(CoachingStageRecord.from(sessionId, action, advance));
        sessionRepository.save(session);
        return new UpdateCoachingStageResponse(sessionId, session.getCurrentStage(), session.getStatus(),
                new UpdateCoachingStageResponse.PreviousStage(advance.stage(), advance.result(), advance.actualSeconds()),
                session.getStageEndsAt());
    }

    @Transactional
    public CoachingTimerResponse updateTimer(UUID memberId, UUID sessionId, UpdateCoachingTimerRequest request) {
        TimerAction action = request == null ? null : TimerAction.parse(request.action());
        if (action == null || request.expectedStage() == null || request.expectedStage() < 1
                || request.occurredAt() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "COACHING_TIMER_ACTION_INVALID",
                    "타이머 동작, 현재 단계와 동작 시각이 올바르지 않습니다.");
        }
        CoachingSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COACHING_SESSION_NOT_FOUND",
                        "코칭 세션을 찾을 수 없습니다."));
        if (!session.belongsTo(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "COACHING_SESSION_FORBIDDEN",
                    "다른 사용자의 코칭 세션에 접근할 수 없습니다.");
        }
        if (session.getStatus() == CoachingSessionStatus.COMPLETED
                || session.getStatus() == CoachingSessionStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.GONE, "COACHING_SESSION_ENDED", "이미 종료된 코칭 세션입니다.");
        }
        if (session.getCurrentStage() != request.expectedStage()) {
            throw new BusinessException(HttpStatus.CONFLICT, "COACHING_STAGE_CONFLICT",
                    "서버의 현재 단계와 요청 단계가 일치하지 않습니다.");
        }
        if ((action == TimerAction.PAUSE && session.getStatus() != CoachingSessionStatus.IN_PROGRESS)
                || (action == TimerAction.RESUME && session.getStatus() != CoachingSessionStatus.PAUSED)) {
            throw new BusinessException(HttpStatus.CONFLICT, "COACHING_TIMER_STATE_CONFLICT",
                    "현재 타이머 상태에서 요청한 동작을 수행할 수 없습니다.");
        }

        Instant receivedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        if (action == TimerAction.PAUSE) session.pause(receivedAt);
        else session.resume(receivedAt);
        sessionRepository.save(session);
        return CoachingTimerResponse.from(session, receivedAt);
    }

    @Transactional
    public CompleteCoachingSessionResponse complete(UUID memberId, UUID sessionId, UUID idempotencyKey,
                                                     CompleteCoachingSessionRequest request) {
        CompletionReason reason = request == null ? null : CompletionReason.parse(request.reason());
        if (idempotencyKey == null || reason == null || request.endedAt() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "COACHING_COMPLETION_INVALID",
                    "종료 사유, 종료 시각과 Idempotency-Key가 필요합니다.");
        }
        String requestHash = hash(sessionId + ":" + reason + ":" + request.endedAt());
        var existing = coachingRecordRepository
                .findFirstByMemberIdAndIdempotencyKeyAndCreatedAtAfterOrderByCreatedAtDesc(
                        memberId, idempotencyKey, Instant.now().minus(Duration.ofHours(24)));
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new BusinessException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_CONFLICT",
                        "동일한 Idempotency-Key를 다른 요청에 사용할 수 없습니다.");
            }
            return completionResponse(existing.get());
        }

        CoachingSession session = sessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "COACHING_SESSION_NOT_FOUND",
                        "코칭 세션을 찾을 수 없습니다."));
        if (!session.belongsTo(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "COACHING_SESSION_FORBIDDEN",
                    "다른 사용자의 코칭 세션에 접근할 수 없습니다.");
        }
        if (session.getStatus() != CoachingSessionStatus.IN_PROGRESS
                && session.getStatus() != CoachingSessionStatus.PAUSED) {
            throw new BusinessException(HttpStatus.CONFLICT, "COACHING_ALREADY_COMPLETED", "이미 종료된 코칭 세션입니다.");
        }
        if (reason == CompletionReason.COMPLETED && !session.isLastStage()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "COACHING_COMPLETION_INVALID",
                    "마지막 단계에서만 완료할 수 있습니다.");
        }

        Instant completedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        ProgressAction finalAction = reason == CompletionReason.COMPLETED
                ? ProgressAction.COMPLETE : ProgressAction.USER_END;
        CoachingSession.StageAdvance finalAdvance = session.finishCurrentStage(reason, request.endedAt(), completedAt);
        stageRecordRepository.saveAndFlush(CoachingStageRecord.from(sessionId, finalAction, finalAdvance));
        var records = stageRecordRepository.findAllBySessionIdOrderByStageAsc(sessionId);
        int completedStages = (int) records.stream().filter(record -> record.getResult() == StageResult.COMPLETED).count();
        int skippedStages = (int) records.stream().filter(record -> record.getResult() == StageResult.SKIPPED).count();
        long totalSeconds = records.stream().mapToLong(CoachingStageRecord::getActualSeconds).sum();

        session.complete(completedAt);
        sessionRepository.save(session);
        CoachingRecord record = coachingRecordRepository.save(CoachingRecord.create(session, reason,
                completedStages, skippedStages, totalSeconds, request.endedAt(), completedAt,
                idempotencyKey, requestHash));
        return completionResponse(record);
    }

    private CompleteCoachingSessionResponse completionResponse(CoachingRecord record) {
        var plan = coachingPlanService.getPlan(record.getMemberId(), record.getMealId());
        var stages = stageRecordRepository.findAllBySessionIdOrderByStageAsc(record.getSessionId());
        return CompleteCoachingSessionResponse.from(record, plan, stages);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
