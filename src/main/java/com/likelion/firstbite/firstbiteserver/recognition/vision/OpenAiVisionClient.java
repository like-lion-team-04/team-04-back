package com.likelion.firstbite.firstbiteserver.recognition.vision;

import com.likelion.firstbite.firstbiteserver.recognition.domain.ImageType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiVisionClient implements VisionClient {
    private static final String BASE_PROMPT = """
            당신은 한국 점심 메뉴 인식기입니다. 이미지에 실제로 보이는 음식 또는 메뉴 항목만 식별하세요.
            recognizedName은 반드시 한글 음식명만 사용하세요. 영어명이나 괄호 속 영어 설명을 함께 쓰지 마세요.
            예: "라면", "떡볶이", "만두", "김치". 동일한 항목을 중복 반환하지 마세요.
            확실하지 않은 항목도 임의로 단정하지 말고 confidence를 낮게 설정하세요.
            정확한 양, 영양성분, 질병, 혈당 수치는 추정하지 마세요. 최대 20개 항목만 반환하세요.
            """;
    private static final String RESULT_SCHEMA = """
            {
              "type":"object",
              "properties":{
                "items":{
                  "type":"array",
                  "maxItems":20,
                  "items":{
                    "type":"object",
                    "properties":{
                      "recognizedName":{"type":"string","description":"한글로만 작성한 표준 음식명. 영어 병기 금지","minLength":1},
                      "confidence":{"type":"number","minimum":0,"maximum":1}
                    },
                    "required":["recognizedName","confidence"],
                    "additionalProperties":false
                  }
                },
                "warnings":{"type":"array","maxItems":10,"items":{"type":"string"}}
              },
              "required":["items","warnings"],
              "additionalProperties":false
            }
            """;

    private final String apiKey;
    private final String model;
    private final URI endpoint;
    private final Duration requestTimeout;
    private final int maxAttempts;
    private final ObjectMapper mapper;
    private final HttpClient http;

    @Autowired
    public OpenAiVisionClient(@Value("${app.openai.api-key:}") String apiKey,
                              @Value("${app.openai.vision-model:gpt-4o-mini}") String model,
                              @Value("${app.openai.endpoint:https://api.openai.com/v1/responses}") String endpoint,
                              @Value("${app.openai.connect-timeout-seconds:5}") long connectTimeoutSeconds,
                              @Value("${app.openai.request-timeout-seconds:40}") long requestTimeoutSeconds,
                              @Value("${app.openai.max-attempts:2}") int maxAttempts,
                              ObjectMapper mapper) {
        this(apiKey, model, URI.create(endpoint), Duration.ofSeconds(connectTimeoutSeconds),
                Duration.ofSeconds(requestTimeoutSeconds), maxAttempts, mapper);
    }

    OpenAiVisionClient(String apiKey, String model, URI endpoint, Duration connectTimeout,
                       Duration requestTimeout, int maxAttempts, ObjectMapper mapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
        this.requestTimeout = requestTimeout;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.mapper = mapper;
        this.http = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
    }

    @Override
    public String recognize(ImageType imageType, String contentType, byte[] bytes) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new VisionRecognitionException("OPENAI_NOT_CONFIGURED", "OpenAI API key is not configured", null);
        }
        String requestBody = createRequestBody(imageType, contentType, bytes);
        VisionRecognitionException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(endpoint)
                        .timeout(requestTimeout)
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .header("X-Client-Request-Id", java.util.UUID.randomUUID().toString())
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() / 100 == 2) return extractAndValidate(response.body(), requestId(response));
                lastFailure = httpFailure(response);
                if (!retryable(response.statusCode()) || attempt == maxAttempts) throw lastFailure;
            } catch (HttpTimeoutException exception) {
                lastFailure = new VisionRecognitionException("OPENAI_TIMEOUT", "OpenAI request timed out", null, exception);
                if (attempt == maxAttempts) throw lastFailure;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new VisionRecognitionException("OPENAI_INTERRUPTED", "OpenAI request was interrupted", null, exception);
            } catch (VisionRecognitionException exception) {
                throw exception;
            } catch (Exception exception) {
                lastFailure = new VisionRecognitionException("OPENAI_UNAVAILABLE", "OpenAI request failed", null, exception);
                if (attempt == maxAttempts) throw lastFailure;
            }
            backoff(attempt);
        }
        throw lastFailure == null
                ? new VisionRecognitionException("OPENAI_UNAVAILABLE", "OpenAI request failed", null)
                : lastFailure;
    }

    private String createRequestBody(ImageType imageType, String contentType, byte[] bytes) {
        try {
            String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            Map<String, Object> format = new LinkedHashMap<>();
            format.put("type", "json_schema");
            format.put("name", "food_recognition");
            format.put("strict", true);
            format.put("schema", mapper.readTree(RESULT_SCHEMA));

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("store", false);
            body.put("max_output_tokens", 1200);
            body.put("input", List.of(Map.of(
                    "role", "user",
                    "content", List.of(
                            Map.of("type", "input_text", "text", BASE_PROMPT + "\n" + typePrompt(imageType)),
                            Map.of("type", "input_image", "image_url", dataUrl, "detail", "auto")))));
            body.put("text", Map.of("format", format));
            return mapper.writeValueAsString(body);
        } catch (Exception exception) {
            throw new VisionRecognitionException("OPENAI_REQUEST_INVALID", "Failed to build OpenAI request", null, exception);
        }
    }

    private String extractAndValidate(String responseBody, String requestId) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            if ("incomplete".equals(root.path("status").asText())) {
                throw new VisionRecognitionException("OPENAI_OUTPUT_INCOMPLETE", "OpenAI output was incomplete", requestId);
            }
            for (JsonNode output : root.path("output")) {
                for (JsonNode content : output.path("content")) {
                    if ("refusal".equals(content.path("type").asText())) {
                        throw new VisionRecognitionException("OPENAI_REFUSED", "OpenAI refused the image request", requestId);
                    }
                    if ("output_text".equals(content.path("type").asText())) {
                        String text = content.path("text").asText();
                        JsonNode result = mapper.readTree(text);
                        if (!result.path("items").isArray() || !result.path("warnings").isArray()) {
                            throw new VisionRecognitionException("OPENAI_OUTPUT_INVALID", "OpenAI output schema is invalid", requestId);
                        }
                        return mapper.writeValueAsString(result);
                    }
                }
            }
            throw new VisionRecognitionException("OPENAI_OUTPUT_INVALID", "OpenAI output text is missing", requestId);
        } catch (VisionRecognitionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new VisionRecognitionException("OPENAI_OUTPUT_INVALID", "Failed to parse OpenAI output", requestId, exception);
        }
    }

    private VisionRecognitionException httpFailure(HttpResponse<String> response) {
        int status = response.statusCode();
        String code = switch (status) {
            case 401, 403 -> "OPENAI_AUTH_FAILED";
            case 429 -> "OPENAI_RATE_LIMITED";
            default -> status >= 500 ? "OPENAI_UNAVAILABLE" : "OPENAI_REQUEST_REJECTED";
        };
        return new VisionRecognitionException(code, "OpenAI returned HTTP " + status, requestId(response));
    }

    private boolean retryable(int status) { return status == 408 || status == 429 || status >= 500; }

    private String requestId(HttpResponse<?> response) {
        return response.headers().firstValue("x-request-id").orElse(null);
    }

    private void backoff(int attempt) {
        try {
            Thread.sleep(300L * attempt);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new VisionRecognitionException("OPENAI_INTERRUPTED", "OpenAI retry was interrupted", null, exception);
        }
    }

    private String typePrompt(ImageType imageType) {
        return switch (imageType) {
            case MENU_BOARD -> "메뉴판에 적힌 음식명만 읽고 가격, 설명, 카테고리명은 제외하세요.";
            case DELIVERY_SCREEN -> "배달 주문 화면에서 실제 주문한 음식명만 추출하고 추천·광고·옵션 문구는 제외하세요.";
            case FOOD_PHOTO -> "실제 음식 사진에서 눈으로 구분 가능한 개별 음식만 식별하세요. 보이지 않는 재료는 추측하지 마세요.";
        };
    }
}
