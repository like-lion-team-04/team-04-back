package com.likelion.firstbite.firstbiteserver.coaching;

import com.likelion.firstbite.firstbiteserver.analysis.repository.MealAnalysisRepository;
import com.likelion.firstbite.firstbiteserver.auth.token.JwtTokenService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CoachingPlanControllerTest {
    @Autowired MockMvc mockMvc;
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
        analysisRepository.deleteAll();
        mealRepository.deleteAll();
        sideMenuRepository.deleteAll();
        foodRepository.deleteAll();
        memberRepository.deleteAll();
        member = memberRepository.save(Member.create("coaching@example.com", "password", "코칭 사용자",
                LocalDate.of(2000, 1, 1), "phone", "coaching-phone-hash", false));
        token = jwtTokenService.issue(member.getId());
    }

    @Test
    void returnsProteinFiberCarbohydrateStagesInOrder() throws Exception {
        Meal meal = meal(member.getId(),
                food("egg-plan", "계란", FoodCategory.OTHER, 30, 1, 0, 6),
                food("vegetable-plan", "채소", FoodCategory.OTHER, 15, 8, 4, 2),
                food("rice-plan", "밥", FoodCategory.RICE, 70, 60, 1, 5));
        analyze(meal.getId());

        mockMvc.perform(get("/api/v1/meals/{mealId}/coaching-plan", meal.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planId").isNotEmpty())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.ruleType").value("PROTEIN_FIRST"))
                .andExpect(jsonPath("$.data.stages.length()").value(3))
                .andExpect(jsonPath("$.data.stages[0].stage").value(1))
                .andExpect(jsonPath("$.data.stages[0].title").value("단백질 음식부터"))
                .andExpect(jsonPath("$.data.stages[0].recommendedSeconds").value(300))
                .andExpect(jsonPath("$.data.stages[0].summary.nutrientName").value("PROTEIN"))
                .andExpect(jsonPath("$.data.stages[0].guide").isNotEmpty())
                .andExpect(jsonPath("$.data.stages[0].items[0].name").isNotEmpty())
                .andExpect(jsonPath("$.data.stages[1].title").value("채소·식이섬유 반찬"))
                .andExpect(jsonPath("$.data.stages[1].recommendedSeconds").value(300))
                .andExpect(jsonPath("$.data.stages[2].title").value("밥·면"))
                .andExpect(jsonPath("$.data.stages[2].recommendedSeconds").doesNotExist())
                .andExpect(jsonPath("$.data.recommendedOrder.length()").value(3))
                .andExpect(jsonPath("$.data.recommendedOrder[0].order").value(1))
                .andExpect(jsonPath("$.data.guideTone").value("NON_RESTRICTIVE"));
    }

    @Test
    void omitsEmptyProteinStageAndRenumbersRemainingStages() throws Exception {
        Meal meal = meal(member.getId(),
                food("vegetable-only", "채소", FoodCategory.OTHER, 15, 8, 4, 2),
                food("rice-only", "밥", FoodCategory.RICE, 70, 60, 1, 4));
        analyze(meal.getId());

        mockMvc.perform(get("/api/v1/meals/{mealId}/coaching-plan", meal.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stages.length()").value(2))
                .andExpect(jsonPath("$.data.stages[0].stage").value(1))
                .andExpect(jsonPath("$.data.stages[0].title").value("채소·식이섬유 반찬"))
                .andExpect(jsonPath("$.data.stages[0].recommendedSeconds").value(300))
                .andExpect(jsonPath("$.data.stages[1].stage").value(2))
                .andExpect(jsonPath("$.data.stages[1].title").value("밥·면"))
                .andExpect(jsonPath("$.data.stages[1].recommendedSeconds").doesNotExist());
    }

    @Test
    void requiresCompletedAnalysis() throws Exception {
        Meal meal = meal(member.getId(), food("draft-plan", "밥", FoodCategory.RICE, 70, 60, 1, 4));

        mockMvc.perform(get("/api/v1/meals/{mealId}/coaching-plan", meal.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ANALYSIS_REQUIRED"));
    }

    @Test
    void forbidsAnotherMembersMeal() throws Exception {
        Member other = memberRepository.save(Member.create("coaching-other@example.com", "password", "다른 사용자",
                LocalDate.of(2000, 1, 1), "other-phone", "coaching-other-phone-hash", false));
        Meal meal = meal(other.getId(), food("other-plan", "밥", FoodCategory.RICE, 70, 60, 1, 4));

        mockMvc.perform(get("/api/v1/meals/{mealId}/coaching-plan", meal.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("MEAL_FORBIDDEN"));
    }

    private void analyze(UUID mealId) throws Exception {
        mockMvc.perform(post("/api/v1/meals/{mealId}/analysis", mealId)
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated());
    }

    private Food food(String code, String name, FoodCategory category, int gi, int carb, int fiber, int protein) {
        Food food = Food.create(UUID.randomUUID(), code, name, "테스트", category, name,
                "1인분", BigDecimal.ONE, ServingUnit.COUNT, BigDecimal.valueOf(gi),
                DataQuality.MEASURED, DataQuality.MEASURED);
        food.updateNutrition(BigDecimal.valueOf(carb), BigDecimal.valueOf(fiber), BigDecimal.valueOf(protein),
                BigDecimal.ZERO, new BigDecimal("100"));
        return foodRepository.save(food);
    }

    private Meal meal(UUID memberId, Food... foods) {
        Meal meal = Meal.draft(memberId, MealSource.MANUAL, null);
        for (Food food : foods) meal.addItem(MealItem.from(food, BigDecimal.ONE));
        return mealRepository.save(meal);
    }
}
