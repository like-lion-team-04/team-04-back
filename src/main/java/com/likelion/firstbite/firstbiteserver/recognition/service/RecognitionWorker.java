package com.likelion.firstbite.firstbiteserver.recognition.service;

import com.likelion.firstbite.firstbiteserver.recognition.repository.RecognitionRepository;
import com.likelion.firstbite.firstbiteserver.recognition.storage.ImageStorage;
import com.likelion.firstbite.firstbiteserver.recognition.vision.VisionClient;
import com.likelion.firstbite.firstbiteserver.recognition.vision.VisionRecognitionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service @RequiredArgsConstructor
public class RecognitionWorker {
    private final RecognitionRepository repository;
    private final ImageStorage storage;
    private final VisionClient visionClient;

    @Async
    public void process(UUID recognitionId) {
        try {
            var recognition = repository.findByIdWithImage(recognitionId).orElseThrow();
            String result = visionClient.recognize(recognition.getImageType(), recognition.getImage().getContentType(),
                    storage.get(recognition.getImage().getObjectKey()));
            complete(recognitionId, result);
        } catch (VisionRecognitionException exception) {
            log.warn("OpenAI recognition failed: recognitionId={}, code={}, requestId={}",
                    recognitionId, exception.getCode(), exception.getRequestId());
            fail(recognitionId, exception.getCode());
        } catch (Exception exception) {
            log.error("Recognition processing failed: recognitionId={}", recognitionId, exception);
            fail(recognitionId, "RECOGNITION_FAILED");
        }
    }

    protected void complete(UUID id, String result) { repository.findById(id).ifPresent(r -> { r.complete(result); repository.save(r); }); }
    protected void fail(UUID id, String code) { repository.findById(id).ifPresent(r -> {
        r.fail(code, "사진 인식에 실패했어요. 메뉴를 직접 선택해 주세요.");
        repository.save(r);
    }); }
}
