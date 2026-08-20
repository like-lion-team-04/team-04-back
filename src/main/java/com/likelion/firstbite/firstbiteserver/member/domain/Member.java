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
    // 이메일·비밀번호 회원은 필수, 소셜 간편가입(이메일 미제공)은 null 가능
    @Column(length = 254) private String email;
    @Column(length = 100) private String passwordHash;
    @Column(nullable = false, length = 50) private String name;
    private LocalDate birthDate;
    @Column(name = "phone_encrypted", length = 512) private String phoneEncrypted;
    @Column(name = "phone_hash", length = 64) private String phoneHash;
    @Column(nullable = false) private boolean marketingAgreed;
    private Instant marketingAgreedAt;
    // 소셜 로그인 제공자 정보 (이메일·비밀번호 회원이면 null)
    @Column(length = 20) private String provider;
    @Column(name = "provider_id", length = 191) private String providerId;
    @Column(name = "profile_image_url", length = 512) private String profileImageUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private MemberStatus status;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    private Instant deletedAt;

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

    /**
     * 소셜(카카오·구글) 신규 회원 생성. 비밀번호/휴대폰/생년월일 없이 가입한다.
     * 이메일은 제공자가 주지 않으면 null일 수 있다(카카오 간편가입).
     */
    public static Member createSocial(String email, String name, String provider, String providerId,
                                      String profileImageUrl) {
        Member member = new Member();
        member.email = email;
        member.name = name;
        member.provider = provider;
        member.providerId = providerId;
        member.profileImageUrl = profileImageUrl;
        member.marketingAgreed = false;
        member.status = MemberStatus.ACTIVE;
        member.createdAt = Instant.now();
        return member;
    }

    /**
     * 기존 이메일·비밀번호 회원에 소셜 제공자를 연동한다.
     */
    public void linkSocial(String provider, String providerId, String profileImageUrl) {
        this.provider = provider;
        this.providerId = providerId;
        if (this.profileImageUrl == null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public void delete(Instant now) {
        this.status = MemberStatus.DELETED;
        this.deletedAt = now;
    }
}
