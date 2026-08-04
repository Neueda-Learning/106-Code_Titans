package com.neueda.__Code_Titans.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.neueda.__Code_Titans.entity.Accounts;
import com.neueda.__Code_Titans.service.AccountService;

@ExtendWith(MockitoExtension.class)
public class AccountCotrollerTest {

    @Mock
    private AccountService accountService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(accountService)).build();
    }

    @Test
    void getAllAccounts_returnsOkWithAccountList() throws Exception {
        Accounts account = new Accounts();
        account.setAccountId(1L);
        account.setAccountNumber("ACC-1001");
        account.setAccountHolderName("Rahul");
        account.setBalance(new BigDecimal("1000.00"));

        when(accountService.getAllAccounts()).thenReturn(List.of(account));

        mockMvc.perform(get("/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Accounts fetched successfully"))
                .andExpect(jsonPath("$.data[0].accountId").value(1))
                .andExpect(jsonPath("$.data[0].accountNumber").value("ACC-1001"));
    }

    @Test
    void getAccountById_whenAccountExists_returnsOkWithAccount() throws Exception {
        Accounts account = new Accounts();
        account.setAccountId(2L);
        account.setAccountNumber("ACC-1002");

        when(accountService.getAccountById(2L)).thenReturn(Optional.of(account));

        mockMvc.perform(get("/accounts/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Account fetched successfully"))
                .andExpect(jsonPath("$.data.accountId").value(2));
    }

    @Test
    void getAccountById_whenAccountMissing_returnsNotFound() throws Exception {
        when(accountService.getAccountById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/accounts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Account not found"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void createAccount_returnsCreatedWithSavedAccount() throws Exception {
        Accounts saved = new Accounts();
        saved.setAccountId(3L);
        saved.setAccountNumber("ACC-1003");
        saved.setAccountHolderName("Alice");

        when(accountService.createAccount(any(Accounts.class))).thenReturn(saved);

        String requestBody = """
                {
                  "accountNumber": "ACC-1003",
                  "accountHolderName": "Alice",
                  "bankName": "Demo Bank",
                  "balance": 500.00,
                  "currency": "USD",
                  "accountStatus": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Account created successfully"))
                .andExpect(jsonPath("$.data.accountId").value(3))
                .andExpect(jsonPath("$.data.accountNumber").value("ACC-1003"));
    }
}
