package com.example.alarms.controllers;

import com.example.alarms.dto.AccountDTO;
import com.example.alarms.services.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public Mono<ResponseEntity<AccountDTO>> createAccount(@RequestBody AccountDTO accountDTO) {
        return accountService.createAccount(accountDTO)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public Flux<AccountDTO> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<AccountDTO>> getAccountById(@PathVariable String id) {
        return accountService.getAccountById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<AccountDTO>> updateAccount(
            @PathVariable String id,
            @RequestBody AccountDTO accountDTO) {
        return accountService.updateAccount(id, accountDTO)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteAccount(@PathVariable String id) {
        return accountService.deleteAccount(id)
                .map(deleted -> ResponseEntity.noContent().build());
    }
}