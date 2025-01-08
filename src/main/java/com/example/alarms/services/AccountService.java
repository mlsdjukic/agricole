package com.example.alarms.services;

import com.example.alarms.dto.AccountDTO;
import com.example.alarms.dto.AccountMapper;
import com.example.alarms.entities.AccountEntity;
import com.example.alarms.repositories.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private  final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    public Mono<AccountDTO> createAccount(AccountDTO accountDTO) {
        AccountEntity accountEntity = accountMapper.toEntity(accountDTO);
        accountEntity.setPassword("{bcrypt}" + passwordEncoder.encode(accountDTO.getPassword()));
        return accountRepository.save(accountEntity)
                .map(accountMapper::toDTO);
    }

    public Flux<AccountDTO> getAllAccounts() {
        return accountRepository.findAll()
                .map(accountMapper::toDTO);
    }

    public Mono<AccountDTO> getAccountById(String id) {
        return accountRepository.findById(id)
                .map(accountMapper::toDTO);

    }

    public Mono<AccountDTO> updateAccount(String id, AccountDTO updatedDTO) {
        return accountRepository.findById(id)
                .flatMap(existingAccount -> {
                    existingAccount.setUsername(updatedDTO.getUsername());
                    existingAccount.setPassword("{bcrypt}" + passwordEncoder.encode(updatedDTO.getPassword()));
                    return accountRepository.save(existingAccount);
                })
                .map(accountMapper::toDTO);
    }

    public Mono<Void> deleteAccount(String id) {
        return accountRepository.deleteById(id);
    }
}
