package com.likelion.firstbite.firstbiteserver.evidence.controller;

import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import com.likelion.firstbite.firstbiteserver.evidence.dto.EvidenceResponse;
import com.likelion.firstbite.firstbiteserver.evidence.service.EvidenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evidence")
@RequiredArgsConstructor
public class EvidenceController {
    private final EvidenceService evidenceService;

    @GetMapping
    public ApiResponse<EvidenceResponse> getEvidence(
            @AuthenticationPrincipal UUID memberId,
            @RequestParam(required = false) UUID analysisId,
            @RequestParam(required = false) String type) {
        return ApiResponse.success(evidenceService.get(memberId, analysisId,
                type == null ? null : type.toUpperCase()));
    }
}
