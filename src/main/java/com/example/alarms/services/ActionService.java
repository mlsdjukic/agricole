package com.example.alarms.services;

import com.example.alarms.dto.ActionDTO;
import com.example.alarms.dto.JsonUtils;
import com.example.alarms.dto.RuleDTO;
import com.example.alarms.entities.ActionEntity;
import com.example.alarms.entities.RuleEntity;
import com.example.alarms.repositories.ActionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

@RequiredArgsConstructor
@Service
public class ActionService {

    private final ActionRepository actionRepository;
    private final RuleService ruleService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Flux<ActionEntity> findActionsInRange(int limit, int offset) {
        return actionRepository.findActionsInRange(limit, offset)
            .flatMap(action ->
                ruleService.getByActionId(action.getId())
                    .collectList()
                    .map(rules -> {
                        action.setRules(new ArrayList<>(rules));
                        return action;
                    })
                )
                .onErrorResume(ex -> Mono.error(new RuntimeException("Failed to fetch actions", ex)));

    }

    public Mono<ActionEntity> create(ActionDTO action, Long userId) {
        return Mono.fromCallable(() -> JsonUtils.toJson(action.getParams()))
                .flatMap(jsonParams -> actionRepository.save(new ActionEntity(null, action.getType(), jsonParams, userId, null, null, null)))
                .flatMap(savedAction -> {
                    // Explicitly check if action.getRules() is null to prevent the NPE
                    if (action.getRules() == null) {
                        return actionRepository.delete(savedAction)
                                .then(Mono.error(new RuntimeException("Failed to create action due to null rules")));
                    }
                    // Continue processing if action.getRules() is not null
                    return Flux.fromIterable(action.getRules())
                            .flatMap(ruleDTO -> saveRuleAndReactions(savedAction, ruleDTO))
                            .collectList()
                            .map(rules -> {
                                savedAction.setRules(rules);
                                return savedAction;
                            });
                })
                .onErrorMap(JsonProcessingException.class, e -> new RuntimeException("Error serializing action parameters", e))
                .onErrorMap(RuntimeException.class, e -> new RuntimeException("Error serializing rule or reaction", e));
    }






    public Mono<ActionEntity> update(ActionDTO action) {
        return Mono.fromCallable(() -> JsonUtils.toJson(action.getParams()))
                .flatMap(jsonParams ->
                        actionRepository.findById(action.getId())
                                .flatMap(existingAction -> updateActionAndSave(existingAction, action, jsonParams))
                                .switchIfEmpty(Mono.error(new RuntimeException("Action not found")))
                )
                .onErrorResume(JsonProcessingException.class, e ->
                        Mono.error(new RuntimeException("Error serializing action parameters", e))
                );
    }


    private Mono<ActionEntity> updateActionAndSave(ActionEntity existingAction, ActionDTO action, String jsonParams) {
        // Update the action entity with new parameters
        existingAction.setType(action.getType());
        existingAction.setParams(jsonParams);

        // Save the updated action
        return actionRepository.save(existingAction)
                .flatMap(savedAction -> updateRulesAndReactions(savedAction, action))
                .onErrorResume(ex -> Mono.error(new RuntimeException("Failed to update action", ex)));

    }

    private Mono<ActionEntity> updateRulesAndReactions(ActionEntity savedAction, ActionDTO action) {
        // First, delete existing reactions associated with the rules
        return ruleService.deleteByActionId(savedAction.getId())
                .then(saveNewRules(savedAction, action))
                .thenReturn(savedAction)
                .onErrorResume(ex -> Mono.error(new RuntimeException("Failed to update rules and reactions", ex)));

    }

    private Mono<ActionEntity> deleteRulesAndReactions(ActionEntity savedAction) {
        // First, delete existing reactions associated with the rules
        return ruleService.deleteByActionId(savedAction.getId())
                .thenReturn(savedAction)
                .onErrorResume(ex -> Mono.error(new IllegalArgumentException("Failed to delete rule with id", ex)));

    }

    private Mono<Void> saveNewRules(ActionEntity savedAction, ActionDTO action) {
        return Flux.fromIterable(action.getRules())
                .flatMap(ruleDTO -> saveRuleAndReactions(savedAction, ruleDTO))
                .collectList() // Collect into a List<RuleEntity>
                .doOnNext(savedAction::setRules) // Set rules to ActionEntity
                .then() // Return Mono<Void>
                .onErrorResume(ex -> Mono.error(new RuntimeException("Failed to save rules", ex)));
    }

    private Mono<RuleEntity> saveRuleAndReactions(ActionEntity savedAction, RuleDTO ruleDTO) {
        return ruleService.create(ruleDTO, savedAction.getId())
                .onErrorResume(ex -> ruleService.deleteByActionId(savedAction.getId())
                        .then(Mono.error(new RuntimeException("Failed to save rules and reactions", ex)))

                );
    }




    /**
     * Fetch all actions.
     *
     * @return Flux of ActionEntity
     */
    public Flux<ActionEntity> getAllActions() {
        return actionRepository.findAll()
                .flatMap(this::attachRulesToAction)
                .onErrorResume(ex -> Mono.error(new RuntimeException("Failed to fetch actions", ex)));

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
     * Create a new action.
     *
     * @param actionEntity ActionEntity to be created
     * @return Mono of ActionEntity
     */
    public Mono<ActionEntity> createAction(ActionEntity actionEntity) {
        return actionRepository.save(actionEntity);
    }

    /**
     * Update an existing action.
     *
     * @param id Action ID
     * @param updatedAction Updated ActionEntity
     * @return Mono of ActionEntity
     */
    public Mono<ActionEntity> updateAction(Long id, ActionEntity updatedAction) {
        return actionRepository.findById(id)
                .flatMap(existingAction -> {
                    existingAction.setType(updatedAction.getType());
                    existingAction.setParams(updatedAction.getParams());
                    return actionRepository.save(existingAction);
                });
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
        return ruleService.getByActionId(action.getId())
                .collectList()
                .map(action::setRules)
                .onErrorResume(ex -> Mono.error(new RuntimeException("Failed to fetch rules", ex)));

    }

}
