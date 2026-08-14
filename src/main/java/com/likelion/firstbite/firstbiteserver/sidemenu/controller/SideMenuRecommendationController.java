package com.likelion.firstbite.firstbiteserver.sidemenu.controller;

import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import com.likelion.firstbite.firstbiteserver.sidemenu.dto.SideMenuRecommendationsResponse;
import com.likelion.firstbite.firstbiteserver.sidemenu.service.SideMenuRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meals/{mealId}/side-menu-recommendations")
@RequiredArgsConstructor
public class SideMenuRecommendationController {
    private final SideMenuRecommendationService recommendationService;

    @GetMapping
    public ApiResponse<SideMenuRecommendationsResponse> recommend(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID mealId,
            @RequestParam(defaultValue = "3") int limit) {
        return ApiResponse.success(recommendationService.recommend(memberId, mealId, limit));
    }
}
