package com.example.alarms.services;


import com.example.alarms.dto.Reaction;
import com.example.alarms.dto.ReactionMapper;
import com.example.alarms.entities.ReactionEntity;
import com.example.alarms.repositories.ReactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final ReactionMapper reactionMapper;

    @Autowired
    public ReactionService(ReactionRepository reactionRepository, ReactionMapper reactionMapper) {
        this.reactionRepository = reactionRepository;
        this.reactionMapper = reactionMapper;
    }

    public List<Reaction> findAll() {
        return reactionRepository.findAll().stream()
                .map(reactionMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<Reaction> findById(Long id) {
        return reactionRepository.findById(id)
                .map(reactionMapper::toDTO);
    }

    public Reaction save(Reaction reactionDTO) {
        ReactionEntity entity = reactionMapper.toEntity(reactionDTO);
        ReactionEntity saved = reactionRepository.save(entity);
        return reactionMapper.toDTO(saved);
    }

    public Reaction update(Long id, Reaction updatedDTO) {
        return reactionRepository.findById(id)
                .map(existing -> {
                    ReactionEntity updated = reactionMapper.toEntity(updatedDTO);
                    updated.setId(id); // Keep original ID
                    ReactionEntity saved = reactionRepository.save(updated);
                    return reactionMapper.toDTO(saved);
                })
                .orElseThrow(() -> new IllegalArgumentException("Reaction with id " + id + " not found"));
    }

    public void deleteById(Long id) {
        reactionRepository.deleteById(id);
    }

}
