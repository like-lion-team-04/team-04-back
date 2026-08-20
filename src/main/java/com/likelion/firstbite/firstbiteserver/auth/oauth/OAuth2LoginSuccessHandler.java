package com.likelion.firstbite.firstbiteserver.auth.oauth;

import com.likelion.firstbite.firstbiteserver.auth.service.AuthSessionService;
import com.likelion.firstbite.firstbiteserver.auth.token.RefreshCookieService;
import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Locale;

/**
 * 소셜 로그인 성공 시 회원을 조회/연동/생성하고, 기존 이메일 로그인과 동일하게
 * Refresh Token 쿠키를 발급한 뒤 프론트로 리다이렉트한다.
 * Access Token은 URL에 노출하지 않고, 프론트가 리다이렉트 후 /auth/refresh 로 획득한다.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final MemberRepository memberRepository;
    private final AuthSessionService authSessionService;
    private final RefreshCookieService cookieService;

    @Value("${app.oauth2.success-redirect:http://localhost:3000/index.html}")
    private String successRedirect;
    @Value("${app.oauth2.failure-redirect:http://localhost:3000/login.html}")
    private String failureRedirect;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String registrationId = token.getAuthorizedClientRegistrationId();
        OAuth2UserInfo info = OAuth2UserInfo.from(registrationId, token.getPrincipal().getAttributes());

        Member member = resolveMember(info);
        String refreshToken = authSessionService.startSession(member);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieService.create(refreshToken).toString());
        response.sendRedirect(successRedirect);
    }

    private Member resolveMember(OAuth2UserInfo info) {
        // 소셜 계정의 실제 식별자는 (provider, providerId)다.
        return memberRepository.findByProviderAndProviderId(info.provider(), info.providerId())
                .orElseGet(() -> {
                    String email = (info.email() == null || info.email().isBlank())
                            ? null : info.email().trim().toLowerCase(Locale.ROOT);
                    String name = resolveName(info, email);
                    // 이메일이 있으면 같은 이메일의 기존 계정에 연동한다(간편가입은 이메일이 없을 수 있음).
                    if (email != null) {
                        Member linked = memberRepository.findByEmail(email)
                                .map(existing -> {
                                    existing.linkSocial(info.provider(), info.providerId(), info.picture());
                                    return existing;
                                })
                                .orElse(null);
                        if (linked != null) return linked;
                    }
                    return memberRepository.save(
                            Member.createSocial(email, name, info.provider(), info.providerId(), info.picture()));
                });
    }

    private String resolveName(OAuth2UserInfo info, String email) {
        if (info.name() != null && !info.name().isBlank()) return info.name();
        if (email != null) return email.substring(0, email.indexOf('@'));
        return "kakao".equals(info.provider()) ? "카카오 사용자" : "사용자";
    }
}
