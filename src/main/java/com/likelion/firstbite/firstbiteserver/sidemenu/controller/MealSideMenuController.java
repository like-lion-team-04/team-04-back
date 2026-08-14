package com.likelion.firstbite.firstbiteserver.sidemenu.controller;

import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import com.likelion.firstbite.firstbiteserver.sidemenu.dto.AddSideMenuRequest;
import com.likelion.firstbite.firstbiteserver.sidemenu.dto.AddSideMenuResponse;
import com.likelion.firstbite.firstbiteserver.sidemenu.dto.RemoveSideMenuResponse;
import com.likelion.firstbite.firstbiteserver.sidemenu.service.MealSideMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/meals/{mealId}/side-menus")
@RequiredArgsConstructor
public class MealSideMenuController {
    private final MealSideMenuService sideMenuService;

    @PostMapping
    public ApiResponse<AddSideMenuResponse> add(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID mealId,
            @RequestBody AddSideMenuRequest request) {
        return ApiResponse.success(sideMenuService.add(memberId, mealId, request));
    }

    @DeleteMapping("/{sideMenuId}")
    public ApiResponse<RemoveSideMenuResponse> remove(
            @AuthenticationPrincipal UUID memberId,
            @PathVariable UUID mealId,
            @PathVariable UUID sideMenuId) {
        return ApiResponse.success(sideMenuService.remove(memberId, mealId, sideMenuId));
    }
}
