package com.example.alarms.repositories;

import com.example.alarms.entities.ActionEntity;
import com.example.alarms.entities.ReservationEntity;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {
    // Add custom queries if needed

    Void deleteByAction(ActionEntity action);

    Optional<ReservationEntity> findByAction(ActionEntity action);

    @Modifying
    @Transactional
    @Query("UPDATE ReservationEntity r SET r.lockedAt = :now WHERE r.id = :jobId AND r.lockedBy = :instanceId")
    int updateHeartbeat(@Param("jobId") Long jobId,
                        @Param("instanceId") String instanceId,
                        @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE ReservationEntity r SET r.status = 'pending', r.lockedBy = NULL WHERE r.id = :jobId AND r.lockedBy = :instanceId")
    int releaseJob(@Param("jobId") Long jobId,
                   @Param("instanceId") String instanceId);

    @Query("""
    SELECT r FROM ReservationEntity r 
    WHERE r.status = 'pending' 
    OR (r.status = 'processing' AND r.lockedAt < :timeoutThreshold)
    ORDER BY r.createdDate ASC
""")
    List<ReservationEntity> findAvailableJobs(LocalDateTime timeoutThreshold, int batchSize);


    @Modifying
    @Transactional
    @Query("""
        UPDATE ReservationEntity r SET 
            r.status = 'processing',
            r.lockedBy = :instanceId,
            r.lockedAt = :now
        WHERE r.id = :jobId AND (
            r.status = 'pending' OR
            (r.status = 'processing' AND r.lockedAt < :timeoutThreshold)
        )
    """)
    int tryAcquireJob(@Param("jobId") Long jobId,
                      @Param("instanceId") String instanceId,
                      @Param("now") LocalDateTime now,
                      @Param("timeoutThreshold") LocalDateTime timeoutThreshold);

}