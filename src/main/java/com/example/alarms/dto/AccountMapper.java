package com.example.alarms.dto;

import com.example.alarms.entities.AccountEntity;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {

    public AccountEntity toEntity(Account dto) {
        AccountEntity entity = new AccountEntity();
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        return entity;
    }

    public Account toDTO(AccountEntity entity) {
        Account dto = new Account();
        dto.setUsername(entity.getUsername());
        dto.setPassword(entity.getPassword());
        return dto;
    }
}
