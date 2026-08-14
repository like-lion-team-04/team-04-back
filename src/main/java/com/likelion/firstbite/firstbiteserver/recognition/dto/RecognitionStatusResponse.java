package com.likelion.firstbite.firstbiteserver.recognition.dto;

import com.likelion.firstbite.firstbiteserver.recognition.domain.RecognitionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecognitionStatusResponse(
        UUID recognitionId,
        RecognitionStatus status,
        List<Item> items,
        List<String> warnings,
        Error error
) {
    public static RecognitionStatusResponse processing(UUID id) {
        return new RecognitionStatusResponse(id, RecognitionStatus.PROCESSING, null, null, null);
    }

    public static RecognitionStatusResponse completed(UUID id, List<Item> items, List<String> warnings) {
        return new RecognitionStatusResponse(id, RecognitionStatus.COMPLETED, items, warnings, null);
    }

    public static RecognitionStatusResponse failed(UUID id, String code, String message) {
        return new RecognitionStatusResponse(id, RecognitionStatus.FAILED, null, null, new Error(code, message));
    }

    public record Item(String temporaryItemId, String recognizedName, BigDecimal confidence,
                       List<Candidate> candidates, BigDecimal estimatedServing, boolean estimated) {}
    public record Candidate(UUID foodId, String name, BigDecimal confidence) {}
    public record Error(String code, String message) {}
}
