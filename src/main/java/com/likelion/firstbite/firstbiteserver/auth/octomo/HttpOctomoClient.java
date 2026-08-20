package com.likelion.firstbite.firstbiteserver.auth.octomo;

import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "app.octomo", name = "mock", havingValue = "false", matchIfMissing = true)
public class HttpOctomoClient implements OctomoClient {
    private static final URI MESSAGE_EXISTS_URI = URI.create("https://api.octoverse.kr/octomo/v1/public/message/exists");
    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpOctomoClient(@Value("${app.octomo.api-key:}") String apiKey, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @Override
    public boolean messageExists(String phoneNumber, String messageText, int withinMinutes) {
        if (apiKey.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,
                    "PHONE_VERIFICATION_PROVIDER_NOT_CONFIGURED", "휴대폰 인증 제공자 설정이 완료되지 않았습니다.");
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "mobileNum", phoneNumber, "text", messageText, "withinMinutes", withinMinutes));
            HttpRequest request = HttpRequest.newBuilder(MESSAGE_EXISTS_URI)
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Octomo " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                        "PHONE_VERIFICATION_RATE_LIMITED", "휴대폰 인증 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) throw providerError();
            JsonNode json = objectMapper.readTree(response.body());
            if (json.get("exists") == null || !json.get("exists").isBoolean()) throw providerError();
            return json.get("exists").asBoolean();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw providerError();
        }
    }

    private BusinessException providerError() {
        return new BusinessException(HttpStatus.BAD_GATEWAY, "PHONE_VERIFICATION_PROVIDER_ERROR",
                "휴대폰 인증 확인에 실패했습니다. 잠시 후 다시 시도해 주세요.");
    }
}
