package com.likelion.firstbite.firstbiteserver.recognition.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recognitions", indexes = {
        @Index(name = "idx_recognitions_member_key_created", columnList = "member_id,idempotency_key,created_at"),
        @Index(name = "idx_recognitions_member_created", columnList = "member_id,created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recognition {
    @Id private UUID id;
    @Column(name = "member_id", nullable = false) private UUID memberId;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "image_id", nullable = false, unique = true)
    private RecognitionImage image;
    @Column(name = "idempotency_key", nullable = false) private UUID idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @Enumerated(EnumType.STRING) @Column(name = "image_type", length = 30) private ImageType imageType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private RecognitionStatus status;
    @Column(name = "result_json", columnDefinition = "text") private String resultJson;
    @Column(name = "error_code", length = 50) private String errorCode;
    @Column(name = "error_message", length = 300) private String errorMessage;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public static Recognition processing(UUID memberId, RecognitionImage image, UUID idempotencyKey,
                                         String requestHash, ImageType imageType, Instant now) {
        Recognition recognition = new Recognition();
        recognition.id = UUID.randomUUID();
        recognition.memberId = memberId;
        recognition.image = image;
        recognition.idempotencyKey = idempotencyKey;
        recognition.requestHash = requestHash;
        recognition.imageType = imageType;
        recognition.status = RecognitionStatus.PROCESSING;
        recognition.createdAt = now;
        recognition.updatedAt = now;
        return recognition;
    }

    public void complete(String resultJson) {
        if (status != RecognitionStatus.PROCESSING) return;
        this.status = RecognitionStatus.COMPLETED;
        this.resultJson = resultJson;
        this.updatedAt = Instant.now();
    }

    public void fail(String code, String message) {
        if (status != RecognitionStatus.PROCESSING) return;
        this.status = RecognitionStatus.FAILED;
        this.errorCode = code;
        this.errorMessage = message;
        this.updatedAt = Instant.now();
    }
}
