package com.neueda.__Code_Titans.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import com.neueda.__Code_Titans.entity.Accounts;

@ExtendWith(MockitoExtension.class)
class AccountRepoTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private AccountRepo accountRepo;

    @BeforeEach
    void setUp() {
        accountRepo = new AccountRepo(jdbcTemplate);
    }

    @Test
    void existsByAccountNumber_whenCountPositive_returnsTrue() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("ACC-1001"))).thenReturn(1);

        boolean exists = accountRepo.existsByAccountNumber("ACC-1001");

        assertTrue(exists);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_whenNoRows_returnsEmpty() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(500L))).thenReturn(List.of());

        Optional<Accounts> result = accountRepo.findById(500L);

        assertFalse(result.isPresent());
    }

    @Test
    void save_setsGeneratedAccountId() {
        Accounts account = new Accounts();
        account.setAccountNumber("ACC-2001");
        account.setAccountHolderName("Alex");
        account.setBankName("Test Bank");
        account.setBalance(new BigDecimal("900.00"));
        account.setCurrency("USD");
        account.setAccountStatus("ACTIVE");

        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            GeneratedKeyHolder generated = (GeneratedKeyHolder) keyHolder;
            Map<String, Object> keyMap = new HashMap<>();
            keyMap.put("account_id", 2001L);
            generated.getKeyList().add(keyMap);
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        Accounts saved = accountRepo.save(account);

        assertEquals(2001L, saved.getAccountId());
    }
}

