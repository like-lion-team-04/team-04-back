package com.likelion.firstbite.firstbiteserver.auth.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieService {
    public static final String COOKIE_NAME = "refreshToken";
    private final long lifetime;
    private final boolean secure;
    public RefreshCookieService(@Value("${app.security.refresh-token-seconds}") long lifetime,
                                @Value("${app.security.refresh-cookie-secure}") boolean secure) {
        this.lifetime=lifetime; this.secure=secure;
    }
    public ResponseCookie create(String token) { return base().value(token).maxAge(lifetime).build(); }
    public ResponseCookie delete() { return base().value("").maxAge(0).build(); }
    private ResponseCookie.ResponseCookieBuilder base() {
        return ResponseCookie.from(COOKIE_NAME).httpOnly(true).secure(secure).sameSite("Lax").path("/api/v1/auth");
    }
    public long lifetime() { return lifetime; }
}
