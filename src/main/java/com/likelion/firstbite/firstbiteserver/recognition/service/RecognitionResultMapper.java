package com.likelion.firstbite.firstbiteserver.recognition.service;

import com.likelion.firstbite.firstbiteserver.food.domain.Food;
import com.likelion.firstbite.firstbiteserver.food.repository.FoodRepository;
import com.likelion.firstbite.firstbiteserver.recognition.dto.RecognitionStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RecognitionResultMapper {
    private static final BigDecimal LOW_CONFIDENCE_THRESHOLD = new BigDecimal("0.70");
    private static final String SERVING_WARNING = "사진만으로 양을 정확히 알 수 없어 1인분으로 설정했어요.";
    private final ObjectMapper objectMapper;
    private final FoodRepository foodRepository;

    public MappedResult map(String resultJson) {
        try {
            JsonNode root = objectMapper.readTree(stripCodeFence(resultJson));
            List<RecognitionStatusResponse.Item> items = new ArrayList<>();
            int index = 1;
            for (JsonNode node : root.path("items")) {
                if (index > 20) break;
                String name = node.path("recognizedName").asText("").trim();
                if (name.isEmpty()) continue;
                BigDecimal confidence = clamp(node.path("confidence").decimalValue());
                List<RecognitionStatusResponse.Candidate> candidates = candidates(name, confidence);
                boolean needsConfirmation = confidence.compareTo(LOW_CONFIDENCE_THRESHOLD) < 0 || candidates.isEmpty();
                var level = needsConfirmation ? RecognitionStatusResponse.ConfidenceLevel.LOW
                        : RecognitionStatusResponse.ConfidenceLevel.HIGH;
                items.add(new RecognitionStatusResponse.Item("tmp-" + index++, name, confidence,
                        level, needsConfirmation, candidates, BigDecimal.ONE, true));
            }
            List<String> warnings = new ArrayList<>();
            for (JsonNode warning : root.path("warnings")) {
                String value = warning.asText("").trim();
                if (!value.isEmpty()) warnings.add(value);
            }
            if (!warnings.contains(SERVING_WARNING)) warnings.add(SERVING_WARNING);
            return new MappedResult(items, warnings);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored recognition result is invalid", exception);
        }
    }

    private List<RecognitionStatusResponse.Candidate> candidates(String name, BigDecimal modelConfidence) {
        List<Food> foods = foodRepository.search(name, null, PageRequest.of(0, 3)).getContent().stream()
                .sorted(java.util.Comparator.comparing((Food food) -> !food.getName().equalsIgnoreCase(name))
                        .thenComparing(Food::getName))
                .toList();
        return java.util.stream.IntStream.range(0, foods.size()).mapToObj(index -> {
            Food food = foods.get(index);
            boolean exact = food.getName().equalsIgnoreCase(name);
            BigDecimal score = exact ? modelConfidence : modelConfidence.min(new BigDecimal("0.75")
                    .subtract(new BigDecimal("0.10").multiply(BigDecimal.valueOf(index))));
            String quality = food.getGiDataQuality().name().equals("MEASURED") ? "MEASURED" : "ESTIMATED";
            return new RecognitionStatusResponse.Candidate(food.getId(), food.getName(), clamp(score),
                    food.getCarbG(), food.getFiberG(), food.getProteinG(), food.getFatG(),
                    food.getCalorieKcal(), food.getGi(), quality);
        }).toList();
    }

    private BigDecimal clamp(BigDecimal value) {
        if (value == null) return BigDecimal.ZERO;
        return value.max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(2, RoundingMode.HALF_UP);
    }

    private String stripCodeFence(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        if (!trimmed.startsWith("```")) return trimmed;
        int firstLineEnd = trimmed.indexOf('\n');
        int lastFence = trimmed.lastIndexOf("```");
        return firstLineEnd >= 0 && lastFence > firstLineEnd
                ? trimmed.substring(firstLineEnd + 1, lastFence).trim() : trimmed;
    }

    public record MappedResult(List<RecognitionStatusResponse.Item> items, List<String> warnings) {}
}
