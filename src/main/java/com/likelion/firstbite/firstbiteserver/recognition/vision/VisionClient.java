package com.likelion.firstbite.firstbiteserver.recognition.vision;

public interface VisionClient {
    String recognize(String contentType, byte[] imageBytes);
}
