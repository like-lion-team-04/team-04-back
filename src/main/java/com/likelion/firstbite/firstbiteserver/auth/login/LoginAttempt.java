package com.likelion.firstbite.firstbiteserver.auth.login;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity @Table(name = "login_attempts") @Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginAttempt {
    @Id @Column(name = "identifier_hash", length = 64) private String identifierHash;
    @Column(name = "failed_count", nullable = false) private int failedCount;
    @Column(name = "window_started_at", nullable = false) private Instant windowStartedAt;
    @Column(name = "locked_until") private Instant lockedUntil;
    public static LoginAttempt start(String hash, Instant now) { LoginAttempt a = new LoginAttempt(); a.identifierHash=hash; a.windowStartedAt=now; return a; }
    public void fail(Instant now) {
        if (windowStartedAt.plusSeconds(900).isBefore(now)) { failedCount=0; windowStartedAt=now; lockedUntil=null; }
        failedCount++; if (failedCount >= 5) lockedUntil=now.plusSeconds(900);
    }
    public boolean isLocked(Instant now) { return lockedUntil != null && lockedUntil.isAfter(now); }
}
