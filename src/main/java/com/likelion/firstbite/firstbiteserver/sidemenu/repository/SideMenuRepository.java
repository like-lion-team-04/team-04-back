package com.likelion.firstbite.firstbiteserver.sidemenu.repository;

import com.likelion.firstbite.firstbiteserver.sidemenu.domain.SideMenu;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import com.likelion.firstbite.firstbiteserver.sidemenu.domain.NutrientFocus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SideMenuRepository extends JpaRepository<SideMenu, UUID> {
    @EntityGraph(attributePaths = "food")
    List<SideMenu> findAllByActiveTrue();

    @EntityGraph(attributePaths = "food")
    Optional<SideMenu> findByIdAndActiveTrue(UUID id);

    @Query("""
            select sideMenu from SideMenu sideMenu
            join fetch sideMenu.food food
            where (:nutrientFocus is null or sideMenu.nutrientFocus = :nutrientFocus)
              and (:activeOnly = false or sideMenu.active = true)
            order by food.name asc, sideMenu.id asc
            """)
    List<SideMenu> findForList(@Param("nutrientFocus") NutrientFocus nutrientFocus,
                               @Param("activeOnly") boolean activeOnly,
                               Pageable pageable);
}
