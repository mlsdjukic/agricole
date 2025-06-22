package com.example.alarms.dto;

import com.example.alarms.entities.ReactionEntity;
import com.example.alarms.entities.RuleEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

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
        Set<ReactionEntity> reactions = new HashSet<>();
        for (Reaction reaction : dto.getReactions()){
            reactions.add(reactionMapper.toEntity(reaction));
        }
        entity.setReactions(reactions);
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
        Set<Reaction> reactions = new HashSet<>();
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
    public Set<Rule> toDtoList(Set<RuleEntity> entities) {
        if (entities == null) {
            return Collections.emptySet();
        }

        Set<Rule> dtoList = new HashSet<>(entities.size());
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
    public Set<RuleEntity> toEntityList(Set<Rule> dtos) {
        if (dtos == null) {
            return Collections.emptySet();
        }

        Set<RuleEntity> entityList = new HashSet<>(dtos.size());
        for (Rule dto : dtos) {
            entityList.add(toEntity(dto));
        }
        return entityList;
    }
}