package com.likelion.firstbite.firstbiteserver.feedback.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coaching_feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoachingFeedback {
    @Id private UUID id;
    @Column(name = "record_id", nullable = false, unique = true) private UUID recordId;
    @Column(name = "member_id", nullable = false) private UUID memberId;
    @Column(name = "sleepiness_score") private Integer sleepinessScore;
    @Column(nullable = false) private boolean skipped;
    @Column(name = "answered_at", nullable = false) private Instant answeredAt;
    @Column(name = "idempotency_key", nullable = false) private UUID idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Column(name = "feedback_count", nullable = false) private int feedbackCount;
    @Column(name = "personalization_updated", nullable = false) private boolean personalizationUpdated;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    public static CoachingFeedback create(UUID recordId, UUID memberId, Integer score, boolean skipped,
                                          Instant answeredAt, UUID key, String hash, Instant now) {
        CoachingFeedback feedback = new CoachingFeedback();
        feedback.id = UUID.randomUUID(); feedback.recordId = recordId; feedback.memberId = memberId;
        feedback.sleepinessScore = score; feedback.skipped = skipped; feedback.answeredAt = answeredAt;
        feedback.idempotencyKey = key; feedback.requestHash = hash; feedback.createdAt = now;
        return feedback;
    }

    public void markResult(int feedbackCount, boolean personalizationUpdated) {
        this.feedbackCount = feedbackCount;
        this.personalizationUpdated = personalizationUpdated;
    }
}
