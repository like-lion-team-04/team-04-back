package com.likelion.firstbite.firstbiteserver.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PhoneCryptoService {
    private static final int GCM_TAG_BITS = 128;
    private final byte[] encryptionKey;
    private final byte[] hmacKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public PhoneCryptoService(@Value("${app.security.phone-encryption-key}") String encryptionSecret,
                              @Value("${app.security.phone-hmac-key}") String hmacSecret) {
        this.encryptionKey = sha256(encryptionSecret);
        this.hmacKey = sha256(hmacSecret);
    }

    public String encrypt(String value) {
        try {
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception exception) {
            throw new IllegalStateException("전화번호 암호화에 실패했습니다.", exception);
        }
    }

    public String decrypt(String encryptedValue) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedValue);
            byte[] iv = java.util.Arrays.copyOfRange(combined, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(combined, 12, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("전화번호 복호화에 실패했습니다.", exception);
        }
    }

    public String hash(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("보안 해시 생성에 실패했습니다.", exception);
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
