package com.likelion.firstbite.firstbiteserver.feedback.controller;

import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import com.likelion.firstbite.firstbiteserver.feedback.dto.PersonalizationResponse;
import com.likelion.firstbite.firstbiteserver.feedback.service.PersonalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/personalization")
@RequiredArgsConstructor
public class PersonalizationController {
    private final PersonalizationService personalizationService;

    @GetMapping
    public ApiResponse<PersonalizationResponse> get(@AuthenticationPrincipal UUID memberId) {
        return ApiResponse.success(personalizationService.get(memberId));
    }
}
