package com.likelion.firstbite.firstbiteserver.member.controller;

import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import com.likelion.firstbite.firstbiteserver.member.dto.AccountMeResponse;
import com.likelion.firstbite.firstbiteserver.member.dto.DeleteAccountRequest;
import com.likelion.firstbite.firstbiteserver.member.dto.DeleteAccountResponse;
import com.likelion.firstbite.firstbiteserver.auth.token.RefreshCookieService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.likelion.firstbite.firstbiteserver.member.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;
    private final RefreshCookieService refreshCookieService;

    @GetMapping("/me")
    public ApiResponse<AccountMeResponse> getMe(@AuthenticationPrincipal UUID memberId) {
        return ApiResponse.success(accountService.getMe(memberId));
    }

    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<DeleteAccountResponse>> deleteMe(
            @AuthenticationPrincipal UUID memberId,
            @Valid @RequestBody DeleteAccountRequest request) {
        DeleteAccountResponse response = accountService.deleteMe(memberId, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookieService.delete().toString())
                .body(ApiResponse.success(response));
    }
}
