package com.example.alarms.services;


import com.example.alarms.dto.JsonUtils;
import com.example.alarms.dto.ReactionDTO;
import com.example.alarms.entities.ReactionEntity;
import com.example.alarms.repositories.ReactionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

    public Flux<ReactionDTO> getAllReactions() {
        return reactionRepository.findAll()
                .map(this::convertToDTO);
    }

    public Mono<ReactionDTO> getReactionById(Long id) {
        return reactionRepository.findById(id)
                .map(this::convertToDTO);
    }

    public Mono<ReactionDTO> createReaction(ReactionDTO reactionDTO) {
        ReactionEntity entity = convertToEntity(reactionDTO);
        return reactionRepository.save(entity)
                .map(this::convertToDTO);
    }

    public Mono<ReactionDTO> updateReaction(Long id, ReactionDTO reactionDTO) {
        return reactionRepository.findById(id)
                .flatMap(existing -> {
                    existing.setName(reactionDTO.getName());
                    existing.setParams(JsonUtils.toJson(reactionDTO.getParams())); // Convert Map to JSON
                    return reactionRepository.save(existing);
                })
                .map(this::convertToDTO);
    }

    public Mono<Void> deleteReaction(Long id) {
        return reactionRepository.deleteById(id);
    }

    private ReactionDTO convertToDTO(ReactionEntity entity) {
        ReactionDTO dto = modelMapper.map(entity, ReactionDTO.class);
        dto.setParams(JsonUtils.fromJson(entity.getParams())); // Convert JSON to Map
        return dto;
    }

    private ReactionEntity convertToEntity(ReactionDTO dto) {
        ReactionEntity entity = modelMapper.map(dto, ReactionEntity.class);
        entity.setParams(JsonUtils.toJson(dto.getParams())); // Convert Map to JSON
        return entity;
    }
}
