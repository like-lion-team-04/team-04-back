package com.likelion.firstbite.firstbiteserver.history.dto;

public record ReuseMealRequest(Boolean includeSideMenus) {
    public boolean shouldIncludeSideMenus() { return includeSideMenus == null || includeSideMenus; }
}
