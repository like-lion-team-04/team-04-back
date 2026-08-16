package com.likelion.firstbite.firstbiteserver.recognition.vision;

import com.likelion.firstbite.firstbiteserver.recognition.domain.ImageType;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiVisionClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsImageWithStrictSchemaAndReturnsStructuredOutput() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = server(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.getResponseHeaders().add("x-request-id", "req_test_123");
            respond(exchange, 200, successResponse());
        });
        OpenAiVisionClient client = client(1);

        String result = client.recognize(ImageType.DELIVERY_SCREEN, "image/png", new byte[]{1, 2, 3});

        JsonNode body = mapper.readTree(requestBody.get());
        assertThat(body.path("model").asText()).isEqualTo("gpt-4o-mini");
        assertThat(body.path("store").asBoolean()).isFalse();
        assertThat(body.path("text").path("format").path("type").asText()).isEqualTo("json_schema");
        assertThat(body.path("text").path("format").path("strict").asBoolean()).isTrue();
        assertThat(body.path("input").get(0).path("content").get(0).path("text").asText())
                .contains("실제 주문한 음식명만 추출");
        assertThat(body.path("input").get(0).path("content").get(1).path("image_url").asText())
                .startsWith("data:image/png;base64,");
        assertThat(mapper.readTree(result).path("items").get(0).path("recognizedName").asText())
                .isEqualTo("떡볶이");
    }

    @Test
    void retriesRateLimitOnce() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = server(exchange -> {
            if (calls.incrementAndGet() == 1) respond(exchange, 429, "{\"error\":{\"message\":\"rate limit\"}}");
            else respond(exchange, 200, successResponse());
        });

        String result = client(2).recognize(ImageType.FOOD_PHOTO, "image/jpeg", new byte[]{4, 5});

        assertThat(calls).hasValue(2);
        assertThat(mapper.readTree(result).path("items")).hasSize(1);
    }

    @Test
    void rejectsMissingApiKeyBeforeCallingOpenAi() {
        OpenAiVisionClient client = new OpenAiVisionClient("", "gpt-4o-mini",
                URI.create("http://localhost:1/v1/responses"), Duration.ofSeconds(1),
                Duration.ofSeconds(1), 1, mapper);

        assertThatThrownBy(() -> client.recognize(ImageType.MENU_BOARD, "image/jpeg", new byte[]{1}))
                .isInstanceOf(VisionRecognitionException.class)
                .extracting(exception -> ((VisionRecognitionException) exception).getCode())
                .isEqualTo("OPENAI_NOT_CONFIGURED");
    }

    private OpenAiVisionClient client(int attempts) {
        return new OpenAiVisionClient("test-key", "gpt-4o-mini",
                URI.create("http://localhost:" + server.getAddress().getPort() + "/v1/responses"),
                Duration.ofSeconds(1), Duration.ofSeconds(3), attempts, mapper);
    }

    private HttpServer server(Handler handler) throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/v1/responses", exchange -> handler.handle(exchange));
        httpServer.start();
        return httpServer;
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private String successResponse() {
        return """
                {"status":"completed","output":[{"type":"message","content":[
                  {"type":"output_text","text":"{\\\"items\\\":[{\\\"recognizedName\\\":\\\"떡볶이\\\",\\\"confidence\\\":0.91}],\\\"warnings\\\":[]}"}
                ]}]}
                """;
    }

    @FunctionalInterface
    private interface Handler {
        void handle(com.sun.net.httpserver.HttpExchange exchange) throws java.io.IOException;
    }
}
