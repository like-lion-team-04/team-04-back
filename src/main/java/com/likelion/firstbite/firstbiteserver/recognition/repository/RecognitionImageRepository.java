package com.likelion.firstbite.firstbiteserver.recognition.repository;

import com.likelion.firstbite.firstbiteserver.recognition.domain.RecognitionImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecognitionImageRepository extends JpaRepository<RecognitionImage, UUID> {
}
