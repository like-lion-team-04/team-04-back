package com.likelion.firstbite.firstbiteserver.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "members", uniqueConstraints = {
        @UniqueConstraint(name = "uk_members_email", columnNames = "email"),
        @UniqueConstraint(name = "uk_members_phone_hash", columnNames = "phone_hash")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id @GeneratedValue private UUID id;
    @Column(nullable = false, length = 254) private String email;
    @Column(nullable = false, length = 100) private String passwordHash;
    @Column(nullable = false, length = 50) private String name;
    @Column(nullable = false) private LocalDate birthDate;
    @Column(name = "phone_encrypted", nullable = false, length = 512) private String phoneEncrypted;
    @Column(name = "phone_hash", nullable = false, length = 64) private String phoneHash;
    @Column(nullable = false) private boolean marketingAgreed;
    private Instant marketingAgreedAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MemberStatus status;
    @Column(nullable = false, updatable = false) private Instant createdAt;

    private Member(String email, String passwordHash, String name, LocalDate birthDate,
                   String phoneEncrypted, String phoneHash, boolean marketingAgreed) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.birthDate = birthDate;
        this.phoneEncrypted = phoneEncrypted;
        this.phoneHash = phoneHash;
        this.marketingAgreed = marketingAgreed;
        this.marketingAgreedAt = marketingAgreed ? Instant.now() : null;
        this.status = MemberStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public static Member create(String email, String passwordHash, String name, LocalDate birthDate,
                                String phoneEncrypted, String phoneHash, boolean marketingAgreed) {
        return new Member(email, passwordHash, name, birthDate, phoneEncrypted, phoneHash, marketingAgreed);
    }
}
