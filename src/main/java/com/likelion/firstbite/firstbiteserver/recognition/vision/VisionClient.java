package com.likelion.firstbite.firstbiteserver.recognition.vision;

import com.likelion.firstbite.firstbiteserver.recognition.domain.ImageType;

public interface VisionClient {
    String recognize(ImageType imageType, String contentType, byte[] imageBytes);
}
