package com.neueda.__Code_Titans.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.neueda.__Code_Titans.entity.PaymentHistory;
import com.neueda.__Code_Titans.repo.PaymentHistoryRepo;

@Service
public class AuditService {

    private final PaymentHistoryRepo paymentHistoryRepo;

    public AuditService(PaymentHistoryRepo paymentHistoryRepo) {
        this.paymentHistoryRepo = paymentHistoryRepo;
    }

    public PaymentHistory recordStatusChange(Long paymentId, String oldStatus, String newStatus, String changedBy,
            String remarks) {
        PaymentHistory paymentHistory = new PaymentHistory();
        paymentHistory.setPaymentId(paymentId);
        paymentHistory.setOldStatus(oldStatus);
        paymentHistory.setNewStatus(newStatus);
        paymentHistory.setChangedAt(LocalDateTime.now());
        paymentHistory.setChangedBy(changedBy == null || changedBy.isBlank() ? "system" : changedBy);
        paymentHistory.setRemarks(remarks);
        return paymentHistoryRepo.save(paymentHistory);
    }

    public PaymentHistory recordStatusChange(Long paymentId, String oldStatus, String newStatus) {
        return recordStatusChange(paymentId, oldStatus, newStatus, "system", null);
    }

    public List<PaymentHistory> getPaymentHistory(Long paymentId) {
        return paymentHistoryRepo.findByPaymentIdOrderByChangedAtAsc(paymentId);
    }

    public Optional<PaymentHistory> getLatestHistory(Long paymentId) {
        return paymentHistoryRepo.findFirstByPaymentIdOrderByChangedAtDesc(paymentId);
    }
}
