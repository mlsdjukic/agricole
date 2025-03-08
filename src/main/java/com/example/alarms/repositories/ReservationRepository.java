package com.example.alarms.repositories;

import com.example.alarms.entities.ReservationEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface ReservationRepository extends ReactiveCrudRepository<ReservationEntity, Long> {
    @Modifying
    @Query("UPDATE reservations SET locked_by = :lockedBy, locked_at = :lockedAt WHERE id = :id AND (locked_by IS NULL OR locked_at < :timeout)")
    Mono<Boolean> tryLock(Long id, String lockedBy, LocalDateTime lockedAt, LocalDateTime timeout);

    Mono<Void> deleteByActionId(Long actionId);


    @Query("""
        SELECT TOP(:batchSize) * FROM reservations 
        WHERE status = 'pending' 
        OR (status = 'processing' AND locked_at < :timeoutThreshold)
    """)
    Flux<ReservationEntity> findAvailableJobs(LocalDateTime timeoutThreshold, int batchSize);

    @Modifying
    @Query("""
        UPDATE reservations 
        SET status = 'processing', 
            locked_by = :instanceId, 
            locked_at = :now 
        WHERE id = :jobId 
        AND (status = 'pending' 
             OR (status = 'processing' 
                 AND locked_at < :timeoutThreshold))
    """)
    Mono<Boolean> tryAcquireJob(Long jobId, String instanceId,
                                LocalDateTime now, LocalDateTime timeoutThreshold);

    @Modifying
    @Query("UPDATE reservations SET locked_at = :now WHERE id = :jobId AND locked_by = :instanceId")
    Mono<Void> updateHeartbeat(Long jobId, String instanceId, LocalDateTime now);

    @Modifying
    @Query("""
        UPDATE reservations 
        SET status = 'pending', 
            locked_by = NULL 
        WHERE id = :jobId 
        AND locked_by = :instanceId
    """)
    Mono<Void> releaseJob(Long jobId, String instanceId);
}