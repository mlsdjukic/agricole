package com.example.alarms.services;

import com.example.alarms.dto.JsonUtils;
import com.example.alarms.dto.Rule;
import com.example.alarms.dto.RuleMapper;
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
    private final RuleMapper ruleMapper;

    /**
     * Create a new rule.
     *
     * @param rule Rule to be created
     * @return Mono of RuleEntity
     */
    public RuleEntity create(Rule rule, Long actionId) {

        RuleEntity entity = ruleMapper.toEntity(rule);
        return ruleRepository.save(entity);
    }

    public RuleEntity update(Long id, Rule updatedDTO) {
        return ruleRepository.findById(id)
                .map(existing -> {
                    RuleEntity updated = ruleMapper.toEntity(updatedDTO);
                    updated.setId(id); // Ensure the ID is preserved
                    return ruleRepository.save(updated);

                })
                .orElseThrow(() -> new IllegalArgumentException("Rule with id " + id + " not found"));
    }

    public void deleteById(Long id) {
        ruleRepository.deleteById(id);
    }

}
