package com.likelion.firstbite.firstbiteserver.sidemenu.service;

import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.sidemenu.domain.NutrientFocus;
import com.likelion.firstbite.firstbiteserver.sidemenu.dto.SideMenuListResponse;
import com.likelion.firstbite.firstbiteserver.sidemenu.repository.SideMenuRepository;
import com.likelion.firstbite.firstbiteserver.food.domain.FoodCategory;
import com.likelion.firstbite.firstbiteserver.food.dto.PageMeta;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SideMenuListService {
    private static final int MAX_PAGE_SIZE = 50;
    private final SideMenuRepository repository;

    @Transactional(readOnly = true)
    public SideMenuListResponse get(String query, String category, String nutrientFocus,
                                    boolean activeOnly, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SIDE_MENU_PAGE_INVALID",
                    "page는 0 이상, size는 1부터 50까지 가능합니다.");
        }
        if (query != null && query.trim().length() > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SIDE_MENU_QUERY_INVALID",
                    "검색어는 100자 이하여야 합니다.");
        }
        NutrientFocus focus = parseFocus(nutrientFocus);
        FoodCategory parsedCategory = parseCategory(category);
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        var result = repository.findForList(normalizedQuery, parsedCategory, focus, activeOnly,
                PageRequest.of(page, size));
        var items = result.stream()
                .map(SideMenuListResponse.Item::from).toList();
        return new SideMenuListResponse(items,
                new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    private NutrientFocus parseFocus(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return NutrientFocus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SIDE_MENU_FILTER_INVALID",
                    "nutrientFocus는 PROTEIN 또는 FIBER여야 합니다.");
        }
    }

    private FoodCategory parseCategory(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return FoodCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SIDE_MENU_CATEGORY_INVALID",
                    "지원하지 않는 음식 카테고리입니다.");
        }
    }
}
