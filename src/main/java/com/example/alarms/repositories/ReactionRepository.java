package com.example.alarms.repositories;

import com.example.alarms.entities.ReactionEntity;
import org.reactivestreams.Publisher;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ReactionRepository extends ReactiveCrudRepository<ReactionEntity, Long> {
    Flux<ReactionEntity> findByRuleId(Long ruleId);

    Mono<Void> deleteByRuleId(Long id);

}
