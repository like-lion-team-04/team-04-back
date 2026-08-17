package com.likelion.firstbite.firstbiteserver.sidemenu.controller;

import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import com.likelion.firstbite.firstbiteserver.sidemenu.dto.SideMenuListResponse;
import com.likelion.firstbite.firstbiteserver.sidemenu.service.SideMenuListService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/side-menus")
@RequiredArgsConstructor
public class SideMenuController {
    private final SideMenuListService service;

    @GetMapping
    public ApiResponse<SideMenuListResponse> get(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String nutrientFocus,
            @RequestParam(defaultValue = "true") boolean activeOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.get(query, category, nutrientFocus, activeOnly, page, size));
    }
}
