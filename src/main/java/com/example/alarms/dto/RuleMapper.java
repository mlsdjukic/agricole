package com.example.alarms.dto;

import com.example.alarms.entities.ReactionEntity;
import com.example.alarms.entities.RuleEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class RuleMapper {

    private final ObjectMapper objectMapper;
    private final ReactionMapper reactionMapper;

    public RuleMapper(ObjectMapper objectMapper, ReactionMapper reactionMapper) {
        this.objectMapper = objectMapper;
        this.reactionMapper = reactionMapper;
    }

    public RuleEntity toEntity(Rule dto) {
        if (dto == null) {
            return null;
        }

        RuleEntity entity = new RuleEntity();
        try {
            // Convert Map to JSON string
            entity.setDefinition(objectMapper.writeValueAsString(dto.getDefinition()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing rule map to JSON", e);
        }
        entity.setName(dto.getName());
        return entity;
    }

    public Rule toDTO(RuleEntity entity) {
        if (entity == null) {
            return null;
        }

        Rule dto = new Rule();
        try {
            // Convert JSON string to Map<String, Object>
            dto.setDefinition(objectMapper.readValue(entity.getDefinition(), new TypeReference<>() {}));
        } catch (Exception e) {
            throw new RuntimeException("Error deserializing JSON to rule map", e);
        }
        List<Reaction> reactions = new ArrayList<>();
        for (ReactionEntity reaction : entity.getReactions()){
            reactions.add(reactionMapper.toDTO(reaction));
        }
        dto.setReactions(reactions);
        dto.setName(entity.getName());
        return dto;
    }

    /**
     * Converts a list of RuleEntity objects to a list of RuleDTO objects
     * @param entities List of RuleEntity objects
     * @return List of RuleDTO objects
     */
    public List<Rule> toDtoList(List<RuleEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }

        List<Rule> dtoList = new ArrayList<>(entities.size());
        for (RuleEntity entity : entities) {
            dtoList.add(toDTO(entity));
        }
        return dtoList;
    }

    /**
     * Converts a list of RuleDTO objects to a list of RuleEntity objects
     * @param dtos List of RuleDTO objects
     * @return List of RuleEntity objects
     */
    public List<RuleEntity> toEntityList(List<Rule> dtos) {
        if (dtos == null) {
            return Collections.emptyList();
        }

        List<RuleEntity> entityList = new ArrayList<>(dtos.size());
        for (Rule dto : dtos) {
            entityList.add(toEntity(dto));
        }
        return entityList;
    }
}