package com.likelion.firstbite.firstbiteserver.recognition.dto;

import com.likelion.firstbite.firstbiteserver.recognition.domain.Recognition;
import com.likelion.firstbite.firstbiteserver.recognition.domain.RecognitionStatus;
import java.time.Instant;
import java.util.UUID;

public record RecognitionAcceptedResponse(UUID recognitionId, UUID imageId, RecognitionStatus status,
                                          String statusUrl, Instant storedAt) {
    public static RecognitionAcceptedResponse from(Recognition value) {
        return new RecognitionAcceptedResponse(value.getId(), value.getImage().getId(), value.getStatus(),
                "/api/v1/recognitions/" + value.getId(), value.getImage().getStoredAt());
    }
}
