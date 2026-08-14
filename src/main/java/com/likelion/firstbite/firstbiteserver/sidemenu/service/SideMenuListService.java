package com.likelion.firstbite.firstbiteserver.sidemenu.service;

import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.sidemenu.domain.NutrientFocus;
import com.likelion.firstbite.firstbiteserver.sidemenu.dto.SideMenuListResponse;
import com.likelion.firstbite.firstbiteserver.sidemenu.repository.SideMenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SideMenuListService {
    private static final int MAX_ITEMS = 100;
    private final SideMenuRepository repository;

    @Transactional(readOnly = true)
    public SideMenuListResponse get(String nutrientFocus, boolean activeOnly) {
        NutrientFocus focus = parseFocus(nutrientFocus);
        var items = repository.findForList(focus, activeOnly, PageRequest.of(0, MAX_ITEMS)).stream()
                .map(SideMenuListResponse.Item::from).toList();
        return new SideMenuListResponse(items);
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
}
