package com.likelion.firstbite.firstbiteserver.sidemenu.dto;

import com.likelion.firstbite.firstbiteserver.sidemenu.domain.SideMenu;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.likelion.firstbite.firstbiteserver.food.dto.PageMeta;

public record SideMenuListResponse(List<Item> items, PageMeta meta) {
    public record Item(UUID sideMenuId, UUID foodId, String name, String category,
                       String nutrientFocus, String description, String imageUrl,
                       String servingDescription, BigDecimal servingAmount, String servingUnit,
                       BigDecimal proteinG, BigDecimal fiberG, BigDecimal carbohydrateG,
                       BigDecimal fatG, BigDecimal sodiumMg, BigDecimal calorieKcal,
                       BigDecimal gi, String giDataQuality, Integer estimatedPrice, boolean active) {
        public static Item from(SideMenu sideMenu) {
            var food = sideMenu.getFood();
            return new Item(sideMenu.getId(), food.getId(), food.getName(), food.getSearchCategory().name(),
                    sideMenu.getNutrientFocus().name(), food.getDescription(), food.getImageUrl(),
                    food.getServingDescription(), food.getServingAmount(), food.getServingUnit().name(),
                    food.getProteinG(), food.getFiberG(), food.getCarbG(), food.getFatG(), food.getSodiumMg(),
                    food.getCalorieKcal(), food.getGi(), food.getGiDataQuality().name(),
                    sideMenu.getEstimatedPrice(), sideMenu.isActive());
        }
    }
}
