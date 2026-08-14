package com.likelion.firstbite.firstbiteserver.analysis.controller;

import com.likelion.firstbite.firstbiteserver.analysis.dto.AnalysisResponse;
import com.likelion.firstbite.firstbiteserver.analysis.dto.AnalysisDetailResponse;
import com.likelion.firstbite.firstbiteserver.analysis.dto.CreateAnalysisRequest;
import com.likelion.firstbite.firstbiteserver.analysis.service.MealAnalysisService;
import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meals/{mealId}/analysis")
@RequiredArgsConstructor
public class MealAnalysisController {
    private final MealAnalysisService analysisService;

    @PostMapping
    public ResponseEntity<ApiResponse<AnalysisResponse>> analyze(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID mealId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody(required = false) CreateAnalysisRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(analysisService.analyze(memberId, mealId, idempotencyKey, request)));
    }

    @GetMapping
    public ApiResponse<AnalysisDetailResponse> getLatest(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID mealId) {
        return ApiResponse.success(analysisService.getLatest(memberId, mealId));
    }
}
