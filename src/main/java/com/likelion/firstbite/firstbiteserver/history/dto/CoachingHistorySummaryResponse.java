package com.likelion.firstbite.firstbiteserver.history.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CoachingHistorySummaryResponse(
        Period period, long coachingCount, long completedCoachingCount,
        long userEndedCoachingCount, long skippedStageCount, BigDecimal completionRate,
        BigDecimal orderAdherenceRate, BigDecimal averageSleepinessScore, List<Daily> daily) {
    public record Period(LocalDate from, LocalDate to) {}
    public record Daily(LocalDate date, boolean completed, Integer sleepinessScore) {}
}
