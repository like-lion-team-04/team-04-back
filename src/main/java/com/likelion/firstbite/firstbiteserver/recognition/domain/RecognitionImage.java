package com.likelion.firstbite.firstbiteserver.recognition.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recognition_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecognitionImage {
    @Id private UUID id;
    @Column(name = "member_id", nullable = false) private UUID memberId;
    @Column(name = "object_key", nullable = false, unique = true, length = 300) private String objectKey;
    @Column(name = "content_type", nullable = false, length = 30) private String contentType;
    @Column(name = "size_bytes", nullable = false) private long sizeBytes;
    @Column(name = "sha256", nullable = false, length = 64) private String sha256;
    @Column(name = "stored_at", nullable = false, updatable = false) private Instant storedAt;

    public static RecognitionImage create(UUID id, UUID memberId, String objectKey, String contentType,
                                          long sizeBytes, String sha256, Instant storedAt) {
        RecognitionImage image = new RecognitionImage();
        image.id = id;
        image.memberId = memberId;
        image.objectKey = objectKey;
        image.contentType = contentType;
        image.sizeBytes = sizeBytes;
        image.sha256 = sha256;
        image.storedAt = storedAt;
        return image;
    }
}
