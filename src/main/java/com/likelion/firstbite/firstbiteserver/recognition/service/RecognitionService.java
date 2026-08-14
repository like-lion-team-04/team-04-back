package com.likelion.firstbite.firstbiteserver.recognition.service;

import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.recognition.domain.*;
import com.likelion.firstbite.firstbiteserver.recognition.dto.RecognitionAcceptedResponse;
import com.likelion.firstbite.firstbiteserver.recognition.dto.RecognitionStatusResponse;
import com.likelion.firstbite.firstbiteserver.recognition.repository.*;
import com.likelion.firstbite.firstbiteserver.recognition.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class RecognitionService {
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private final RecognitionRepository repository;
    private final RecognitionImageRepository imageRepository;
    private final ImageStorage storage;
    private final ApplicationEventPublisher events;
    private final RecognitionResultMapper resultMapper;

    @Transactional
    public RecognitionAcceptedResponse create(UUID memberId, UUID idempotencyKey, MultipartFile file, ImageType imageType) {
        byte[] bytes = readAndValidate(file);
        String hash = sha256(bytes);
        var existing = repository.findFirstByMemberIdAndIdempotencyKeyAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                memberId, idempotencyKey, Instant.now().minus(Duration.ofHours(24)));
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(hash)) throw new BusinessException(HttpStatus.CONFLICT,
                    "IDEMPOTENCY_KEY_CONFLICT", "동일한 멱등키에 다른 이미지가 사용되었습니다.");
            return RecognitionAcceptedResponse.from(existing.get());
        }
        UUID imageId = UUID.randomUUID();
        String extension = switch (file.getContentType()) { case "image/png" -> "png"; case "image/webp" -> "webp"; default -> "jpg"; };
        String key = "recognitions/" + memberId + "/" + imageId + "." + extension;
        storage.put(key, file.getContentType(), bytes);
        try {
            Instant now = Instant.now();
            var image = imageRepository.save(RecognitionImage.create(imageId, memberId, key, file.getContentType(), bytes.length, hash, now));
            var recognition = repository.save(Recognition.processing(memberId, image, idempotencyKey, hash, imageType, now));
            events.publishEvent(new RecognitionCreatedEvent(recognition.getId()));
            return RecognitionAcceptedResponse.from(recognition);
        } catch (RuntimeException exception) { storage.delete(key); throw exception; }
    }

    @Transactional(readOnly = true)
    public RecognitionStatusResponse getStatus(UUID memberId, UUID recognitionId) {
        Recognition recognition = repository.findById(recognitionId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "RECOGNITION_NOT_FOUND", "인식 작업을 찾을 수 없습니다."));
        if (!recognition.getMemberId().equals(memberId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "RECOGNITION_FORBIDDEN", "다른 사용자의 인식 작업은 조회할 수 없습니다.");
        }
        return switch (recognition.getStatus()) {
            case PROCESSING -> RecognitionStatusResponse.processing(recognition.getId());
            case FAILED -> RecognitionStatusResponse.failed(recognition.getId(), recognition.getErrorCode(), recognition.getErrorMessage());
            case COMPLETED -> {
                RecognitionResultMapper.MappedResult mapped = resultMapper.map(recognition.getResultJson());
                yield RecognitionStatusResponse.completed(recognition.getId(), mapped.items(), mapped.warnings());
            }
        };
    }

    private byte[] readAndValidate(MultipartFile file) {
        if (file == null || file.isEmpty() || !TYPES.contains(file.getContentType())) throw new BusinessException(HttpStatus.BAD_REQUEST, "IMAGE_INVALID", "지원하지 않는 이미지입니다.");
        if (file.getSize() > MAX_SIZE) throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE", "이미지는 10MB 이하여야 합니다.");
        try { return file.getBytes(); } catch (Exception e) { throw new BusinessException(HttpStatus.BAD_REQUEST, "IMAGE_INVALID", "이미지를 읽을 수 없습니다."); }
    }
    private String sha256(byte[] bytes) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (Exception e) { throw new IllegalStateException(e); } }
    public record RecognitionCreatedEvent(UUID recognitionId) {}
}
