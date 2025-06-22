package com.example.alarms.services;

import com.example.alarms.dto.AlarmRequest;
import com.example.alarms.dto.AlarmResponse;
import com.example.alarms.entities.AlarmEntity;
import com.example.alarms.repositories.AlarmRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AlarmService {

    private final AlarmRepository alarmRepository;

    public AlarmService(AlarmRepository alarmRepository) {
        this.alarmRepository = alarmRepository;
    }

    // Save or update an alarm
    public AlarmResponse save(AlarmRequest alarmRequest) {
        AlarmEntity alarmEntity = toEntity(alarmRequest);
        AlarmEntity saved = alarmRepository.save(alarmEntity);
        return toResponseDto(saved);
    }

    // Get all alarms
    public List<AlarmResponse> getAll() {
        return alarmRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    // Get alarm by ID
    public Optional<AlarmResponse> getById(Long id) {
        return alarmRepository.findById(id)
                .map(this::toResponseDto);
    }

    // Delete alarm by ID
    public void deleteById(Long id) {
        alarmRepository.deleteById(id);
    }

    // Get the last record by ruleId
    public Optional<AlarmResponse> getLastRecordByRuleId(Long ruleId) {
        return alarmRepository.findFirstByRuleIdOrderByIdDesc(ruleId)
                .map(this::toResponseDto);
    }

    // Get all alarms with pagination
    public List<AlarmResponse> getAllWithPagination(int page, int size) {
        return alarmRepository.findAll(PageRequest.of(page, size)).stream()
                .map(this::toResponseDto)
                .toList();
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
