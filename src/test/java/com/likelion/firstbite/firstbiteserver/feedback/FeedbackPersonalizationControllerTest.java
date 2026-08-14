package com.likelion.firstbite.firstbiteserver.feedback;

import com.likelion.firstbite.firstbiteserver.auth.token.JwtTokenService;
import com.likelion.firstbite.firstbiteserver.analysis.repository.MealAnalysisRepository;
import com.likelion.firstbite.firstbiteserver.coaching.domain.*;
import com.likelion.firstbite.firstbiteserver.coaching.repository.*;
import com.likelion.firstbite.firstbiteserver.feedback.repository.*;
import com.likelion.firstbite.firstbiteserver.food.domain.*;
import com.likelion.firstbite.firstbiteserver.food.repository.FoodRepository;
import com.likelion.firstbite.firstbiteserver.meal.domain.*;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FeedbackPersonalizationControllerTest {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    @Autowired MockMvc mockMvc;
    @Autowired CoachingFeedbackRepository feedbackRepository;
    @Autowired MealAnalysisRepository analysisRepository;
    @Autowired PersonalizationProfileRepository profileRepository;
    @Autowired CoachingRecordRepository recordRepository;
    @Autowired CoachingStageRecordRepository stageRecordRepository;
    @Autowired CoachingSessionRepository sessionRepository;
    @Autowired MealRepository mealRepository;
    @Autowired FoodRepository foodRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired JwtTokenService jwtTokenService;
    private Member member;
    private String token;
    private Food food;

    @BeforeEach
    void setUp() {
        feedbackRepository.deleteAll(); profileRepository.deleteAll(); recordRepository.deleteAll(); analysisRepository.deleteAll();
        stageRecordRepository.deleteAll(); sessionRepository.deleteAll(); mealRepository.deleteAll();
        foodRepository.deleteAll(); memberRepository.deleteAll();
        member = memberRepository.save(Member.create("feedback@example.com", "password", "피드백 사용자",
                LocalDate.of(2000, 1, 1), "phone", "feedback-phone-hash", false));
        token = jwtTokenService.issue(member.getId());
        food = foodRepository.save(food());
    }

    @Test
    void findsYesterdayPendingFeedbackAndSubmitsOneTapScore() throws Exception {
        CoachingRecord record = completedRecord(1);
        String date = record.getCompletedAt().atZone(SEOUL).toLocalDate().toString();

        mockMvc.perform(get("/api/v1/feedbacks/pending").header("Authorization", bearer()).param("date", date))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.pending").value(true))
                .andExpect(jsonPath("$.data.recordId").value(record.getId().toString()))
                .andExpect(jsonPath("$.data.scale.min").value(1)).andExpect(jsonPath("$.data.scale.max").value(5));

        mockMvc.perform(post("/api/v1/coaching-records/{recordId}/feedback", record.getId())
                        .header("Authorization", bearer()).header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body(4)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.sleepinessScore").value(4))
                .andExpect(jsonPath("$.data.feedbackCount").value(1))
                .andExpect(jsonPath("$.data.personalizationUpdated").value(false));

        mockMvc.perform(get("/api/v1/feedbacks/pending").header("Authorization", bearer()).param("date", date))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.pending").value(false));
    }

    @Test
    void replaysIdenticalResponseForSameIdempotencyKey() throws Exception {
        CoachingRecord record = completedRecord(1);
        UUID key = UUID.randomUUID();
        String requestBody = body(3);
        String first = mockMvc.perform(post("/api/v1/coaching-records/{recordId}/feedback", record.getId())
                        .header("Authorization", bearer()).header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/v1/coaching-records/{recordId}/feedback", record.getId())
                        .header("Authorization", bearer()).header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(second).isEqualTo(first);
        assertThat(feedbackRepository.count()).isEqualTo(1);
    }

    @Test
    void activatesPersonalizationAfterThreeValidAnswers() throws Exception {
        for (int i = 0; i < 3; i++) {
            CoachingRecord record = completedRecord(i + 1);
            mockMvc.perform(post("/api/v1/coaching-records/{recordId}/feedback", record.getId())
                            .header("Authorization", bearer()).header("Idempotency-Key", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON).content(body(4)))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(get("/api/v1/personalization").header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.feedbackCount").value(3))
                .andExpect(jsonPath("$.data.coefficient").value(0.9))
                .andExpect(jsonPath("$.data.direction").value("GENTLER"));

        Meal personalizedMeal = Meal.draft(member.getId(), MealSource.MANUAL, null);
        personalizedMeal.addItem(MealItem.from(food, BigDecimal.ONE));
        mealRepository.save(personalizedMeal);
        mockMvc.perform(post("/api/v1/meals/{mealId}/analysis", personalizedMeal.getId())
                        .header("Authorization", bearer()).header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"usePersonalization\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.personalCoefficient").value(0.9));
    }

    @Test
    void rejectsInvalidScoreCombinationAndDuplicateRecord() throws Exception {
        CoachingRecord record = completedRecord(1);
        mockMvc.perform(post("/api/v1/coaching-records/{recordId}/feedback", record.getId())
                        .header("Authorization", bearer()).header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sleepinessScore\":5,\"skipped\":true,\"answeredAt\":\"" + Instant.now() + "\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("FEEDBACK_VALUE_INVALID"));
        mockMvc.perform(post("/api/v1/coaching-records/{recordId}/feedback", record.getId())
                        .header("Authorization", bearer()).header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body(2)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/coaching-records/{recordId}/feedback", record.getId())
                        .header("Authorization", bearer()).header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body(3)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("FEEDBACK_ALREADY_EXISTS"));
    }

    private CoachingRecord completedRecord(int sequence) {
        Meal meal = Meal.draft(member.getId(), MealSource.MANUAL, null);
        meal.addItem(MealItem.from(food, BigDecimal.ONE)); mealRepository.save(meal);
        Instant completedAt = LocalDate.now(SEOUL).minusDays(1).atTime(12, sequence)
                .atZone(SEOUL).toInstant();
        CoachingSession session = CoachingSession.start(meal.getId(), member.getId(), 1, 3, 300,
                UUID.randomUUID(), "start-" + sequence, completedAt.minusSeconds(600));
        session.complete(completedAt); sessionRepository.save(session);
        return recordRepository.save(CoachingRecord.create(session, CompletionReason.COMPLETED,
                3, 0, 600, completedAt, completedAt, UUID.randomUUID(), "complete-" + sequence));
    }

    private Food food() {
        Food value = Food.create(UUID.randomUUID(), "feedback-food", "밥", "테스트", FoodCategory.RICE,
                "ㅂ", "1인분", BigDecimal.ONE, ServingUnit.G, new BigDecimal("50"),
                DataQuality.MEASURED, DataQuality.MEASURED);
        value.updateNutrition(new BigDecimal("20"), new BigDecimal("2"), new BigDecimal("5"),
                BigDecimal.ONE, new BigDecimal("120"));
        return value;
    }

    private String bearer() { return "Bearer " + token; }
    private String body(int score) {
        return "{\"sleepinessScore\":" + score + ",\"skipped\":false,\"answeredAt\":\"" + Instant.now() + "\"}";
    }
}
