package com.example.alarms.repositories;

import com.example.alarms.dto.ActionWithRules;
import com.example.alarms.dto.ActionWithRulesProjection;
import com.example.alarms.entities.ActionEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface ActionRepository extends ReactiveCrudRepository<ActionEntity, Long> {

//    @Query("""
//    SELECT
//        a.id,
//        a.type,
//        a.params
//    FROM actions a
//    WHERE a.id BETWEEN :startId AND :endId
//    """)
//    Flux<ActionEntity> findActionsInRange(@Param("startId") Long startId, @Param("endId") Long endId);

    @Query("""
    SELECT 
        a.id, 
        a.type, 
        a.params 
    FROM actions a
    ORDER BY a.id ASC
    LIMIT :limit OFFSET :offset
    """)
    Flux<ActionEntity> findActionsInRange(@Param("limit") int limit, @Param("offset") int offset);

    @Query("""
    SELECT
        a.id AS action_id,
        a.type AS action_type,
        a.params AS action_params,
        r.id AS rule_id,
        r.name AS rule_name,
        r.rule AS rule 
    FROM actions a 
    LEFT JOIN rules r ON a.id = r.action_id
    WHERE a.id BETWEEN :startId AND :endId
    """)
    Flux<ActionWithRules> findActionWithRulesById(@Param("startId") Long startId, @Param("endId") Long endId);
}
