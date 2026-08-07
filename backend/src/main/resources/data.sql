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
    idempotency_key,
    created_at,
    updated_at
) VALUES
(1, 2, 150.00, 'USD', 'Invoice INV-1001', 'COMPLETED', NULL, NULL, 'idem-0001', '2026-08-01 09:15:00', '2026-08-01 09:18:12'),
(2, 3, 220.50, 'USD', 'Subscription fee', 'COMPLETED', NULL, NULL, 'idem-0002', '2026-08-01 11:42:10', '2026-08-01 11:45:30'),
(3, 4, 75.25, 'EUR', 'Travel reimbursement', 'FAILED', 'INSUFFICIENT_FUNDS', 'Insufficient account balance', 'idem-0003', '2026-08-01 14:05:50', '2026-08-01 14:06:20'),
(4, 5, 40.00, 'GBP', 'Gift transfer', 'COMPLETED', NULL, NULL, 'idem-0004', '2026-08-01 16:25:11', '2026-08-01 16:29:01'),
(5, 6, 500.00, 'USD', 'Rent payment', 'FAILED', 'ACCOUNT_BLOCKED', 'Source account is suspended', 'idem-0005', '2026-08-02 08:12:42', '2026-08-02 08:13:10'),
(6, 7, 130.10, 'EUR', 'Utility bill', 'COMPLETED', NULL, NULL, 'idem-0006', '2026-08-02 09:45:00', '2026-08-02 09:49:17'),
(7, 8, 915.00, 'USD', 'Laptop purchase', 'COMPLETED', NULL, NULL, 'idem-0007', '2026-08-02 12:01:05', '2026-08-02 12:08:40'),
(8, 9, 1200.00, 'JPY', 'Supplier payout', 'COMPLETED', NULL, NULL, 'idem-0008', '2026-08-02 15:20:19', '2026-08-02 15:26:54'),
(9, 10, 310.75, 'EUR', 'Insurance premium', 'FAILED', 'LIMIT_EXCEEDED', 'Daily transaction limit exceeded', 'idem-0009', '2026-08-03 10:14:22', '2026-08-03 10:15:01'),
(10, 11, 60.00, 'USD', 'Dining split', 'COMPLETED', NULL, NULL, 'idem-0010', '2026-08-03 11:33:44', '2026-08-03 11:35:29'),
(11, 12, 4500.00, 'INR', 'Quarterly tax', 'COMPLETED', NULL, NULL, 'idem-0011', '2026-08-03 13:22:08', '2026-08-03 13:28:52'),
(12, 13, 95.30, 'GBP', 'Mobile recharge', 'FAILED', 'ACCOUNT_INACTIVE', 'Source account is inactive', 'idem-0012', '2026-08-04 09:05:00', '2026-08-04 09:05:27'),
(13, 14, 780.00, 'KRW', 'Vendor settlement', 'COMPLETED', NULL, NULL, 'idem-0013', '2026-08-04 14:42:39', '2026-08-04 14:50:13'),
(14, 15, 145.99, 'USD', 'Event ticket', 'COMPLETED', NULL, NULL, 'idem-0014', '2026-08-05 17:18:20', '2026-08-05 17:21:05'),
(15, 1, 250.00, 'EUR', 'Savings transfer', 'CREATED', NULL, NULL, 'idem-0015', '2026-08-06 07:55:00', '2026-08-06 07:55:00');

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'COMPLETED', 'system', 'Payment completed', '2026-08-01 09:18:12'
FROM payments WHERE idempotency_key = 'idem-0001';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'COMPLETED', 'system', 'Payment completed', '2026-08-01 11:45:30'
FROM payments WHERE idempotency_key = 'idem-0002';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'FAILED', 'system', 'Failure due to insufficient funds', '2026-08-01 14:06:20'
FROM payments WHERE idempotency_key = 'idem-0003';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'COMPLETED', 'system', 'Payment completed', '2026-08-01 16:29:01'
FROM payments WHERE idempotency_key = 'idem-0004';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'FAILED', 'system', 'Source account is suspended', '2026-08-02 08:13:10'
FROM payments WHERE idempotency_key = 'idem-0005';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'COMPLETED', 'system', 'Payment completed', '2026-08-02 09:49:17'
FROM payments WHERE idempotency_key = 'idem-0006';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'COMPLETED', 'system', 'Payment completed', '2026-08-02 12:08:40'
FROM payments WHERE idempotency_key = 'idem-0007';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'COMPLETED', 'system', 'Payment completed', '2026-08-02 15:26:54'
FROM payments WHERE idempotency_key = 'idem-0008';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'FAILED', 'system', 'Transaction limit exceeded', '2026-08-03 10:15:01'
FROM payments WHERE idempotency_key = 'idem-0009';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'COMPLETED', 'system', 'Payment completed', '2026-08-03 11:35:29'
FROM payments WHERE idempotency_key = 'idem-0010';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'COMPLETED', 'system', 'Payment completed', '2026-08-03 13:28:52'
FROM payments WHERE idempotency_key = 'idem-0011';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'FAILED', 'system', 'Source account is inactive', '2026-08-04 09:05:27'
FROM payments WHERE idempotency_key = 'idem-0012';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'COMPLETED', 'system', 'Payment completed', '2026-08-04 14:50:13'
FROM payments WHERE idempotency_key = 'idem-0013';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, 'CREATED', 'COMPLETED', 'system', 'Payment completed', '2026-08-05 17:21:05'
FROM payments WHERE idempotency_key = 'idem-0014';

INSERT INTO payment_history (payment_id, old_status, new_status, changed_by, remarks, changed_at)
SELECT payment_id, NULL, 'CREATED', 'system', 'Payment initialized', '2026-08-06 07:55:00'
FROM payments WHERE idempotency_key = 'idem-0015';