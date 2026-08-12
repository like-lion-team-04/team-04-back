package com.likelion.firstbite.firstbiteserver.auth.service;

import com.likelion.firstbite.firstbiteserver.auth.dto.SignUpRequest;
import com.likelion.firstbite.firstbiteserver.auth.dto.SignUpResponse;
import com.likelion.firstbite.firstbiteserver.auth.phone.PhoneNumberNormalizer;
import com.likelion.firstbite.firstbiteserver.auth.security.PhoneCryptoService;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import com.likelion.firstbite.firstbiteserver.member.domain.TermsAgreement;
import com.likelion.firstbite.firstbiteserver.member.domain.TermsType;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import com.likelion.firstbite.firstbiteserver.member.repository.TermsAgreementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final String TERMS_VERSION = "1.0";
    private final MemberRepository memberRepository;
    private final TermsAgreementRepository termsAgreementRepository;
    private final PhoneVerificationService phoneVerificationService;
    private final PhoneCryptoService phoneCryptoService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (memberRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(HttpStatus.CONFLICT, "AUTH_EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다.");
        }

        PhoneVerificationService.VerifiedPhone phone = phoneVerificationService.consume(request.verificationToken());
        if (memberRepository.existsByPhoneHash(phone.hash())) {
            throw new BusinessException(HttpStatus.CONFLICT, "AUTH_PHONE_NUMBER_DUPLICATED", "이미 가입된 휴대폰 번호입니다.");
        }

        Member member = Member.create(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.name(),
                request.birthDate(),
                phoneCryptoService.encrypt(phone.normalizedNumber()),
                phone.hash(),
                request.marketingAgreed()
        );
        try {
            memberRepository.saveAndFlush(member);
            termsAgreementRepository.saveAll(List.of(
                    TermsAgreement.of(member, TermsType.TERMS_OF_SERVICE, TERMS_VERSION, true),
                    TermsAgreement.of(member, TermsType.PRIVACY_POLICY, TERMS_VERSION, true),
                    TermsAgreement.of(member, TermsType.MARKETING, TERMS_VERSION, request.marketingAgreed())
            ));
        } catch (DataIntegrityViolationException exception) {
            String detail = String.valueOf(exception.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
            if (detail.contains("phone")) {
                throw new BusinessException(HttpStatus.CONFLICT, "AUTH_PHONE_NUMBER_DUPLICATED", "이미 가입된 휴대폰 번호입니다.");
            }
            throw new BusinessException(HttpStatus.CONFLICT, "AUTH_EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다.");
        }
        return SignUpResponse.from(member, PhoneNumberNormalizer.mask(phone.normalizedNumber()));
    }
}
