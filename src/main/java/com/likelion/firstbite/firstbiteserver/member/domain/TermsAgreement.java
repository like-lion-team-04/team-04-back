package com.likelion.firstbite.firstbiteserver.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "terms_agreements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TermsAgreement {
    @Id @GeneratedValue private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) private Member member;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private TermsType termsType;
    @Column(nullable = false, length = 30) private String termsVersion;
    @Column(nullable = false) private boolean agreed;
    @Column(nullable = false) private Instant agreedAt;

    private TermsAgreement(Member member, TermsType type, String version, boolean agreed) {
        this.member = member;
        this.termsType = type;
        this.termsVersion = version;
        this.agreed = agreed;
        this.agreedAt = Instant.now();
    }

    public static TermsAgreement of(Member member, TermsType type, String version, boolean agreed) {
        return new TermsAgreement(member, type, version, agreed);
    }
}
