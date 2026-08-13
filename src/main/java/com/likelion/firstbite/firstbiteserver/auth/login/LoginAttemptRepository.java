package com.likelion.firstbite.firstbiteserver.auth.login;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LoginAttempt> findByIdentifierHash(String identifierHash);
}
