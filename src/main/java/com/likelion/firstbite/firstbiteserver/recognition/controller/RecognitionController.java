package com.likelion.firstbite.firstbiteserver.recognition.controller;

import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import com.likelion.firstbite.firstbiteserver.recognition.domain.ImageType;
import com.likelion.firstbite.firstbiteserver.recognition.dto.RecognitionAcceptedResponse;
import com.likelion.firstbite.firstbiteserver.recognition.dto.RecognitionStatusResponse;
import com.likelion.firstbite.firstbiteserver.recognition.service.RecognitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/recognitions") @RequiredArgsConstructor
public class RecognitionController {
    private final RecognitionService service;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<RecognitionAcceptedResponse> create(@AuthenticationPrincipal UUID memberId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestPart("image") MultipartFile image,
            @RequestParam(value = "imageType", defaultValue = "FOOD_PHOTO") ImageType imageType) {
        return ApiResponse.success(service.create(memberId, idempotencyKey, image, imageType));
    }

    @GetMapping("/{recognitionId}")
    public ApiResponse<RecognitionStatusResponse> getStatus(@AuthenticationPrincipal UUID memberId,
                                                             @PathVariable UUID recognitionId) {
        return ApiResponse.success(service.getStatus(memberId, recognitionId));
    }
}
