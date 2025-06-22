package com.example.alarms.services;

import com.example.alarms.dto.*;
import com.example.alarms.entities.ActionEntity;
import com.example.alarms.entities.ReactionEntity;
import com.example.alarms.entities.RuleEntity;
import com.example.alarms.exceptions.*;
import com.example.alarms.repositories.ActionRepository;
import com.example.alarms.services.utils.ActionValidator;
import com.example.alarms.services.utils.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Null;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class ActionService {

    private final ActionRepository actionRepository;
    private final ActionMapper actionMapper;

    public ActionEntity create(Action action, Long userId) throws JsonProcessingException {
        List<String> validationErrors = ActionValidator.validateCreateUpdateRequest(action);

        if (!validationErrors.isEmpty()) {
            // Throw an exception with validation errors
            String errorMessage = String.join("; ", validationErrors);
            throw new InvalidActionException(errorMessage);
        }

        ActionEntity entity = actionMapper.toEntity(action);
        entity.setUserId(userId);
        for (RuleEntity rule : entity.getRules()) {
            rule.setAction(entity);
            for (ReactionEntity reaction : rule.getReactions()){
                reaction.setRule(rule);
            }
        }
        return actionRepository.save(entity);
    }

    public ActionEntity update(Action action, Long id) throws JsonProcessingException, IllegalArgumentException {
        if (action == null) {
            throw new InvalidActionException("Action cannot be null");
        }

        if (id == null) {
            throw new InvalidActionException("Action ID cannot be null");
        }

        List<String> validationErrors = ActionValidator.validateCreateUpdateRequest(action);

        if (!validationErrors.isEmpty()) {
            // Throw an exception with validation errors
            String errorMessage = String.join("; ", validationErrors);
            log.warn("Action validation failed: {}", errorMessage);
            throw new InvalidActionException(errorMessage);
        }

        ActionEntity actionToUpdate = actionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Action with id " + id + " not found"));

        ActionEntity updated = actionMapper.toEntity(action);

        updated.setUserId(actionToUpdate.getUserId());
        updated.setCreatedDate(actionToUpdate.getCreatedDate());
        for (RuleEntity rule : updated.getRules()) {
            rule.setAction(updated);
            for (ReactionEntity reaction : rule.getReactions()){
                reaction.setRule(rule);
            }
        }
        updated.setId(id); // Ensure ID remains unchanged
        return actionRepository.save(updated);
    }

    /**
     * Fetch an action by its ID.
     *
     * @param id Action ID
     * @return Mono of ActionEntity
     */
    public Optional<ActionEntity> getActionById(Long id) {
        return actionRepository.findById(id);
    }

    public Optional<ActionEntity> getActionByIdWithRulesAndReactions(Long id) {
        return actionRepository.findByIdWithRulesAndReactions(id);
    }

    /**
     * Delete an action by its ID.
     *
     * @param id Action ID
     * @return Mono<Void>
     */
    public void deleteAction(Long id) {
        actionRepository.deleteById(id);
    }

    public List<ActionEntity> getAll(Pageable pageable) {
        return new ArrayList<>(actionRepository.findAll());
    }
}