package com.likelion.firstbite.firstbiteserver.coaching;

import com.likelion.firstbite.firstbiteserver.analysis.repository.MealAnalysisRepository;
import com.likelion.firstbite.firstbiteserver.auth.token.JwtTokenService;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingSessionRepository;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingStageRecordRepository;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingRecordRepository;
import com.likelion.firstbite.firstbiteserver.food.domain.*;
import com.likelion.firstbite.firstbiteserver.food.repository.FoodRepository;
import com.likelion.firstbite.firstbiteserver.meal.domain.*;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import com.likelion.firstbite.firstbiteserver.sidemenu.repository.SideMenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CoachingSessionControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired CoachingSessionRepository sessionRepository;
    @Autowired CoachingStageRecordRepository stageRecordRepository;
    @Autowired CoachingRecordRepository coachingRecordRepository;
    @Autowired MealAnalysisRepository analysisRepository;
    @Autowired MealRepository mealRepository;
    @Autowired SideMenuRepository sideMenuRepository;
    @Autowired FoodRepository foodRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired JwtTokenService jwtTokenService;

    private Member member;
    private String token;

    @BeforeEach
    void setUp() {
        coachingRecordRepository.deleteAll();
        stageRecordRepository.deleteAll();
        sessionRepository.deleteAll();
        analysisRepository.deleteAll();
        mealRepository.deleteAll();
        sideMenuRepository.deleteAll();
        foodRepository.deleteAll();
        memberRepository.deleteAll();
        member = memberRepository.save(Member.create("session@example.com", "password", "세션 사용자",
                LocalDate.of(2000, 1, 1), "phone", "session-phone-hash", false));
        token = jwtTokenService.issue(member.getId());
    }

    @Test
    void startsFirstStageTimer() throws Exception {
        Meal meal = analyzedMeal(member.getId(), "start");

        mockMvc.perform(startRequest(meal.getId(), 1, UUID.randomUUID()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sessionId").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.currentStage").value(1))
                .andExpect(jsonPath("$.data.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.stageEndsAt").isNotEmpty());
    }

    @Test
    void returnsSameSessionForSameIdempotencyKey() throws Exception {
        Meal meal = analyzedMeal(member.getId(), "same-key");
        UUID key = UUID.randomUUID();

        String first = mockMvc.perform(startRequest(meal.getId(), 1, key))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(startRequest(meal.getId(), 1, key))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(second).isEqualTo(first);
        org.assertj.core.api.Assertions.assertThat(sessionRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsSameIdempotencyKeyForDifferentRequest() throws Exception {
        Meal firstMeal = analyzedMeal(member.getId(), "key-first");
        Meal secondMeal = analyzedMeal(member.getId(), "key-second");
        UUID key = UUID.randomUUID();
        mockMvc.perform(startRequest(firstMeal.getId(), 1, key)).andExpect(status().isCreated());

        mockMvc.perform(startRequest(secondMeal.getId(), 1, key))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_CONFLICT"));
    }

    @Test
    void rejectsChangedPlanVersion() throws Exception {
        Meal meal = analyzedMeal(member.getId(), "changed-plan");

        mockMvc.perform(startRequest(meal.getId(), 2, UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("COACHING_PLAN_CHANGED"));
    }

    @Test
    void preventsTwoActiveSessionsForSameMember() throws Exception {
        Meal firstMeal = analyzedMeal(member.getId(), "active-first");
        Meal secondMeal = analyzedMeal(member.getId(), "active-second");
        mockMvc.perform(startRequest(firstMeal.getId(), 1, UUID.randomUUID())).andExpect(status().isCreated());

        mockMvc.perform(startRequest(secondMeal.getId(), 1, UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("COACHING_ALREADY_ACTIVE"));
    }

    @Test
    void rejectsMealWithoutAnalysis() throws Exception {
        Meal meal = meal(member.getId(), "not-ready");

        mockMvc.perform(startRequest(meal.getId(), 1, UUID.randomUUID()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("MEAL_NOT_READY"));
    }

    @Test
    void savesNextStageProgressAndActualInterval() throws Exception {
        Meal meal = analyzedMeal(member.getId(), "progress-next");
        UUID sessionId = startSession(meal.getId());

        mockMvc.perform(patchProgress(sessionId, "NEXT", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.data.currentStage").value(2))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.previousStage.stage").value(1))
                .andExpect(jsonPath("$.data.previousStage.result").value("COMPLETED"))
                .andExpect(jsonPath("$.data.previousStage.actualSeconds").isNumber())
                .andExpect(jsonPath("$.data.stageEndsAt").isNotEmpty());

        org.assertj.core.api.Assertions.assertThat(stageRecordRepository.countBySessionId(sessionId)).isEqualTo(1);
    }

    @Test
    void recordsSkippedStage() throws Exception {
        Meal meal = analyzedMeal(member.getId(), "progress-skip");
        UUID sessionId = startSession(meal.getId());

        mockMvc.perform(patchProgress(sessionId, "SKIP", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previousStage.result").value("SKIPPED"));
    }

    @Test
    void rejectsStaleExpectedStageWithoutDuplicateTransition() throws Exception {
        Meal meal = analyzedMeal(member.getId(), "progress-conflict");
        UUID sessionId = startSession(meal.getId());
        mockMvc.perform(patchProgress(sessionId, "AUTO_ADVANCE", 1)).andExpect(status().isOk());

        mockMvc.perform(patchProgress(sessionId, "AUTO_ADVANCE", 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("COACHING_STAGE_CONFLICT"));
        org.assertj.core.api.Assertions.assertThat(stageRecordRepository.countBySessionId(sessionId)).isEqualTo(1);
    }

    @Test
    void rejectsUnknownProgressAction() throws Exception {
        Meal meal = analyzedMeal(member.getId(), "progress-invalid");
        UUID sessionId = startSession(meal.getId());

        mockMvc.perform(patchProgress(sessionId, "PAUSE", 1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COACHING_ACTION_INVALID"));
    }

    @Test
    void requiresCompleteEndpointAtLastStage() throws Exception {
        Meal meal = analyzedMeal(member.getId(), "progress-last");
        UUID sessionId = startSession(meal.getId());
        mockMvc.perform(patchProgress(sessionId, "NEXT", 1)).andExpect(status().isOk());
        mockMvc.perform(patchProgress(sessionId, "NEXT", 2)).andExpect(status().isOk());

        mockMvc.perform(patchProgress(sessionId, "NEXT", 3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COACHING_ACTION_INVALID"));
    }

    @Test
    void completesLastStageAndCreatesCoachingRecord() throws Exception {
        Meal meal = analyzedMeal(member.getId(), "complete-all");
        UUID sessionId = startSession(meal.getId());
        mockMvc.perform(patchProgress(sessionId, "NEXT", 1)).andExpect(status().isOk());
        mockMvc.perform(patchProgress(sessionId, "NEXT", 2)).andExpect(status().isOk());

        mockMvc.perform(completeRequest(sessionId, "COMPLETED", UUID.randomUUID(), java.time.Instant.now()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recordId").isNotEmpty())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.summary.completedStages").value(3))
                .andExpect(jsonPath("$.data.summary.skippedStages").value(0))
                .andExpect(jsonPath("$.data.summary.totalSeconds").isNumber());

        org.assertj.core.api.Assertions.assertThat(coachingRecordRepository.count()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(stageRecordRepository.countBySessionId(sessionId)).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(sessionRepository.findById(sessionId).orElseThrow().getStatus().name())
                .isEqualTo("COMPLETED");
    }

    @Test
    void userCanEndBeforeLastStage() throws Exception {
        Meal meal = analyzedMeal(member.getId(), "user-ended");
        UUID sessionId = startSession(meal.getId());

        mockMvc.perform(completeRequest(sessionId, "USER_ENDED", UUID.randomUUID(), java.time.Instant.now()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.summary.completedStages").value(0))
                .andExpect(jsonPath("$.data.summary.skippedStages").value(1));
    }

    @Test
    void completionIsIdempotentForSameKeyAndRequest() throws Exception {
        Meal meal = analyzedMeal(member.getId(), "complete-key");
        UUID sessionId = startSession(meal.getId());
        UUID key = UUID.randomUUID();
        java.time.Instant endedAt = java.time.Instant.now();

        String first = mockMvc.perform(completeRequest(sessionId, "USER_ENDED", key, endedAt))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(completeRequest(sessionId, "USER_ENDED", key, endedAt))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(second).isEqualTo(first);
        org.assertj.core.api.Assertions.assertThat(coachingRecordRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsCompletedReasonBeforeLastStage() throws Exception {
        Meal meal = analyzedMeal(member.getId(), "complete-early");
        UUID sessionId = startSession(meal.getId());

        mockMvc.perform(completeRequest(sessionId, "COMPLETED", UUID.randomUUID(), java.time.Instant.now()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COACHING_COMPLETION_INVALID"));
    }

    @Test
    void rejectsSecondCompletionWithDifferentKey() throws Exception {
        Meal meal = analyzedMeal(member.getId(), "complete-twice");
        UUID sessionId = startSession(meal.getId());
        mockMvc.perform(completeRequest(sessionId, "USER_ENDED", UUID.randomUUID(), java.time.Instant.now()))
                .andExpect(status().isCreated());

        mockMvc.perform(completeRequest(sessionId, "USER_ENDED", UUID.randomUUID(), java.time.Instant.now()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("COACHING_ALREADY_COMPLETED"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder startRequest(
            UUID mealId, int version, UUID key) {
        return post("/api/v1/coaching-sessions")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mealId\":\"%s\",\"planVersion\":%d}".formatted(mealId, version));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder patchProgress(
            UUID sessionId, String action, int expectedStage) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/v1/coaching-sessions/{sessionId}", sessionId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"action":"%s","expectedStage":%d,"occurredAt":"%s"}
                        """.formatted(action, expectedStage, java.time.Instant.now()));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder completeRequest(
            UUID sessionId, String reason, UUID key, java.time.Instant endedAt) {
        return post("/api/v1/coaching-sessions/{sessionId}/complete", sessionId)
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"%s\",\"endedAt\":\"%s\"}".formatted(reason, endedAt));
    }

    private UUID startSession(UUID mealId) throws Exception {
        mockMvc.perform(startRequest(mealId, 1, UUID.randomUUID())).andExpect(status().isCreated());
        return sessionRepository.findAll().get(0).getId();
    }

    private Meal analyzedMeal(UUID memberId, String prefix) throws Exception {
        Meal meal = meal(memberId, prefix);
        mockMvc.perform(post("/api/v1/meals/{mealId}/analysis", meal.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        return meal;
    }

    private Meal meal(UUID memberId, String prefix) {
        Food egg = food(prefix + "-egg", "계란", FoodCategory.OTHER, 30, 1, 0, 6);
        Food rice = food(prefix + "-rice", "밥", FoodCategory.RICE, 70, 60, 1, 4);
        Food vegetable = food(prefix + "-vegetable", "채소", FoodCategory.OTHER, 15, 8, 4, 2);
        Meal meal = Meal.draft(memberId, MealSource.MANUAL, null);
        meal.addItem(MealItem.from(egg, BigDecimal.ONE));
        meal.addItem(MealItem.from(vegetable, BigDecimal.ONE));
        meal.addItem(MealItem.from(rice, BigDecimal.ONE));
        return mealRepository.save(meal);
    }

    private Food food(String code, String name, FoodCategory category, int gi, int carb, int fiber, int protein) {
        Food food = Food.create(UUID.randomUUID(), code, name, "테스트", category, name,
                "1인분", BigDecimal.ONE, ServingUnit.COUNT, BigDecimal.valueOf(gi),
                DataQuality.MEASURED, DataQuality.MEASURED);
        food.updateNutrition(BigDecimal.valueOf(carb), BigDecimal.valueOf(fiber), BigDecimal.valueOf(protein),
                BigDecimal.ZERO, new BigDecimal("100"));
        return foodRepository.save(food);
    }
}
