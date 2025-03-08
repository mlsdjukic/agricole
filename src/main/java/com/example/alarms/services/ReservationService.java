package com.example.alarms.services;

import com.example.alarms.entities.ReservationEntity;
import com.example.alarms.repositories.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ActionService actionService;

    public Mono<Void> updateHeartbeat(Long jobId, String instanceId, LocalDateTime now) {
        return reservationRepository.updateHeartbeat(jobId, instanceId, now);
    }
    public Mono<Void> releaseJob(Long jobId, String instanceId) {
        return reservationRepository.releaseJob(jobId, instanceId);
    }
    public Flux<ReservationEntity> findAvailableJobs(LocalDateTime jobTimeout, int batchSize) {
        return reservationRepository.findAvailableJobs(jobTimeout, batchSize);
    }

    public Flux<ReservationEntity> findAll() {
        return reservationRepository.findAll();
    }

    public Mono<ReservationEntity> findById(Long id) {
        return reservationRepository.findById(id);
    }

    public Mono<ReservationEntity> save(ReservationEntity job) {
        return reservationRepository.save(job);
    }

    public Mono<Void> deleteById(Long id) {
        return reservationRepository.deleteById(id);
    }

    @Transactional
    public Mono<ReservationEntity> update(Long id, ReservationEntity job) {
        return reservationRepository.findById(id)
                .flatMap(existingJob -> {
                    existingJob.setStatus(job.getStatus());
                    existingJob.setActionId(job.getActionId());
                    existingJob.setLockedBy(job.getLockedBy());
                    existingJob.setLocked_at(job.getLocked_at());
                    return reservationRepository.save(existingJob);
                });
    }

    public Mono<Void> deleteByActionId(Long actionId) {
        return reservationRepository.deleteByActionId(actionId);
    }

    public Mono<Boolean> tryAcquireJob(Long id, String instanceId, LocalDateTime now, LocalDateTime timeoutThreshold) {
        return reservationRepository.tryAcquireJob(id, instanceId, now, timeoutThreshold);
    }

}
