package com.likelion.firstbite.firstbiteserver.food;

import com.likelion.firstbite.firstbiteserver.food.repository.FoodRepository;
import com.likelion.firstbite.firstbiteserver.food.service.FoodService;
import com.likelion.firstbite.firstbiteserver.sidemenu.domain.NutrientFocus;
import com.likelion.firstbite.firstbiteserver.sidemenu.repository.SideMenuRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class FoodSeedMigrationTest {
    @Autowired FoodRepository foodRepository;
    @Autowired FoodService foodService;
    @Autowired SideMenuRepository sideMenuRepository;

    @Test
    @Transactional(readOnly = true)
    void seedsFoodsAndMakesThemSearchable() {
        assertThat(foodRepository.count()).isEqualTo(58);

        FoodService.SearchResult result = foodService.search("백미밥", null, 1, 20);
        assertThat(result.data().items())
                .singleElement()
                .satisfies(food -> assertThat(food.name()).isEqualTo("백미밥"));

        assertThat(sideMenuRepository.findAll())
                .hasSize(15)
                .allSatisfy(sideMenu -> assertThat(sideMenu.getEstimatedPrice()).isPositive());
        assertThat(sideMenuRepository.findAll())
                .filteredOn(sideMenu -> sideMenu.getFood().getFoodCode().equals("almonds"))
                .singleElement()
                .satisfies(sideMenu -> assertThat(sideMenu.getNutrientFocus()).isEqualTo(NutrientFocus.FIBER));
    }
}
