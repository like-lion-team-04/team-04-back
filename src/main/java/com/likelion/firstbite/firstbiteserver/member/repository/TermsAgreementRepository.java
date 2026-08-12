package com.likelion.firstbite.firstbiteserver.member.repository;

import com.likelion.firstbite.firstbiteserver.member.domain.TermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, UUID> {
}
