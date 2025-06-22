package com.example.alarms.repositories;

import com.example.alarms.entities.AlarmEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<AlarmEntity, Long> {

    @Query(value = "SELECT * FROM alarms WHERE rule_id = :ruleId ORDER BY id DESC LIMIT 1", nativeQuery = true)
    Optional<AlarmEntity> findFirstByRuleIdOrderByIdDesc(Long ruleId);

    List<AlarmEntity> findAll();
}
