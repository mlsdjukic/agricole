package com.example.alarms.repositories;

import com.example.alarms.entities.RuleEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<RuleEntity, Long> {
    // Add custom queries if needed
}
