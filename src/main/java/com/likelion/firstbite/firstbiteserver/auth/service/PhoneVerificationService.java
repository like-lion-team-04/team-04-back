package com.likelion.firstbite.firstbiteserver.auth.service;

import com.likelion.firstbite.firstbiteserver.auth.dto.*;
import com.likelion.firstbite.firstbiteserver.auth.phone.*;
import com.likelion.firstbite.firstbiteserver.auth.security.PhoneCryptoService;
import com.likelion.firstbite.firstbiteserver.auth.sms.SmsSender;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {
    private final PhoneVerificationRepository repository;
    private final MemberRepository memberRepository;
    private final PhoneCryptoService cryptoService;
    private final SmsSender smsSender;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public PhoneVerificationSendResponse send(PhoneVerificationSendRequest request) {
        String phone = PhoneNumberNormalizer.normalize(request.phoneNumber());
        String phoneHash = cryptoService.hash(phone);
        if (memberRepository.existsByPhoneHash(phoneHash)) {
            throw new BusinessException(HttpStatus.CONFLICT, "AUTH_PHONE_NUMBER_DUPLICATED", "이미 가입된 휴대폰 번호입니다.");
        }
        Instant now = Instant.now();
        repository.findTopByPhoneHashOrderByCreatedAtDesc(phoneHash).ifPresent(previous -> {
            if (previous.getResendAvailableAt().isAfter(now)) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "PHONE_VERIFICATION_RATE_LIMITED", "인증번호는 60초 후 다시 요청할 수 있습니다.");
            }
        });
        String code = "%06d".formatted(random.nextInt(1_000_000));
        smsSender.sendVerificationCode(phone, code);
        repository.save(PhoneVerification.issue(phoneHash, cryptoService.encrypt(phone), cryptoService.hash(code), now));
        return new PhoneVerificationSendResponse(300, 60);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public PhoneVerificationConfirmResponse confirm(PhoneVerificationConfirmRequest request) {
        String phone = PhoneNumberNormalizer.normalize(request.phoneNumber());
        PhoneVerification verification = repository.findTopByPhoneHashOrderByCreatedAtDesc(cryptoService.hash(phone))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PHONE_VERIFICATION_NOT_FOUND", "인증 요청을 찾을 수 없습니다."));
        Instant now = Instant.now();
        if (verification.getStatus() == PhoneVerificationStatus.LOCKED || verification.getAttemptCount() >= 5) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "PHONE_VERIFICATION_ATTEMPTS_EXCEEDED", "인증번호 입력 가능 횟수를 초과했습니다.");
        }
        if (verification.getStatus() != PhoneVerificationStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "PHONE_VERIFICATION_ALREADY_CONFIRMED", "이미 처리된 인증 요청입니다.");
        }
        if (verification.getCodeExpiresAt().isBefore(now)) {
            throw new BusinessException(HttpStatus.GONE, "PHONE_VERIFICATION_CODE_EXPIRED", "인증번호가 만료되었습니다.");
        }
        if (!verification.getCodeHash().equals(cryptoService.hash(request.code()))) {
            verification.recordFailure();
            throw new BusinessException(HttpStatus.BAD_REQUEST, "PHONE_VERIFICATION_CODE_INVALID", "인증번호가 올바르지 않습니다.");
        }
        String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        verification.confirm(cryptoService.hash(rawToken), now);
        return new PhoneVerificationConfirmResponse(rawToken, 600);
    }

    @Transactional
    public VerifiedPhone consume(String rawToken) {
        PhoneVerification verification = repository.findByTokenHash(cryptoService.hash(rawToken))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "PHONE_VERIFICATION_TOKEN_INVALID", "유효하지 않은 휴대폰 인증 토큰입니다."));
        if (verification.getStatus() == PhoneVerificationStatus.USED) {
            throw new BusinessException(HttpStatus.CONFLICT, "PHONE_VERIFICATION_TOKEN_USED", "이미 사용된 휴대폰 인증 토큰입니다.");
        }
        if (verification.getStatus() != PhoneVerificationStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "PHONE_VERIFICATION_TOKEN_INVALID", "유효하지 않은 휴대폰 인증 토큰입니다.");
        }
        if (verification.getTokenExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.GONE, "PHONE_VERIFICATION_TOKEN_EXPIRED", "휴대폰 인증 토큰이 만료되었습니다.");
        }
        verification.markUsed();
        return new VerifiedPhone(cryptoService.decrypt(verification.getPhoneEncrypted()), verification.getPhoneHash());
    }

    public record VerifiedPhone(String normalizedNumber, String hash) {}
}
