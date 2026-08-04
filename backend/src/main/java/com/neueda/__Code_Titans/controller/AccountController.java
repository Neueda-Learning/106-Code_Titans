package com.neueda.__Code_Titans.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.__Code_Titans.entity.Accounts;
import com.neueda.__Code_Titans.service.AccountService;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // GET /accounts - returns all accounts
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAccounts() {
        List<Accounts> accounts = accountService.getAllAccounts();
        return buildResponse(HttpStatus.OK, "Accounts fetched successfully", accounts);
    }

    // GET /accounts/{id} - returns one account by ID
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAccountById(@PathVariable Long id) {
        Optional<Accounts> account = accountService.getAccountById(id);
        if (account.isPresent()) {
            return buildResponse(HttpStatus.OK, "Account fetched successfully", account.get());
        }
        return buildResponse(HttpStatus.NOT_FOUND, "Account not found", null);
    }

    // POST /accounts - create a new account
    @PostMapping
    public ResponseEntity<Map<String, Object>> createAccount(@RequestBody Accounts account) {
        Accounts savedAccount = accountService.createAccount(account);
        return buildResponse(HttpStatus.CREATED, "Account created successfully", savedAccount);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        body.put("data", data);
        return ResponseEntity.status(status).body(body);
    }
}
