package com.likelion.firstbite.firstbiteserver.recognition.vision;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Component
public class OpenAiVisionClient implements VisionClient {
    private static final String PROMPT = """
            Identify all food/menu items visible in this image. Return JSON only as
            {"items":[{"recognizedName":"Korean food name","confidence":0.0}],"warnings":[]}.
            Include at most 20 items, confidence must be 0 to 1, and do not infer exact serving size.
            """;
    private final String apiKey;
    private final String model;
    private final ObjectMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public OpenAiVisionClient(@Value("${app.openai.api-key:}") String apiKey,
                              @Value("${app.openai.vision-model:gpt-4o-mini}") String model, ObjectMapper mapper) {
        this.apiKey = apiKey; this.model = model; this.mapper = mapper;
    }

    @Override public String recognize(String contentType, byte[] bytes) {
        if (apiKey.isBlank()) throw new IllegalStateException("OPENAI_API_KEY is not configured");
        try {
            String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            Map<String,Object> body = Map.of("model", model, "input", java.util.List.of(Map.of(
                    "role", "user", "content", java.util.List.of(
                            Map.of("type", "input_text", "text", PROMPT),
                            Map.of("type", "input_image", "image_url", dataUrl, "detail", "auto")))));
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/responses"))
                    .timeout(Duration.ofSeconds(40)).header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("OpenAI response " + response.statusCode());
            JsonNode root = mapper.readTree(response.body());
            JsonNode output = root.path("output");
            for (JsonNode message : output) for (JsonNode content : message.path("content"))
                if ("output_text".equals(content.path("type").asText())) return content.path("text").asText();
            throw new IllegalStateException("OpenAI output text missing");
        } catch (Exception exception) { throw new IllegalStateException("Vision recognition failed", exception); }
    }
}
