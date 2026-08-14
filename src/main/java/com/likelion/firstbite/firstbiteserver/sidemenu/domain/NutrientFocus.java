package com.likelion.firstbite.firstbiteserver.sidemenu.domain;

import java.math.BigDecimal;

public enum NutrientFocus {
    PROTEIN("0.0400"), FIBER("0.0300");

    private final BigDecimal reliefDelta;

    NutrientFocus(String reliefDelta) {
        this.reliefDelta = new BigDecimal(reliefDelta);
    }

    public BigDecimal reliefDelta() {
        return reliefDelta;
    }
}
