package com.likelion.firstbite.firstbiteserver.history;

import com.likelion.firstbite.firstbiteserver.auth.token.JwtTokenService;
import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import com.likelion.firstbite.firstbiteserver.coaching.domain.*;
import com.likelion.firstbite.firstbiteserver.coaching.repository.*;
import com.likelion.firstbite.firstbiteserver.food.domain.*;
import com.likelion.firstbite.firstbiteserver.food.repository.FoodRepository;
import com.likelion.firstbite.firstbiteserver.history.repository.MealReuseRepository;
import com.likelion.firstbite.firstbiteserver.meal.domain.*;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class HistoryEvidenceControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired CoachingRecordRepository recordRepository;
    @Autowired CoachingStageRecordRepository stageRecordRepository;
    @Autowired CoachingSessionRepository sessionRepository;
    @Autowired MealReuseRepository reuseRepository;
    @Autowired MealRepository mealRepository;
    @Autowired FoodRepository foodRepository;
    private Member member;
    private String token;

    @BeforeEach
    void setUp() {
        reuseRepository.deleteAll();
        recordRepository.deleteAll();
        stageRecordRepository.deleteAll();
        sessionRepository.deleteAll();
        mealRepository.deleteAll();
        foodRepository.deleteAll();
        memberRepository.deleteAll();
        member = memberRepository.save(Member.create("history@example.com", "password", "기록 사용자",
                LocalDate.of(2000, 1, 1), "phone", "history-phone-hash", false));
        token = jwtTokenService.issue(member.getId());
    }

    @Test
    void returnsEmptyHistoryWithPageMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/coaching-records").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.totalElements").value(0));
    }

    @Test
    void returnsEmptyWeeklySummary() throws Exception {
        mockMvc.perform(get("/api/v1/coaching-records/summary")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-08-03").param("to", "2026-08-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coachingCount").value(0))
                .andExpect(jsonPath("$.data.completionRate").value(0.0))
                .andExpect(jsonPath("$.data.daily.length()").value(7));
    }

    @Test
    void rejectsSummaryLongerThanThirtyOneDays() throws Exception {
        mockMvc.perform(get("/api/v1/coaching-records/summary")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-07-01").param("to", "2026-08-09"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("HISTORY_PERIOD_INVALID"));
    }

    @Test
    void returnsFilteredEvidenceAndCalculationFormula() throws Exception {
        mockMvc.perform(get("/api/v1/evidence").header("Authorization", "Bearer " + token).param("type", "GI"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sources.length()").value(1))
                .andExpect(jsonPath("$.data.sources[0].type").value("GI"))
                .andExpect(jsonPath("$.data.calculation.glFormula").value("GI × availableCarbohydrateG / 100"));
    }

    @Test
    void rejectsUnknownEvidenceType() throws Exception {
        mockMvc.perform(get("/api/v1/evidence").header("Authorization", "Bearer " + token).param("type", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("EVIDENCE_FILTER_INVALID"));
    }

    @Test
    void returnsNotFoundForMissingRecordAndReuseSource() throws Exception {
        UUID recordId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/coaching-records/{recordId}", recordId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COACHING_RECORD_NOT_FOUND"));
        mockMvc.perform(post("/api/v1/coaching-records/{recordId}/reuse", recordId)
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"includeSideMenus\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COACHING_RECORD_NOT_FOUND"));
    }

    @Test
    void returnsStoredHistoryDetailAndReusesItsMenu() throws Exception {
        Food rice = food("history-rice", "밥");
        Food egg = food("history-egg", "계란");
        Meal meal = Meal.draft(member.getId(), MealSource.MANUAL, null);
        meal.addItem(MealItem.from(rice, BigDecimal.ONE));
        meal.addItem(MealItem.from(egg, new BigDecimal("0.5")));
        mealRepository.save(meal);
        Instant completedAt = Instant.parse("2026-08-09T04:20:00Z");
        CoachingSession session = CoachingSession.start(meal.getId(), member.getId(), 1, 3, 300,
                UUID.randomUUID(), "start-hash", completedAt.minusSeconds(600));
        session.complete(completedAt);
        sessionRepository.save(session);
        CoachingRecord record = recordRepository.save(CoachingRecord.create(session, CompletionReason.USER_ENDED,
                1, 1, 600, completedAt, completedAt, UUID.randomUUID(), "complete-hash"));

        mockMvc.perform(get("/api/v1/coaching-records").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].mealName").value("밥 외 1개"))
                .andExpect(jsonPath("$.data.items[0].completedStages").value(1))
                .andExpect(jsonPath("$.data.items[0].totalStages").value(3));

        mockMvc.perform(get("/api/v1/coaching-records/{recordId}", record.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[1].name").value("계란"));

        mockMvc.perform(post("/api/v1/coaching-records/{recordId}/reuse", record.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"includeSideMenus\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.source").value("REUSE"))
                .andExpect(jsonPath("$.data.copiedItemCount").value(2))
                .andExpect(jsonPath("$.data.recalculationRequired").value(true));
    }

    private Food food(String code, String name) {
        Food food = Food.create(UUID.randomUUID(), code, name, "테스트", FoodCategory.RICE,
                name, "1인분", BigDecimal.ONE, ServingUnit.G, new BigDecimal("50"),
                DataQuality.MEASURED, DataQuality.MEASURED);
        food.updateNutrition(new BigDecimal("20"), new BigDecimal("2"),
                new BigDecimal("5"), BigDecimal.ONE, new BigDecimal("120"));
        return foodRepository.save(food);
    }
}
