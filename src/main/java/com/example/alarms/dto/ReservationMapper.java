package com.example.alarms.dto;

import com.example.alarms.entities.ReservationEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    @Autowired
    private ActionMapper actionMapper;

    public ReservationEntity toEntity(Reservation dto) {
        if (dto == null) return null;

        ReservationEntity entity = new ReservationEntity();
        entity.setId(dto.getId());
        entity.setStatus(dto.getStatus());
        entity.setLockedBy(dto.getLockedBy());
        entity.setCreatedDate(dto.getCreatedDate());
        entity.setLockedAt(dto.getLockedAt());

        if (dto.getAction() != null) {
            entity.setAction(actionMapper.toEntity(dto.getAction()));
        }

        return entity;
    }

    public Reservation toDto(ReservationEntity entity) {
        if (entity == null) return null;

        Reservation dto = new Reservation();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setLockedBy(entity.getLockedBy());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLockedAt(entity.getLockedAt());

        if (entity.getAction() != null) {
            dto.setAction(actionMapper.toDto(entity.getAction()));
        }

        return dto;
    }
}

