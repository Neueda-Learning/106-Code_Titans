package com.neueda.__Code_Titans.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import com.neueda.__Code_Titans.entity.PaymentHistory;

@ExtendWith(MockitoExtension.class)
class PaymentHistoryRepoTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PaymentHistoryRepo paymentHistoryRepo;

    @BeforeEach
    void setUp() {
        paymentHistoryRepo = new PaymentHistoryRepo(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findFirstByPaymentIdOrderByChangedAtDesc_returnsFirstRow() {
        PaymentHistory latest = new PaymentHistory();
        latest.setHistoryId(99L);
        latest.setPaymentId(10L);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(10L))).thenReturn(List.of(latest));

        Optional<PaymentHistory> result = paymentHistoryRepo.findFirstByPaymentIdOrderByChangedAtDesc(10L);

        assertTrue(result.isPresent());
        assertEquals(99L, result.get().getHistoryId());
    }

    @Test
    void countByNewStatus_whenDbReturnsNull_returnsZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("FAILED"))).thenReturn(null);

        long result = paymentHistoryRepo.countByNewStatus("FAILED");

        assertEquals(0L, result);
    }

    @Test
    void save_setsGeneratedHistoryId() {
        PaymentHistory history = new PaymentHistory();
        history.setPaymentId(10L);
        history.setOldStatus("CREATED");
        history.setNewStatus("VALIDATED");
        history.setChangedAt(LocalDateTime.now());
        history.setChangedBy("system");
        history.setRemarks("auto-check");

        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            GeneratedKeyHolder generated = (GeneratedKeyHolder) keyHolder;
            Map<String, Object> keyMap = new HashMap<>();
            keyMap.put("history_id", 77L);
            generated.getKeyList().add(keyMap);
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        PaymentHistory saved = paymentHistoryRepo.save(history);

        assertEquals(77L, saved.getHistoryId());
    }
}

