package com.example.alarms.services;

import com.example.alarms.dto.AlarmRequestDTO;
import com.example.alarms.dto.AlarmResponseDTO;
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
    public Mono<AlarmResponseDTO> save(AlarmRequestDTO AlarmRequestDTO) {
        AlarmEntity alarmEntity = toEntity(AlarmRequestDTO);
        return alarmRepository.save(alarmEntity)
                .map(this::toResponseDto);
    }

    // Get alarm by ID
    public Mono<AlarmResponseDTO> getById(Long id) {
        return alarmRepository.findById(id)
                .map(this::toResponseDto);
    }

    // Delete alarm by ID
    public Mono<Void> deleteById(Long id) {
        return alarmRepository.deleteById(id);
    }

    // Get the last record by ruleId
    public Mono<AlarmResponseDTO> getLastRecordByRuleId(Long ruleId) {
        return alarmRepository.findFirstByRuleIdOrderByIdDesc()
                .map(this::toResponseDto);
    }

    // Get all alarms with pagination
    public Flux<AlarmResponseDTO> getAllWithPagination(int page, int size) {
        return alarmRepository.findAllBy()
                .skip((long) page * size)
                .take(size)
                .map(this::toResponseDto);
    }

    // Mapping methods
    private AlarmEntity toEntity(AlarmRequestDTO dto) {
        AlarmEntity entity = new AlarmEntity();
        entity.setRuleId(dto.getRuleId());
        entity.setMessage(dto.getMessage());
        return entity;
    }

    private AlarmResponseDTO toResponseDto(AlarmEntity entity) {
        AlarmResponseDTO dto = new AlarmResponseDTO();
        dto.setId(entity.getId());
        dto.setRuleId(entity.getRuleId());
        dto.setMessage(entity.getMessage());
        return dto;
    }
}

