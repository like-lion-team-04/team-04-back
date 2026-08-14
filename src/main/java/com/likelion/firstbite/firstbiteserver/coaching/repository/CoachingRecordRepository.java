package com.likelion.firstbite.firstbiteserver.coaching.repository;

import com.likelion.firstbite.firstbiteserver.coaching.domain.CoachingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface CoachingRecordRepository extends JpaRepository<CoachingRecord, UUID> {
    Optional<CoachingRecord> findFirstByMemberIdAndIdempotencyKeyAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID memberId, UUID idempotencyKey, Instant cutoff);

    @Query("""
            select record from CoachingRecord record
            where record.memberId = :memberId
              and (:fromInstant is null or record.completedAt >= :fromInstant)
              and (:toExclusive is null or record.completedAt < :toExclusive)
            order by record.completedAt desc, record.id desc
            """)
    Page<CoachingRecord> findHistory(@Param("memberId") UUID memberId,
                                     @Param("fromInstant") Instant fromInstant,
                                     @Param("toExclusive") Instant toExclusive,
                                     Pageable pageable);

    @Query("""
            select record from CoachingRecord record
            where record.memberId = :memberId
              and record.completedAt >= :fromInstant
              and record.completedAt < :toExclusive
            order by record.completedAt asc
            """)
    List<CoachingRecord> findHistoryRange(@Param("memberId") UUID memberId,
                                          @Param("fromInstant") Instant fromInstant,
                                          @Param("toExclusive") Instant toExclusive);
}
