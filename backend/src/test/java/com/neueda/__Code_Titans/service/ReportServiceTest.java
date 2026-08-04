package com.neueda.__Code_Titans.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	private ReportService reportService;

	@BeforeEach
	void setUp() {
		reportService = new ReportService(jdbcTemplate);
	}

	@Test
	void getSummary_returnsSummaryFromJdbcLayer() {
		ReportService.SummaryReport expected = new ReportService.SummaryReport(
				20L,
				5L,
				12L,
				4L,
				4L,
				new BigDecimal("3200.00"),
				new BigDecimal("60.00"));

		when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class))).thenReturn(expected);

		ReportService.SummaryReport result = reportService.getSummary();

		assertNotNull(result);
		assertEquals(20L, result.totalPayments());
		assertEquals(12L, result.completedPayments());
		assertEquals(new BigDecimal("3200.00"), result.totalAmount());
	}

	@Test
	@SuppressWarnings("unchecked")
	void getTopSenders_whenLimitNotPositive_usesDefaultLimit() {
		ReportService.PartyReport row = new ReportService.PartyReport(
				1L,
				"ACC-1",
				"Rahul",
				"Demo",
				3L,
				new BigDecimal("100.00"));

		when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(5))).thenReturn(List.of(row));

		List<ReportService.PartyReport> result = reportService.getTopSenders(0);

		assertEquals(1, result.size());
		assertEquals("ACC-1", result.get(0).accountNumber());
	}

	@Test
	@SuppressWarnings("unchecked")
	void getDailyPayments_whenDaysNotPositive_returnsDataForSafeRange() {
		ReportService.DailyReport day = new ReportService.DailyReport(
				LocalDate.of(2026, 8, 4),
				2L,
				new BigDecimal("150.00"));

		when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any())).thenReturn(List.of(day));

		List<ReportService.DailyReport> result = reportService.getDailyPayments(0);

		assertEquals(1, result.size());
		assertEquals(LocalDate.of(2026, 8, 4), result.get(0).reportDate());
	}

	@Test
	@SuppressWarnings("unchecked")
	void getHourlyPayments_whenDateNull_fallsBackToCurrentDateWindow() {
		ReportService.HourlyReport hour = new ReportService.HourlyReport(
				"09:00",
				4L,
				new BigDecimal("700.00"));

		when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any())).thenReturn(List.of(hour));

		List<ReportService.HourlyReport> result = reportService.getHourlyPayments((LocalDate) null);

		assertEquals(1, result.size());
		assertEquals("09:00", result.get(0).reportHour());
	}

	@Test
	@SuppressWarnings("unchecked")
	void getLargestPayments_whenLimitNotPositive_usesDefaultLimit() {
		ReportService.LargestPaymentReport largest = new ReportService.LargestPaymentReport(
				99L,
				"invoice",
				new BigDecimal("999.00"),
				"USD",
				"COMPLETED",
				null,
				"S-001",
				"Sender",
				"D-001",
				"Receiver");

		when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(10))).thenReturn(List.of(largest));

		List<ReportService.LargestPaymentReport> result = reportService.getLargestPayments(-1);

		assertEquals(1, result.size());
		assertEquals(99L, result.get(0).paymentId());
	}

	@Test
	@SuppressWarnings("unchecked")
	void getFailureReasons_whenLimitNotPositive_usesDefaultLimit() {
		ReportService.FailureReport failure = new ReportService.FailureReport(
				"INVALID_ACCOUNT",
				"Source account does not exist",
				3L);

		when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(10))).thenReturn(List.of(failure));

		List<ReportService.FailureReport> result = reportService.getFailureReasons(0);

		assertEquals(1, result.size());
		assertEquals("INVALID_ACCOUNT", result.get(0).errorCode());
	}
}
