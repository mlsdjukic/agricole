package com.example.alarms.services;

import com.example.alarms.dto.ActionDTO;
import com.example.alarms.dto.JsonUtils;
import com.example.alarms.dto.RuleDTO;
import com.example.alarms.entities.ActionEntity;
import com.example.alarms.entities.ReactionEntity;
import com.example.alarms.entities.RuleEntity;
import com.example.alarms.repositories.ActionRepository;
import com.example.alarms.repositories.ReactionRepository;
import com.example.alarms.repositories.RuleRepository;
import com.example.alarms.rules.Rule;
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
    private final RuleRepository ruleRepository;
    private final ReactionRepository reactionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Flux<ActionEntity> findActionsInRange(int limit, int offset) {
        return actionRepository.findActionsInRange(limit, offset)
            .flatMap(action ->
                    ruleRepository.findByActionId(action.getId())
                            .flatMap(ruleEntity -> {
                                return reactionRepository.findByRuleId(ruleEntity.getId())
                                        .collectList()
                                        .map(reactions -> {
                                            ruleEntity.setReactions(new ArrayList<>(reactions));
                                            return ruleEntity;
                                        });
                            })
                            .collectList()
                            .map(rules -> {
                                action.setRules(new ArrayList<>(rules));
                                return action;
                            })
            );
    }

    public Mono<ActionEntity> create(ActionDTO action, Long userId) {
        return Mono.fromCallable(() -> JsonUtils.toJson(action.getParams()))
                .flatMap(jsonParams -> actionRepository.save(new ActionEntity(null, action.getType(), jsonParams, userId, null, null, null)))
                .flatMap(savedAction -> Flux.fromIterable(action.getRules())
                        .flatMap(ruleDTO -> Mono.fromCallable(() -> JsonUtils.toJson(ruleDTO.getRule()))
                                .flatMap(ruleJson -> ruleRepository.save(new RuleEntity(null, ruleDTO.getName(), ruleJson, savedAction.getId(), null, null, null)))
                                .flatMap(savedRule -> Flux.fromIterable(ruleDTO.getReactions())
                                        .flatMap(reactionDTO -> Mono.fromCallable(() -> JsonUtils.toJson(reactionDTO.getParams()))
                                                .map(reactionJson -> new ReactionEntity(null, reactionDTO.getName(), reactionJson, savedRule.getId(), null, null))
                                        )
                                        .collectList()
                                        .flatMap(reactionEntities -> reactionRepository.saveAll(reactionEntities).collectList()) // ✅ Fix: Collect saved entities into a Mono
                                        .map(savedReactions -> {
                                            savedRule.setReactions(savedReactions);
                                            return savedRule;
                                        })
                                ))
                        .collectList()
                        .map(rules -> {
                            savedAction.setRules(rules);
                            return savedAction;
                        })
//                        .thenReturn(savedAction)
                )
                .onErrorMap(JsonProcessingException.class, e -> new RuntimeException("Error serializing action parameters", e))
                .onErrorMap(RuntimeException.class, e -> new RuntimeException("Error serializing rule or reaction", e));
    }


//    public Mono<ActionEntity> update(ActionDTO action) {
//
//        try {
//            String jsonParams = objectMapper.writeValueAsString(action.getParams());
//
//            // Find the existing action by ID
//            return actionRepository.findById(action.getId())
//                    .flatMap(existingAction -> {
//                        // Serialize the action parameters
//
//                        // Update the action entity with new parameters
//                        existingAction.setType(action.getType());
//                        existingAction.setParams(jsonParams);
//
//                        // Save the updated action
//                        return actionRepository.save(existingAction)
//                                .flatMap(savedAction -> {
//
//                                    ruleRepository.findByActionId(savedAction.getId())
//                                            .flatMap(ruleEntity -> {
//                                                return reactionRepository.deleteById(ruleEntity.getActionId());
//                                            });
//                                    // Remove existing rules for the action
//                                    return ruleRepository.deleteByActionId(savedAction.getId())
//                                            .then(Mono.just(savedAction));
//                                })
//                                .flatMap(savedAction -> Flux.fromIterable(action.getRules())
//                                        .flatMap(ruleDTO -> Mono.fromCallable(() -> JsonUtils.toJson(ruleDTO.getRule()))
//                                                .flatMap(ruleJson -> ruleRepository.save(new RuleEntity(null, ruleDTO.getName(), ruleJson, savedAction.getId(), null, null, null)))
//                                                .flatMap(savedRule -> Flux.fromIterable(ruleDTO.getReactions())
//                                                        .flatMap(reactionDTO -> Mono.fromCallable(() -> JsonUtils.toJson(reactionDTO.getReaction()))
//                                                                .map(reactionJson -> new ReactionEntity(null, reactionDTO.getName(), reactionJson, savedRule.getId(), null, null))
//                                                        )
//                                                        .collectList()
//                                                        .flatMap(reactionEntities -> reactionRepository.saveAll(reactionEntities).collectList()) // ✅ Fix: Collect saved entities into a Mono
//                                                        .map(savedReactions -> {
//                                                            savedRule.setReactions(savedReactions);
//                                                            return savedRule;
//                                                        })
//                                                ))
//                                        .collectList()
//                                        .thenReturn(savedAction)
//                                )
//                                ;
//                    })
//                    .switchIfEmpty(Mono.error(new RuntimeException("Action not found")));
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException("Error serializing action parameters", e);
//        }
//    }

//    // Helper method to update reactions
//    private Mono<Void> updateReactions(List<RuleEntity> updatedRules, ActionDTO actionDTO) {
//        return Flux.fromIterable(updatedRules)
//                .flatMap(updatedRule -> {
//                    List<ReactionDTO> updatedReactions = actionDTO.getRules().stream()
//                            .filter(ruleDTO -> ruleDTO.getId() != null && ruleDTO.getId().equals(updatedRule.getId()))
//                            .flatMap(ruleDTO -> ruleDTO.getReactions().stream())
//                            .toList();
//
//                    return reactionRepository.findByRuleId(updatedRule.getId())
//                            .collectList()
//                            .flatMap(existingReactions -> {
//                                Map<Long, ReactionDTO> updatedReactionsMap = updatedReactions.stream()
//                                        .filter(reactionDTO -> reactionDTO.getId() != null)
//                                        .collect(Collectors.toMap(ReactionDTO::getId, reactionDTO -> reactionDTO));
//
//                                // Delete removed reactions
//                                List<Long> updatedReactionIds = updatedReactionsMap.keySet().stream().toList();
//                                List<ReactionEntity> reactionsToDelete = existingReactions.stream()
//                                        .filter(reaction -> !updatedReactionIds.contains(reaction.getId()))
//                                        .toList();
//
//                                // Prepare reactions to update or create
//                                List<Mono<ReactionEntity>> reactionMonos = updatedReactions.stream()
//                                        .map(reactionDTO -> {
//                                            Mono<ReactionEntity> reactionMono;
//                                            if (reactionDTO.getId() != null) {
//                                                reactionMono = reactionRepository.findById(reactionDTO.getId())
//                                                        .switchIfEmpty(Mono.error(new RuntimeException("Reaction not found")))
//                                                        .flatMap(existingReaction -> {
//                                                            existingReaction.setName(reactionDTO.getName());
//                                                            return Mono.fromCallable(() -> JsonUtils.toJson(reactionDTO.getReaction()))
//                                                                    .flatMap(jsonReaction -> {
//                                                                        existingReaction.setReaction(jsonReaction);
//                                                                        return reactionRepository.save(existingReaction);
//                                                                    });
//                                                        });
//                                            } else {
//                                                // Create new reaction
//                                                reactionMono = Mono.fromCallable(() -> JsonUtils.toJson(reactionDTO.getReaction()))
//                                                        .flatMap(jsonReaction -> reactionRepository.save(
//                                                                new ReactionEntity(null, reactionDTO.getName(), jsonReaction, updatedRule.getId(), null, null)
//                                                        ));
//                                            }
//                                            return reactionMono;
//                                        })
//                                        .toList();
//
//                                // Delete removed reactions and process updates
//                                return reactionRepository.deleteAll(reactionsToDelete)
//                                        .thenMany(Flux.merge(reactionMonos))
//                                        .then();
//                            });
//                })
//                .then();
//    }


    public Mono<ActionEntity> update(ActionDTO action) {
        try {
            String jsonParams = objectMapper.writeValueAsString(action.getParams());

            // Find the existing action by ID
            return actionRepository.findById(action.getId())
                    .flatMap(existingAction -> updateActionAndSave(existingAction, action, jsonParams))
                    .switchIfEmpty(Mono.error(new RuntimeException("Action not found")));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing action parameters", e);
        }
    }

    private Mono<ActionEntity> updateActionAndSave(ActionEntity existingAction, ActionDTO action, String jsonParams) {
        // Update the action entity with new parameters
        existingAction.setType(action.getType());
        existingAction.setParams(jsonParams);

        // Save the updated action
        return actionRepository.save(existingAction)
                .flatMap(savedAction -> updateRulesAndReactions(savedAction, action));
    }

    private Mono<ActionEntity> updateRulesAndReactions(ActionEntity savedAction, ActionDTO action) {
        // First, delete existing reactions associated with the rules
        return deleteExistingReactions(savedAction)
                // Then, delete the rules for the action
                .then(ruleRepository.deleteByActionId(savedAction.getId()))
                .then(saveNewRules(savedAction, action))
                .thenReturn(savedAction);
    }

    private Mono<ActionEntity> deleteRulesAndReactions(ActionEntity savedAction) {
        // First, delete existing reactions associated with the rules
        return deleteExistingReactions(savedAction)
                // Then, delete the rules for the action
                .then(ruleRepository.deleteByActionId(savedAction.getId()))
                .thenReturn(savedAction);
    }

    private Mono<Void> deleteExistingReactions(ActionEntity savedAction) {
        // Find the rules related to the action and delete reactions for each rule
        return ruleRepository.findByActionId(savedAction.getId())
                .flatMap(ruleEntity ->
                        reactionRepository.deleteByRuleId(ruleEntity.getId()) // Delete reactions by rule ID
                )
                .then();
    }

    private Mono<Void> saveNewRules(ActionEntity savedAction, ActionDTO action) {
        return Flux.fromIterable(action.getRules())
                .flatMap(ruleDTO -> saveRuleAndReactions(savedAction, ruleDTO))
                .collectList() // Collect into a List<RuleEntity>
                .doOnNext(savedAction::setRules) // Set rules to ActionEntity
                .then(); // Return Mono<Void>
    }

    private Mono<RuleEntity> saveRuleAndReactions(ActionEntity savedAction, RuleDTO ruleDTO) {
        return Mono.fromCallable(() -> JsonUtils.toJson(ruleDTO.getRule()))
                .flatMap(ruleJson -> ruleRepository.save(new RuleEntity(null, ruleDTO.getName(), ruleJson, savedAction.getId(), null, null, null)))
                .flatMap(savedRule -> saveReactions(savedRule, ruleDTO)
                .thenReturn(savedRule));
    }

    private Mono<Void> saveReactions(RuleEntity savedRule, RuleDTO ruleDTO) {
        return Flux.fromIterable(ruleDTO.getReactions())
                .flatMap(reactionDTO -> Mono.fromCallable(() -> JsonUtils.toJson(reactionDTO.getParams()))
                        .map(reactionJson -> new ReactionEntity(null, reactionDTO.getName(), reactionJson, savedRule.getId(), null, null))
                )
                .collectList()
                .flatMap(reactionEntities -> reactionRepository.saveAll(reactionEntities).collectList())
                .map(savedReactions -> {
                    savedRule.setReactions(savedReactions);
                    return savedRule;
                })
                .then();
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
        return actionRepository.findById(id)
                .flatMap(this::deleteRulesAndReactions)
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
