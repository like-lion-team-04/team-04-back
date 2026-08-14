package com.likelion.firstbite.firstbiteserver.meal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "meals", indexes = @Index(name = "idx_meals_member_created", columnList = "member_id,created_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Meal {
    @Id private UUID id;
    @Column(name = "member_id", nullable = false) private UUID memberId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MealSource source;
    @Column(name = "recognition_id") private UUID recognitionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MealStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "meal", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt asc")
    private List<MealItem> items = new ArrayList<>();

    public static Meal draft(UUID memberId, MealSource source, UUID recognitionId) {
        Meal meal = new Meal();
        meal.id = UUID.randomUUID();
        meal.memberId = memberId;
        meal.source = source;
        meal.recognitionId = recognitionId;
        meal.status = MealStatus.DRAFT;
        meal.createdAt = Instant.now();
        meal.updatedAt = meal.createdAt;
        return meal;
    }

    public void addItem(MealItem item) {
        items.add(item);
        item.attachTo(this);
    }

    public void clearItems() {
        items.clear();
        updatedAt = Instant.now();
    }

    public boolean belongsTo(UUID memberId) {
        return this.memberId.equals(memberId);
    }

    public void markAnalyzed() {
        status = MealStatus.ANALYZED;
        updatedAt = Instant.now();
    }
}
