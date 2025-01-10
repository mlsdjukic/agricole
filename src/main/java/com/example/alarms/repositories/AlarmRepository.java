package com.example.alarms.repositories;

import com.example.alarms.entities.AlarmEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface AlarmRepository extends ReactiveCrudRepository<AlarmEntity, Long> {

    Mono<AlarmEntity> findFirstByRuleIdOrderByIdDesc();

    Flux<AlarmEntity> findAllBy();
}
