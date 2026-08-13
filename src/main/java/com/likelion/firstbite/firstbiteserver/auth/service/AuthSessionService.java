package com.likelion.firstbite.firstbiteserver.auth.service;

import com.likelion.firstbite.firstbiteserver.auth.dto.LoginRequest;
import com.likelion.firstbite.firstbiteserver.auth.dto.LoginResponse;
import com.likelion.firstbite.firstbiteserver.auth.dto.TokenResponse;
import com.likelion.firstbite.firstbiteserver.auth.login.LoginAttempt;
import com.likelion.firstbite.firstbiteserver.auth.login.LoginAttemptRepository;
import com.likelion.firstbite.firstbiteserver.auth.token.*;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import com.likelion.firstbite.firstbiteserver.member.domain.MemberStatus;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class AuthSessionService {
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final TokenHashService tokenHashService;
    private final RefreshCookieService cookieService;

    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResult login(LoginRequest request) {
        String email=request.email().trim().toLowerCase(Locale.ROOT);
        String identifierHash=tokenHashService.hash(email);
        Instant now=Instant.now();
        LoginAttempt attempt=loginAttemptRepository.findByIdentifierHash(identifierHash)
                .orElseGet(() -> LoginAttempt.start(identifierHash, now));
        if (attempt.isLocked(now)) throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "AUTH_LOGIN_RATE_LIMITED", "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요.");

        Member member=memberRepository.findByEmail(email).orElse(null);
        if (member == null || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            attempt.fail(now); loginAttemptRepository.save(attempt);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "AUTH_ACCOUNT_DISABLED", "사용할 수 없는 계정입니다.");
        }
        loginAttemptRepository.deleteById(identifierHash);
        String refreshToken=UUID.randomUUID()+"."+UUID.randomUUID();
        refreshTokenRepository.save(RefreshToken.issue(member, tokenHashService.hash(refreshToken), now, cookieService.lifetime()));
        String accessToken=jwtTokenService.issue(member.getId());
        return new LoginResult(new LoginResponse(member.getId(), member.getName(), "Bearer", accessToken,
                jwtTokenService.accessTokenSeconds()), refreshToken);
    }

    @Transactional
    public void logout(UUID authenticatedMemberId, String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) return;
        refreshTokenRepository.findByTokenHash(tokenHashService.hash(rawRefreshToken)).ifPresent(token -> {
            if (token.getMember().getId().equals(authenticatedMemberId)) token.revoke(Instant.now());
        });
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public RefreshResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_INVALID", "유효한 Refresh Token이 필요합니다.");
        }

        RefreshToken current = refreshTokenRepository.findByTokenHash(tokenHashService.hash(rawRefreshToken))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_INVALID", "유효하지 않은 Refresh Token입니다."));
        Instant now = Instant.now();

        if (current.getRevokedAt() != null) {
            refreshTokenRepository.findAllByMemberIdAndRevokedAtIsNull(current.getMember().getId())
                    .forEach(token -> token.revoke(now));
            throw new BusinessException(HttpStatus.CONFLICT, "AUTH_REFRESH_REUSED", "폐기된 Refresh Token이 재사용되었습니다. 다시 로그인해 주세요.");
        }
        if (!current.getExpiresAt().isAfter(now) || current.getMember().getStatus() != MemberStatus.ACTIVE) {
            current.revoke(now);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_INVALID", "만료되었거나 사용할 수 없는 Refresh Token입니다.");
        }

        current.revoke(now);
        String rotatedRefreshToken = UUID.randomUUID() + "." + UUID.randomUUID();
        refreshTokenRepository.save(RefreshToken.issue(current.getMember(), tokenHashService.hash(rotatedRefreshToken),
                now, cookieService.lifetime()));
        String accessToken = jwtTokenService.issue(current.getMember().getId());
        return new RefreshResult(new TokenResponse("Bearer", accessToken, jwtTokenService.accessTokenSeconds()),
                rotatedRefreshToken);
    }

    public record LoginResult(LoginResponse response, String refreshToken) {}
    public record RefreshResult(TokenResponse response, String refreshToken) {}
}
