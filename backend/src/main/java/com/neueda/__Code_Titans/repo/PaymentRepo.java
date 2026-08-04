package com.neueda.__Code_Titans.repo;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.neueda.__Code_Titans.entity.Payments;

@Repository
public class PaymentRepo {

    private static final String BASE_SELECT = """
            SELECT payment_id,
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
            FROM payments
            """;

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Payments> paymentRowMapper = this::mapPayment;

    public PaymentRepo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Payments save(Payments payment) {
        if (payment.getPaymentId() == null) {
            return insert(payment);
        }
        update(payment);
        return payment;
    }

    public Optional<Payments> findById(Long paymentId) {
        List<Payments> payments = jdbcTemplate.query(
                BASE_SELECT + "WHERE payment_id = ?",
                paymentRowMapper,
                paymentId);
        return payments.stream().findFirst();
    }

    public List<Payments> findAll() {
        return jdbcTemplate.query(BASE_SELECT + "ORDER BY payment_id DESC", paymentRowMapper);
    }

    public Optional<Payments> findByIdempotencyKey(String idempotencyKey) {
        List<Payments> payments = jdbcTemplate.query(
                BASE_SELECT + "WHERE idempotency_key = ?",
                paymentRowMapper,
                idempotencyKey);
        return payments.stream().findFirst();
    }

    public boolean existsById(Long paymentId) {
        return findById(paymentId).isPresent();
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payments", Long.class);
        return count != null ? count : 0L;
    }

    public void deleteById(Long paymentId) {
        jdbcTemplate.update("DELETE FROM payments WHERE payment_id = ?", paymentId);
    }

    private Payments insert(Payments payment) {
        String sql = """
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
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setLong(1, payment.getSourceAccountId());
            preparedStatement.setLong(2, payment.getDestinationAccountId());
            preparedStatement.setBigDecimal(3, payment.getAmount());
            preparedStatement.setString(4, payment.getCurrency());
            preparedStatement.setString(5, payment.getReference());
            preparedStatement.setString(6, payment.getStatus());
            preparedStatement.setString(7, payment.getErrorCode());
            preparedStatement.setString(8, payment.getErrorMessage());
            preparedStatement.setString(9, payment.getIdempotencyKey());
            return preparedStatement;
        }, keyHolder);

        Number generatedKey = keyHolder.getKey();
        if (generatedKey != null) {
            payment.setPaymentId(generatedKey.longValue());
        }
        return payment;
    }

    private void update(Payments payment) {
        String sql = """
                UPDATE payments
                SET source_account_id = ?,
                    destination_account_id = ?,
                    amount = ?,
                    currency = ?,
                    reference = ?,
                    status = ?,
                    error_code = ?,
                    error_message = ?,
                    idempotency_key = ?
                WHERE payment_id = ?
                """;

        jdbcTemplate.update(sql,
                payment.getSourceAccountId(),
                payment.getDestinationAccountId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReference(),
                payment.getStatus(),
                payment.getErrorCode(),
                payment.getErrorMessage(),
                payment.getIdempotencyKey(),
                payment.getPaymentId());
    }

    private Payments mapPayment(ResultSet resultSet, int rowNum) throws SQLException {
        Payments payment = new Payments();
        payment.setPaymentId(resultSet.getLong("payment_id"));
        payment.setSourceAccountId(resultSet.getLong("source_account_id"));
        payment.setDestinationAccountId(resultSet.getLong("destination_account_id"));
        payment.setAmount(resultSet.getBigDecimal("amount"));
        payment.setCurrency(resultSet.getString("currency"));
        payment.setReference(resultSet.getString("reference"));
        payment.setStatus(resultSet.getString("status"));
        payment.setErrorCode(resultSet.getString("error_code"));
        payment.setErrorMessage(resultSet.getString("error_message"));
        payment.setIdempotencyKey(resultSet.getString("idempotency_key"));

        Timestamp createdAt = resultSet.getTimestamp("created_at");
        if (createdAt != null) {
            payment.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = resultSet.getTimestamp("updated_at");
        if (updatedAt != null) {
            payment.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return payment;
    }
}
