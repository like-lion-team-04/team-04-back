package com.likelion.firstbite.firstbiteserver.history.controller;

import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import com.likelion.firstbite.firstbiteserver.history.dto.CoachingHistoryListResponse;
import com.likelion.firstbite.firstbiteserver.history.dto.CoachingHistoryDetailResponse;
import com.likelion.firstbite.firstbiteserver.history.dto.CoachingHistorySummaryResponse;
import com.likelion.firstbite.firstbiteserver.history.dto.ReuseMealRequest;
import com.likelion.firstbite.firstbiteserver.history.dto.ReuseMealResponse;
import com.likelion.firstbite.firstbiteserver.history.service.CoachingHistoryDetailService;
import com.likelion.firstbite.firstbiteserver.history.service.CoachingHistorySummaryService;
import com.likelion.firstbite.firstbiteserver.history.service.MealReuseService;
import com.likelion.firstbite.firstbiteserver.history.service.CoachingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/coaching-records")
@RequiredArgsConstructor
public class CoachingHistoryController {
    private final CoachingHistoryService historyService;
    private final CoachingHistoryDetailService detailService;
    private final CoachingHistorySummaryService summaryService;
    private final MealReuseService reuseService;

    @GetMapping
    public ApiResponse<CoachingHistoryListResponse> getHistory(
            @AuthenticationPrincipal UUID memberId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = historyService.getHistory(memberId, from, to, page, size);
        return ApiResponse.success(result.data(), result.meta());
    }

    @GetMapping("/{recordId}")
    public ApiResponse<CoachingHistoryDetailResponse> getDetail(
            @AuthenticationPrincipal UUID memberId, @PathVariable UUID recordId) {
        return ApiResponse.success(detailService.getDetail(memberId, recordId));
    }

    @GetMapping("/summary")
    public ApiResponse<CoachingHistorySummaryResponse> getSummary(
            @AuthenticationPrincipal UUID memberId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String timezone) {
        return ApiResponse.success(summaryService.summarize(memberId, from, to, timezone));
    }

    @PostMapping("/{recordId}/reuse")
    public ResponseEntity<ApiResponse<ReuseMealResponse>> reuse(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID recordId,
            @RequestHeader("Idempotency-Key") UUID idempotencyKey,
            @RequestBody(required = false) ReuseMealRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(reuseService.reuse(memberId, recordId, idempotencyKey, request)));
    }
}
