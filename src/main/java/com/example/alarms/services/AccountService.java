package com.example.alarms.services;

import com.example.alarms.dto.Account;
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

    public Mono<Account> createAccount(Account account) {
        AccountEntity accountEntity = accountMapper.toEntity(account);
        accountEntity.setPassword("{bcrypt}" + passwordEncoder.encode(account.getPassword()));
        return accountRepository.save(accountEntity)
                .map(accountMapper::toDTO);
    }

    public Flux<Account> getAllAccounts() {
        return accountRepository.findAll()
                .map(accountMapper::toDTO);
    }

    public Mono<Account> getAccountById(String id) {
        return accountRepository.findById(id)
                .map(accountMapper::toDTO);

    }

    public Mono<Account> updateAccount(String id, Account updatedDTO) {
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
