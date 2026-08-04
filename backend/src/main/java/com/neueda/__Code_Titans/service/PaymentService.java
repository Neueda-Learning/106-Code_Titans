package com.neueda.__Code_Titans.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.neueda.__Code_Titans.entity.Accounts;
import com.neueda.__Code_Titans.entity.Payments;
import com.neueda.__Code_Titans.repo.AccountRepo;
import com.neueda.__Code_Titans.repo.PaymentRepo;

@Service
public class PaymentService {

    private final PaymentRepo paymentRepo;
    private final AccountRepo accountRepo;

    // Supported currencies
    private static final String[] SUPPORTED_CURRENCIES = {"USD", "EUR", "GBP", "INR"};

    public PaymentService(PaymentRepo paymentRepo, AccountRepo accountRepo) {
        this.paymentRepo = paymentRepo;
        this.accountRepo = accountRepo;
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
    public Payments createPayment(Long sourceAccountId, Long destinationAccountId,
                                   BigDecimal amount, String currency, String reference,
                                   String idempotencyKey) {

        Payments payment = new Payments();
        payment.setSourceAccountId(sourceAccountId);
        payment.setDestinationAccountId(destinationAccountId);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setReference(reference);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        // Perform validations
        ValidationResult validationResult = validatePayment(sourceAccountId, destinationAccountId,
                amount, currency, idempotencyKey);

        if (!validationResult.isValid()) {
            payment.setStatus("FAILED");
            payment.setErrorCode(validationResult.getErrorCode());
            payment.setErrorMessage(validationResult.getErrorMessage());
        } else {
            payment.setStatus("CREATED");
        }

        // Save and return
        return paymentRepo.save(payment);
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
    public Iterable<Payments> getAllPayments() {
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
    public Payments updatePaymentStatus(Long paymentId, String newStatus) {
        Optional<Payments> optionalPayment = paymentRepo.findById(paymentId);
        if (optionalPayment.isPresent()) {
            Payments payment = optionalPayment.get();
            payment.setStatus(newStatus);
            payment.setUpdatedAt(LocalDateTime.now());
            return paymentRepo.save(payment);
        }
        return null;
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
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
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
        if (currency == null || currency.isEmpty()) {
            return false;
        }
        for (String supported : SUPPORTED_CURRENCIES) {
            if (supported.equals(currency)) {
                return true;
            }
        }
        return false;
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
