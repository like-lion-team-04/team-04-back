package com.likelion.firstbite.firstbiteserver.meal.controller;

import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import com.likelion.firstbite.firstbiteserver.meal.dto.CreateMealRequest;
import com.likelion.firstbite.firstbiteserver.meal.dto.CreateMealResponse;
import com.likelion.firstbite.firstbiteserver.meal.dto.UpdateMealItemsRequest;
import com.likelion.firstbite.firstbiteserver.meal.dto.UpdateMealItemsResponse;
import com.likelion.firstbite.firstbiteserver.meal.service.MealService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meals")
@RequiredArgsConstructor
public class MealController {
    private final MealService mealService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateMealResponse> create(
            @AuthenticationPrincipal UUID memberId,
            @RequestBody CreateMealRequest request) {
        return ApiResponse.success(mealService.createDraft(memberId, request));
    }

    @PutMapping("/{mealId}/items")
    public ApiResponse<UpdateMealItemsResponse> replaceItems(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID mealId,
            @RequestBody UpdateMealItemsRequest request) {
        return ApiResponse.success(mealService.replaceItems(memberId, mealId, request));
    }
}
