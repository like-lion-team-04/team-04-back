package com.likelion.firstbite.firstbiteserver.recognition;

import com.likelion.firstbite.firstbiteserver.auth.token.JwtTokenService;
import com.likelion.firstbite.firstbiteserver.food.domain.*;
import com.likelion.firstbite.firstbiteserver.food.repository.FoodRepository;
import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import com.likelion.firstbite.firstbiteserver.recognition.domain.*;
import com.likelion.firstbite.firstbiteserver.recognition.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RecognitionStatusControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired RecognitionRepository recognitionRepository;
    @Autowired RecognitionImageRepository imageRepository;
    @Autowired FoodRepository foodRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired JwtTokenService jwtTokenService;

    private Member member;
    private String token;

    @BeforeEach
    void setUp() {
        recognitionRepository.deleteAll();
        imageRepository.deleteAll();
        foodRepository.deleteAll();
        memberRepository.deleteAll();
        member = memberRepository.save(Member.create("recognition@example.com", "password-hash", "테스터",
                LocalDate.of(2000, 1, 1), "phone", "recognition-phone-hash", false));
        token = jwtTokenService.issue(member.getId());
        foodRepository.save(food("떡볶이", "tteokbokki", "ㄸㅂㅇ"));
        foodRepository.save(food("국물 떡볶이", "soup-tteokbokki", "ㄱㅁㄸㅂㅇ"));
    }

    @Test
    void returnsProcessingStatusWithoutItems() throws Exception {
        Recognition recognition = processing(member.getId());
        mockMvc.perform(get("/api/v1/recognitions/{id}", recognition.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"))
                .andExpect(jsonPath("$.data.items").doesNotExist())
                .andExpect(jsonPath("$.data.error").doesNotExist());
    }

    @Test
    void returnsCompletedItemsMappedToFoodCandidates() throws Exception {
        Recognition recognition = processing(member.getId());
        recognition.complete("""
                {"items":[{"recognizedName":"떡볶이","confidence":0.87}],"warnings":[]}
                """);
        recognitionRepository.save(recognition);

        mockMvc.perform(get("/api/v1/recognitions/{id}", recognition.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.items[0].temporaryItemId").value("tmp-1"))
                .andExpect(jsonPath("$.data.items[0].recognizedName").value("떡볶이"))
                .andExpect(jsonPath("$.data.items[0].confidence").value(0.87))
                .andExpect(jsonPath("$.data.items[0].confidenceLevel").value("HIGH"))
                .andExpect(jsonPath("$.data.items[0].needsConfirmation").value(false))
                .andExpect(jsonPath("$.data.items[0].candidates[0].name").value("떡볶이"))
                .andExpect(jsonPath("$.data.items[0].candidates[0].gi").value(70))
                .andExpect(jsonPath("$.data.items[0].estimatedServing").value(1))
                .andExpect(jsonPath("$.data.items[0].estimated").value(true))
                .andExpect(jsonPath("$.data.warnings[0]").value("사진만으로 양을 정확히 알 수 없어 1인분으로 설정했어요."));
    }

    @Test
    void returnsFailedAsNormalStatusResponse() throws Exception {
        Recognition recognition = processing(member.getId());
        recognition.fail("RECOGNITION_FAILED", "사진 인식에 실패했어요. 메뉴를 직접 선택해 주세요.");
        recognitionRepository.save(recognition);

        mockMvc.perform(get("/api/v1/recognitions/{id}", recognition.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.error.code").value("RECOGNITION_FAILED"));
    }

    @Test
    void forbidsAnotherMembersRecognition() throws Exception {
        Member other = memberRepository.save(Member.create("other-recognition@example.com", "password-hash", "다른 사용자",
                LocalDate.of(2000, 1, 1), "other-phone", "other-recognition-phone-hash", false));
        Recognition recognition = processing(other.getId());

        mockMvc.perform(get("/api/v1/recognitions/{id}", recognition.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("RECOGNITION_FORBIDDEN"));
    }

    @Test
    void returnsNotFoundForUnknownRecognition() throws Exception {
        mockMvc.perform(get("/api/v1/recognitions/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RECOGNITION_NOT_FOUND"));
    }

    private Recognition processing(UUID memberId) {
        UUID imageId = UUID.randomUUID();
        RecognitionImage image = imageRepository.save(RecognitionImage.create(imageId, memberId,
                "recognitions/" + memberId + "/" + imageId + ".jpg", "image/jpeg", 10, "a".repeat(64), Instant.now()));
        return recognitionRepository.save(Recognition.processing(memberId, image, UUID.randomUUID(), "b".repeat(64),
                ImageType.FOOD_PHOTO, Instant.now()));
    }

    private Food food(String name, String code, String initials) {
        return Food.create(UUID.randomUUID(), code, name, "분식", FoodCategory.BUNSIK, initials,
                "1인분", BigDecimal.ONE, ServingUnit.COUNT, new BigDecimal("70"),
                DataQuality.MEASURED, DataQuality.ESTIMATED);
    }
}
