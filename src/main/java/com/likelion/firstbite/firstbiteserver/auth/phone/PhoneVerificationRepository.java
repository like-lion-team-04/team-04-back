package com.likelion.firstbite.firstbiteserver.auth.phone;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, UUID> {
    Optional<PhoneVerification> findTopByPhoneHashOrderByCreatedAtDesc(String phoneHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select verification from PhoneVerification verification where verification.id = :id")
    Optional<PhoneVerification> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PhoneVerification> findByTokenHash(String tokenHash);
}
