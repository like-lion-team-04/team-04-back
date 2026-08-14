package com.likelion.firstbite.firstbiteserver.coaching.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coaching_stage_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_coaching_stage_records_session_stage", columnNames = {"session_id", "stage"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoachingStageRecord {
    @Id private UUID id;
    @Column(name = "session_id", nullable = false) private UUID sessionId;
    @Column(nullable = false) private int stage;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ProgressAction action;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StageResult result;
    @Column(name = "actual_seconds", nullable = false) private long actualSeconds;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "received_at", nullable = false) private Instant receivedAt;

    public static CoachingStageRecord from(UUID sessionId, ProgressAction action,
                                           CoachingSession.StageAdvance advance) {
        CoachingStageRecord record = new CoachingStageRecord();
        record.id = UUID.randomUUID();
        record.sessionId = sessionId;
        record.stage = advance.stage();
        record.action = action;
        record.result = advance.result();
        record.actualSeconds = advance.actualSeconds();
        record.occurredAt = advance.occurredAt();
        record.receivedAt = advance.receivedAt();
        return record;
    }
}
