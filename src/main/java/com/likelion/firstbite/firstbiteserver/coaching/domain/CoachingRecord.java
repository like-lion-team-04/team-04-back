package com.likelion.firstbite.firstbiteserver.coaching.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coaching_records", indexes = {
        @Index(name = "idx_coaching_records_member_completed", columnList = "member_id,completed_at"),
        @Index(name = "idx_coaching_records_idempotency", columnList = "member_id,idempotency_key,created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoachingRecord {
    @Id private UUID id;
    @Column(name = "session_id", nullable = false, unique = true) private UUID sessionId;
    @Column(name = "meal_id", nullable = false) private UUID mealId;
    @Column(name = "member_id", nullable = false) private UUID memberId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private CompletionReason reason;
    @Column(name = "completed_stages", nullable = false) private int completedStages;
    @Column(name = "skipped_stages", nullable = false) private int skippedStages;
    @Column(name = "total_stages", nullable = false) private int totalStages;
    @Column(name = "total_seconds", nullable = false) private long totalSeconds;
    @Column(name = "client_ended_at", nullable = false) private Instant clientEndedAt;
    @Column(name = "completed_at", nullable = false) private Instant completedAt;
    @Column(name = "idempotency_key", nullable = false) private UUID idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public static CoachingRecord create(CoachingSession session, CompletionReason reason,
                                        int completedStages, int skippedStages, long totalSeconds,
                                        Instant clientEndedAt, Instant completedAt,
                                        UUID idempotencyKey, String requestHash) {
        CoachingRecord record = new CoachingRecord();
        record.id = UUID.randomUUID();
        record.sessionId = session.getId();
        record.mealId = session.getMealId();
        record.memberId = session.getMemberId();
        record.reason = reason;
        record.completedStages = completedStages;
        record.skippedStages = skippedStages;
        record.totalStages = session.getTotalStages();
        record.totalSeconds = totalSeconds;
        record.clientEndedAt = clientEndedAt;
        record.completedAt = completedAt;
        record.idempotencyKey = idempotencyKey;
        record.requestHash = requestHash;
        record.createdAt = completedAt;
        return record;
    }
}
