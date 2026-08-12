package com.likelion.firstbite.firstbiteserver.auth.phone;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, UUID> {
    Optional<PhoneVerification> findTopByPhoneHashOrderByCreatedAtDesc(String phoneHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PhoneVerification> findByTokenHash(String tokenHash);
}
