package com.neueda.__Code_Titans.repo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.neueda.__Code_Titans.entity.PaymentHistory;

@Repository
public class PaymentHistoryRepo {

    private final JdbcTemplate jdbcTemplate;

    public PaymentHistoryRepo(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<PaymentHistory> paymentHistoryRowMapper = this::mapRow;

    public List<PaymentHistory> findByPaymentIdOrderByChangedAtAsc(Long paymentId) {
        String sql = """
                SELECT history_id, payment_id, old_status, new_status, changed_at, changed_by, remarks
                FROM payment_history
                WHERE payment_id = ?
                ORDER BY changed_at ASC
                """;
        return jdbcTemplate.query(sql, paymentHistoryRowMapper, paymentId);
    }

    public Optional<PaymentHistory> findFirstByPaymentIdOrderByChangedAtDesc(Long paymentId) {
        String sql = """
                SELECT history_id, payment_id, old_status, new_status, changed_at, changed_by, remarks
                FROM payment_history
                WHERE payment_id = ?
                ORDER BY changed_at DESC
                LIMIT 1
                """;
        List<PaymentHistory> results = jdbcTemplate.query(sql, paymentHistoryRowMapper, paymentId);
        return results.stream().findFirst();
    }

    public List<PaymentHistory> findByNewStatus(String newStatus) {
        String sql = """
                SELECT history_id, payment_id, old_status, new_status, changed_at, changed_by, remarks
                FROM payment_history
                WHERE new_status = ?
                """;
        return jdbcTemplate.query(sql, paymentHistoryRowMapper, newStatus);
    }

    public long countByNewStatus(String newStatus) {
        String sql = "SELECT COUNT(*) FROM payment_history WHERE new_status = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, newStatus);
        return count == null ? 0L : count;
    }

    public List<PaymentHistory> findByChangedAtBetween(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
                SELECT history_id, payment_id, old_status, new_status, changed_at, changed_by, remarks
                FROM payment_history
                WHERE changed_at BETWEEN ? AND ?
                ORDER BY changed_at ASC
                """;
        return jdbcTemplate.query(sql, paymentHistoryRowMapper, startTime, endTime);
    }

    public List<PaymentHistory> findByPaymentIdAndNewStatusOrderByChangedAtAsc(Long paymentId, String newStatus) {
        String sql = """
                SELECT history_id, payment_id, old_status, new_status, changed_at, changed_by, remarks
                FROM payment_history
                WHERE payment_id = ? AND new_status = ?
                ORDER BY changed_at ASC
                """;
        return jdbcTemplate.query(sql, paymentHistoryRowMapper, paymentId, newStatus);
    }

    private PaymentHistory mapRow(ResultSet rs, int rowNum) throws SQLException {
        PaymentHistory paymentHistory = new PaymentHistory();
        paymentHistory.setHistoryId(rs.getLong("history_id"));
        paymentHistory.setPaymentId(rs.getLong("payment_id"));
        paymentHistory.setOldStatus(rs.getString("old_status"));
        paymentHistory.setNewStatus(rs.getString("new_status"));
        paymentHistory.setChangedAt(rs.getObject("changed_at", LocalDateTime.class));
        paymentHistory.setChangedBy(rs.getString("changed_by"));
        paymentHistory.setRemarks(rs.getString("remarks"));
        return paymentHistory;
    }
}
