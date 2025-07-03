package com.example.alarms.services;

import com.example.alarms.dto.AlarmType;
import com.example.alarms.dto.AlarmTypeMapper;
import com.example.alarms.entities.AlarmTypeEntity;
import com.example.alarms.repositories.AlarmTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlarmTypeServiceTest {

    @Mock
    private AlarmTypeRepository repository;

    @Mock
    private AlarmTypeMapper mapper;

    @InjectMocks
    private AlarmTypeService service;

    private final Long alarmTypeId = 1L;
    private final AlarmType dto = new AlarmType();
    private final AlarmTypeEntity entity = new AlarmTypeEntity();

    @BeforeEach
    void setup() {
        dto.setId(alarmTypeId);
        entity.setId(alarmTypeId);
    }

    @Test
    void create_shouldSaveAndReturnDto() {
        when(mapper.toEntity(dto)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(Mono.just(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        StepVerifier.create(service.create(dto))
                .expectNext(dto)
                .verifyComplete();

        verify(repository).save(entity);
        verify(mapper).toEntity(dto);
        verify(mapper).toDto(entity);
    }

    @Test
    void getById_shouldReturnDto() {
        when(repository.findById(alarmTypeId)).thenReturn(Mono.just(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        StepVerifier.create(service.getById(alarmTypeId))
                .expectNext(dto)
                .verifyComplete();

        verify(repository).findById(alarmTypeId);
        verify(mapper).toDto(entity);
    }

    @Test
    void getAll_shouldReturnAllDtos() {
        AlarmTypeEntity anotherEntity = new AlarmTypeEntity();
        AlarmType anotherDto = new AlarmType();
        when(repository.findAll()).thenReturn(Flux.just(entity, anotherEntity));
        when(mapper.toDto(entity)).thenReturn(dto);
        when(mapper.toDto(anotherEntity)).thenReturn(anotherDto);

        StepVerifier.create(service.getAll())
                .expectNext(dto, anotherDto)
                .verifyComplete();

        verify(repository).findAll();
    }

    @Test
    void update_shouldModifyAndReturnDto() {
        when(repository.findById(alarmTypeId)).thenReturn(Mono.just(entity));
        doAnswer(invocation -> {
            // Simulate mapping update logic
            return null;
        }).when(mapper).updateEntityFromDto(dto, entity);
        when(repository.save(entity)).thenReturn(Mono.just(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        StepVerifier.create(service.update(alarmTypeId, dto))
                .expectNext(dto)
                .verifyComplete();

        verify(repository).findById(alarmTypeId);
        verify(mapper).updateEntityFromDto(dto, entity);
        verify(repository).save(entity);
        verify(mapper).toDto(entity);
    }

    @Test
    void delete_shouldCallRepositoryDeleteById() {
        when(repository.deleteById(alarmTypeId)).thenReturn(Mono.empty());

        StepVerifier.create(service.delete(alarmTypeId))
                .verifyComplete();

        verify(repository).deleteById(alarmTypeId);
    }
}
