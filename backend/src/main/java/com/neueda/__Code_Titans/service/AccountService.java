package com.neueda.__Code_Titans.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.neueda.__Code_Titans.entity.Accounts;
import com.neueda.__Code_Titans.repo.AccountRepo;

@Service
public class AccountService {

    private final AccountRepo accountRepo;

    public AccountService(AccountRepo accountRepo) {
        this.accountRepo = accountRepo;
    }

    // Get all accounts
    public List<Accounts> getAllAccounts() {
        return accountRepo.findAll();
    }

    // Get one account by ID
    public Optional<Accounts> getAccountById(Long accountId) {
        return accountRepo.findById(accountId);
    }

    // Create a new account
    public Accounts createAccount(Accounts account) {
        return accountRepo.save(account);
    }
}

