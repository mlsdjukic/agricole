package com.example.alarms.services;

import com.example.alarms.dto.AlarmRequest;
import com.example.alarms.dto.AlarmResponse;
import com.example.alarms.entities.AlarmEntity;
import com.example.alarms.repositories.AlarmRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AlarmService {

    private final AlarmRepository alarmRepository;

    public AlarmService(AlarmRepository alarmRepository) {
        this.alarmRepository = alarmRepository;
    }

    // Save or update an alarm
    public Mono<AlarmResponse> save(AlarmRequest AlarmRequest) {
        AlarmEntity alarmEntity = toEntity(AlarmRequest);
        return alarmRepository.save(alarmEntity)
                .map(this::toResponseDto);
    }

    public Flux<AlarmResponse> getAll() {
        return alarmRepository.findAll()
                .map(this::toResponseDto);
    }

    // Get alarm by ID
    public Mono<AlarmResponse> getById(Long id) {
        return alarmRepository.findById(id)
                .map(this::toResponseDto);
    }

    // Delete alarm by ID
    public Mono<Void> deleteById(Long id) {
        return alarmRepository.deleteById(id);
    }

    // Get the last record by ruleId
    public Mono<AlarmResponse> getLastRecordByRuleId(Long ruleId) {
        return alarmRepository.findFirstByRuleIdOrderByIdDesc(ruleId)
                .map(this::toResponseDto);
    }

    // Get all alarms with pagination
    public Flux<AlarmResponse> getAllWithPagination(int page, int size) {
        return alarmRepository.findAllBy()
                .skip((long) page * size)
                .take(size)
                .map(this::toResponseDto);
    }

    // Mapping methods
    private AlarmEntity toEntity(AlarmRequest dto) {
        AlarmEntity entity = new AlarmEntity();
        entity.setRuleId(dto.getRuleId());
        entity.setMessage(dto.getMessage());
        return entity;
    }

    private AlarmResponse toResponseDto(AlarmEntity entity) {
        AlarmResponse dto = new AlarmResponse();
        dto.setId(entity.getId());
        dto.setRuleId(entity.getRuleId());
        dto.setMessage(entity.getMessage());
        return dto;
    }
}

