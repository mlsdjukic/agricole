package com.example.alarms.services;

import com.example.alarms.dto.*;
import com.example.alarms.entities.ActionEntity;
import com.example.alarms.entities.RuleEntity;
import com.example.alarms.exceptions.*;
import com.example.alarms.repositories.ActionRepository;
import com.example.alarms.services.utils.ActionValidator;
import com.example.alarms.services.utils.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class ActionService {

    private final ActionRepository actionRepository;
    private final RuleService ruleService;

    public Mono<ActionEntity> create(ActionRequest action, Long userId) {
        List<String> validationErrors = ActionValidator.validateCreateUpdateRequest(action);

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> paramsMap = mapper.convertValue(action.getParams(),
                new TypeReference<Map<String, Object>>() {});

        if (!validationErrors.isEmpty()) {
            // Throw an exception with validation errors
            String errorMessage = String.join("; ", validationErrors);
            return Mono.error(new InvalidActionException(errorMessage));
        }
        return Mono.fromCallable(() -> JsonUtils.toJson(paramsMap))
                .flatMap(jsonParams -> actionRepository.save(new ActionEntity(
                        null,
                        action.getType(),
                        jsonParams,
                        userId,
                        action.getAlarmTypeId(),
                        action.getAlarmClassId(),
                        null,
                        null,
                        null)))
                .flatMap(savedAction ->
                        Flux.fromIterable(action.getRules())
                        .flatMap(rule -> saveRuleAndReactions(savedAction, rule))
                        .collectList()
                        .map(rules -> {
                            savedAction.setRules(rules);
                            return savedAction;
                        }))
                .onErrorMap(JsonProcessingException.class, e ->
                        new SerializationException("Error serializing action parameters", e))
                .onErrorMap(e -> !(e instanceof InvalidActionException ||
                                e instanceof UserNotFoundException ||
                                e instanceof RuleProcessingException ||
                                e instanceof SerializationException),
                        e -> new RuntimeException("Unexpected error creating action", e));
    }

    public Mono<ActionEntity> update(ActionRequest action, Long id) {
        if (action == null) {
            return Mono.error(new InvalidActionException("Action cannot be null"));
        }

        if (id == null) {
            return Mono.error(new InvalidActionException("Action ID cannot be null"));
        }

        List<String> validationErrors = ActionValidator.validateCreateUpdateRequest(action);

        if (!validationErrors.isEmpty()) {
            // Throw an exception with validation errors
            String errorMessage = String.join("; ", validationErrors);
            log.warn("Action validation failed: {}", errorMessage);
            return Mono.error(new InvalidActionException(errorMessage));
        }

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> paramsMap = mapper.convertValue(action.getParams(),
                new TypeReference<Map<String, Object>>() {});

        return Mono.fromCallable(() -> JsonUtils.toJson(paramsMap))
                .flatMap(jsonParams ->
                        actionRepository.findById(id)
                                .switchIfEmpty(Mono.error(new EntityNotFoundException("Action not found with ID: " + id)))
                                .flatMap(existingAction -> updateActionAndSave(existingAction, action, jsonParams))
                )
                .onErrorMap(JsonProcessingException.class, e ->
                        new SerializationException("Error serializing action parameters", e))
                .onErrorMap(e -> !(e instanceof InvalidActionException ||
                                e instanceof UserNotFoundException ||
                                e instanceof RuleProcessingException ||
                                e instanceof EntityNotFoundException ||
                                e instanceof SerializationException),
                        e -> new RuntimeException("Unexpected error creating action", e));
    }


    private Mono<ActionEntity> updateActionAndSave(ActionEntity existingAction, ActionRequest action, String jsonParams) {
        // Update the action entity with new parameters
        existingAction.setType(action.getType());
        existingAction.setParams(jsonParams);

        // Save the updated action
        return actionRepository.save(existingAction)
                .flatMap(savedAction -> updateRulesAndReactions(savedAction, action))
                .doOnError(e -> log.error("Failed to update action {}: {}", existingAction.getId(), e.getMessage()));

    }

    private Mono<ActionEntity> updateRulesAndReactions(ActionEntity savedAction, ActionRequest action) {
        if (savedAction == null) {
            return Mono.error(new InvalidActionException("Saved action cannot be null"));
        }

        if (action == null || action.getRules() == null) {
            return Mono.error(new InvalidActionException("Action or rules cannot be null"));
        }

        // First, delete existing reactions associated with the rules
        return ruleService.deleteByActionId(savedAction.getId())
                .then(saveNewRules(savedAction, action))
                .thenReturn(savedAction)
                .onErrorResume(ex -> Mono.error(new RuleProcessingException("Failed to update rules and reactions", ex)));

    }

    private Mono<Void> saveNewRules(ActionEntity savedAction, ActionRequest action) {
        return Flux.fromIterable(action.getRules())
                .flatMap(ruleDTO -> saveRuleAndReactions(savedAction, ruleDTO))
                .collectList() // Collect into a List<RuleEntity>
                .doOnNext(savedAction::setRules) // Set rules to ActionEntity
                .then() // Return Mono<Void>
                .onErrorResume(ex -> Mono.error(new RuleProcessingException("Failed to save rules", ex)));
    }

    private Mono<RuleEntity> saveRuleAndReactions(ActionEntity savedAction, Rule rule) {
        if (savedAction == null) {
            return Mono.error(new InvalidActionException("Action cannot be null"));
        }

        if (rule == null) {
            return Mono.error(new InvalidActionException("Rule cannot be null"));
        }

        return ruleService.create(rule, savedAction.getId())
                .onErrorResume(ex -> {
                    // Log error but continue with other rules
                    log.error("Failed to save rule for action {}: {}",
                            savedAction.getId(), ex.getMessage());
                    return Mono.empty(); // Skip this rule but continue with others
                });
    }


    private Mono<ActionEntity> deleteRulesAndReactions(ActionEntity savedAction) {
        // First, delete existing reactions associated with the rules
        return ruleService.deleteByActionId(savedAction.getId())
                .thenReturn(savedAction)
                .onErrorResume(ex -> Mono.error(new RuleProcessingException("Failed to delete rule with id", ex)));

    }

    /**
     * Fetch an action by its ID.
     *
     * @param id Action ID
     * @return Mono of ActionEntity
     */
    public Mono<ActionEntity> getActionById(Long id) {
        return actionRepository.findById(id)
                .flatMap(this::attachRulesToAction);
    }

    /**
     * Delete an action by its ID.
     *
     * @param id Action ID
     * @return Mono<Void>
     */
    public Mono<Void> deleteAction(Long id) {
        return actionRepository.findById(id)
                .flatMap(this::deleteRulesAndReactions)
                .then(actionRepository.deleteById(id))
                .onErrorResume(ex -> Mono.error(new RuntimeException("Failed to delete action", ex)));

    }

    /**
     * Attach rules to an ActionEntity based on actionId.
     *
     * @param action ActionEntity
     * @return Mono of ActionEntity with rules
     */
    private Mono<ActionEntity> attachRulesToAction(ActionEntity action) {
        if (action == null) {
            return Mono.error(new IllegalArgumentException("Action cannot be null"));
        }
        return ruleService.getByActionId(action.getId())
                .collectList()
                .map(action::setRules)
                .onErrorResume(ex -> Mono.error(new RuntimeException("Failed to fetch rules", ex)));

    }

    public Flux<ActionEntity> getAll(Pageable pageable) {
        return actionRepository.findAllBy(pageable)
                .flatMap(this::attachRulesToAction)
                .onErrorMap(e -> {
                    // Log the error
                    log.error("Error while fetching actions: {}", e.getMessage());

                    // Return the original exception
                    return e;
                });
    }


    @Getter
    public static class ActionResult {
        private final boolean valid;
        private final ValidationResult validationResult;
        private final Map<String, Object> data;

        private ActionResult(boolean valid, ValidationResult validationResult, Map<String, Object> data) {
            this.valid = valid;
            this.validationResult = validationResult;
            this.data = data;
        }

        /**
         * Create a successful action result
         */
        public static ActionResult success(Map<String, Object> data) {
            return new ActionResult(true, ValidationResult.success(), data);
        }

        /**
         * Create an invalid action result with validation errors
         */
        public static ActionResult invalid(ValidationResult validationResult) {
            return new ActionResult(false, validationResult, null);
        }

    }

}
