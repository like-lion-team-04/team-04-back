package com.likelion.firstbite.firstbiteserver.sidemenu;

import com.likelion.firstbite.firstbiteserver.analysis.repository.MealAnalysisRepository;
import com.likelion.firstbite.firstbiteserver.auth.token.JwtTokenService;
import com.likelion.firstbite.firstbiteserver.food.domain.*;
import com.likelion.firstbite.firstbiteserver.food.repository.FoodRepository;
import com.likelion.firstbite.firstbiteserver.meal.domain.*;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import com.likelion.firstbite.firstbiteserver.sidemenu.domain.NutrientFocus;
import com.likelion.firstbite.firstbiteserver.sidemenu.domain.SideMenu;
import com.likelion.firstbite.firstbiteserver.sidemenu.repository.SideMenuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SideMenuRecommendationControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired SideMenuRepository sideMenuRepository;
    @Autowired MealAnalysisRepository analysisRepository;
    @Autowired MealRepository mealRepository;
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
        member = memberRepository.save(Member.create("side@example.com", "password", "사이드 사용자",
                LocalDate.of(2000, 1, 1), "phone", "side-phone-hash", false));
        token = jwtTokenService.issue(member.getId());
    }

    @Test
    void listsSideMenusAndFiltersByNutrientFocus() throws Exception {
        SideMenu egg = side("list-egg", "Egg", NutrientFocus.PROTEIN, 1, 0, 6, 800);
        side("list-cabbage", "Cabbage", NutrientFocus.FIBER, 5, 3, 1, 700);

        mockMvc.perform(get("/api/v1/side-menus")
                        .header("Authorization", "Bearer " + token)
                        .param("nutrientFocus", "PROTEIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].sideMenuId").value(egg.getId().toString()))
                .andExpect(jsonPath("$.data.items[0].name").value("Egg"))
                .andExpect(jsonPath("$.data.items[0].nutrientFocus").value("PROTEIN"))
                .andExpect(jsonPath("$.data.items[0].proteinG").value(6))
                .andExpect(jsonPath("$.data.items[0].fiberG").value(0))
                .andExpect(jsonPath("$.data.items[0].carbohydrateG").value(1))
                .andExpect(jsonPath("$.data.items[0].fatG").value(0))
                .andExpect(jsonPath("$.data.items[0].estimatedPrice").value(800))
                .andExpect(jsonPath("$.data.items[0].active").value(true));
    }

    @Test
    void rejectsUnknownSideMenuNutrientFocus() throws Exception {
        mockMvc.perform(get("/api/v1/side-menus")
                        .header("Authorization", "Bearer " + token)
                        .param("nutrientFocus", "CARBOHYDRATE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SIDE_MENU_FILTER_INVALID"));
    }

    @Test
    void requiresAuthenticationForSideMenuList() throws Exception {
        mockMvc.perform(get("/api/v1/side-menus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recommendsUpToLimitByNutrientDeficitAndAmount() throws Exception {
        Meal meal = meal(member.getId(), food("rice", "밥", 70, 60, 1, 2));
        side("egg", "삶은 계란", NutrientFocus.PROTEIN, 1, 0, 6, 800);
        side("tofu", "두부", NutrientFocus.PROTEIN, 2, 1, 8, 1200);
        side("cabbage", "양배추", NutrientFocus.FIBER, 5, 3, 1, 700);

        mockMvc.perform(get("/api/v1/meals/{mealId}/side-menu-recommendations", meal.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].name").value("두부"))
                .andExpect(jsonPath("$.data.items[0].nutrientFocus").value("PROTEIN"))
                .andExpect(jsonPath("$.data.items[0].reason").value("단백질 8g을 보완해요."))
                .andExpect(jsonPath("$.data.items[0].expectedReliefDelta").value(0.04))
                .andExpect(jsonPath("$.data.items[0].estimatedPrice").value(1200))
                .andExpect(jsonPath("$.data.items[1].name").value("삶은 계란"));
    }

    @Test
    void returnsNotNeededWhenMealMeetsInternalTargets() throws Exception {
        Meal meal = meal(member.getId(), food("balanced", "균형 메뉴", 40, 20, 6, 16));
        side("egg-balanced", "계란", NutrientFocus.PROTEIN, 1, 0, 6, 800);

        mockMvc.perform(get("/api/v1/meals/{mealId}/side-menu-recommendations", meal.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("SIDE_MENU_NOT_NEEDED"));
    }

    @Test
    void rejectsLimitOutsideOneToThree() throws Exception {
        Meal meal = meal(member.getId(), food("limit-rice", "밥", 70, 60, 1, 2));

        mockMvc.perform(get("/api/v1/meals/{mealId}/side-menu-recommendations", meal.getId())
                        .header("Authorization", "Bearer " + token)
                        .param("limit", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SIDE_MENU_LIMIT_INVALID"));
    }

    @Test
    void forbidsAnotherMembersMeal() throws Exception {
        Member other = memberRepository.save(Member.create("side-other@example.com", "password", "다른 사용자",
                LocalDate.of(2000, 1, 1), "other-phone", "side-other-phone-hash", false));
        Meal meal = meal(other.getId(), food("other-rice", "다른 밥", 70, 60, 1, 2));

        mockMvc.perform(get("/api/v1/meals/{mealId}/side-menu-recommendations", meal.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("MEAL_FORBIDDEN"));
    }

    @Test
    void addsSideMenuAndImmediatelyRecalculatesAnalysis() throws Exception {
        Meal meal = meal(member.getId(), food("add-rice", "밥", 70, 60, 1, 2));
        SideMenu egg = side("add-egg", "삶은 계란", NutrientFocus.PROTEIN, 1, 0, 6, 800);

        mockMvc.perform(post("/api/v1/meals/{mealId}/side-menus", meal.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sideMenuId":"%s","servingMultiplier":1}
                                """.formatted(egg.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mealId").value(meal.getId().toString()))
                .andExpect(jsonPath("$.data.addedItem.mealItemId").isNotEmpty())
                .andExpect(jsonPath("$.data.addedItem.name").value("삶은 계란"))
                .andExpect(jsonPath("$.data.analysis.reliefRate").value(0.11))
                .andExpect(jsonPath("$.data.coachingPlanVersion").value(2));

        org.assertj.core.api.Assertions.assertThat(analysisRepository.count()).isEqualTo(1);
        Meal updated = mealRepository.findById(meal.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getStatus()).isEqualTo(MealStatus.ANALYZED);
        org.assertj.core.api.Assertions.assertThat(updated.getCoachingPlanVersion()).isEqualTo(2);
    }

    @Test
    void usesOneServingWhenMultiplierIsOmitted() throws Exception {
        Meal meal = meal(member.getId(), food("default-rice", "밥", 70, 60, 1, 2));
        SideMenu egg = side("default-egg", "계란", NutrientFocus.PROTEIN, 1, 0, 6, 800);

        mockMvc.perform(post("/api/v1/meals/{mealId}/side-menus", meal.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sideMenuId\":\"%s\"}".formatted(egg.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysis.reliefRate").value(0.11));
    }

    @Test
    void rejectsAddingSameSideMenuTwice() throws Exception {
        Meal meal = meal(member.getId(), food("duplicate-rice", "밥", 70, 60, 1, 2));
        SideMenu egg = side("duplicate-egg", "계란", NutrientFocus.PROTEIN, 1, 0, 6, 800);
        String body = "{\"sideMenuId\":\"%s\",\"servingMultiplier\":1}".formatted(egg.getId());

        mockMvc.perform(post("/api/v1/meals/{mealId}/side-menus", meal.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/meals/{mealId}/side-menus", meal.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SIDE_MENU_ALREADY_ADDED"));
    }

    @Test
    void rejectsUnsupportedSideMenuMultiplier() throws Exception {
        Meal meal = meal(member.getId(), food("invalid-rice", "밥", 70, 60, 1, 2));
        SideMenu egg = side("invalid-egg", "계란", NutrientFocus.PROTEIN, 1, 0, 6, 800);

        mockMvc.perform(post("/api/v1/meals/{mealId}/side-menus", meal.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sideMenuId\":\"%s\",\"servingMultiplier\":1.2}".formatted(egg.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SIDE_MENU_REQUEST_INVALID"));
    }

    @Test
    void removesAddedSideMenuAndRecalculatesAnalysis() throws Exception {
        Meal meal = meal(member.getId(), food("remove-rice", "밥", 70, 60, 1, 2));
        SideMenu egg = side("remove-egg", "삶은 계란", NutrientFocus.PROTEIN, 1, 0, 6, 800);
        addSideMenu(meal.getId(), egg.getId());

        mockMvc.perform(delete("/api/v1/meals/{mealId}/side-menus/{sideMenuId}", meal.getId(), egg.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mealId").value(meal.getId().toString()))
                .andExpect(jsonPath("$.data.removedSideMenuId").value(egg.getId().toString()))
                .andExpect(jsonPath("$.data.analysis.reliefRate").value(0.07))
                .andExpect(jsonPath("$.data.coachingPlanVersion").value(3));

        org.assertj.core.api.Assertions.assertThat(analysisRepository.count()).isEqualTo(2);
    }

    @Test
    void returnsNotInMealWhenRemovingSideMenuTwice() throws Exception {
        Meal meal = meal(member.getId(), food("remove-twice-rice", "밥", 70, 60, 1, 2));
        SideMenu egg = side("remove-twice-egg", "계란", NutrientFocus.PROTEIN, 1, 0, 6, 800);
        addSideMenu(meal.getId(), egg.getId());
        mockMvc.perform(delete("/api/v1/meals/{mealId}/side-menus/{sideMenuId}", meal.getId(), egg.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/meals/{mealId}/side-menus/{sideMenuId}", meal.getId(), egg.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SIDE_MENU_NOT_IN_MEAL"));
    }

    @Test
    void cannotRemoveSideMenuFromAnotherMembersMeal() throws Exception {
        Member other = memberRepository.save(Member.create("remove-other@example.com", "password", "다른 사용자",
                LocalDate.of(2000, 1, 1), "remove-other-phone", "remove-other-phone-hash", false));
        Meal meal = meal(other.getId(), food("remove-other-rice", "밥", 70, 60, 1, 2));
        SideMenu egg = side("remove-other-egg", "계란", NutrientFocus.PROTEIN, 1, 0, 6, 800);

        mockMvc.perform(delete("/api/v1/meals/{mealId}/side-menus/{sideMenuId}", meal.getId(), egg.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("MEAL_FORBIDDEN"));
    }

    private void addSideMenu(UUID mealId, UUID sideMenuId) throws Exception {
        mockMvc.perform(post("/api/v1/meals/{mealId}/side-menus", mealId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sideMenuId\":\"%s\",\"servingMultiplier\":1}".formatted(sideMenuId)))
                .andExpect(status().isOk());
    }

    private SideMenu side(String code, String name, NutrientFocus focus, int carb, int fiber,
                          int protein, int price) {
        return sideMenuRepository.save(SideMenu.create(UUID.randomUUID(),
                food(code, name, 20, carb, fiber, protein), focus, price));
    }

    private Food food(String code, String name, int gi, int carb, int fiber, int protein) {
        Food food = Food.create(UUID.randomUUID(), code, name, "원재료", FoodCategory.OTHER, name,
                "1인분", BigDecimal.ONE, ServingUnit.COUNT, BigDecimal.valueOf(gi),
                DataQuality.MEASURED, DataQuality.MEASURED);
        food.updateNutrition(BigDecimal.valueOf(carb), BigDecimal.valueOf(fiber), BigDecimal.valueOf(protein),
                BigDecimal.ZERO, new BigDecimal("100"));
        return foodRepository.save(food);
    }

    private Meal meal(UUID memberId, Food food) {
        Meal meal = Meal.draft(memberId, MealSource.MANUAL, null);
        meal.addItem(MealItem.from(food, BigDecimal.ONE));
        return mealRepository.save(meal);
    }
}
