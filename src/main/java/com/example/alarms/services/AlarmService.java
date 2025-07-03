package com.example.alarms.services;

import com.example.alarms.dto.*;
import com.example.alarms.entities.AlarmClassEntity;
import com.example.alarms.entities.AlarmEntity;
import com.example.alarms.entities.AlarmTypeEntity;
import com.example.alarms.exceptions.*;
import com.example.alarms.repositories.AlarmClassRepository;
import com.example.alarms.repositories.AlarmRepository;
import com.example.alarms.repositories.AlarmTypeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.data.domain.Pageable;


import java.util.HashMap;
import java.util.Map;

@Service
public class AlarmService {

    private final AlarmRepository alarmRepository;
    private final AlarmTypeRepository alarmTypeRepository;
    private final AlarmClassRepository alarmClassRepository;
    private final AlarmMapper alarmMapper;
    private final AlarmTypeMapper alarmTypeMapper;
    private final AlarmClassMapper alarmClassMapper;

    public AlarmService(AlarmRepository alarmRepository, AlarmTypeRepository alarmTypeRepository, AlarmClassRepository alarmClassRepository, AlarmMapper alarmMapper, AlarmTypeMapper alarmTypeMapper, AlarmClassMapper alarmClassMapper) {
        this.alarmRepository = alarmRepository;
        this.alarmTypeRepository = alarmTypeRepository;
        this.alarmClassRepository = alarmClassRepository;
        this.alarmMapper = alarmMapper;
        this.alarmTypeMapper = alarmTypeMapper;
        this.alarmClassMapper = alarmClassMapper;
    }

    // Save or update an alarm
    public Mono<AlarmResponse> save(AlarmRequest alarmRequest) {
        AlarmEntity alarmEntity = alarmMapper.toEntity(alarmRequest);
        alarmEntity.setId(null);
        return alarmRepository.save(alarmEntity)
                .map(alarmMapper::toDto);
    }

    public Mono<AlarmResponse> update(AlarmRequest alarmRequest, Long id, Long userId) {
        if (alarmRequest == null) {
            return Mono.error(new InvalidActionException("Alarm cannot be null"));
        }

        if (id == null) {
            return Mono.error(new InvalidActionException("Alarm ID cannot be null"));
        }
        return alarmRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Alarm not found")))
                .flatMap(existing ->
                        Mono.fromCallable(() -> {
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("userid", userId);
                                    return JsonUtils.toJson(map);
                                })
                                .onErrorMap(JsonProcessingException.class, e -> new RuntimeException("Failed to serialize JSON", e))
                                .flatMap(json -> {
                                    existing.setRuleId(alarmRequest.getRuleId());
                                    existing.setMessage(alarmRequest.getMessage());
                                    existing.setStatus(alarmRequest.getStatus());
                                    existing.setArchived(alarmRequest.getArchived());
                                    existing.setCreatedFrom(alarmRequest.getCreatedFrom());
                                    existing.setMetadata(alarmRequest.getMetadata());
                                    existing.setRelation(json);
                                    existing.setAlarmTypeId(alarmRequest.getAlarmTypeId());
                                    existing.setAlarmClassId(alarmRequest.getAlarmClassId());

                                    return alarmRepository.save(existing);
                                })
                )
                .map(alarmMapper::toDto);

    }

    public Flux<AlarmWithTypeAndClass> getAll() {
        return alarmRepository.findAll()
                .filter(alarmEntity -> !alarmEntity.getArchived())
                .flatMap(alarm -> {
                    Mono<AlarmTypeEntity> alarmTypeMono = (alarm.getAlarmTypeId() != null)
                            ? alarmTypeRepository.findById(alarm.getAlarmTypeId()).onErrorResume(e -> Mono.empty())
                            : Mono.empty();

                    Mono<AlarmClassEntity> alarmClassMono = (alarm.getAlarmClassId() != null)
                            ? alarmClassRepository.findById(alarm.getAlarmClassId()).onErrorResume(e -> Mono.empty())
                            : Mono.empty();

                    return Mono.zip(alarmTypeMono.defaultIfEmpty(new AlarmTypeEntity()), alarmClassMono.defaultIfEmpty(new AlarmClassEntity()))
                            .map(tuple -> {
                                AlarmTypeEntity type = tuple.getT1();
                                AlarmClassEntity alarmClass = tuple.getT2();

                                return new AlarmWithTypeAndClass(
                                        alarmMapper.toDto(alarm),
                                        alarmTypeMapper.toDto(type),
                                        alarmClassMapper.toDto(alarmClass)
                                );
                            });
                });
    }


    // Get alarm by ID
    public Mono<AlarmResponse> getById(Long id) {
        return alarmRepository.findById(id)
                .filter(alarmEntity -> !alarmEntity.getArchived())
                .map(alarmMapper::toDto);
    }

    // Delete alarm by ID
    public Mono<Void> deleteById(Long id, Long userId) {
        return alarmRepository.findById(id)
                .switchIfEmpty(Mono.error(new EntityNotFoundException("Alarm not found")))
                .flatMap(existing ->
                        Mono.fromCallable(() -> {
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("userid", userId);
                                    return JsonUtils.toJson(map);
                                })
                                .onErrorMap(JsonProcessingException.class, e -> new RuntimeException("Failed to serialize JSON", e))
                                .flatMap(json -> {
                                    existing.setArchived(true);
                                    existing.setRelation(json);
                                    return alarmRepository.save(existing);
                                })
                )
                .then();
    }

    // Get all alarms with pagination
    public Flux<AlarmResponse> getAllWithPagination(Pageable pageable) {
        return alarmRepository.findAllBy(pageable)
                .filter(alarmEntity -> !alarmEntity.getArchived())

                .map(alarmMapper::toDto);
    }

    public Mono<AlarmWithTypeAndClass> getAlarmWithTypeAndClass(Long alarmId) {
        return alarmRepository.findById(alarmId)
                .filter(alarmEntity -> !alarmEntity.getArchived())
                .flatMap(alarm ->
                        {
                            Mono<AlarmTypeEntity> alarmTypeMono = (alarm.getAlarmTypeId() != null)
                                    ? alarmTypeRepository.findById(alarm.getAlarmTypeId()).onErrorResume(e -> Mono.empty())
                                    : Mono.empty();

                            Mono<AlarmClassEntity> alarmClassMono = (alarm.getAlarmClassId() != null)
                                    ? alarmClassRepository.findById(alarm.getAlarmClassId()).onErrorResume(e -> Mono.empty())
                                    : Mono.empty();

                            return Mono.zip(alarmTypeMono.defaultIfEmpty(new AlarmTypeEntity()), alarmClassMono.defaultIfEmpty(new AlarmClassEntity())
                            ).map(tuple ->
                                    new AlarmWithTypeAndClass(
                                            alarmMapper.toDto(alarm),
                                            alarmTypeMapper.toDto(tuple.getT1()),
                                            alarmClassMapper.toDto(tuple.getT2())));
                        }

                );
    }
}

