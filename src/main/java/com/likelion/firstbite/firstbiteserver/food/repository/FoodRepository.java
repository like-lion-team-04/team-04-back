package com.likelion.firstbite.firstbiteserver.food.repository;

import com.likelion.firstbite.firstbiteserver.food.domain.Food;
import com.likelion.firstbite.firstbiteserver.food.domain.FoodCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface FoodRepository extends JpaRepository<Food, UUID> {
    @Query("""
            select food from Food food
            where food.active = true
              and (:category is null or food.searchCategory = :category)
              and (:query = '' or lower(food.name) like lower(concat('%', :query, '%'))
                   or food.initials like concat('%', :query, '%'))
            order by food.name asc
            """)
    Page<Food> search(@Param("query") String query,
                      @Param("category") FoodCategory category,
                      Pageable pageable);
}
