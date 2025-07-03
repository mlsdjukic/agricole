package com.example.alarms.repositories;

import com.example.alarms.entities.ActionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ActionRepository extends ReactiveCrudRepository<ActionEntity, Long> {

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

    Flux<ActionEntity> findAllBy(Pageable pageable);

}
