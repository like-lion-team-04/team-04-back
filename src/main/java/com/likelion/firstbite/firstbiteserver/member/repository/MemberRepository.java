package com.likelion.firstbite.firstbiteserver.member.repository;

import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    boolean existsByEmail(String email);
    boolean existsByPhoneHash(String phoneHash);
}
