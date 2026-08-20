package com.likelion.firstbite.firstbiteserver.auth.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieService {
    public static final String COOKIE_NAME = "refreshToken";
    private final long lifetime;
    private final boolean secure;
    private final String sameSite;
    public RefreshCookieService(@Value("${app.security.refresh-token-seconds}") long lifetime,
                                @Value("${app.security.refresh-cookie-secure}") boolean secure,
                                @Value("${app.security.refresh-cookie-same-site:Lax}") String sameSite) {
        this.lifetime=lifetime; this.secure=secure; this.sameSite=sameSite;
    }
    public ResponseCookie create(String token) { return base().value(token).maxAge(lifetime).build(); }
    public ResponseCookie delete() { return base().value("").maxAge(0).build(); }
    private ResponseCookie.ResponseCookieBuilder base() {
        return ResponseCookie.from(COOKIE_NAME).httpOnly(true).secure(secure).sameSite(sameSite).path("/api/v1/auth");
    }
    public long lifetime() { return lifetime; }
}
