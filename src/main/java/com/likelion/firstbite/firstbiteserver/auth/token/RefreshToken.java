package com.likelion.firstbite.firstbiteserver.auth.token;

import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "refresh_tokens") @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Member member;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "revoked_at") private Instant revokedAt;

    public static RefreshToken issue(Member member, String hash, Instant now, long lifetime) {
        RefreshToken token = new RefreshToken(); token.member = member; token.tokenHash = hash;
        token.createdAt = now; token.expiresAt = now.plusSeconds(lifetime); return token;
    }
    public void revoke(Instant now) { if (revokedAt == null) revokedAt = now; }
}
