package com.likelion.firstbite.firstbiteserver.coaching.controller;

import com.likelion.firstbite.firstbiteserver.coaching.dto.CoachingPlanResponse;
import com.likelion.firstbite.firstbiteserver.coaching.service.CoachingPlanService;
import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meals/{mealId}/coaching-plan")
@RequiredArgsConstructor
public class CoachingPlanController {
    private final CoachingPlanService coachingPlanService;

    @GetMapping
    public ApiResponse<CoachingPlanResponse> getPlan(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID mealId) {
        return ApiResponse.success(coachingPlanService.getPlan(memberId, mealId));
    }
}
