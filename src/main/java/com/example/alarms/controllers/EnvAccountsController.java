package com.example.alarms.controllers;

import com.example.alarms.dto.EwsAccounts;
import com.example.alarms.services.EnvService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EnvAccountsController {

    private final EnvService accountService;

    public EnvAccountsController(EnvService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/envaccounts")
    public List<EwsAccounts> getMailAccounts() {
        return accountService.getAccounts();
    }
}
