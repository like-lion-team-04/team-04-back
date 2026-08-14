package com.likelion.firstbite.firstbiteserver.history.service;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CompletionReason;
import com.likelion.firstbite.firstbiteserver.coaching.repository.CoachingRecordRepository;
import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import com.likelion.firstbite.firstbiteserver.history.dto.CoachingHistorySummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoachingHistorySummaryService {
    private final CoachingRecordRepository recordRepository;

    @Transactional(readOnly = true)
    public CoachingHistorySummaryResponse summarize(UUID memberId, LocalDate from, LocalDate to, String timezone) {
        ZoneId zone = resolveZone(timezone);
        if (from == null || to == null || from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > 30) {
            throw invalidPeriod();
        }
        var records = recordRepository.findHistoryRange(memberId, from.atStartOfDay(zone).toInstant(),
                to.plusDays(1).atStartOfDay(zone).toInstant());
        long count = records.size();
        long completed = records.stream().filter(r -> r.getReason() == CompletionReason.COMPLETED).count();
        long totalStages = records.stream().mapToLong(r -> r.getTotalStages()).sum();
        long completedStages = records.stream().mapToLong(r -> r.getCompletedStages()).sum();
        BigDecimal completionRate = rate(completed, count);
        BigDecimal adherenceRate = rate(completedStages, totalStages);

        var dailyMap = new LinkedHashMap<LocalDate, Boolean>();
        from.datesUntil(to.plusDays(1)).forEach(date -> dailyMap.put(date, false));
        records.forEach(record -> {
            LocalDate date = record.getCompletedAt().atZone(zone).toLocalDate();
            if (record.getReason() == CompletionReason.COMPLETED) dailyMap.put(date, true);
        });
        var daily = dailyMap.entrySet().stream()
                .map(e -> new CoachingHistorySummaryResponse.Daily(e.getKey(), e.getValue(), null)).toList();
        return new CoachingHistorySummaryResponse(new CoachingHistorySummaryResponse.Period(from, to), count,
                completionRate, adherenceRate, null, daily);
    }

    private ZoneId resolveZone(String timezone) {
        try { return timezone == null || timezone.isBlank() ? CoachingHistoryService.SERVICE_ZONE : ZoneId.of(timezone); }
        catch (DateTimeException exception) { throw invalidPeriod(); }
    }

    private BigDecimal rate(long numerator, long denominator) {
        return denominator == 0 ? BigDecimal.ZERO.setScale(2) : BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BusinessException invalidPeriod() {
        return new BusinessException(HttpStatus.BAD_REQUEST, "HISTORY_PERIOD_INVALID",
                "조회 기간은 필수이며 최대 31일이어야 합니다.");
    }
}
