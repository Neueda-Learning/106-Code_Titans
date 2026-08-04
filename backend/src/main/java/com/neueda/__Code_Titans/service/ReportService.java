package com.neueda.__Code_Titans.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@SuppressWarnings("unused")
public class ReportService {

	private final JdbcTemplate jdbcTemplate;

	public ReportService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public SummaryReport getSummary() {
		String sql = """
				SELECT
					COUNT(*) AS total_payments,
					COALESCE(SUM(CASE WHEN DATE(created_at) = CURDATE() THEN 1 ELSE 0 END), 0) AS payments_today,
					COALESCE(SUM(CASE WHEN status IN ('COMPLETED', 'SUCCESS') THEN 1 ELSE 0 END), 0) AS completed_payments,
					COALESCE(SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END), 0) AS failed_payments,
					COALESCE(SUM(CASE WHEN status IN ('CREATED', 'VALIDATED', 'SENT') THEN 1 ELSE 0 END), 0) AS pending_payments,
					COALESCE(SUM(CASE WHEN status IN ('COMPLETED', 'SUCCESS') THEN amount ELSE 0 END), 0) AS total_amount,
					COALESCE(ROUND(
						100.0 * SUM(CASE WHEN status IN ('COMPLETED', 'SUCCESS') THEN 1 ELSE 0 END) / NULLIF(COUNT(*), 0),
						2
					), 0) AS success_rate
				FROM payments
				""";

		return jdbcTemplate.queryForObject(sql, (ResultSet rs, int rowNum) -> new SummaryReport(
				rs.getLong("total_payments"),
				rs.getLong("payments_today"),
				rs.getLong("completed_payments"),
				rs.getLong("failed_payments"),
				rs.getLong("pending_payments"),
				rs.getBigDecimal("total_amount"),
				rs.getBigDecimal("success_rate")
		));
	}

	public List<PartyReport> getTopSenders() {
		return getTopSenders(5);
	}

	public List<PartyReport> getTopSenders(int limit) {
		String sql = """
				SELECT
					a.account_id,
					a.account_number,
					a.account_holder_name,
					a.bank_name,
					COUNT(*) AS payment_count,
					COALESCE(SUM(p.amount), 0) AS total_amount
				FROM payments p
				INNER JOIN accounts a ON a.account_id = p.source_account_id
				WHERE p.status IN ('COMPLETED', 'SUCCESS')
				GROUP BY a.account_id, a.account_number, a.account_holder_name, a.bank_name
				ORDER BY total_amount DESC, payment_count DESC, a.account_holder_name ASC
				LIMIT ?
				""";

		return jdbcTemplate.query(sql, partyReportMapper(), safeLimit(limit, 5));
	}

	public List<PartyReport> getTopReceivers() {
		return getTopReceivers(5);
	}

	public List<PartyReport> getTopReceivers(int limit) {
		String sql = """
				SELECT
					a.account_id,
					a.account_number,
					a.account_holder_name,
					a.bank_name,
					COUNT(*) AS payment_count,
					COALESCE(SUM(p.amount), 0) AS total_amount
				FROM payments p
				INNER JOIN accounts a ON a.account_id = p.destination_account_id
				WHERE p.status IN ('COMPLETED', 'SUCCESS')
				GROUP BY a.account_id, a.account_number, a.account_holder_name, a.bank_name
				ORDER BY total_amount DESC, payment_count DESC, a.account_holder_name ASC
				LIMIT ?
				""";

		return jdbcTemplate.query(sql, partyReportMapper(), safeLimit(limit, 5));
	}

	public List<StatusReport> getStatusBreakdown() {
		String sql = """
				SELECT
					CASE
						WHEN status = 'SUCCESS' THEN 'COMPLETED'
						ELSE status
					END AS normalized_status,
					COUNT(*) AS payment_count,
					COALESCE(SUM(amount), 0) AS total_amount
				FROM payments
				GROUP BY CASE
					WHEN status = 'SUCCESS' THEN 'COMPLETED'
					ELSE status
				END
				ORDER BY payment_count DESC, normalized_status ASC
				""";

		return jdbcTemplate.query(sql, statusReportMapper());
	}

	public List<DailyReport> getDailyPayments() {
		return getDailyPayments(7);
	}

	public List<DailyReport> getDailyPayments(int days) {
		int safeDays = Math.max(days, 1);
		LocalDate startDate = LocalDate.now().minusDays(safeDays - 1L);
		LocalDateTime start = startDate.atStartOfDay();
		LocalDateTime end = LocalDate.now().plusDays(1L).atStartOfDay();

		String sql = """
				SELECT
					DATE(created_at) AS report_date,
					COUNT(*) AS payment_count,
					COALESCE(SUM(amount), 0) AS total_amount
				FROM payments
				WHERE created_at >= ?
				  AND created_at < ?
				GROUP BY DATE(created_at)
				ORDER BY report_date ASC
				""";

		return jdbcTemplate.query(sql, dailyReportMapper(), start, end);
	}

	public List<HourlyReport> getHourlyPayments() {
		return getHourlyPayments(LocalDate.now());
	}

	public List<HourlyReport> getHourlyPayments(LocalDate date) {
		LocalDate safeDate = date != null ? date : LocalDate.now();
		LocalDateTime start = safeDate.atStartOfDay();
		LocalDateTime end = safeDate.plusDays(1L).atStartOfDay();

		String sql = """
				SELECT
					HOUR(created_at) AS report_hour,
					COUNT(*) AS payment_count,
					COALESCE(SUM(amount), 0) AS total_amount
				FROM payments
				WHERE created_at >= ?
				  AND created_at < ?
				GROUP BY HOUR(created_at)
				ORDER BY report_hour ASC
				""";

		return jdbcTemplate.query(sql, hourlyReportMapper(), start, end);
	}

	public List<LargestPaymentReport> getLargestPayments() {
		return getLargestPayments(10);
	}

	public List<LargestPaymentReport> getLargestPayments(int limit) {
		String sql = """
				SELECT
					p.payment_id,
					p.reference,
					p.amount,
					p.currency,
					CASE
						WHEN p.status = 'SUCCESS' THEN 'COMPLETED'
						ELSE p.status
					END AS normalized_status,
					p.created_at,
					sa.account_number AS source_account_number,
					sa.account_holder_name AS source_account_holder_name,
					da.account_number AS destination_account_number,
					da.account_holder_name AS destination_account_holder_name
				FROM payments p
				INNER JOIN accounts sa ON sa.account_id = p.source_account_id
				INNER JOIN accounts da ON da.account_id = p.destination_account_id
				WHERE p.status IN ('COMPLETED', 'SUCCESS')
				ORDER BY p.amount DESC, p.created_at DESC
				LIMIT ?
				""";

		return jdbcTemplate.query(sql, largestPaymentMapper(), safeLimit(limit, 10));
	}

	public List<FailureReport> getFailureReasons() {
		return getFailureReasons(10);
	}

	public List<FailureReport> getFailureReasons(int limit) {
		String sql = """
				SELECT
					error_code,
					error_message,
					COUNT(*) AS failure_count
				FROM payments
				WHERE status = 'FAILED'
				GROUP BY error_code, error_message
				ORDER BY failure_count DESC, error_code ASC
				LIMIT ?
				""";

		return jdbcTemplate.query(sql, failureReportMapper(), safeLimit(limit, 10));
	}

	private RowMapper<PartyReport> partyReportMapper() {
		return (ResultSet rs, int rowNum) -> new PartyReport(
				rs.getLong("account_id"),
				rs.getString("account_number"),
				rs.getString("account_holder_name"),
				rs.getString("bank_name"),
				rs.getLong("payment_count"),
				rs.getBigDecimal("total_amount")
		);
	}

	private RowMapper<StatusReport> statusReportMapper() {
		return (ResultSet rs, int rowNum) -> new StatusReport(
				rs.getString("normalized_status"),
				rs.getLong("payment_count"),
				rs.getBigDecimal("total_amount")
		);
	}

	private RowMapper<DailyReport> dailyReportMapper() {
		return (ResultSet rs, int rowNum) -> new DailyReport(
				rs.getDate("report_date").toLocalDate(),
				rs.getLong("payment_count"),
				rs.getBigDecimal("total_amount")
		);
	}

	private RowMapper<HourlyReport> hourlyReportMapper() {
		return (ResultSet rs, int rowNum) -> new HourlyReport(
				String.format("%02d:00", rs.getInt("report_hour")),
				rs.getLong("payment_count"),
				rs.getBigDecimal("total_amount")
		);
	}

	private RowMapper<LargestPaymentReport> largestPaymentMapper() {
		return (ResultSet rs, int rowNum) -> new LargestPaymentReport(
				rs.getLong("payment_id"),
				rs.getString("reference"),
				rs.getBigDecimal("amount"),
				rs.getString("currency"),
				rs.getString("normalized_status"),
				rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
				rs.getString("source_account_number"),
				rs.getString("source_account_holder_name"),
				rs.getString("destination_account_number"),
				rs.getString("destination_account_holder_name")
		);
	}

	private RowMapper<FailureReport> failureReportMapper() {
		return (ResultSet rs, int rowNum) -> new FailureReport(
				rs.getString("error_code"),
				rs.getString("error_message"),
				rs.getLong("failure_count")
		);
	}

	private int safeLimit(int requestedLimit, int defaultLimit) {
		return requestedLimit > 0 ? requestedLimit : defaultLimit;
	}

	public record SummaryReport(
			long totalPayments,
			long paymentsToday,
			long completedPayments,
			long failedPayments,
			long pendingPayments,
			BigDecimal totalAmount,
			BigDecimal successRate
	) {
	}

	public record PartyReport(
			Long accountId,
			String accountNumber,
			String accountHolderName,
			String bankName,
			long paymentCount,
			BigDecimal totalAmount
	) {
	}

	public record StatusReport(
			String status,
			long paymentCount,
			BigDecimal totalAmount
	) {
	}

	public record DailyReport(
			LocalDate reportDate,
			long paymentCount,
			BigDecimal totalAmount
	) {
	}

	public record HourlyReport(
			String reportHour,
			long paymentCount,
			BigDecimal totalAmount
	) {
	}

	public record LargestPaymentReport(
			Long paymentId,
			String reference,
			BigDecimal amount,
			String currency,
			String status,
			LocalDateTime createdAt,
			String sourceAccountNumber,
			String sourceAccountHolderName,
			String destinationAccountNumber,
			String destinationAccountHolderName
	) {
	}

	public record FailureReport(
			String errorCode,
			String errorMessage,
			long failureCount
	) {
	}
}
