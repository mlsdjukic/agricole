package com.example.alarms.dto;

import com.example.alarms.entities.AccountEntity;
import com.example.alarms.entities.ReactionEntity;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReactionMapper {

    private final ObjectMapper objectMapper;

    public ReactionMapper() {
        this.objectMapper = new ObjectMapper();
    }

    public ReactionEntity toEntity(ReactionDTO dto) {
        return objectMapper.convertValue(dto, ReactionEntity.class);
    }

    public ReactionDTO toDTO(ReactionEntity entity) {
        return objectMapper.convertValue(entity, ReactionDTO.class);
    }
}
