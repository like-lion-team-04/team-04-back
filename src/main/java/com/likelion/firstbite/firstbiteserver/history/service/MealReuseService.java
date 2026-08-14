package com.likelion.firstbite.firstbiteserver.history.service;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingRecord;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingRecordRepository;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.history.domain.MealReuse;
import com.likelion.firstbite.firstbiteserver.history.dto.ReuseMealRequest;
import com.likelion.firstbite.firstbiteserver.history.dto.ReuseMealResponse;
import com.likelion.firstbite.firstbiteserver.history.repository.MealReuseRepository;
import com.likelion.firstbite.firstbiteserver.meal.domain.Meal;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealItem;
import com.likelion.firstbite.firstbiteserver.meal.domain.MealSource;
import com.likelion.firstbite.firstbiteserver.meal.repository.MealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MealReuseService {
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private final CoachingRecordRepository recordRepository;
    private final MealRepository mealRepository;
    private final MealReuseRepository reuseRepository;

    @Transactional
    public ReuseMealResponse reuse(UUID memberId, UUID recordId, UUID key, ReuseMealRequest request) {
        if (key == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED",
                "Idempotency-Key가 필요합니다.");
        boolean includeSideMenus = request == null || request.shouldIncludeSideMenus();
        String hash = hash(recordId + ":" + includeSideMenus);
        Instant now = Instant.now();
        var existing = reuseRepository.findFirstByMemberIdAndIdempotencyKeyAndCreatedAtAfterOrderByCreatedAtDesc(
                memberId, key, now.minus(IDEMPOTENCY_TTL));
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(hash)) throw new BusinessException(HttpStatus.CONFLICT,
                    "IDEMPOTENCY_KEY_CONFLICT", "동일한 키를 다른 요청에 사용할 수 없습니다.");
            return response(existing.get());
        }
        CoachingRecord record = recordRepository.findById(recordId)
                .filter(r -> r.getMemberId().equals(memberId))
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "COACHING_RECORD_NOT_FOUND", "코칭 기록을 찾을 수 없습니다."));
        Meal source = mealRepository.findAllByIdIn(List.of(record.getMealId())).stream().findFirst()
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "MEAL_NOT_FOUND", "식사를 찾을 수 없습니다."));
        Meal target = Meal.draft(memberId, MealSource.REUSE, null);
        int count = 0;
        for (var item : source.getItems()) {
            if (!includeSideMenus && item.getSideMenu() != null) continue;
            if (!item.getFood().isActive()) throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "REUSE_FOOD_UNAVAILABLE", "현재 사용할 수 없는 음식이 포함되어 있습니다.");
            target.addItem(item.getSideMenu() == null
                    ? MealItem.from(item.getFood(), item.getServingMultiplier())
                    : MealItem.fromSideMenu(item.getSideMenu(), item.getServingMultiplier()));
            count++;
        }
        mealRepository.save(target);
        MealReuse reuse = reuseRepository.save(MealReuse.create(memberId, recordId, target.getId(), key, hash, count, now));
        return response(reuse);
    }

    private ReuseMealResponse response(MealReuse reuse) {
        return new ReuseMealResponse(reuse.getNewMealId(), "DRAFT", "REUSE", reuse.getCopiedItemCount(), true);
    }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
}
