package com.likelion.firstbite.firstbiteserver.recognition.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component @RequiredArgsConstructor
public class RecognitionEventHandler {
    private final RecognitionWorker worker;
    @TransactionalEventListener
    public void handle(RecognitionService.RecognitionCreatedEvent event) { worker.process(event.recognitionId()); }
}
