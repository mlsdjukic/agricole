package com.example.alarms.repositories;

import com.example.alarms.entities.ReactionEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReactionRepository extends JpaRepository<ReactionEntity, Long> {
    // Add custom queries if needed
}
