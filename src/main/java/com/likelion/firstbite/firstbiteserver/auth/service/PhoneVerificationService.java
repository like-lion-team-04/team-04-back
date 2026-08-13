package com.likelion.firstbite.firstbiteserver.auth.service;

import com.likelion.firstbite.firstbiteserver.auth.dto.*;
import com.likelion.firstbite.firstbiteserver.auth.octomo.OctomoClient;
import com.likelion.firstbite.firstbiteserver.auth.phone.*;
import com.likelion.firstbite.firstbiteserver.auth.security.PhoneCryptoService;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {
    private static final char[] CHALLENGE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int CHALLENGE_MINUTES = 5;

    private final PhoneVerificationRepository repository;
    private final MemberRepository memberRepository;
    private final PhoneCryptoService cryptoService;
    private final OctomoClient octomoClient;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.octomo.receiver-number:16663538}")
    private String receiverNumber;

    @Transactional
    public PhoneVerificationSendResponse send(PhoneVerificationSendRequest request) {
        String normalizedPhone = PhoneNumberNormalizer.normalize(request.phoneNumber());
        String phoneHash = cryptoService.hash(normalizedPhone);
        if (memberRepository.existsByPhoneHash(phoneHash)) {
            throw new BusinessException(HttpStatus.CONFLICT, "AUTH_PHONE_NUMBER_DUPLICATED", "이미 가입된 휴대폰 번호입니다.");
        }

        Instant now = Instant.now();
        repository.findTopByPhoneHashOrderByCreatedAtDesc(phoneHash).ifPresent(previous -> {
            if (previous.getResendAvailableAt().isAfter(now)) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "PHONE_VERIFICATION_RATE_LIMITED",
                        "인증 요청 후 60초가 지나야 다시 요청할 수 있습니다.");
            }
        });

        String challenge = "FIRSTBITE " + randomChallenge(8);
        PhoneVerification verification = repository.save(PhoneVerification.issue(
                phoneHash,
                cryptoService.encrypt(normalizedPhone),
                cryptoService.hash(challenge),
                cryptoService.encrypt(challenge),
                now));
        String encodedBody = URLEncoder.encode(challenge, StandardCharsets.UTF_8).replace("+", "%20");
        return new PhoneVerificationSendResponse(
                verification.getId(), receiverNumber, challenge,
                "sms:" + receiverNumber + "?body=" + encodedBody, 300);
    }

    @Transactional
    public PhoneVerificationConfirmResponse confirm(PhoneVerificationConfirmRequest request) {
        PhoneVerification verification = repository.findByIdForUpdate(request.requestId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "PHONE_VERIFICATION_NOT_FOUND",
                        "인증 요청을 찾을 수 없습니다."));
        Instant now = Instant.now();
        if (verification.getStatus() != PhoneVerificationStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT, "PHONE_VERIFICATION_ALREADY_CONFIRMED",
                    "이미 처리된 인증 요청입니다.");
        }
        if (verification.getCodeExpiresAt().isBefore(now)) {
            throw new BusinessException(HttpStatus.GONE, "PHONE_VERIFICATION_EXPIRED", "휴대폰 인증 요청이 만료되었습니다.");
        }

        String phoneNumber = toOctomoPhone(cryptoService.decrypt(verification.getPhoneEncrypted()));
        String challenge = cryptoService.decrypt(verification.getChallengeEncrypted());
        if (!octomoClient.messageExists(phoneNumber, challenge, CHALLENGE_MINUTES)) {
            return PhoneVerificationConfirmResponse.pending();
        }

        String rawToken = UUID.randomUUID() + "." + UUID.randomUUID();
        verification.confirm(cryptoService.hash(rawToken), now);
        return PhoneVerificationConfirmResponse.verified(rawToken);
    }

    @Transactional
    public VerifiedPhone consume(String rawToken) {
        PhoneVerification verification = repository.findByTokenHash(cryptoService.hash(rawToken))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "PHONE_VERIFICATION_TOKEN_INVALID",
                        "유효하지 않은 휴대폰 인증 토큰입니다."));
        if (verification.getStatus() == PhoneVerificationStatus.USED) {
            throw new BusinessException(HttpStatus.CONFLICT, "PHONE_VERIFICATION_TOKEN_USED",
                    "이미 사용된 휴대폰 인증 토큰입니다.");
        }
        if (verification.getStatus() != PhoneVerificationStatus.CONFIRMED) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "PHONE_VERIFICATION_TOKEN_INVALID",
                    "유효하지 않은 휴대폰 인증 토큰입니다.");
        }
        if (verification.getTokenExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.GONE, "PHONE_VERIFICATION_TOKEN_EXPIRED",
                    "휴대폰 인증 토큰이 만료되었습니다.");
        }
        verification.markUsed();
        return new VerifiedPhone(cryptoService.decrypt(verification.getPhoneEncrypted()), verification.getPhoneHash());
    }

    private String randomChallenge(int length) {
        StringBuilder value = new StringBuilder(length);
        for (int i = 0; i < length; i++) value.append(CHALLENGE_ALPHABET[random.nextInt(CHALLENGE_ALPHABET.length)]);
        return value.toString();
    }

    private String toOctomoPhone(String normalizedPhone) {
        return "0" + normalizedPhone.substring(3);
    }

    public record VerifiedPhone(String normalizedNumber, String hash) {}
}
