package com.example.alarms.dto;

import com.example.alarms.entities.AccountEntity;
import com.example.alarms.entities.ReactionEntity;
import com.example.alarms.entities.RuleEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ReactionMapper {

    private final ObjectMapper objectMapper;

    public ReactionMapper() {
        this.objectMapper = new ObjectMapper();
    }

    public ReactionEntity toEntity(Reaction dto) {
        if (dto == null) {
            return null;
        }

        ReactionEntity entity = new ReactionEntity();
        try {
            // Convert Map to JSON string
            entity.setParams(objectMapper.writeValueAsString(dto.getParams()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing rule map to JSON", e);
        }
        entity.setName(dto.getName());
        return entity;    }

    public Reaction toDTO(ReactionEntity entity) {
        if (entity == null) {
            return null;
        }

        Reaction dto = new Reaction();
        try {
            // Convert JSON string to Map<String, Object>
            dto.setParams(objectMapper.readValue(entity.getParams(), new TypeReference<>() {}));
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing JSON to rule map", e);
        }

        dto.setName(entity.getName());
        return dto;    }
}
