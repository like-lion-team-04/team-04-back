package com.likelion.firstbite.firstbiteserver.member.service;

import com.likelion.firstbite.firstbiteserver.auth.phone.PhoneNumberNormalizer;
import com.likelion.firstbite.firstbiteserver.auth.security.PhoneCryptoService;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import com.likelion.firstbite.firstbiteserver.member.domain.MemberStatus;
import com.likelion.firstbite.firstbiteserver.member.dto.AccountMeResponse;
import com.likelion.firstbite.firstbiteserver.member.dto.DeleteAccountRequest;
import com.likelion.firstbite.firstbiteserver.member.dto.DeleteAccountResponse;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import com.likelion.firstbite.firstbiteserver.auth.token.RefreshTokenRepository;
import com.likelion.firstbite.firstbiteserver.feedback.service.PersonalizationService;
import com.likelion.firstbite.firstbiteserver.feedback.dto.PersonalizationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final MemberRepository memberRepository;
    private final PhoneCryptoService phoneCryptoService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PersonalizationService personalizationService;

    @Transactional(readOnly = true)
    public AccountMeResponse getMe(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .filter(found -> found.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."));
        String phoneNumber = phoneCryptoService.decrypt(member.getPhoneEncrypted());
        PersonalizationResponse personalization = personalizationService.get(memberId);
        return new AccountMeResponse(
                member.getId(),
                member.getName(),
                member.getEmail(),
                PhoneNumberNormalizer.mask(phoneNumber),
                member.getBirthDate(),
                member.isMarketingAgreed(),
                member.getStatus(),
                new AccountMeResponse.PersonalizationSummary(personalization.enabled(), personalization.feedbackCount()),
                member.getCreatedAt()
        );
    }

    @Transactional
    public DeleteAccountResponse deleteMe(UUID memberId, DeleteAccountRequest request) {
        if (!"탈퇴".equals(request.confirmText())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ACCOUNT_CONFIRMATION_INVALID", "탈퇴 확인 문구가 일치하지 않습니다.");
        }
        Member member = memberRepository.findById(memberId)
                .filter(found -> found.getStatus() == MemberStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계정을 찾을 수 없습니다."));
        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED", "본인 확인에 실패했습니다.");
        }
        Instant now = Instant.now();
        member.delete(now);
        refreshTokenRepository.findAllByMemberIdAndRevokedAtIsNull(memberId)
                .forEach(token -> token.revoke(now));
        return new DeleteAccountResponse(now);
    }
}
