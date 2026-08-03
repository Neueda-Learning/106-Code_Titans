CREATE TABLE IF NOT EXISTS accounts(
account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
account_number VARCHAR(20) NOT NULL UNIQUE,
account_holder_name VARCHAR(100) NOT NULL,
bank_name VARCHAR(100),
balance DECIMAL(12, 2) NOT NULL,
currency VARCHAR(3) NOT NULL,
account_status VARCHAR(20) DEFAULT 'ACTIVE',
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payments(
payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
source_account_id BIGINT NOT NULL,
destination_account_id BIGINT NOT NULL,
amount DECIMAL(12, 2) NOT NULL,
currency VARCHAR(3) NOT NULL,
reference VARCHAR(255),
status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
error_code VARCHAR(50),
error_message VARCHAR(255),
idempotency_key VARCHAR(100) UNIQUE,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
CONSTRAINT fk_source_account FOREIGN KEY (source_account_id) REFERENCES accounts(account_id),
CONSTRAINT fk_destination_account FOREIGN KEY (destination_account_id) REFERENCES accounts(account_id)
);

CREATE TABLE IF NOT EXISTS payment_history(
history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
payment_id BIGINT NOT NULL,
old_status VARCHAR(20),
new_status VARCHAR(20) NOT NULL,
changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
changed_by VARCHAR(100),
remarks VARCHAR(255),
CONSTRAINT fk_payment_history FOREIGN KEY (payment_id) REFERENCES payments(payment_id)
);
