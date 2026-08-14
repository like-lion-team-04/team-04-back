package com.likelion.firstbite.firstbiteserver.history.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meal_reuses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MealReuse {
    @Id private UUID id;
    @Column(name="member_id", nullable=false) private UUID memberId;
    @Column(name="source_record_id", nullable=false) private UUID sourceRecordId;
    @Column(name="new_meal_id", nullable=false) private UUID newMealId;
    @Column(name="idempotency_key", nullable=false) private UUID idempotencyKey;
    @Column(name="request_hash", nullable=false, length=64) private String requestHash;
    @Column(name="copied_item_count", nullable=false) private int copiedItemCount;
    @Column(name="created_at", nullable=false) private Instant createdAt;

    public static MealReuse create(UUID memberId, UUID sourceRecordId, UUID newMealId,
                                   UUID key, String hash, int count, Instant now) {
        MealReuse reuse = new MealReuse();
        reuse.id = UUID.randomUUID(); reuse.memberId = memberId; reuse.sourceRecordId = sourceRecordId;
        reuse.newMealId = newMealId; reuse.idempotencyKey = key; reuse.requestHash = hash;
        reuse.copiedItemCount = count; reuse.createdAt = now;
        return reuse;
    }
}
