package com.example.alarms.services;

import com.example.alarms.dto.JsonUtils;
import com.example.alarms.dto.RuleDTO;
import com.example.alarms.entities.ReactionEntity;
import com.example.alarms.entities.RuleEntity;
import com.example.alarms.repositories.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@RequiredArgsConstructor
@Service
public class RuleService {

    private final RuleRepository ruleRepository;
    private final ReactionService reactionService;

    public  Flux<RuleEntity> getByActionId(Long actionID){
        return ruleRepository.findByActionId(actionID)
                .flatMap(ruleEntity -> reactionService.findByRuleId(ruleEntity.getId())
                        .collectList()
                        .map(reactions -> {
                            ruleEntity.setReactions(new ArrayList<>(reactions));
                            return ruleEntity;
                        }))
                .onErrorResume(ex -> Mono.error(new RuntimeException("Failed to fetch rule", ex)));

    }


    /**
     * Fetch all rules.
     *
     * @return Flux of RuleEntity
     */
    public Flux<RuleEntity> getAllRules() {
        return ruleRepository.findAll();
    }

    /**
     * Fetch a rule by its ID.
     *
     * @param id Rule ID
     * @return Mono of RuleEntity
     */
    public Mono<RuleEntity> getRuleById(Long id) {
        return ruleRepository.findById(id);
    }

    /**
     * Create a new rule.
     *
     * @param ruleDTO RuleDTO to be created
     * @return Mono of RuleEntity
     */
    public Mono<RuleEntity> create(RuleDTO ruleDTO, Long actionId) {

        return Mono.fromCallable(() -> JsonUtils.toJson(ruleDTO.getRule()))
                .flatMap(ruleJson -> ruleRepository.save(new RuleEntity(null, ruleDTO.getName(), ruleJson, actionId, null, null, null)))
                .flatMap(savedRule -> saveReactions(savedRule, ruleDTO)
                    .thenReturn(savedRule)
                    .onErrorResume(ex -> reactionService.deleteByRuleId(savedRule.getId())
                            .then(Mono.error(new RuntimeException("Failed to save reactions", ex)))

                ));
    }

    private Mono<Void> saveReactions(RuleEntity savedRule, RuleDTO ruleDTO) {
        if (ruleDTO.getReactions().isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(ruleDTO.getReactions())
                .flatMap(reactionDTO -> Mono.fromCallable(() -> JsonUtils.toJson(reactionDTO.getParams()))
                        .map(reactionJson -> new ReactionEntity(null, reactionDTO.getName(), reactionJson, savedRule.getId(), null, null))
                )
                .collectList()
                .flatMap(reactionEntities -> reactionService.saveAll(reactionEntities).collectList())
                .map(savedReactions -> {
                    savedRule.setReactions(savedReactions);
                    return savedRule;
                })
                .then()
                .onErrorResume(ex -> Mono.error(new RuntimeException(ex)));
    }

    /**
     * Update an existing rule.
     *
     * @param id Rule ID
     * @param updatedRule Updated RuleEntity
     * @return Mono of RuleEntity
     */
    public Mono<RuleEntity> updateRule(Long id, RuleEntity updatedRule) {
        return ruleRepository.findById(id)
                .flatMap(existingRule -> {
                    existingRule.setName(updatedRule.getName());
                    existingRule.setRule(updatedRule.getRule());
                    existingRule.setActionId(updatedRule.getActionId());
                    return ruleRepository.save(existingRule);
                });
    }

    /**
     * Delete a rule by its ID.
     *
     * @param id Rule ID
     * @return Mono<Void>
     */
    public Mono<Void> deleteRule(Long id) {
        return ruleRepository.deleteById(id);
    }

    public Mono<Void> deleteByActionId(Long actionId) {
        return ruleRepository.findByActionId(actionId)
                .flatMap(ruleEntity ->
                        reactionService.deleteByRuleId(ruleEntity.getId()) // Delete reactions by rule ID
                )
                .then(ruleRepository.deleteByActionId(actionId))
                .onErrorResume(ex -> Mono.error(new RuntimeException(ex)));

    }
}
