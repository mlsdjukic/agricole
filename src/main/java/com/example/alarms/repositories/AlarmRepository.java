package com.example.alarms.repositories;

import com.example.alarms.entities.AlarmEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface AlarmRepository extends ReactiveCrudRepository<AlarmEntity, Long> {


    @Query("SELECT * FROM alarms WHERE rule_id = :ruleId ORDER BY id DESC LIMIT 1")
    Mono<AlarmEntity> findFirstByRuleIdOrderByIdDesc(Long ruleId);

    Flux<AlarmEntity> findAllBy();
}
