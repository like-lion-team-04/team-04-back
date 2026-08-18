package com.likelion.firstbite.firstbiteserver.coaching.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

@Entity
@Table(name = "coaching_sessions", indexes = {
        @Index(name = "idx_coaching_sessions_member_status", columnList = "member_id,status"),
        @Index(name = "idx_coaching_sessions_idempotency", columnList = "member_id,idempotency_key,created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoachingSession {
    @Id private UUID id;
    @Column(name = "meal_id", nullable = false) private UUID mealId;
    @Column(name = "member_id", nullable = false) private UUID memberId;
    @Column(name = "plan_version", nullable = false) private int planVersion;
    @Column(name = "total_stages", nullable = false) private int totalStages;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CoachingSessionStatus status;
    @Column(name = "current_stage", nullable = false) private int currentStage;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "stage_started_at", nullable = false) private Instant stageStartedAt;
    @Column(name = "stage_ends_at") private Instant stageEndsAt;
    @Column(name = "paused_at") private Instant pausedAt;
    @Column(name = "paused_remaining_seconds") private Long pausedRemainingSeconds;
    @Column(name = "completed_at") private Instant completedAt;
    @Column(name = "idempotency_key", nullable = false) private UUID idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public static CoachingSession start(UUID mealId, UUID memberId, int planVersion, int totalStages,
                                        Integer firstStageSeconds, UUID idempotencyKey, String requestHash,
                                        Instant now) {
        CoachingSession session = new CoachingSession();
        session.id = UUID.randomUUID();
        session.mealId = mealId;
        session.memberId = memberId;
        session.planVersion = planVersion;
        session.totalStages = totalStages;
        session.status = CoachingSessionStatus.IN_PROGRESS;
        session.currentStage = 1;
        session.startedAt = now;
        session.stageStartedAt = now;
        session.stageEndsAt = firstStageSeconds == null ? null : now.plusSeconds(firstStageSeconds);
        session.idempotencyKey = idempotencyKey;
        session.requestHash = requestHash;
        session.createdAt = now;
        session.updatedAt = now;
        return session;
    }

    public StageAdvance advance(ProgressAction action, Instant occurredAt, Instant receivedAt) {
        int previousStage = currentStage;
        long actualSeconds = Math.max(0, Duration.between(stageStartedAt, receivedAt).getSeconds());
        StageResult result = action == ProgressAction.SKIP ? StageResult.SKIPPED : StageResult.COMPLETED;
        currentStage++;
        stageStartedAt = receivedAt;
        stageEndsAt = currentStage < totalStages ? receivedAt.plusSeconds(300) : null;
        updatedAt = receivedAt;
        return new StageAdvance(previousStage, result, actualSeconds, occurredAt, receivedAt);
    }

    public void pause(Instant receivedAt) {
        if (status != CoachingSessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("진행 중인 세션만 일시정지할 수 있습니다.");
        }
        pausedRemainingSeconds = stageEndsAt == null
                ? null
                : Math.max(0, Duration.between(receivedAt, stageEndsAt).getSeconds());
        pausedAt = receivedAt;
        stageEndsAt = null;
        status = CoachingSessionStatus.PAUSED;
        updatedAt = receivedAt;
    }

    public void resume(Instant receivedAt) {
        if (status != CoachingSessionStatus.PAUSED) {
            throw new IllegalStateException("일시정지된 세션만 재개할 수 있습니다.");
        }
        Duration pausedDuration = Duration.between(pausedAt, receivedAt);
        stageStartedAt = stageStartedAt.plus(pausedDuration);
        stageEndsAt = pausedRemainingSeconds == null ? null : receivedAt.plusSeconds(pausedRemainingSeconds);
        pausedAt = null;
        pausedRemainingSeconds = null;
        status = CoachingSessionStatus.IN_PROGRESS;
        updatedAt = receivedAt;
    }

    public long remainingSeconds(Instant now) {
        if (status == CoachingSessionStatus.PAUSED) {
            return pausedRemainingSeconds == null ? 0 : pausedRemainingSeconds;
        }
        return stageEndsAt == null ? 0 : Math.max(0, Duration.between(now, stageEndsAt).getSeconds());
    }

    public StageAdvance finishCurrentStage(CompletionReason reason, Instant occurredAt, Instant receivedAt) {
        Instant effectiveEnd = status == CoachingSessionStatus.PAUSED ? pausedAt : receivedAt;
        long actualSeconds = Math.max(0, Duration.between(stageStartedAt, effectiveEnd).getSeconds());
        StageResult result = reason == CompletionReason.COMPLETED ? StageResult.COMPLETED : StageResult.SKIPPED;
        return new StageAdvance(currentStage, result, actualSeconds, occurredAt, receivedAt);
    }

    public void complete(Instant completedAt) {
        status = CoachingSessionStatus.COMPLETED;
        this.completedAt = completedAt;
        stageEndsAt = null;
        pausedAt = null;
        pausedRemainingSeconds = null;
        updatedAt = completedAt;
    }

    public boolean belongsTo(UUID memberId) {
        return this.memberId.equals(memberId);
    }

    public boolean isLastStage() {
        return currentStage >= totalStages;
    }

    public record StageAdvance(int stage, StageResult result, long actualSeconds,
                               Instant occurredAt, Instant receivedAt) {}
}
