package com.likelion.firstbite.firstbiteserver.meal.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record UpdateMealItemsRequest(List<Item> items) {
    public record Item(UUID foodId, BigDecimal servingMultiplier) {}
}
