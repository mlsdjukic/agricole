package com.example.alarms.dto;

import com.example.alarms.entities.ActionEntity;
import com.example.alarms.exceptions.SerializationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class ActionMapper {

    private final ObjectMapper objectMapper;
    private final RuleMapper ruleMapper;

    public ActionMapper(ObjectMapper objectMapper, RuleMapper ruleMapper) {
        this.objectMapper = objectMapper;
        this.ruleMapper = ruleMapper;
    }

    /**
     * Converts ActionDTO to ActionEntity
     * @param dto The ActionDTO to convert
     * @return ActionEntity with data from DTO
     * @throws JsonProcessingException if params serialization fails
     */
    public ActionEntity toEntity(ActionRequest dto) throws JsonProcessingException {
        if (dto == null) {
            return null;
        }

        ActionEntity entity = new ActionEntity();
        entity.setType(dto.getType());

        // Convert Map to JSON string
        if (dto.getParams() != null) {
            entity.setParams(JsonUtils.toJson(dto.getParams()));
        }

        // Map rules if present
        if (dto.getRules() != null && !dto.getRules().isEmpty()) {
            entity.setRules(ruleMapper.toEntityList(dto.getRules()));
        }

        return entity;
    }

    /**
     * Converts ActionEntity to ActionDTO
     * @param entity The ActionEntity to convert
     * @return ActionDTO with data from entity
     */
    public ActionRequest toDto(ActionEntity entity) {
        if (entity == null) {
            return null;
        }

        ActionRequest dto = new ActionRequest();
        dto.setType(entity.getType());

        // Convert JSON string to Map
        if (entity.getParams() != null && !entity.getParams().isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> paramsMap = objectMapper.readValue(
                        entity.getParams(), Map.class);
                dto.setParams(paramsMap);
            } catch (JsonProcessingException e) {
                // Log error and set empty map
                dto.setParams(Collections.emptyMap());
            }
        }

        // Map rules if present
        if (entity.getRules() != null && !entity.getRules().isEmpty()) {
            dto.setRules(ruleMapper.toDtoList(entity.getRules()));
        }

        return dto;
    }

    /**
     * Converts a list of ActionEntity objects to a list of ActionDTO objects
     * @param entities List of ActionEntity objects
     * @return List of ActionDTO objects
     */
    public List<ActionRequest> toDtoList(List<ActionEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }

        List<ActionRequest> dtoList = new ArrayList<>(entities.size());
        for (ActionEntity entity : entities) {
            dtoList.add(toDto(entity));
        }
        return dtoList;
    }

    /**
     * Converts a list of ActionDTO objects to a list of ActionEntity objects
     * @param dtos List of ActionDTO objects
     * @return List of ActionEntity objects
     * @throws JsonProcessingException if params serialization fails
     */
    public List<ActionEntity> toEntityList(List<ActionRequest> dtos) throws JsonProcessingException {
        if (dtos == null) {
            return Collections.emptyList();
        }

        List<ActionEntity> entityList = new ArrayList<>(dtos.size());
        for (ActionRequest dto : dtos) {
            entityList.add(toEntity(dto));
        }
        return entityList;
    }

    /**
     * Reactive version of toEntity that handles exceptions with Mono
     * @param dto The ActionDTO to convert
     * @return Mono<ActionEntity> with converted entity or error
     */
    public Mono<ActionEntity> toEntityReactive(ActionRequest dto) {
        try {
            return Mono.just(toEntity(dto));
        } catch (JsonProcessingException e) {
            return Mono.error(new SerializationException("Failed to serialize params", e));
        }
    }

    /**
     * Updates an existing entity with DTO data
     * @param entity The existing entity to update
     * @param dto The DTO with updated data
     * @return Updated entity
     * @throws JsonProcessingException if params serialization fails
     */
    public ActionEntity updateEntityFromDto(ActionEntity entity, ActionRequest dto) throws JsonProcessingException {
        if (entity == null || dto == null) {
            return entity;
        }

        if (dto.getType() != null) {
            entity.setType(dto.getType());
        }

        if (dto.getParams() != null) {
            entity.setParams(objectMapper.writeValueAsString(dto.getParams()));
        }

        return entity;
    }

    /**
     * Converts ActionEntity to ActionResponse
     * @param entity The ActionEntity to convert
     * @return ActionDTO with data from entity
     */
    public ActionResponse toActionResponse(ActionEntity entity) {
        if (entity == null) {
            return null;
        }

        ActionResponse dto = new ActionResponse();
        dto.setType(entity.getType());
        dto.setId(entity.getId());
        dto.setAlarmTypeId(entity.getAlarmTypeId());
        dto.setAlarmClassId(entity.getAlarmClassId());

        // Convert JSON string to Map
        if (entity.getParams() != null && !entity.getParams().isEmpty()) {
            try {

                Map<String, Object> paramsMap = objectMapper.readValue(
                        entity.getParams(), Map.class);
                dto.setParams(paramsMap);
            } catch (JsonProcessingException e) {
                // Log error and set empty map
                dto.setParams(Collections.emptyMap());
            }
        }

        // Map rules if present
        if (entity.getRules() != null && !entity.getRules().isEmpty()) {
            dto.setRules(ruleMapper.toDtoList(entity.getRules()));
        }

        return dto;
    }
}
