package com.example.alarms.services;

import com.example.alarms.dto.Account;
import com.example.alarms.dto.EwsAccounts;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EnvService {

    @Autowired
    private AccountService accountService;

    private final List<EwsAccountDetails> accounts = new ArrayList<>();

    @PostConstruct
    public void loadAccountsFromEnv() {
        Map<String, String> env = System.getenv();
        List<String> accountKeys = env.keySet().stream()
                .filter(key -> key.startsWith("ACCOUNT_"))
                .sorted()
                .toList();

        for (String key : accountKeys) {
            String value = env.get(key); // Format: type|url|username|password
            if (value != null) {
                String[] parts = value.split("\\|");
                if (parts.length >= 4) {
                    String type = parts[0];
                    String url = parts[1];
                    String username = parts[2];
                    String password = parts[3];
                    accounts.add(new EwsAccountDetails(type, url, username, password));
                }
            }
        }
    }


    // Mapper from internal to DTO without password
    private EwsAccounts toDto(EwsAccountDetails account) {
        return new EwsAccounts(account.getType(), account.getUrl(), account.getUsername());
    }

    public List<EwsAccounts> getAccounts() {
        return accounts.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Optional: Access internal accounts with passwords if needed
    public List<EwsAccountDetails> getInternalAccounts() {
        return Collections.unmodifiableList(accounts);
    }

    public Optional<EwsAccountDetails> findByUrlAndUsername(String url, String username) {
        return accounts.stream()
                .filter(acc -> acc.getUrl().equals(url) && acc.getUsername().equals(username))
                .findFirst();
    }

    @Getter
    @Setter
    public static class EwsAccountDetails  {
        private String type;
        private String url;
        private String username;
        private String password;

        public EwsAccountDetails(String type, String url, String username, String password) {
            this.type = type;
            this.url = url;
            this.username = username;
            this.password = password;
        }
    }

    @PostConstruct
    public void initUserAccount() {
        Account existingAccount = null;
        Map<String, String> env = System.getenv();
        String adminValue = env.get("ADMIN_ACCOUNT");

        if (adminValue == null || adminValue.isBlank()) {
            log.warn("No ADMIN_ACCOUNT environment variable found.");
            return;
        }

        String[] parts = adminValue.split("\\|");
        if (parts.length < 2) {
            log.error("ADMIN_ACCOUNT env var has invalid format.");
            return;
        }
        String username = parts[0];
        String password = parts[1];

        // Check if admin account already exists
        try {
            existingAccount = accountService.getAccountByUsername(username).block();
        } catch (Exception e) {
            log.error("Failed to fetch admin account from db");
        }

        if (existingAccount == null){
            accountService.createAccount(new Account(username,password)).subscribe();
        }
    }
}
