package com.likelion.firstbite.firstbiteserver.feedback.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "personalization_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalizationProfile {
    @Id @Column(name = "member_id") private UUID memberId;
    @Column(name = "feedback_count", nullable = false) private int feedbackCount;
    @Column(precision = 4, scale = 2) private BigDecimal coefficient;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PersonalizationDirection direction;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public static PersonalizationProfile initial(UUID memberId, Instant now) {
        PersonalizationProfile profile = new PersonalizationProfile();
        profile.memberId = memberId; profile.feedbackCount = 0; profile.coefficient = null;
        profile.direction = PersonalizationDirection.STANDARD; profile.updatedAt = now;
        return profile;
    }

    public void update(int count, BigDecimal coefficient, PersonalizationDirection direction, Instant now) {
        this.feedbackCount = count; this.coefficient = coefficient; this.direction = direction; this.updatedAt = now;
    }
}
