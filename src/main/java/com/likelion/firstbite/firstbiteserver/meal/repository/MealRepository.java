package com.likelion.firstbite.firstbiteserver.meal.repository;

import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface MealRepository extends JpaRepository<Meal, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select meal from Meal meal where meal.id = :id")
    Optional<Meal> findByIdForUpdate(@Param("id") UUID id);
}
