package com.likelion.firstbite.firstbiteserver.food;

import com.likelion.firstbite.firstbiteserver.auth.token.JwtTokenService;
import com.likelion.firstbite.firstbiteserver.food.domain.DataQuality;
import com.likelion.firstbite.firstbiteserver.food.domain.Food;
import com.likelion.firstbite.firstbiteserver.food.domain.FoodCategory;
import com.likelion.firstbite.firstbiteserver.food.domain.ServingUnit;
import com.likelion.firstbite.firstbiteserver.food.repository.FoodRepository;
import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FoodControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired FoodRepository foodRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired JwtTokenService jwtTokenService;

    private String accessToken;

    @BeforeEach
    void setUp() {
        foodRepository.deleteAll();
        memberRepository.deleteAll();
        Member member = memberRepository.save(Member.create(
                "food-user@example.com", "password-hash", "테스터", LocalDate.of(2000, 1, 1),
                "encrypted-phone", "food-test-phone-hash", false));
        accessToken = jwtTokenService.issue(member.getId());

        foodRepository.save(food("떡볶이", "tteokbokki", "ㄸㅂㅇ", "분식", FoodCategory.FLOUR, "95"));
        foodRepository.save(food("백미밥", "white_rice", "ㅂㅁㅂ", "밥류", FoodCategory.RICE, "70"));
    }

    @Test
    void searchesFoodsByInitialsAndReturnsOneBasedPageMeta() throws Exception {
        mockMvc.perform(get("/api/v1/foods")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("query", "ㄸㅂㅇ")
                        .param("category", "FLOUR")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].name").value("떡볶이"))
                .andExpect(jsonPath("$.data.items[0].category").value("FLOUR"))
                .andExpect(jsonPath("$.data.items[0].dataQuality").value("ESTIMATED"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.totalElements").value(1));
    }

    @Test
    void rejectsInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/foods")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("FOOD_SEARCH_INVALID"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/foods"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));
    }

    private Food food(String name, String code, String initials, String originalCategory,
                      FoodCategory category, String gi) {
        return Food.create(UUID.randomUUID(), code, name, originalCategory, category, initials,
                "1인분", BigDecimal.ONE, ServingUnit.COUNT, new BigDecimal(gi),
                DataQuality.UNKNOWN, DataQuality.ESTIMATED);
    }
}
