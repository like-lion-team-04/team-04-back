package com.likelion.firstbite.firstbiteserver.food.service;

import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.food.domain.Food;
import com.likelion.firstbite.firstbiteserver.food.domain.FoodCategory;
import com.likelion.firstbite.firstbiteserver.food.dto.FoodSearchResponse;
import com.likelion.firstbite.firstbiteserver.food.dto.PageMeta;
import com.likelion.firstbite.firstbiteserver.food.repository.FoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FoodService {
    private final FoodRepository foodRepository;

    @Transactional(readOnly = true)
    public SearchResult search(String rawQuery, FoodCategory category, int page, int size) {
        String query = rawQuery == null ? "" : rawQuery.trim();
        if (query.length() > 50 || page < 1 || size < 1 || size > 50) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "FOOD_SEARCH_INVALID", "검색 조건이 올바르지 않습니다.");
        }
        Page<Food> result = foodRepository.search(query, category, PageRequest.of(page - 1, size));
        return new SearchResult(
                new FoodSearchResponse(result.getContent().stream().map(FoodSearchResponse.Item::from).toList()),
                new PageMeta(page, size, result.getTotalElements(), result.getTotalPages()));
    }

    public record SearchResult(FoodSearchResponse data, PageMeta meta) {}
}
