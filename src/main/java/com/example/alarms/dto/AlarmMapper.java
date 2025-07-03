package com.example.alarms.dto;

import com.example.alarms.entities.AlarmEntity;
import org.springframework.stereotype.Component;

@Component
public class AlarmMapper {
    public AlarmEntity toEntity(Alarm dto) {
        AlarmEntity entity = new AlarmEntity();
        entity.setId(dto.getId());
        entity.setRuleId(dto.getRuleId());
        entity.setMessage(dto.getMessage());
        entity.setArchived(dto.getArchived());
        entity.setMetadata(dto.getMetadata());
        entity.setRelation(dto.getRelation());
        entity.setStatus(dto.getStatus());
        entity.setAlarmTypeId(dto.getAlarmTypeId());
        entity.setAlarmClassId(dto.getAlarmClassId());
        entity.setCreatedDate(dto.getCreatedDate());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }

    public Alarm toDto(AlarmEntity entity) {
        Alarm dto = new Alarm();
        dto.setId(entity.getId());
        dto.setRuleId(entity.getRuleId());
        dto.setMessage(entity.getMessage());
        dto.setArchived(entity.getArchived());
        dto.setMetadata(entity.getMetadata());
        dto.setRelation(entity.getRelation());
        dto.setStatus(entity.getStatus());
        dto.setAlarmTypeId(entity.getAlarmTypeId());
        dto.setAlarmClassId(entity.getAlarmClassId());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
