package com.example.alarms.services;

import com.example.alarms.dto.ActionDTO;
import com.example.alarms.entities.ActionEntity;
import com.example.alarms.entities.RuleEntity;
import com.example.alarms.repositories.ActionRepository;
import com.example.alarms.repositories.RuleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ActionService {

    private final ActionRepository actionRepository;
    private final RuleRepository ruleRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Flux<ActionEntity> findActionsInRange(int limit, int offset) {
        return actionRepository.findActionsInRange(limit, offset)
            .flatMap(action ->
                    ruleRepository.findByActionId(action.getId())
                            .collectList()
                            .map(rules -> {
                                action.setRules(new ArrayList<>(rules));
                                return action;
                            })
            );
    }

//    @Transactional
//    public Mono<ActionEntity> create(ActionDTO action) {
//
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//
//            String json = objectMapper.writeValueAsString(action.getParams());
//            return  // Save the new item
//
//                    actionRepository.save(new ActionEntity(null, action.getType(), json, null, null,null))
//                            // Save the links to the tags
//                            .flatMap(savedAction -> {
//
//                                List<RuleEntity > rules = action.getRules().stream()
//                                        .map(rule -> {
//                                            try {
//                                                String ruleJson = objectMapper.writeValueAsString(rule.getRule());
//                                                return new RuleEntity(null, rule.getName(), ruleJson, savedAction.getId(), null, null);
//                                            } catch (JsonProcessingException e) {
//                                                return null;
//                                            }
//
//                                        })
//                                        .toList();
//
//                                return ruleRepository.saveAll(rules)
//                                        .collectList()
//                                        .map(savedRules -> {
//                                            // Attach the saved rules to the action and return it
//                                            savedAction.setRules(savedRules);
//                                            return savedAction;
//                                        });
//                            });
//
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//
//    }

    public Mono<ActionEntity> createWithRules(ActionDTO action) {
        try {
            // Serialize the action parameters
            String jsonParams = objectMapper.writeValueAsString(action.getParams());

            // Save the new action
            return actionRepository.save(new ActionEntity(null, action.getType(), jsonParams, null, null, null))
                    .flatMap(savedAction -> {
                        // Map ActionDTO rules to RuleEntity
                        List<RuleEntity> rules = action.getRules().stream()
                                .map(rule -> {
                                    try {
                                        String ruleJson = objectMapper.writeValueAsString(rule.getRule());
                                        return new RuleEntity(null, rule.getName(), ruleJson, savedAction.getId(), null, null);
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException("Error serializing rule", e);
                                    }
                                })
                                .collect(Collectors.toList());

                        // Save rules and attach them to the action
                        return ruleRepository.saveAll(rules)
                                .collectList()
                                .map(savedAction::setRules);
                    });

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing action parameters", e);
        }
    }

    public Mono<ActionEntity> update(ActionDTO action) {

        try {
            String jsonParams = objectMapper.writeValueAsString(action.getParams());

            // Find the existing action by ID
            return actionRepository.findById(action.getId())
                    .flatMap(existingAction -> {
                        // Serialize the action parameters

                        // Update the action entity with new parameters
                        existingAction.setType(action.getType());
                        existingAction.setParams(jsonParams);

                        // Save the updated action
                        return actionRepository.save(existingAction)
                                .flatMap(savedAction -> {
                                    // Remove existing rules for the action
                                    return ruleRepository.deleteByActionId(savedAction.getId())
                                            .then(Mono.just(savedAction));
                                })
                                .flatMap(updatedAction -> {
                                    // Map ActionDTO rules to RuleEntity
                                    List<RuleEntity> rules = action.getRules().stream()
                                            .map(rule -> {
                                                try {
                                                    String ruleJson = objectMapper.writeValueAsString(rule.getRule());
                                                    return new RuleEntity(null, rule.getName(), ruleJson, updatedAction.getId(), null, null);
                                                } catch (JsonProcessingException e) {
                                                    throw new RuntimeException("Error serializing rule", e);
                                                }
                                            })
                                            .collect(Collectors.toList());

                                    // Save the new rules and attach them to the updated action
                                    return ruleRepository.saveAll(rules)
                                            .collectList()
                                            .map(updatedAction::setRules);
                                });
                    })
                    .switchIfEmpty(Mono.error(new RuntimeException("Action not found")));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing action parameters", e);
        }
    }

    public Mono<Void> delete(Long actionId) {
        // Check if the action ID is null or empty and return Bad Request if so
        if (actionId == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action ID cannot be null or empty"));
        }

        // Find the action by ID
        return actionRepository.findById(actionId)
                .flatMap(existingAction -> {
                    // Remove existing rules for the action
                    return ruleRepository.deleteByActionId(existingAction.getId())
                            .then(actionRepository.delete(existingAction))
                            .then(Mono.empty()); // Return empty Mono when delete is successful
                });
    }


    /**
     * Fetch all actions.
     *
     * @return Flux of ActionEntity
     */
    public Flux<ActionEntity> getAllActions() {
        return actionRepository.findAll()
                .flatMap(this::attachRulesToAction);
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
        return ruleRepository.deleteByActionId(id)
                .then(actionRepository.deleteById(id));
    }

    /**
     * Attach rules to an ActionEntity based on actionId.
     *
     * @param action ActionEntity
     * @return Mono of ActionEntity with rules
     */
    private Mono<ActionEntity> attachRulesToAction(ActionEntity action) {
        return ruleRepository.findByActionId(action.getId())
                .collectList()
                .map(action::setRules);
    }

}
