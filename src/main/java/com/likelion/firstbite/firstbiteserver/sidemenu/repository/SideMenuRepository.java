package com.likelion.firstbite.firstbiteserver.sidemenu.repository;

import com.likelion.firstbite.firstbiteserver.sidemenu.domain.SideMenu;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.likelion.firstbite.firstbiteserver.sidemenu.domain.NutrientFocus;
import com.likelion.firstbite.firstbiteserver.food.domain.FoodCategory;

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
            join sideMenu.food food
            where (:nutrientFocus is null or sideMenu.nutrientFocus = :nutrientFocus)
              and (:category is null or food.searchCategory = :category)
              and (:query is null or lower(food.name) like lower(concat('%', :query, '%'))
                   or lower(food.initials) like lower(concat('%', :query, '%')))
              and (:activeOnly = false or sideMenu.active = true)
            order by food.name asc, sideMenu.id asc
            """)
    @EntityGraph(attributePaths = "food")
    Page<SideMenu> findForList(@Param("query") String query,
                               @Param("category") FoodCategory category,
                               @Param("nutrientFocus") NutrientFocus nutrientFocus,
                               @Param("activeOnly") boolean activeOnly, Pageable pageable);
}
