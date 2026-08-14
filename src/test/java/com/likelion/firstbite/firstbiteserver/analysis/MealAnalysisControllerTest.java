package com.likelion.firstbite.firstbiteserver.analysis;

import com.likelion.firstbite.firstbiteserver.analysis.repository.MealAnalysisRepository;
import com.likelion.firstbite.firstbiteserver.auth.token.JwtTokenService;
import com.likelion.firstbite.firstbiteserver.food.domain.*;
import com.likelion.firstbite.firstbiteserver.food.repository.FoodRepository;
import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealItem;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealSource;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MealAnalysisControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired MealAnalysisRepository analysisRepository;
    @Autowired MealRepository mealRepository;
    @Autowired FoodRepository foodRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired JwtTokenService jwtTokenService;

    private Member member;
    private String accessToken;

    @BeforeEach
    void setUp() {
        analysisRepository.deleteAll();
        mealRepository.deleteAll();
        foodRepository.deleteAll();
        memberRepository.deleteAll();
        member = memberRepository.save(Member.create("analysis@example.com", "password-hash", "분석 사용자",
                LocalDate.of(2000, 1, 1), "encrypted-phone", "analysis-phone-hash", false));
        accessToken = jwtTokenService.issue(member.getId());
    }

    @Test
    void calculatesBaselineGlWithServingMultiplier() throws Exception {
        Food rice = food("rice", "밥", new BigDecimal("70"), new BigDecimal("50"),
                new BigDecimal("2"), new BigDecimal("5"), DataQuality.MEASURED);
        Meal meal = meal(member.getId(), rice, new BigDecimal("1.5"));

        mockMvc.perform(post("/api/v1/meals/{mealId}/analysis", meal.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usePersonalization\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.baselineGl").value(50.4))
                .andExpect(jsonPath("$.data.recommendedGl").value(46.87))
                .andExpect(jsonPath("$.data.reliefRate").value(0.07))
                .andExpect(jsonPath("$.data.personalCoefficient").value(1.0))
                .andExpect(jsonPath("$.data.estimatedItemRatio").value(0.0))
                .andExpect(jsonPath("$.data.disclaimer").value("개인 혈당 예측이 아닌 상대 비교입니다."));
    }

    @Test
    void returnsSameAnalysisForSameIdempotencyKey() throws Exception {
        Food food = food("mixed", "혼합식", new BigDecimal("50"), new BigDecimal("20"),
                new BigDecimal("1"), new BigDecimal("3"), DataQuality.ESTIMATED);
        Meal meal = meal(member.getId(), food, BigDecimal.ONE);
        UUID key = UUID.randomUUID();

        String first = mockMvc.perform(post("/api/v1/meals/{mealId}/analysis", meal.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/v1/meals/{mealId}/analysis", meal.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(second).isEqualTo(first);
        org.assertj.core.api.Assertions.assertThat(analysisRepository.count()).isEqualTo(1);
    }

    @Test
    void rejectsIdempotencyKeyUsedForDifferentMeal() throws Exception {
        Food food = food("tofu", "두부", new BigDecimal("15"), new BigDecimal("2"),
                BigDecimal.ZERO, new BigDecimal("8"), DataQuality.MEASURED);
        Meal firstMeal = meal(member.getId(), food, BigDecimal.ONE);
        Meal secondMeal = meal(member.getId(), food, BigDecimal.ONE);
        UUID key = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/meals/{mealId}/analysis", firstMeal.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/meals/{mealId}/analysis", secondMeal.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("IDEMPOTENCY_KEY_CONFLICT"));
    }

    @Test
    void forbidsAnalyzingAnotherMembersMeal() throws Exception {
        Member other = memberRepository.save(Member.create("other-analysis@example.com", "password-hash", "다른 사용자",
                LocalDate.of(2000, 1, 1), "other-phone", "other-analysis-phone-hash", false));
        Food food = food("egg", "계란", new BigDecimal("30"), new BigDecimal("1"),
                BigDecimal.ZERO, new BigDecimal("6"), DataQuality.MEASURED);
        Meal meal = meal(other.getId(), food, BigDecimal.ONE);

        mockMvc.perform(post("/api/v1/meals/{mealId}/analysis", meal.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("MEAL_FORBIDDEN"));
    }

    @Test
    void getsLatestAnalysisWithCurvesQualityAndSources() throws Exception {
        Food measured = food("rice-detail", "밥", new BigDecimal("70"), new BigDecimal("50"),
                new BigDecimal("2"), new BigDecimal("5"), DataQuality.MEASURED);
        Food estimated = food("vegetable-detail", "채소", new BigDecimal("15"), new BigDecimal("10"),
                new BigDecimal("4"), new BigDecimal("2"), DataQuality.ESTIMATED);
        Meal meal = Meal.draft(member.getId(), MealSource.MANUAL, null);
        meal.addItem(MealItem.from(measured, BigDecimal.ONE));
        meal.addItem(MealItem.from(estimated, BigDecimal.ONE));
        mealRepository.save(meal);
        createAnalysis(meal.getId(), UUID.randomUUID());

        mockMvc.perform(get("/api/v1/meals/{mealId}/analysis", meal.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysisId").isNotEmpty())
                .andExpect(jsonPath("$.data.baseline.gl").value(34.5))
                .andExpect(jsonPath("$.data.baseline.curve.length()").value(5))
                .andExpect(jsonPath("$.data.baseline.curve[2]").value(1.0))
                .andExpect(jsonPath("$.data.recommended.gl").value(32.09))
                .andExpect(jsonPath("$.data.recommended.curve[2]").value(0.93))
                .andExpect(jsonPath("$.data.reliefRate").value(0.07))
                .andExpect(jsonPath("$.data.dataQuality").value("MIXED"))
                .andExpect(jsonPath("$.data.sources.length()").value(2))
                .andExpect(jsonPath("$.data.sources[0].evidenceId").value("MFDS_NUTRITION_DB"));
    }

    @Test
    void returnsNotFoundWhenAnalysisDoesNotExist() throws Exception {
        Food food = food("no-analysis", "미분석 음식", new BigDecimal("30"), new BigDecimal("10"),
                BigDecimal.ZERO, BigDecimal.ONE, DataQuality.MEASURED);
        Meal meal = meal(member.getId(), food, BigDecimal.ONE);

        mockMvc.perform(get("/api/v1/meals/{mealId}/analysis", meal.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ANALYSIS_NOT_FOUND"));
    }

    @Test
    void forbidsReadingAnotherMembersAnalysis() throws Exception {
        Member other = memberRepository.save(Member.create("analysis-owner@example.com", "password-hash", "소유자",
                LocalDate.of(2000, 1, 1), "owner-phone", "owner-phone-hash", false));
        Food food = food("owned-analysis", "소유 음식", new BigDecimal("30"), new BigDecimal("10"),
                BigDecimal.ZERO, BigDecimal.ONE, DataQuality.MEASURED);
        Meal meal = meal(other.getId(), food, BigDecimal.ONE);

        mockMvc.perform(get("/api/v1/meals/{mealId}/analysis", meal.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("MEAL_FORBIDDEN"));
    }

    private void createAnalysis(UUID mealId, UUID key) throws Exception {
        mockMvc.perform(post("/api/v1/meals/{mealId}/analysis", mealId)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
    }

    private Food food(String code, String name, BigDecimal gi, BigDecimal carb, BigDecimal fiber,
                      BigDecimal protein, DataQuality quality) {
        Food food = Food.create(UUID.randomUUID(), code, name, "테스트", FoodCategory.OTHER, name,
                "1인분", BigDecimal.ONE, ServingUnit.COUNT, gi, quality, quality);
        food.updateNutrition(carb, fiber, protein, BigDecimal.ZERO, new BigDecimal("100"));
        return foodRepository.save(food);
    }

    private Meal meal(UUID memberId, Food food, BigDecimal multiplier) {
        Meal meal = Meal.draft(memberId, MealSource.MANUAL, null);
        meal.addItem(MealItem.from(food, multiplier));
        return mealRepository.save(meal);
    }
}
