package com.likelion.firstbite.firstbiteserver.feedback.service;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingRecord;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingRecordRepository;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.feedback.domain.CoachingFeedback;
import com.likelion.firstbite.firstbiteserver.feedback.dto.*;
import com.likelion.firstbite.firstbiteserver.feedback.repository.CoachingFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final int RESPONSE_WINDOW_DAYS = 2;
    private final CoachingFeedbackRepository feedbackRepository;
    private final CoachingRecordRepository recordRepository;
    private final PersonalizationService personalizationService;

    @Transactional(readOnly = true)
    public PendingFeedbackResponse getPending(UUID memberId, LocalDate date) {
        LocalDate target = date == null ? LocalDate.now(SERVICE_ZONE).minusDays(1) : date;
        Instant from = target.atStartOfDay(SERVICE_ZONE).toInstant();
        Instant to = target.plusDays(1).atStartOfDay(SERVICE_ZONE).toInstant();
        Instant now = Instant.now();
        return recordRepository.findHistoryRange(memberId, from, to).stream()
                .filter(record -> feedbackRepository.findByRecordId(record.getId()).isEmpty())
                .filter(record -> !now.isBefore(eligibleStart(record)))
                .filter(record -> !now.isAfter(expiresExclusive(record)))
                .findFirst()
                .map(record -> new PendingFeedbackResponse(true, record.getId(), "어제 오후는 어땠어요?",
                        new PendingFeedbackResponse.Scale(1, 5), expiresExclusive(record).minusSeconds(1)))
                .orElseGet(PendingFeedbackResponse::none);
    }

    @Transactional
    public SubmitFeedbackResponse submit(UUID memberId, UUID recordId, UUID key, SubmitFeedbackRequest request) {
        validateRequest(key, request);
        boolean skipped = Boolean.TRUE.equals(request.skipped());
        String requestHash = hash(recordId + ":" + request.sleepinessScore() + ":" + skipped + ":" + request.answeredAt());
        Instant now = Instant.now();
        var replay = feedbackRepository.findFirstByMemberIdAndIdempotencyKeyAndCreatedAtAfterOrderByCreatedAtDesc(
                memberId, key, now.minus(IDEMPOTENCY_TTL));
        if (replay.isPresent()) {
            if (!replay.get().getRequestHash().equals(requestHash)) throw new BusinessException(HttpStatus.CONFLICT,
                    "IDEMPOTENCY_KEY_CONFLICT", "동일한 키를 다른 요청에 사용할 수 없습니다.");
            return response(replay.get());
        }
        CoachingRecord record = recordRepository.findById(recordId)
                .filter(value -> value.getMemberId().equals(memberId))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "COACHING_RECORD_NOT_FOUND", "코칭 기록을 찾을 수 없습니다."));
        if (feedbackRepository.findByRecordId(recordId).isPresent()) throw new BusinessException(HttpStatus.CONFLICT,
                "FEEDBACK_ALREADY_EXISTS", "이미 피드백을 제출한 기록입니다.");
        if (!eligible(record, request.answeredAt(), now)) throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                "FEEDBACK_NOT_ELIGIBLE", "아직 응답 대상이 아니거나 응답 기간이 지났습니다.");

        CoachingFeedback feedback = feedbackRepository.save(CoachingFeedback.create(recordId, memberId,
                request.sleepinessScore(), skipped, request.answeredAt(), key, requestHash, now));
        boolean updated = false;
        if (!skipped) {
            personalizationService.refresh(memberId, now);
            updated = feedbackRepository.countByMemberIdAndSkippedFalse(memberId) >= PersonalizationService.ACTIVATION_COUNT;
        }
        int count = Math.toIntExact(feedbackRepository.countByMemberIdAndSkippedFalse(memberId));
        feedback.markResult(count, updated);
        return response(feedbackRepository.save(feedback));
    }

    private void validateRequest(UUID key, SubmitFeedbackRequest request) {
        if (key == null || request == null || request.skipped() == null || request.answeredAt() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FEEDBACK_VALUE_INVALID", "필수 피드백 값이 없습니다.");
        }
        boolean skipped = request.skipped();
        Integer score = request.sleepinessScore();
        if (skipped && score != null || !skipped && (score == null || score < 1 || score > 5)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FEEDBACK_VALUE_INVALID", "점수와 건너뛰기 조합이 올바르지 않습니다.");
        }
    }

    private boolean eligible(CoachingRecord record, Instant answeredAt, Instant now) {
        Instant start = eligibleStart(record);
        Instant end = expiresExclusive(record);
        return !answeredAt.isBefore(start) && answeredAt.isBefore(end) && !answeredAt.isAfter(now.plusSeconds(60));
    }

    private Instant eligibleStart(CoachingRecord record) {
        return record.getCompletedAt().atZone(SERVICE_ZONE).toLocalDate().plusDays(1)
                .atStartOfDay(SERVICE_ZONE).toInstant();
    }

    private Instant expiresExclusive(CoachingRecord record) {
        return record.getCompletedAt().atZone(SERVICE_ZONE).toLocalDate().plusDays(1 + RESPONSE_WINDOW_DAYS)
                .atStartOfDay(SERVICE_ZONE).toInstant();
    }

    private SubmitFeedbackResponse response(CoachingFeedback feedback) {
        return new SubmitFeedbackResponse(feedback.getId(), feedback.getRecordId(), feedback.getSleepinessScore(),
                feedback.getFeedbackCount(), feedback.isPersonalizationUpdated());
    }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
