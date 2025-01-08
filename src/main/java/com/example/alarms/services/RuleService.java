package com.example.alarms.services;

import com.example.alarms.entities.RuleEntity;
import com.example.alarms.repositories.RuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class RuleService {

    private final RuleRepository ruleRepository;

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
     * @param ruleEntity RuleEntity to be created
     * @return Mono of RuleEntity
     */
    public Mono<RuleEntity> createRule(RuleEntity ruleEntity) {
        return ruleRepository.save(ruleEntity);
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
}
