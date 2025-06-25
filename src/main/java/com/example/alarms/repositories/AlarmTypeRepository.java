package com.example.alarms.repositories;

import com.example.alarms.entities.AlarmTypeEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface AlarmTypeRepository extends ReactiveCrudRepository<AlarmTypeEntity, Long> {
    Mono<AlarmTypeEntity> findByName(String name);
}

