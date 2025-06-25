package com.example.alarms.repositories;

import com.example.alarms.entities.AlarmClassEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlarmClassRepository extends ReactiveCrudRepository<AlarmClassEntity, Long> {
}
