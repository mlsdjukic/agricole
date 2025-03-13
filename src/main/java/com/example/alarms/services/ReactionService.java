package com.example.alarms.services;


import com.example.alarms.entities.ReactionEntity;
import com.example.alarms.repositories.ReactionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Transactional
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public ReactionService(ReactionRepository reactionRepository, ModelMapper modelMapper) {
        this.reactionRepository = reactionRepository;
        this.modelMapper = modelMapper;
    }

    public Flux<ReactionEntity> saveAll(List<ReactionEntity> reactionEntities) {
        return reactionRepository.saveAll(reactionEntities)
                .onErrorResume(ex -> Flux.error(new RuntimeException("Error saving reactions", ex))  // Propagate the error up
                );
    }

    public Flux<ReactionEntity> findByRuleId(Long ruleId) {
        return reactionRepository.findByRuleId(ruleId)
                .onErrorResume(ex -> Mono.error(new IllegalArgumentException("No rule with this id", ex)));
    }

    public Mono<Void> deleteByRuleId(Long ruleId) {
        return reactionRepository.deleteByRuleId(ruleId)
                .onErrorResume(ex -> Mono.error(new IllegalArgumentException("No rule with this id", ex)));

    }
}
