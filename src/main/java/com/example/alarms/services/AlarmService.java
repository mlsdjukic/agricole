package com.example.alarms.services;

import com.example.alarms.dto.Alarm;
import com.example.alarms.dto.AlarmMapper;
import com.example.alarms.dto.AlarmWithTypeAndClass;
import com.example.alarms.entities.AlarmEntity;
import com.example.alarms.exceptions.*;
import com.example.alarms.repositories.AlarmClassRepository;
import com.example.alarms.repositories.AlarmRepository;
import com.example.alarms.repositories.AlarmTypeRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final AlarmTypeRepository alarmTypeRepository;
    private final AlarmClassRepository alarmClassRepository;
    private final AlarmMapper alarmMapper;

    public AlarmService(AlarmRepository alarmRepository, AlarmTypeRepository alarmTypeRepository, AlarmClassRepository alarmClassRepository, AlarmMapper alarmMapper) {
        this.alarmRepository = alarmRepository;
        this.alarmTypeRepository = alarmTypeRepository;
        this.alarmClassRepository = alarmClassRepository;
        this.alarmMapper = alarmMapper;
    }

    // Save or update an alarm
    public Mono<Alarm> save(Alarm alarmRequest) {
        AlarmEntity alarmEntity = alarmMapper.toEntity(alarmRequest);
        return alarmRepository.save(alarmEntity)
                .map(alarmMapper::toDto);
    }

    public Mono<Alarm> update(Alarm alarmRequest, Long id) {
        if (alarmRequest == null) {
            return Mono.error(new InvalidActionException("Alarm cannot be null"));
        }

        if (id == null) {
            return Mono.error(new InvalidActionException("Alarm ID cannot be null"));
        }
        return alarmRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Alarm not found")))
                .flatMap(existing -> {
                    // Manually update fields
                    existing.setRuleId(alarmRequest.getRuleId());
                    existing.setMessage(alarmRequest.getMessage());
                    existing.setStatus(alarmRequest.getStatus());
                    existing.setArchived(alarmRequest.getArchived());
                    existing.setCreatedFrom(alarmRequest.getCreatedFrom());
                    existing.setMetadata(alarmRequest.getMetadata());
                    existing.setRelation(alarmRequest.getRelation());
                    existing.setAlarmTypeId(alarmRequest.getAlarmTypeId());
                    existing.setAlarmClassId(alarmRequest.getAlarmClassId());
                    // Don't set createdDate or updatedAt manually

                    return alarmRepository.save(existing);
                })
                .map(alarmMapper::toDto);

    }

    public Flux<Alarm> getAll() {
        return alarmRepository.findAll()
                .map(alarmMapper::toDto);
    }

    // Get alarm by ID
    public Mono<Alarm> getById(Long id) {
        return alarmRepository.findById(id)
                .map(alarmMapper::toDto);
    }

    // Delete alarm by ID
    public Mono<Void> deleteById(Long id) {
        return alarmRepository.deleteById(id);
    }

    // Get the last record by ruleId
    public Mono<Alarm> getLastRecordByRuleId(Long ruleId) {
        return alarmRepository.findFirstByRuleIdOrderByIdDesc(ruleId)
                .map(alarmMapper::toDto);
    }

    // Get all alarms with pagination
    public Flux<Alarm> getAllWithPagination(int page, int size) {
        return alarmRepository.findAllBy()
                .skip((long) page * size)
                .take(size)
                .map(alarmMapper::toDto);
    }

    public Mono<AlarmWithTypeAndClass> getAlarmWithTypeAndClass(Long alarmId) {
        return alarmRepository.findById(alarmId)
                .flatMap(alarm ->
                        Mono.zip(
                                alarmTypeRepository.findById(alarm.getAlarmTypeId()),
                                alarmClassRepository.findById(alarm.getAlarmClassId())
                        ).map(tuple -> new AlarmWithTypeAndClass(alarm, tuple.getT1(), tuple.getT2()))
                );
    }
}

