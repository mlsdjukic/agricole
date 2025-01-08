package com.example.alarms.repositories;

import com.example.alarms.entities.AccountEntity;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserDetailsRepository extends ReactiveSortingRepository<AccountEntity, String> {
    Mono<AccountEntity> findByUsername(String username);
}
