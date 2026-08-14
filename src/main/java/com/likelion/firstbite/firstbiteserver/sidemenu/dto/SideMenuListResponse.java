package com.likelion.firstbite.firstbiteserver.sidemenu.dto;

import com.likelion.firstbite.firstbiteserver.sidemenu.domain.SideMenu;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record SideMenuListResponse(List<Item> items) {
    public record Item(UUID sideMenuId, String name, String nutrientFocus,
                       BigDecimal proteinG, BigDecimal fiberG, BigDecimal carbohydrateG,
                       BigDecimal fatG, Integer estimatedPrice, boolean active) {
        public static Item from(SideMenu sideMenu) {
            var food = sideMenu.getFood();
            return new Item(sideMenu.getId(), food.getName(), sideMenu.getNutrientFocus().name(),
                    food.getProteinG(), food.getFiberG(), food.getCarbG(), food.getFatG(),
                    sideMenu.getEstimatedPrice(), sideMenu.isActive());
        }
    }
}
