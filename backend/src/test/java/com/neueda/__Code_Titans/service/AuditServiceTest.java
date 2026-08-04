package com.neueda.__Code_Titans.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.__Code_Titans.entity.PaymentHistory;
import com.neueda.__Code_Titans.repo.PaymentHistoryRepo;

@ExtendWith(MockitoExtension.class)
public class AuditServiceTest {

	@Mock
	private PaymentHistoryRepo paymentHistoryRepo;

	private AuditService auditService;

	@BeforeEach
	void setUp() {
		auditService = new AuditService(paymentHistoryRepo);
	}

	@Test
	void recordStatusChange_whenChangedByBlank_usesSystemAsChangedBy() {
		when(paymentHistoryRepo.save(any(PaymentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PaymentHistory result = auditService.recordStatusChange(10L, "CREATED", "VALIDATED", "  ", "ok");

		assertEquals(10L, result.getPaymentId());
		assertEquals("CREATED", result.getOldStatus());
		assertEquals("VALIDATED", result.getNewStatus());
		assertEquals("system", result.getChangedBy());
		assertEquals("ok", result.getRemarks());
		assertTrue(result.getChangedAt() != null);
	}

	@Test
	void recordStatusChange_overloadedMethod_setsSystemAndNullRemarks() {
		when(paymentHistoryRepo.save(any(PaymentHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

		PaymentHistory result = auditService.recordStatusChange(11L, "VALIDATED", "SENT");

		assertEquals("system", result.getChangedBy());
		assertEquals(null, result.getRemarks());
	}

	@Test
	void getPaymentHistory_returnsHistoryFromRepository() {
		PaymentHistory one = new PaymentHistory();
		one.setPaymentId(99L);
		one.setNewStatus("CREATED");
		PaymentHistory two = new PaymentHistory();
		two.setPaymentId(99L);
		two.setNewStatus("FAILED");
		when(paymentHistoryRepo.findByPaymentIdOrderByChangedAtAsc(99L)).thenReturn(List.of(one, two));

		List<PaymentHistory> result = auditService.getPaymentHistory(99L);

		assertEquals(2, result.size());
		assertEquals("CREATED", result.get(0).getNewStatus());
		assertEquals("FAILED", result.get(1).getNewStatus());
		verify(paymentHistoryRepo).findByPaymentIdOrderByChangedAtAsc(99L);
	}

	@Test
	void getLatestHistory_returnsLatestEntryWhenPresent() {
		PaymentHistory latest = new PaymentHistory();
		latest.setPaymentId(5L);
		latest.setNewStatus("COMPLETED");
		when(paymentHistoryRepo.findFirstByPaymentIdOrderByChangedAtDesc(5L)).thenReturn(Optional.of(latest));

		Optional<PaymentHistory> result = auditService.getLatestHistory(5L);

		assertTrue(result.isPresent());
		assertEquals("COMPLETED", result.get().getNewStatus());
	}
}
