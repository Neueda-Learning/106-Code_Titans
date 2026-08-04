package com.neueda.__Code_Titans.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.__Code_Titans.entity.Accounts;
import com.neueda.__Code_Titans.repo.AccountRepo;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

	@Mock
	private AccountRepo accountRepo;

	private AccountService accountService;

	@BeforeEach
	void setUp() {
		accountService = new AccountService(accountRepo);
	}

	@Test
	void getAllAccounts_returnsAllAccountsFromRepository() {
		Accounts first = account(1L, "ACC-1");
		Accounts second = account(2L, "ACC-2");
		when(accountRepo.findAll()).thenReturn(List.of(first, second));

		List<Accounts> result = accountService.getAllAccounts();

		assertEquals(2, result.size());
		assertEquals("ACC-1", result.get(0).getAccountNumber());
		assertEquals("ACC-2", result.get(1).getAccountNumber());
	}

	@Test
	void getAccountById_whenAccountExists_returnsAccount() {
		Accounts account = account(10L, "ACC-10");
		when(accountRepo.findById(10L)).thenReturn(Optional.of(account));

		Optional<Accounts> result = accountService.getAccountById(10L);

		assertTrue(result.isPresent());
		assertEquals("ACC-10", result.get().getAccountNumber());
	}

	@Test
	void getAccountById_whenAccountMissing_returnsEmpty() {
		when(accountRepo.findById(999L)).thenReturn(Optional.empty());

		Optional<Accounts> result = accountService.getAccountById(999L);

		assertFalse(result.isPresent());
	}

	@Test
	void createAccount_returnsSavedAccount() {
		Accounts input = account(null, "ACC-20");
		Accounts saved = account(20L, "ACC-20");
		when(accountRepo.save(input)).thenReturn(saved);

		Accounts result = accountService.createAccount(input);

		assertEquals(20L, result.getAccountId());
		assertEquals("ACC-20", result.getAccountNumber());
		verify(accountRepo).save(input);
	}

	private Accounts account(Long id, String number) {
		Accounts account = new Accounts();
		account.setAccountId(id);
		account.setAccountNumber(number);
		account.setAccountHolderName("Holder");
		account.setBankName("Demo Bank");
		account.setBalance(new BigDecimal("100.00"));
		account.setCurrency("USD");
		account.setAccountStatus("ACTIVE");
		return account;
	}
}
