package com.example.alarms.dto;

import com.example.alarms.entities.RuleEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class RuleMapper {

    private final ObjectMapper objectMapper;

    public RuleMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RuleEntity toEntity(RuleDTO dto) {
        RuleEntity entity = new RuleEntity();
        try {
            // Convert Map to JSON string
            entity.setRule(objectMapper.writeValueAsString(dto.getRule()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing rule map to JSON", e);
        }
        entity.setName(dto.getName());
        entity.setId(dto.getId());
        return entity;
    }

    public RuleDTO toDTO(RuleEntity entity) {
        RuleDTO dto = new RuleDTO();
        try {
            // Convert JSON string to Map<String, Object>
            dto.setRule(objectMapper.readValue(entity.getRule(), new TypeReference<>() {}));
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing JSON to rule map", e);
        }
        dto.setName(entity.getName());
        dto.setId(entity.getId());
        return dto;
    }
}
