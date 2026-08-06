-- Reset child tables first to satisfy foreign key constraints on reruns.
DELETE FROM payment_history;
DELETE FROM payments;
DELETE FROM accounts;

ALTER TABLE accounts AUTO_INCREMENT = 1;
ALTER TABLE payments AUTO_INCREMENT = 1;
ALTER TABLE payment_history AUTO_INCREMENT = 1;

INSERT INTO accounts (account_number, account_holder_name, bank_name, balance, currency, account_status) VALUES
('ACC100001', 'Alice Johnson', 'City Bank', 12000.00, 'USD', 'ACTIVE'),
('ACC100002', 'Bob Smith', 'Global Trust', 8450.25, 'USD', 'ACTIVE'),
('ACC100003', 'Carlos Rivera', 'First Capital', 15200.50, 'EUR', 'ACTIVE'),
('ACC100004', 'Diana Lee', 'Union Finance', 6400.75, 'GBP', 'ACTIVE'),
('ACC100005', 'Ethan Brown', 'City Bank', 2300.00, 'USD', 'SUSPENDED'),
('ACC100006', 'Farah Khan', 'Summit Bank', 9800.10, 'EUR', 'ACTIVE'),
('ACC100007', 'Grace Miller', 'Global Trust', 4310.00, 'USD', 'ACTIVE'),
('ACC100008', 'Hiro Tanaka', 'Pacific Credit', 21990.95, 'JPY', 'ACTIVE'),
('ACC100009', 'Isabella Rossi', 'Continental Bank', 5075.40, 'EUR', 'ACTIVE'),
('ACC100010', 'James Wilson', 'Union Finance', 1115.25, 'USD', 'ACTIVE'),
('ACC100011', 'Kavya Patel', 'Summit Bank', 16000.00, 'INR', 'ACTIVE'),
('ACC100012', 'Liam Walker', 'First Capital', 3890.00, 'GBP', 'INACTIVE'),
('ACC100013', 'Mina Park', 'Pacific Credit', 7425.75, 'KRW', 'ACTIVE'),
('ACC100014', 'Noah Garcia', 'City Bank', 13450.30, 'USD', 'ACTIVE'),
('ACC100015', 'Olivia Martin', 'Continental Bank', 2999.99, 'EUR', 'ACTIVE');

INSERT INTO payments (
    source_account_id,
    destination_account_id,
    amount,
    currency,
    reference,
    status,
    error_code,
    error_message,
    idempotency_key
) VALUES
(1, 2, 150.00, 'USD', 'Invoice INV-1001', 'COMPLETED', NULL, NULL, 'idem-0001'),
(2, 3, 220.50, 'USD', 'Subscription fee', 'COMPLETED', NULL, NULL, 'idem-0002'),
(3, 4, 75.25, 'EUR', 'Travel reimbursement', 'FAILED', 'INSUFFICIENT_FUNDS', 'Insufficient account balance', 'idem-0003'),
(4, 5, 40.00, 'GBP', 'Gift transfer', 'COMPLETED', NULL, NULL, 'idem-0004'),
(5, 6, 500.00, 'USD', 'Rent payment', 'FAILED', 'ACCOUNT_BLOCKED', 'Source account is suspended', 'idem-0005'),
(6, 7, 130.10, 'EUR', 'Utility bill', 'COMPLETED', NULL, NULL, 'idem-0006'),
(7, 8, 915.00, 'USD', 'Laptop purchase', 'COMPLETED', NULL, NULL, 'idem-0007'),
(8, 9, 1200.00, 'JPY', 'Supplier payout', 'COMPLETED', NULL, NULL, 'idem-0008'),
(9, 10, 310.75, 'EUR', 'Insurance premium', 'FAILED', 'LIMIT_EXCEEDED', 'Daily transaction limit exceeded', 'idem-0009'),
(10, 11, 60.00, 'USD', 'Dining split', 'COMPLETED', NULL, NULL, 'idem-0010'),
(11, 12, 4500.00, 'INR', 'Quarterly tax', 'COMPLETED', NULL, NULL, 'idem-0011'),
(12, 13, 95.30, 'GBP', 'Mobile recharge', 'FAILED', 'ACCOUNT_INACTIVE', 'Source account is inactive', 'idem-0012'),
(13, 14, 780.00, 'KRW', 'Vendor settlement', 'COMPLETED', NULL, NULL, 'idem-0013'),
(14, 15, 145.99, 'USD', 'Event ticket', 'COMPLETED', NULL, NULL, 'idem-0014'),
(15, 1, 250.00, 'EUR', 'Savings transfer', 'CREATED', NULL, NULL, 'idem-0015');

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks) VALUES
(1, 'CREATED', 'COMPLETED', 'system', 'Payment completed'),
(2, 'CREATED', 'COMPLETED', 'system', 'Payment completed'),
(3, 'CREATED', 'FAILED', 'system', 'Failure due to insufficient funds'),
(4, 'CREATED', 'COMPLETED', 'system', 'Payment completed'),
(5, 'CREATED', 'FAILED', 'system', 'Source account is suspended'),
(6, 'CREATED', 'COMPLETED', 'system', 'Payment completed'),
(7, 'CREATED', 'COMPLETED', 'system', 'Payment completed'),
(8, 'CREATED', 'COMPLETED', 'system', 'Payment completed'),
(9, 'CREATED', 'FAILED', 'system', 'Transaction limit exceeded'),
(10, 'CREATED', 'COMPLETED', 'system', 'Payment completed'),
(11, 'CREATED', 'COMPLETED', 'system', 'Payment completed'),
(12, 'CREATED', 'FAILED', 'system', 'Source account is inactive'),
(13, 'CREATED', 'COMPLETED', 'system', 'Payment completed'),
(14, 'CREATED', 'COMPLETED', 'system', 'Payment completed'),
(15, NULL, 'CREATED', 'system', 'Payment initialized');
