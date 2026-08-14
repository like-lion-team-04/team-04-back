package com.likelion.firstbite.firstbiteserver.meal;

import com.likelion.firstbite.firstbiteserver.auth.token.JwtTokenService;
import com.likelion.firstbite.firstbiteserver.food.domain.*;
import com.likelion.firstbite.firstbiteserver.food.repository.FoodRepository;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealItem;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealSource;
import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MealControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired MealRepository mealRepository;
    @Autowired FoodRepository foodRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired JwtTokenService jwtTokenService;

    private Food food;
    private Food replacementFood;
    private Member member;
    private String accessToken;

    @BeforeEach
    void setUp() {
        mealRepository.deleteAll();
        foodRepository.deleteAll();
        memberRepository.deleteAll();
        member = memberRepository.save(Member.create(
                "meal-user@example.com", "password-hash", "테스터", LocalDate.of(2000, 1, 1),
                "encrypted-phone", "meal-test-phone-hash", false));
        accessToken = jwtTokenService.issue(member.getId());
        food = foodRepository.save(Food.create(UUID.randomUUID(), "boiled-egg", "삶은 계란", "단백질",
                FoodCategory.OTHER, "ㅅㅇㄱㄹ", "1개", BigDecimal.ONE, ServingUnit.COUNT,
                new BigDecimal("30"), DataQuality.MEASURED, DataQuality.ESTIMATED));
        replacementFood = foodRepository.save(Food.create(UUID.randomUUID(), "tofu", "두부", "단백질",
                FoodCategory.OTHER, "ㄷㅂ", "1모", BigDecimal.ONE, ServingUnit.COUNT,
                new BigDecimal("15"), DataQuality.MEASURED, DataQuality.MEASURED));
    }

    @Test
    void createsManualMealDraft() throws Exception {
        mockMvc.perform(post("/api/v1/meals")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "source": "MANUAL",
                                  "items": [{"foodId": "%s", "servingMultiplier": 1.5}]
                                }
                                """.formatted(food.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.items[0].foodId").value(food.getId().toString()))
                .andExpect(jsonPath("$.data.items[0].name").value("삶은 계란"))
                .andExpect(jsonPath("$.data.items[0].servingMultiplier").value(1.5))
                .andExpect(jsonPath("$.data.items[0].estimated").value(true));
    }

    @Test
    void rejectsUnsupportedServingMultiplier() throws Exception {
        mockMvc.perform(post("/api/v1/meals")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"source":"MANUAL","items":[{"foodId":"%s","servingMultiplier":1.2}]}
                                """.formatted(food.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MEAL_ITEMS_INVALID"));
    }

    @Test
    void rejectsUnknownFood() throws Exception {
        mockMvc.perform(post("/api/v1/meals")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"source":"MANUAL","items":[{"foodId":"%s","servingMultiplier":1}]}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("FOOD_NOT_FOUND"));
    }

    @Test
    void imageSourceRequiresRecognitionId() throws Exception {
        mockMvc.perform(post("/api/v1/meals")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"source":"IMAGE","items":[{"foodId":"%s","servingMultiplier":1}]}
                                """.formatted(food.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MEAL_ITEMS_INVALID"));
    }

    @Test
    void rejectsEmptyItemsWithMealErrorCode() throws Exception {
        mockMvc.perform(post("/api/v1/meals")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"MANUAL\",\"items\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MEAL_ITEMS_INVALID"));
    }

    @Test
    void replacesAllItemsInDraftMeal() throws Exception {
        Meal meal = draftMeal(member.getId());

        mockMvc.perform(put("/api/v1/meals/{mealId}/items", meal.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"foodId":"%s","servingMultiplier":0.5}]}
                                """.formatted(replacementFood.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mealId").value(meal.getId().toString()))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].foodId").value(replacementFood.getId().toString()))
                .andExpect(jsonPath("$.data.items[0].name").value("두부"))
                .andExpect(jsonPath("$.data.items[0].servingMultiplier").value(0.5));
    }

    @Test
    void forbidsUpdatingAnotherMembersMeal() throws Exception {
        Member other = memberRepository.save(Member.create(
                "other@example.com", "password-hash", "다른 사용자", LocalDate.of(2000, 1, 1),
                "other-encrypted-phone", "other-phone-hash", false));
        Meal meal = draftMeal(other.getId());

        mockMvc.perform(put("/api/v1/meals/{mealId}/items", meal.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"foodId":"%s","servingMultiplier":1}]}
                                """.formatted(replacementFood.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("MEAL_FORBIDDEN"));
    }

    @Test
    void rejectsUpdatingAnalyzedMeal() throws Exception {
        Meal meal = draftMeal(member.getId());
        meal.markAnalyzed();
        mealRepository.save(meal);

        mockMvc.perform(put("/api/v1/meals/{mealId}/items", meal.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"foodId":"%s","servingMultiplier":1}]}
                                """.formatted(replacementFood.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MEAL_ALREADY_CONFIRMED"));
    }

    private Meal draftMeal(UUID memberId) {
        Meal meal = Meal.draft(memberId, MealSource.MANUAL, null);
        meal.addItem(MealItem.from(food, BigDecimal.ONE));
        return mealRepository.save(meal);
    }
}
