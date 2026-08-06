package com.neueda.__Code_Titans.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.__Code_Titans.entity.Accounts;
import com.neueda.__Code_Titans.entity.PaymentHistory;
import com.neueda.__Code_Titans.entity.Payments;
import com.neueda.__Code_Titans.repo.AccountRepo;
import com.neueda.__Code_Titans.repo.PaymentRepo;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepo paymentRepo;

    @Mock
    private AccountRepo accountRepo;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createPayment_whenValid_createsPaymentAndWritesAudit() {
        Accounts source = account(1L, "USD", new BigDecimal("1000.00"));
        Accounts destination = account(2L, "USD", new BigDecimal("300.00"));

        when(accountRepo.findById(1L)).thenReturn(Optional.of(source));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(destination));
        when(paymentRepo.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(paymentRepo.save(any(Payments.class))).thenAnswer(invocation -> {
            Payments saved = invocation.getArgument(0);
            saved.setPaymentId(101L);
            return saved;
        });

        Payments result = paymentService.createPayment(
                1L,
                2L,
                new BigDecimal("150.00"),
                "usd",
                "invoice-42",
                "idem-1");

        assertEquals(101L, result.getPaymentId());
        assertEquals("CREATED", result.getStatus());
        assertEquals("USD", result.getCurrency());
        verify(auditService).recordStatusChange(101L, null, "CREATED", "system", "Payment created");
    }

    @Test
    void createPayment_whenDuplicateIdempotency_marksFailed() {
        Accounts source = account(1L, "USD", new BigDecimal("1000.00"));
        Accounts destination = account(2L, "USD", new BigDecimal("500.00"));
        Payments existing = new Payments();
        existing.setPaymentId(88L);

        when(accountRepo.findById(1L)).thenReturn(Optional.of(source));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(destination));
        when(paymentRepo.findByIdempotencyKey("idem-dup")).thenReturn(Optional.of(existing));
        when(paymentRepo.save(any(Payments.class))).thenAnswer(invocation -> {
            Payments saved = invocation.getArgument(0);
            saved.setPaymentId(102L);
            return saved;
        });

        Payments result = paymentService.createPayment(
                1L,
                2L,
                new BigDecimal("50.00"),
                "USD",
                "retry",
                "idem-dup");

        assertEquals("FAILED", result.getStatus());
        assertEquals("DUPLICATE_PAYMENT", result.getErrorCode());
        verify(auditService).recordStatusChange(102L, null, "FAILED", "system", "Payment created");
    }

    @Test
    void updatePaymentStatus_whenTransitionAllowed_updatesAndAudits() {
        Payments current = new Payments();
        current.setPaymentId(300L);
        current.setStatus("CREATED");

        when(paymentRepo.findById(300L)).thenReturn(Optional.of(current));
        when(paymentRepo.save(any(Payments.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Payments> updated = paymentService.updatePaymentStatus(300L, "validated", "maker1", "passed checks");

        assertTrue(updated.isPresent());
        assertEquals("VALIDATED", updated.get().getStatus());
        verify(auditService).recordStatusChange(300L, "CREATED", "VALIDATED", "maker1", "passed checks");
        verify(accountRepo, never()).updateBalance(anyLong(), any());
    }

    @Test
    void updatePaymentStatus_whenTransitionToCompleted_settlesBalancesAndAudits() {
        Payments current = new Payments();
        current.setPaymentId(302L);
        current.setStatus("SENT");
        current.setSourceAccountId(1L);
        current.setDestinationAccountId(2L);
        current.setAmount(new BigDecimal("125.00"));

        Accounts source = account(1L, "USD", new BigDecimal("500.00"));
        Accounts destination = account(2L, "USD", new BigDecimal("300.00"));

        when(paymentRepo.findById(302L)).thenReturn(Optional.of(current));
        when(accountRepo.findById(1L)).thenReturn(Optional.of(source));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(destination));
        when(accountRepo.updateBalance(1L, new BigDecimal("375.00"))).thenReturn(1);
        when(accountRepo.updateBalance(2L, new BigDecimal("425.00"))).thenReturn(1);
        when(paymentRepo.save(any(Payments.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<Payments> updated = paymentService.updatePaymentStatus(302L, "COMPLETED", "maker1", "settled");

        assertTrue(updated.isPresent());
        assertEquals("COMPLETED", updated.get().getStatus());
        verify(accountRepo).updateBalance(1L, new BigDecimal("375.00"));
        verify(accountRepo).updateBalance(2L, new BigDecimal("425.00"));
        verify(auditService).recordStatusChange(302L, "SENT", "COMPLETED", "maker1", "settled");
    }

    @Test
    void updatePaymentStatus_whenTransitionToCompletedAndInsufficientBalance_throwsException() {
        Payments current = new Payments();
        current.setPaymentId(303L);
        current.setStatus("SENT");
        current.setSourceAccountId(1L);
        current.setDestinationAccountId(2L);
        current.setAmount(new BigDecimal("900.00"));

        Accounts source = account(1L, "USD", new BigDecimal("100.00"));
        Accounts destination = account(2L, "USD", new BigDecimal("300.00"));

        when(paymentRepo.findById(303L)).thenReturn(Optional.of(current));
        when(accountRepo.findById(1L)).thenReturn(Optional.of(source));
        when(accountRepo.findById(2L)).thenReturn(Optional.of(destination));

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.updatePaymentStatus(303L, "COMPLETED", "maker1", "settled"));

        verify(accountRepo, never()).updateBalance(eq(1L), any());
        verify(accountRepo, never()).updateBalance(eq(2L), any());
        verify(paymentRepo, never()).save(any(Payments.class));
        verify(auditService, never()).recordStatusChange(anyLong(), any(), any(), any(), any());
    }

    @Test
    void updatePaymentStatus_whenTransitionNotAllowed_throwsException() {
        Payments current = new Payments();
        current.setPaymentId(301L);
        current.setStatus("FAILED");

        when(paymentRepo.findById(301L)).thenReturn(Optional.of(current));

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.updatePaymentStatus(301L, "COMPLETED", "maker1", "override"));

        verify(paymentRepo, never()).save(any(Payments.class));
        verify(auditService, never()).recordStatusChange(anyLong(), any(), any(), any(), any());
    }

    @Test
    void updatePaymentStatus_whenPaymentMissing_returnsEmpty() {
        when(paymentRepo.findById(999L)).thenReturn(Optional.empty());

        Optional<Payments> updated = paymentService.updatePaymentStatus(999L, "VALIDATED", "maker", "n/a");

        assertFalse(updated.isPresent());
        verify(auditService, never()).recordStatusChange(anyLong(), any(), any(), any(), any());
    }

    @Test
    void getPaymentHistory_delegatesToAuditService() {
        PaymentHistory history = new PaymentHistory();
        history.setPaymentId(400L);
        history.setNewStatus("CREATED");

        when(auditService.getPaymentHistory(400L)).thenReturn(List.of(history));

        List<PaymentHistory> result = paymentService.getPaymentHistory(400L);

        assertEquals(1, result.size());
        assertEquals("CREATED", result.get(0).getNewStatus());
        verify(auditService).getPaymentHistory(400L);
    }

    private Accounts account(Long id, String currency, BigDecimal balance) {
        Accounts account = new Accounts();
        account.setAccountId(id);
        account.setCurrency(currency);
        account.setBalance(balance);
        account.setAccountStatus("ACTIVE");
        return account;
    }
}

