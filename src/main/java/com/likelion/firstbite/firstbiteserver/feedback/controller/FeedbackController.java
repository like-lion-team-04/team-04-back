package com.likelion.firstbite.firstbiteserver.feedback.controller;

import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import com.likelion.firstbite.firstbiteserver.feedback.dto.*;
import com.likelion.firstbite.firstbiteserver.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackService feedbackService;

    @GetMapping("/api/v1/feedbacks/pending")
    public ApiResponse<PendingFeedbackResponse> getPending(
            @AuthenticationPrincipal UUID memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(feedbackService.getPending(memberId, date));
    }

    @PostMapping("/api/v1/coaching-records/{recordId}/feedback")
    public ResponseEntity<ApiResponse<SubmitFeedbackResponse>> submit(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID recordId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody SubmitFeedbackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(feedbackService.submit(memberId, recordId, idempotencyKey, request)));
    }
}
