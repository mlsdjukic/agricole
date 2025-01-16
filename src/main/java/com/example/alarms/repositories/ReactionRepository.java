package com.example.alarms.repositories;

import com.example.alarms.entities.ReactionEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReactionRepository extends ReactiveCrudRepository<ReactionEntity, Long> {
}
