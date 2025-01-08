package com.example.alarms.repositories;

import com.example.alarms.entities.RuleEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface RuleRepository extends ReactiveCrudRepository<RuleEntity, Long> {

    Flux<RuleEntity> findByActionId(Long actionId);
    Mono<Void> deleteByActionId(Long actionId);
}
