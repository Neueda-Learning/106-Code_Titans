package com.neueda.__Code_Titans.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neueda.__Code_Titans.entity.Accounts;
import com.neueda.__Code_Titans.entity.PaymentHistory;
import com.neueda.__Code_Titans.entity.Payments;
import com.neueda.__Code_Titans.repo.AccountRepo;
import com.neueda.__Code_Titans.repo.PaymentRepo;

@Service
public class PaymentService {

    private final PaymentRepo paymentRepo;
    private final AccountRepo accountRepo;
    private final AuditService auditService;

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("USD", "EUR", "GBP", "INR");
    private static final Set<String> SUPPORTED_STATUSES = Set.of("CREATED", "VALIDATED", "SENT", "COMPLETED", "FAILED");

    public PaymentService(PaymentRepo paymentRepo, AccountRepo accountRepo, AuditService auditService) {
        this.paymentRepo = paymentRepo;
        this.accountRepo = accountRepo;
        this.auditService = auditService;
    }

    /**
     * Creates a payment with comprehensive validation.
     * 
     * If validation fails, the payment is saved with status FAILED and error details.
     * If validation passes, the payment is saved with status CREATED.
     * 
     * @param sourceAccountId Source account ID
     * @param destinationAccountId Destination account ID
     * @param amount Payment amount
     * @param currency Currency code
     * @param reference Payment reference
     * @param idempotencyKey Unique idempotency key
     * @return Created payment
     */
    @Transactional
    public Payments createPayment(Long sourceAccountId, Long destinationAccountId,
            BigDecimal amount, String currency, String reference,
            String idempotencyKey) {

        Payments payment = new Payments();
        payment.setSourceAccountId(sourceAccountId);
        payment.setDestinationAccountId(destinationAccountId);
        payment.setAmount(amount);
        payment.setCurrency(normalizeCurrency(currency));
        payment.setReference(reference);
        payment.setIdempotencyKey(idempotencyKey);

        ValidationResult validationResult = validatePayment(sourceAccountId, destinationAccountId,
                amount, payment.getCurrency(), idempotencyKey);

        if (!validationResult.isValid()) {
            payment.setStatus("FAILED");
            payment.setErrorCode(validationResult.getErrorCode());
            payment.setErrorMessage(validationResult.getErrorMessage());
        } else {
            payment.setStatus("CREATED");
        }

        Payments savedPayment = paymentRepo.save(payment);
        auditService.recordStatusChange(savedPayment.getPaymentId(), null, savedPayment.getStatus(), "system",
                "Payment created");
        return savedPayment;
    }

    /**
     * Retrieves a payment by ID
     */
    public Optional<Payments> getPaymentById(Long paymentId) {
        return paymentRepo.findById(paymentId);
    }

    /**
     * Retrieves all payments
     */
    public List<Payments> getAllPayments() {
        return paymentRepo.findAll();
    }

    /**
     * Retrieves a payment by idempotency key (for duplicate detection)
     */
    public Optional<Payments> getPaymentByIdempotencyKey(String idempotencyKey) {
        return paymentRepo.findByIdempotencyKey(idempotencyKey);
    }

    /**
     * Updates payment status
     */
    @Transactional
    public Optional<Payments> updatePaymentStatus(Long paymentId, String newStatus, String changedBy, String remarks) {
        Optional<Payments> optionalPayment = paymentRepo.findById(paymentId);
        if (optionalPayment.isEmpty()) {
            return Optional.empty();
        }

        Payments payment = optionalPayment.get();
        String normalizedNewStatus = normalizeStatus(newStatus);
        String currentStatus = normalizeStatus(payment.getStatus());

        if (!SUPPORTED_STATUSES.contains(normalizedNewStatus)) {
            throw new IllegalArgumentException("Unsupported status: " + newStatus);
        }

        if (!isTransitionAllowed(currentStatus, normalizedNewStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status transition: " + currentStatus + " -> " + normalizedNewStatus);
        }

        if (isSettlementTransition(currentStatus, normalizedNewStatus)) {
            settleAccountsForPayment(payment);
        }

        payment.setStatus(normalizedNewStatus);
        Payments updatedPayment = paymentRepo.save(payment);
        auditService.recordStatusChange(paymentId, currentStatus, normalizedNewStatus, changedBy, remarks);

        return Optional.of(updatedPayment);
    }

    public Optional<Payments> updatePaymentStatus(Long paymentId, String newStatus) {
        return updatePaymentStatus(paymentId, newStatus, "system", null);
    }

    public List<PaymentHistory> getPaymentHistory(Long paymentId) {
        return auditService.getPaymentHistory(paymentId);
    }

    /**
     * Comprehensive validation logic for payments
     */
    private ValidationResult validatePayment(Long sourceAccountId, Long destinationAccountId,
                                             BigDecimal amount, String currency,
                                             String idempotencyKey) {

        // Rule 1: Source account exists
        Optional<Accounts> sourceAccount = accountRepo.findById(sourceAccountId);
        if (sourceAccount.isEmpty()) {
            return new ValidationResult(false, "INVALID_ACCOUNT",
                    "Source account does not exist");
        }

        // Rule 2: Destination account exists
        Optional<Accounts> destinationAccount = accountRepo.findById(destinationAccountId);
        if (destinationAccount.isEmpty()) {
            return new ValidationResult(false, "INVALID_ACCOUNT",
                    "Destination account does not exist");
        }

        // Rule 3: Source account != Destination account
        if (sourceAccountId.equals(destinationAccountId)) {
            return new ValidationResult(false, "SAME_ACCOUNT",
                    "Source and destination accounts cannot be the same");
        }

        // Rule 4: Amount > 0
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return new ValidationResult(false, "INVALID_AMOUNT",
                    "Amount must be greater than zero");
        }

        // Rule 5: Currency is supported
        if (!isCurrencySupported(currency)) {
            return new ValidationResult(false, "INVALID_AMOUNT",
                    "Currency " + currency + " is not supported");
        }

        Accounts source = sourceAccount.get();

        // Rule 5a: Source account currency matches payment currency
        if (!source.getCurrency().equals(currency)) {
            return new ValidationResult(false, "INVALID_AMOUNT",
                    "Payment currency does not match source account currency");
        }

        // Rule 5b: Destination account currency matches payment currency
        Accounts destination = destinationAccount.get();
        if (!destination.getCurrency().equals(currency)) {
            return new ValidationResult(false, "INVALID_AMOUNT",
                    "Payment currency does not match destination account currency");
        }

        // Rule 6: Source account has sufficient balance
        if (source.getBalance().compareTo(amount) < 0) {
            return new ValidationResult(false, "INSUFFICIENT_FUNDS",
                    "Source account has insufficient balance");
        }

        // Rule 7: Duplicate payment detection using idempotency key
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<Payments> existingPayment = paymentRepo.findByIdempotencyKey(idempotencyKey);
            if (existingPayment.isPresent()) {
                return new ValidationResult(false, "DUPLICATE_PAYMENT",
                        "A payment with this idempotency key already exists");
            }
        }

        // All validations passed
        return new ValidationResult(true, null, null);
    }

    /**
     * Checks if the given currency is supported
     */
    private boolean isCurrencySupported(String currency) {
        return currency != null && SUPPORTED_CURRENCIES.contains(currency);
    }

    private boolean isTransitionAllowed(String currentStatus, String newStatus) {
        if (currentStatus == null || currentStatus.isBlank()) {
            return "CREATED".equals(newStatus);
        }

        if (currentStatus.equals(newStatus)) {
            return true;
        }

        return switch (currentStatus) {
            case "CREATED" -> "VALIDATED".equals(newStatus) || "FAILED".equals(newStatus);
            case "VALIDATED" -> "SENT".equals(newStatus) || "FAILED".equals(newStatus);
            case "SENT" -> "COMPLETED".equals(newStatus) || "FAILED".equals(newStatus);
            case "COMPLETED", "FAILED" -> false;
            default -> false;
        };
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCurrency(String currency) {
        return currency == null ? null : currency.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isSettlementTransition(String currentStatus, String newStatus) {
        return !"COMPLETED".equals(currentStatus) && "COMPLETED".equals(newStatus);
    }

    private void settleAccountsForPayment(Payments payment) {
        Long sourceAccountId = payment.getSourceAccountId();
        Long destinationAccountId = payment.getDestinationAccountId();
        BigDecimal amount = payment.getAmount();

        if (sourceAccountId == null || destinationAccountId == null || amount == null) {
            throw new IllegalArgumentException("Payment is missing settlement details.");
        }

        Accounts sourceAccount = accountRepo.findById(sourceAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Source account does not exist"));
        Accounts destinationAccount = accountRepo.findById(destinationAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Destination account does not exist"));

        if (sourceAccount.getBalance() == null || sourceAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Source account has insufficient balance");
        }

        BigDecimal sourceNextBalance = sourceAccount.getBalance().subtract(amount);
        BigDecimal destinationCurrent = destinationAccount.getBalance() == null
                ? BigDecimal.ZERO
                : destinationAccount.getBalance();
        BigDecimal destinationNextBalance = destinationCurrent.add(amount);

        int sourceUpdated = accountRepo.updateBalance(sourceAccountId, sourceNextBalance);
        int destinationUpdated = accountRepo.updateBalance(destinationAccountId, destinationNextBalance);

        if (sourceUpdated != 1 || destinationUpdated != 1) {
            throw new IllegalStateException("Failed to update account balances for settlement.");
        }
    }

    /**
     * Inner class to represent validation result
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorCode;
        private final String errorMessage;

        public ValidationResult(boolean valid, String errorCode, String errorMessage) {
            this.valid = valid;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
