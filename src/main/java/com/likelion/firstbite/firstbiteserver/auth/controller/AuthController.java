package com.likelion.firstbite.firstbiteserver.auth.controller;

import com.likelion.firstbite.firstbiteserver.auth.dto.SignUpRequest;
import com.likelion.firstbite.firstbiteserver.auth.dto.SignUpResponse;
import com.likelion.firstbite.firstbiteserver.auth.dto.PhoneVerificationSendRequest;
import com.likelion.firstbite.firstbiteserver.auth.dto.PhoneVerificationSendResponse;
import com.likelion.firstbite.firstbiteserver.auth.dto.PhoneVerificationConfirmRequest;
import com.likelion.firstbite.firstbiteserver.auth.dto.PhoneVerificationConfirmResponse;
import com.likelion.firstbite.firstbiteserver.auth.service.AuthService;
import com.likelion.firstbite.firstbiteserver.auth.service.PhoneVerificationService;
import com.likelion.firstbite.firstbiteserver.auth.service.AuthSessionService;
import com.likelion.firstbite.firstbiteserver.auth.dto.LoginRequest;
import com.likelion.firstbite.firstbiteserver.auth.dto.LoginResponse;
import com.likelion.firstbite.firstbiteserver.auth.token.RefreshCookieService;
import com.likelion.firstbite.firstbiteserver.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PhoneVerificationService phoneVerificationService;
    private final AuthSessionService authSessionService;
    private final RefreshCookieService refreshCookieService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthSessionService.LoginResult result=authSessionService.login(request);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                refreshCookieService.create(result.refreshToken()).toString())
                .body(ApiResponse.success(result.response()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UUID memberId,
            @CookieValue(name = RefreshCookieService.COOKIE_NAME, required = false) String refreshToken) {
        authSessionService.logout(memberId, refreshToken);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, refreshCookieService.delete().toString()).build();
    }

    @PostMapping("/phone-verifications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PhoneVerificationSendResponse> sendVerification(
            @Valid @RequestBody PhoneVerificationSendRequest request) {
        return ApiResponse.success(phoneVerificationService.send(request));
    }

    @PostMapping("/phone-verifications/confirm")
    public ApiResponse<PhoneVerificationConfirmResponse> confirmVerification(
            @Valid @RequestBody PhoneVerificationConfirmRequest request) {
        return ApiResponse.success(phoneVerificationService.confirm(request));
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        return ApiResponse.success(authService.signUp(request));
    }
}
