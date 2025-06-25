package com.example.alarms.services;

import com.example.alarms.dto.AlarmType;
import com.example.alarms.dto.AlarmTypeMapper;
import com.example.alarms.repositories.AlarmTypeRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class AlarmTypeService {

    private final AlarmTypeRepository repository;
    private final AlarmTypeMapper mapper;

    public AlarmTypeService(AlarmTypeRepository repository, AlarmTypeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Mono<AlarmType> create(AlarmType dto) {
        return repository.save(mapper.toEntity(dto))
                .map(mapper::toDto);
    }

    public Mono<AlarmType> getById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto);
    }

    public Flux<AlarmType> getAll() {
        return repository.findAll()
                .map(mapper::toDto);
    }

    public Mono<AlarmType> update(Long id, AlarmType dto) {
        return repository.findById(id)
                .flatMap(existing -> {
                    mapper.updateEntityFromDto(dto, existing);
                    return repository.save(existing);
                })
                .map(mapper::toDto);
    }

    public Mono<Void> delete(Long id) {
        return repository.deleteById(id);
    }
}

