package com.likelion.firstbite.firstbiteserver.recognition.storage;

import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Component
public class S3ImageStorage implements ImageStorage {
    private final S3Client s3;
    private final String bucket;

    public S3ImageStorage(S3Client s3, @Value("${app.aws.s3-bucket:}") String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override public void put(String key, String contentType, byte[] bytes) {
        requireConfigured();
        try {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType)
                    .serverSideEncryption(ServerSideEncryption.AES256).build(), RequestBody.fromBytes(bytes));
        } catch (S3Exception exception) { throw storageError(); }
    }

    @Override public byte[] get(String key) {
        requireConfigured();
        try { return s3.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key).build()).asByteArray(); }
        catch (S3Exception exception) { throw storageError(); }
    }

    @Override public void delete(String key) {
        if (bucket.isBlank()) return;
        try { s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build()); }
        catch (S3Exception ignored) { }
    }

    private void requireConfigured() { if (bucket.isBlank()) throw storageError(); }
    private BusinessException storageError() {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "IMAGE_STORAGE_ERROR", "이미지 저장소를 사용할 수 없습니다.");
    }
}
