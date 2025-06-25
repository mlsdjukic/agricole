package com.example.alarms.dto;

import com.example.alarms.entities.AlarmClassEntity;
import org.springframework.stereotype.Component;

@Component
public class AlarmClassMapper {

    public AlarmClass toDto(AlarmClassEntity entity) {
        if (entity == null) return null;
        AlarmClass dto = new AlarmClass();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setMetadata(entity.getMetadata());
        return dto;
    }

    public AlarmClassEntity toEntity(AlarmClass dto) {
        if (dto == null) return null;
        AlarmClassEntity entity = new AlarmClassEntity();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setMetadata(dto.getMetadata());
        return entity;
    }

    public void updateEntityFromDto(AlarmClass dto, AlarmClassEntity entity) {
        entity.setName(dto.getName());
        entity.setMetadata(dto.getMetadata());
    }
}

