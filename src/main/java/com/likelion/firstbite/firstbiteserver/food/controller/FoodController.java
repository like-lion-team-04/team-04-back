package com.likelion.firstbite.firstbiteserver.food.controller;

import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import com.likelion.firstbite.firstbiteserver.food.domain.FoodCategory;
import com.likelion.firstbite.firstbiteserver.food.dto.FoodSearchResponse;
import com.likelion.firstbite.firstbiteserver.food.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/foods")
@RequiredArgsConstructor
public class FoodController {
    private final FoodService foodService;

    @GetMapping
    public ApiResponse<FoodSearchResponse> search(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) FoodCategory category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        FoodService.SearchResult result = foodService.search(query, category, page, size);
        return ApiResponse.success(result.data(), result.meta());
    }
}
