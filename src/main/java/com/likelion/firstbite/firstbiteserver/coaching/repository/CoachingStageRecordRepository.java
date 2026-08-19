package com.likelion.firstbite.firstbiteserver.coaching.repository;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingStageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;
import java.util.List;

public interface CoachingStageRecordRepository extends JpaRepository<CoachingStageRecord, UUID> {
    long countBySessionId(UUID sessionId);
    List<CoachingStageRecord> findAllBySessionIdOrderByStageAsc(UUID sessionId);
    List<CoachingStageRecord> findAllBySessionIdInOrderBySessionIdAscStageAsc(Collection<UUID> sessionIds);
}
