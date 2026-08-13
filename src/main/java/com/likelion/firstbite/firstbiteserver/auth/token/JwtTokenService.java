package com.likelion.firstbite.firstbiteserver.auth.token;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenService {
    private final SecretKey key;
    private final long accessTokenSeconds;

    public JwtTokenService(@Value("${app.security.jwt-secret}") String secret,
                           @Value("${app.security.access-token-seconds}") long accessTokenSeconds) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) throw new IllegalStateException("JWT_SECRET은 32바이트 이상이어야 합니다.");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenSeconds = accessTokenSeconds;
    }

    public String issue(UUID memberId) {
        Instant now = Instant.now();
        return Jwts.builder().subject(memberId.toString()).issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenSeconds))).signWith(key).compact();
    }

    public UUID parseMemberId(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public long accessTokenSeconds() { return accessTokenSeconds; }
}
