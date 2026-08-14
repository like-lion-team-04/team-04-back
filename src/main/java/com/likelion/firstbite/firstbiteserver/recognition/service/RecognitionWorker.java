package com.likelion.firstbite.firstbiteserver.recognition.service;

import com.likelion.firstbite.firstbiteserver.recognition.repository.RecognitionRepository;
import com.likelion.firstbite.firstbiteserver.recognition.storage.ImageStorage;
import com.likelion.firstbite.firstbiteserver.recognition.vision.VisionClient;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service @RequiredArgsConstructor
public class RecognitionWorker {
    private final RecognitionRepository repository;
    private final ImageStorage storage;
    private final VisionClient visionClient;

    @Async
    public void process(UUID recognitionId) {
        try {
            var recognition = repository.findById(recognitionId).orElseThrow();
            String result = visionClient.recognize(recognition.getImage().getContentType(), storage.get(recognition.getImage().getObjectKey()));
            complete(recognitionId, result);
        } catch (Exception exception) { fail(recognitionId); }
    }

    protected void complete(UUID id, String result) { repository.findById(id).ifPresent(r -> { r.complete(result); repository.save(r); }); }
    protected void fail(UUID id) { repository.findById(id).ifPresent(r -> { r.fail("RECOGNITION_FAILED", "사진 인식에 실패했어요. 메뉴를 직접 선택해 주세요."); repository.save(r); }); }
}
