package com.likelion.firstbite.firstbiteserver.recognition.storage;

public interface ImageStorage {
    void put(String objectKey, String contentType, byte[] bytes);
    byte[] get(String objectKey);
    void delete(String objectKey);
}
