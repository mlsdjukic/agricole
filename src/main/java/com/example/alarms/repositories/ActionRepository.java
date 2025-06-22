package com.example.alarms.repositories;

import com.example.alarms.entities.ActionEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ActionRepository extends JpaRepository<ActionEntity, Long> {
    // Add custom queries if needed
    @Query("SELECT DISTINCT a FROM ActionEntity a " +
            "LEFT JOIN FETCH a.rules r " +
            "LEFT JOIN FETCH r.reactions " +
            "WHERE a.id = :id")
    Optional<ActionEntity> findByIdWithRulesAndReactions(@Param("id") Long id);
}
