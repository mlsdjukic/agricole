package com.example.alarms.services;

import com.example.alarms.dto.Account;
import com.example.alarms.dto.AccountMapper;
import com.example.alarms.entities.AccountEntity;
import com.example.alarms.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountMapper accountMapper;

    public Account createAccount(Account account) {
        AccountEntity accountEntity = accountMapper.toEntity(account);
        accountEntity.setPassword(passwordEncoder.encode(account.getPassword()));
        AccountEntity saved = accountRepository.save(accountEntity);
        return accountMapper.toDTO(saved);
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(accountMapper::toDTO)
                .toList();
    }

    public Account getAccountById(String id) {
        Optional<AccountEntity> entityOpt = accountRepository.findById(id);
        return entityOpt.map(accountMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + id));
    }

    public Account updateAccount(String id, Account updatedDTO) {
        AccountEntity existing = accountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + id));

        existing.setUsername(updatedDTO.getUsername());
        existing.setPassword("{bcrypt}" + passwordEncoder.encode(updatedDTO.getPassword()));
        AccountEntity updated = accountRepository.save(existing);
        return accountMapper.toDTO(updated);
    }

    public void deleteAccount(String id) {
        accountRepository.deleteById(id);
    }
}
