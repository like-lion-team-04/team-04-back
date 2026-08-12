package com.likelion.firstbite.firstbiteserver.auth.sms;

import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class NcpSmsSender implements SmsSender {
    private final boolean enabled;
    private final String accessKey;
    private final String secretKey;
    private final String serviceId;
    private final String fromNumber;
    private final HttpClient client = HttpClient.newHttpClient();

    public NcpSmsSender(@Value("${app.sms.ncp.enabled}") boolean enabled,
                        @Value("${app.sms.ncp.access-key}") String accessKey,
                        @Value("${app.sms.ncp.secret-key}") String secretKey,
                        @Value("${app.sms.ncp.service-id}") String serviceId,
                        @Value("${app.sms.ncp.from-number}") String fromNumber) {
        this.enabled = enabled;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.serviceId = serviceId;
        this.fromNumber = fromNumber;
    }

    @Override
    public void sendVerificationCode(String phoneNumber, String code) {
        if (!enabled) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "SMS_PROVIDER_NOT_CONFIGURED", "SMS 발송 설정이 완료되지 않았습니다.");
        }
        try {
            String path = "/sms/v2/services/" + serviceId + "/messages";
            String timestamp = String.valueOf(System.currentTimeMillis());
            String signature = signature("POST", path, timestamp);
            String recipient = phoneNumber.replace("+82", "0");
            String body = "{\"type\":\"SMS\",\"from\":\"" + escape(fromNumber)
                    + "\",\"content\":\"[첫입] 인증번호는 " + code
                    + "입니다.\",\"messages\":[{\"to\":\"" + escape(recipient) + "\"}]}";
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://sens.apigw.ntruss.com" + path))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("x-ncp-apigw-timestamp", timestamp)
                    .header("x-ncp-iam-access-key", accessKey)
                    .header("x-ncp-apigw-signature-v2", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw new IllegalStateException("NCP status=" + response.statusCode());
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "SMS_PROVIDER_ERROR", "인증번호 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private String signature(String method, String path, String timestamp) throws Exception {
        String message = method + " " + path + "\n" + timestamp + "\n" + accessKey;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    }

    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
}
