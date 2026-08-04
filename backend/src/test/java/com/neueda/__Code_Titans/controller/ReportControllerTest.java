package com.neueda.__Code_Titans.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.neueda.__Code_Titans.service.ReportService;

@ExtendWith(MockitoExtension.class)
public class ReportControllerTest {

	@Mock
	private ReportService reportService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new ReportController(reportService)).build();
	}

	@Test
	void getSummary_returnsOkWithSummaryPayload() throws Exception {
		ReportService.SummaryReport summary = new ReportService.SummaryReport(
				10L,
				3L,
				6L,
				2L,
				2L,
				new BigDecimal("1200.00"),
				new BigDecimal("60.00"));

		when(reportService.getSummary()).thenReturn(summary);

		mockMvc.perform(get("/reports/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.message").value("Summary report fetched successfully"))
				.andExpect(jsonPath("$.data.totalPayments").value(10))
				.andExpect(jsonPath("$.data.completedPayments").value(6));
	}

	@Test
	void getTopSenders_whenLimitProvided_usesLimitedServiceCall() throws Exception {
		ReportService.PartyReport sender = new ReportService.PartyReport(
				1L,
				"ACC-1001",
				"Rahul",
				"Demo Bank",
				5L,
				new BigDecimal("900.00"));

		when(reportService.getTopSenders(2)).thenReturn(List.of(sender));

		mockMvc.perform(get("/reports/top-senders").param("limit", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.message").value("Top senders fetched successfully"))
				.andExpect(jsonPath("$.data[0].accountId").value(1))
				.andExpect(jsonPath("$.data[0].paymentCount").value(5));

		verify(reportService).getTopSenders(2);
	}

	@Test
	void getTopSenders_whenLimitMissing_usesDefaultServiceCall() throws Exception {
		when(reportService.getTopSenders()).thenReturn(List.of());

		mockMvc.perform(get("/reports/top-senders"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.message").value("Top senders fetched successfully"));

		verify(reportService).getTopSenders();
	}

	@Test
	void getHourlyPayments_whenDateProvided_returnsHourlyData() throws Exception {
		ReportService.HourlyReport hourlyReport = new ReportService.HourlyReport(
				"09:00",
				4L,
				new BigDecimal("400.00"));

		when(reportService.getHourlyPayments(LocalDate.of(2026, 8, 4))).thenReturn(List.of(hourlyReport));

		mockMvc.perform(get("/reports/hourly").param("date", "2026-08-04"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.message").value("Hourly payments report fetched successfully"))
				.andExpect(jsonPath("$.data[0].reportHour").value("09:00"));

		verify(reportService).getHourlyPayments(LocalDate.of(2026, 8, 4));
	}

	@Test
	void getHourlyPayments_whenDateMissing_usesDefaultServiceCall() throws Exception {
		when(reportService.getHourlyPayments()).thenReturn(List.of());

		mockMvc.perform(get("/reports/hourly"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.message").value("Hourly payments report fetched successfully"));

		verify(reportService).getHourlyPayments();
	}

	@Test
	void getHourlyPayments_whenDateInvalid_returnsBadRequest() throws Exception {
		mockMvc.perform(get("/reports/hourly").param("date", "04-08-2026"))
				.andExpect(status().isBadRequest());
	}
}
