package com.example.alarms.services;

import com.example.alarms.entities.AccountEntity;
import com.example.alarms.entities.security.SecurityAccount;
import com.example.alarms.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserDetailsServiceImpl implements ReactiveUserDetailsService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        Mono<AccountEntity> account = accountRepository.findByUsername(username);

        // Convert AccountEntity to UserDetails
        return account.map(SecurityAccount::new);
    }
}