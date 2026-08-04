package com.neueda.__Code_Titans.controller;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.__Code_Titans.entity.PaymentHistory;
import com.neueda.__Code_Titans.entity.Payments;
import com.neueda.__Code_Titans.service.PaymentService;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPayment(@RequestBody CreatePaymentRequest request) {
        Payments payment = paymentService.createPayment(
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount(),
                request.getCurrency(),
                request.getReference(),
                request.getIdempotencyKey());

        if ("FAILED".equalsIgnoreCase(payment.getStatus())) {
            return buildResponse(HttpStatus.BAD_REQUEST, false, "Payment validation failed", payment);
        }
        return buildResponse(HttpStatus.CREATED, true, "Payment created", payment);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllPayments() {
        return buildResponse(HttpStatus.OK, true, "Payments fetched", paymentService.getAllPayments());
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<Map<String, Object>> getPaymentById(@PathVariable Long paymentId) {
        Optional<Payments> payment = paymentService.getPaymentById(paymentId);
        return payment
                .map(value -> buildResponse(HttpStatus.OK, true, "Payment fetched", value))
                .orElseGet(() -> buildResponse(HttpStatus.NOT_FOUND, false, "Payment not found", null));
    }

    @PutMapping("/{paymentId}/status")
    public ResponseEntity<Map<String, Object>> updatePaymentStatus(@PathVariable Long paymentId,
            @RequestBody UpdateStatusRequest request) {
        try {
            Optional<Payments> updatedPayment = paymentService.updatePaymentStatus(
                    paymentId,
                    request.getStatus(),
                    request.getChangedBy(),
                    request.getRemarks());

            return updatedPayment
                    .map(value -> buildResponse(HttpStatus.OK, true, "Payment status updated", value))
                    .orElseGet(() -> buildResponse(HttpStatus.NOT_FOUND, false, "Payment not found", null));
        } catch (IllegalArgumentException exception) {
            return buildResponse(HttpStatus.BAD_REQUEST, false, exception.getMessage(), null);
        }
    }

    @GetMapping("/{paymentId}/history")
    public ResponseEntity<Map<String, Object>> getPaymentHistory(@PathVariable Long paymentId) {
        if (paymentService.getPaymentById(paymentId).isEmpty()) {
            return buildResponse(HttpStatus.NOT_FOUND, false, "Payment not found", null);
        }
        List<PaymentHistory> history = paymentService.getPaymentHistory(paymentId);
        return buildResponse(HttpStatus.OK, true, "Payment history fetched", history);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, boolean success, String message,
            Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return ResponseEntity.status(status).body(response);
    }

    public static class CreatePaymentRequest {
        private Long sourceAccountId;
        private Long destinationAccountId;
        private BigDecimal amount;
        private String currency;
        private String reference;
        private String idempotencyKey;

        public Long getSourceAccountId() {
            return sourceAccountId;
        }

        public void setSourceAccountId(Long sourceAccountId) {
            this.sourceAccountId = sourceAccountId;
        }

        public Long getDestinationAccountId() {
            return destinationAccountId;
        }

        public void setDestinationAccountId(Long destinationAccountId) {
            this.destinationAccountId = destinationAccountId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getReference() {
            return reference;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }
    }

    public static class UpdateStatusRequest {
        private String status;
        private String changedBy;
        private String remarks;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getChangedBy() {
            return changedBy;
        }

        public void setChangedBy(String changedBy) {
            this.changedBy = changedBy;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }
    }
}
