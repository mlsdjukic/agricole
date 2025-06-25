package com.example.alarms.services;

import com.example.alarms.dto.AlarmClass;
import com.example.alarms.dto.AlarmClassMapper;
import com.example.alarms.entities.AlarmClassEntity;
import com.example.alarms.repositories.AlarmClassRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AlarmClassService {

    private final AlarmClassRepository repository;
    private final AlarmClassMapper mapper;

    public AlarmClassService(AlarmClassRepository repository, AlarmClassMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Mono<AlarmClass> create(AlarmClass dto) {
        AlarmClassEntity entity = mapper.toEntity(dto);
        return repository.save(entity)
                .map(mapper::toDto);
    }

    public Mono<AlarmClass> getById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto);
    }

    public Flux<AlarmClass> getAll() {
        return repository.findAll()
                .map(mapper::toDto);
    }

    public Mono<AlarmClass> update(Long id, AlarmClass updatedDto) {
        return repository.findById(id)
                .flatMap(existing -> {
                    mapper.updateEntityFromDto(updatedDto, existing);
                    return repository.save(existing);
                })
                .map(mapper::toDto);
    }

    public Mono<Void> delete(Long id) {
        return repository.deleteById(id);
    }
}
