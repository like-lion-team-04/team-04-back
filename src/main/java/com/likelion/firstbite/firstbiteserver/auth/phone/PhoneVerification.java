package com.likelion.firstbite.firstbiteserver.auth.phone;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "phone_verifications", indexes = {
        @Index(name = "idx_phone_verification_phone_created", columnList = "phone_hash,created_at"),
        @Index(name = "idx_phone_verification_token", columnList = "token_hash", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification {
    @Id @GeneratedValue private UUID id;
    @Column(name = "phone_hash", nullable = false, length = 64) private String phoneHash;
    @Column(name = "phone_encrypted", nullable = false, length = 512) private String phoneEncrypted;
    @Column(name = "code_hash", nullable = false, length = 64) private String codeHash;
    @Column(name = "challenge_encrypted", nullable = false, length = 512) private String challengeEncrypted;
    @Column(name = "token_hash", length = 64) private String tokenHash;
    @Column(name = "code_expires_at", nullable = false) private Instant codeExpiresAt;
    @Column(name = "token_expires_at") private Instant tokenExpiresAt;
    @Column(name = "resend_available_at", nullable = false) private Instant resendAvailableAt;
    @Column(name = "attempt_count", nullable = false) private int attemptCount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PhoneVerificationStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public static PhoneVerification issue(String phoneHash, String phoneEncrypted, String codeHash,
                                           String challengeEncrypted, Instant now) {
        PhoneVerification verification = new PhoneVerification();
        verification.phoneHash = phoneHash;
        verification.phoneEncrypted = phoneEncrypted;
        verification.codeHash = codeHash;
        verification.challengeEncrypted = challengeEncrypted;
        verification.codeExpiresAt = now.plusSeconds(300);
        verification.resendAvailableAt = now.plusSeconds(60);
        verification.status = PhoneVerificationStatus.PENDING;
        verification.createdAt = now;
        return verification;
    }

    public void recordFailure() {
        attemptCount++;
        if (attemptCount >= 5) status = PhoneVerificationStatus.LOCKED;
    }

    public void confirm(String tokenHash, Instant now) {
        this.tokenHash = tokenHash;
        this.tokenExpiresAt = now.plusSeconds(600);
        this.status = PhoneVerificationStatus.CONFIRMED;
    }

    public void markUsed() { this.status = PhoneVerificationStatus.USED; }
}
