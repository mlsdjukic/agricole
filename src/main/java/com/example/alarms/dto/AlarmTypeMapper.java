package com.example.alarms.dto;

import com.example.alarms.entities.AlarmTypeEntity;
import org.springframework.stereotype.Component;

@Component
public class AlarmTypeMapper {

    public AlarmType toDto(AlarmTypeEntity entity) {
        if (entity == null) return null;
        AlarmType dto = new AlarmType();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setMetadata(entity.getMetadata());
        return dto;
    }

    public AlarmTypeEntity toEntity(AlarmType dto) {
        if (dto == null) return null;
        AlarmTypeEntity entity = new AlarmTypeEntity();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setMetadata(dto.getMetadata());
        return entity;
    }

    public void updateEntityFromDto(AlarmType dto, AlarmTypeEntity entity) {
        entity.setName(dto.getName());
        entity.setMetadata(dto.getMetadata());
    }
}

