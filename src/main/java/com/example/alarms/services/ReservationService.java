package com.example.alarms.services;

import com.example.alarms.dto.ReservationMapper;
import com.example.alarms.entities.ActionEntity;
import com.example.alarms.entities.ReservationEntity;
import com.example.alarms.repositories.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public void updateHeartbeat(Long jobId, String instanceId, LocalDateTime now) {
        reservationRepository.updateHeartbeat(jobId, instanceId, now);
    }
    public void releaseJob(Long jobId, String instanceId) {
        reservationRepository.releaseJob(jobId, instanceId);
    }
    public List<ReservationEntity> findAvailableJobs(LocalDateTime jobTimeout, int batchSize) {
        return reservationRepository.findAvailableJobs(jobTimeout, batchSize);
    }

    public ReservationEntity save(ReservationEntity job) {
        return reservationRepository.save(job);
    }

    public void deleteByActionId(ActionEntity action) {
        Optional<ReservationEntity> reservationEntity = reservationRepository.findByAction(action);
        reservationEntity.ifPresent(reservationRepository::delete);
    }

    public int tryAcquireJob(Long id, String instanceId, LocalDateTime now, LocalDateTime timeoutThreshold) {
        return reservationRepository.tryAcquireJob(id, instanceId, now, timeoutThreshold);
    }

}
