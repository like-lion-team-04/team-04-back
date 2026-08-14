package com.likelion.firstbite.firstbiteserver.feedback.repository;

import com.likelion.firstbite.firstbiteserver.feedback.domain.PersonalizationProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PersonalizationProfileRepository extends JpaRepository<PersonalizationProfile, UUID> {}
