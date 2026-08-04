package com.neueda.__Code_Titans.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
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

import com.neueda.__Code_Titans.entity.Payments;

@ExtendWith(MockitoExtension.class)
class PaymentRepoTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PaymentRepo paymentRepo;

    @BeforeEach
    void setUp() {
        paymentRepo = new PaymentRepo(jdbcTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_returnsFirstPayment() {
        Payments first = new Payments();
        first.setPaymentId(1L);
        Payments second = new Payments();
        second.setPaymentId(2L);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
                .thenReturn(List.of(first, second));

        Optional<Payments> result = paymentRepo.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getPaymentId());
    }

    @Test
    void count_returnsZeroWhenDatabaseReturnsNull() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(null);

        long result = paymentRepo.count();

        assertEquals(0L, result);
    }

    @Test
    void save_whenNewPayment_assignsGeneratedId() {
        Payments newPayment = payment(1L, 2L, "CREATED");

        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            GeneratedKeyHolder generated = (GeneratedKeyHolder) keyHolder;
            Map<String, Object> keyMap = new HashMap<>();
            keyMap.put("payment_id", 55L);
            generated.getKeyList().add(keyMap);
            return 1;
        }).when(jdbcTemplate).update(any(PreparedStatementCreator.class), any(KeyHolder.class));

        Payments saved = paymentRepo.save(newPayment);

        assertEquals(55L, saved.getPaymentId());
    }

    @Test
    void save_whenExistingPayment_runsUpdateSql() {
        Payments existing = payment(4L, 5L, "VALIDATED");
        existing.setPaymentId(99L);
        existing.setReference("invoice-9");

        paymentRepo.save(existing);

        verify(jdbcTemplate).update(anyString(),
                eq(4L),
                eq(5L),
                eq(new BigDecimal("100.00")),
                eq("USD"),
                eq("invoice-9"),
                eq("VALIDATED"),
                eq(null),
                eq(null),
                eq("idem-key"),
                eq(99L));
    }

    private Payments payment(Long sourceId, Long destinationId, String status) {
        Payments payment = new Payments();
        payment.setSourceAccountId(sourceId);
        payment.setDestinationAccountId(destinationId);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency("USD");
        payment.setStatus(status);
        payment.setIdempotencyKey("idem-key");
        return payment;
    }
}

