package com.likelion.firstbite.firstbiteserver.coaching.controller;

import com.likelion.firstbite.firstbiteserver.coaching.dto.CoachingSessionResponse;
import com.likelion.firstbite.firstbiteserver.coaching.dto.StartCoachingSessionRequest;
import com.likelion.firstbite.firstbiteserver.coaching.dto.UpdateCoachingStageRequest;
import com.likelion.firstbite.firstbiteserver.coaching.dto.UpdateCoachingStageResponse;
import com.likelion.firstbite.firstbiteserver.coaching.dto.CompleteCoachingSessionRequest;
import com.likelion.firstbite.firstbiteserver.coaching.dto.CompleteCoachingSessionResponse;
import com.likelion.firstbite.firstbiteserver.coaching.service.CoachingSessionService;
import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coaching-sessions")
@RequiredArgsConstructor
public class CoachingSessionController {
    private final CoachingSessionService sessionService;

    @PostMapping
    public ResponseEntity<ApiResponse<CoachingSessionResponse>> start(
            @AuthenticationPrincipal UUID memberId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody StartCoachingSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sessionService.start(memberId, idempotencyKey, request)));
    }

    @PatchMapping("/{sessionId}")
    public ApiResponse<UpdateCoachingStageResponse> updateStage(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID sessionId,
            @RequestBody UpdateCoachingStageRequest request) {
        return ApiResponse.success(sessionService.updateStage(memberId, sessionId, request));
    }

    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<ApiResponse<CompleteCoachingSessionResponse>> complete(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID sessionId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody CompleteCoachingSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sessionService.complete(memberId, sessionId, idempotencyKey, request)));
    }
}
