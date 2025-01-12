package com.example.alarms.dto;

import com.example.alarms.entities.RuleEntity;
import org.springframework.stereotype.Component;

@Component
public class RuleMapper {

    public RuleEntity toEntity(RuleDTO dto) {
        RuleEntity entity = new RuleEntity();
        entity.setRule(dto.getRule());
        entity.setName(dto.getName());
        entity.setId(dto.getId());
        return entity;
    }

    public RuleDTO toDTO(RuleEntity entity) {
        RuleDTO dto = new RuleDTO();
        dto.setRule(entity.getRule());
        dto.setName(entity.getName());
        dto.setId(entity.getId());
        return dto;
    }
}
