package com.likelion.firstbite.firstbiteserver.sidemenu.domain;

import com.likelion.firstbite.firstbiteserver.food.domain.Food;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "side_menus", indexes = @Index(name = "idx_side_menus_focus_active", columnList = "nutrient_focus,is_active"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SideMenu {
    @Id private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "food_id", nullable = false, unique = true)
    private Food food;
    @Enumerated(EnumType.STRING) @Column(name = "nutrient_focus", nullable = false, length = 20)
    private NutrientFocus nutrientFocus;
    @Column(name = "estimated_price", nullable = false) private int estimatedPrice;
    @Column(name = "is_active", nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public static SideMenu create(UUID id, Food food, NutrientFocus nutrientFocus, int estimatedPrice) {
        SideMenu sideMenu = new SideMenu();
        sideMenu.id = id;
        sideMenu.food = food;
        sideMenu.nutrientFocus = nutrientFocus;
        sideMenu.estimatedPrice = estimatedPrice;
        sideMenu.active = true;
        sideMenu.createdAt = Instant.now();
        sideMenu.updatedAt = sideMenu.createdAt;
        return sideMenu;
    }
}
