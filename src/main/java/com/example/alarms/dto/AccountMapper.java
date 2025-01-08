package com.example.alarms.dto;

import com.example.alarms.entities.AccountEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountEntity toEntity(AccountDTO dto) {
        AccountEntity entity = new AccountEntity();
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        return entity;
    }

    public AccountDTO toDTO(AccountEntity entity) {
        AccountDTO dto = new AccountDTO();
        dto.setUsername(entity.getUsername());
        dto.setPassword(entity.getPassword());
        return dto;
    }
}
