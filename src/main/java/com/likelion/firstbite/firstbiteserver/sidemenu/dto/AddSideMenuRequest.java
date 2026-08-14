package com.likelion.firstbite.firstbiteserver.sidemenu.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AddSideMenuRequest(UUID sideMenuId, BigDecimal servingMultiplier) {
    public BigDecimal effectiveServingMultiplier() {
        return servingMultiplier == null ? BigDecimal.ONE : servingMultiplier;
    }
}
