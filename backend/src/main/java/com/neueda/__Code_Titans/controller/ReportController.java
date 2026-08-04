package com.neueda.__Code_Titans.controller;

import java.time.LocalDate;
import java.util.List;

import com.neueda.__Code_Titans.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
public class ReportController {

	private final ReportService reportService;

	public ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	@GetMapping("/summary")
	public ReportService.SummaryReport getSummary() {
		return reportService.getSummary();
	}

	@GetMapping("/top-senders")
	public List<ReportService.PartyReport> getTopSenders(
			@RequestParam(name = "limit", required = false) Integer limit
	) {
		return limit != null ? reportService.getTopSenders(limit) : reportService.getTopSenders();
	}

	@GetMapping("/top-receivers")
	public List<ReportService.PartyReport> getTopReceivers(
			@RequestParam(name = "limit", required = false) Integer limit
	) {
		return limit != null ? reportService.getTopReceivers(limit) : reportService.getTopReceivers();
	}

	@GetMapping("/status")
	public List<ReportService.StatusReport> getStatusBreakdown() {
		return reportService.getStatusBreakdown();
	}

	@GetMapping("/daily")
	public List<ReportService.DailyReport> getDailyPayments(
			@RequestParam(name = "days", required = false) Integer days
	) {
		return days != null ? reportService.getDailyPayments(days) : reportService.getDailyPayments();
	}

	@GetMapping("/hourly")
	public List<ReportService.HourlyReport> getHourlyPayments(
			@RequestParam(name = "date", required = false)
			@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	) {
		return date != null ? reportService.getHourlyPayments(date) : reportService.getHourlyPayments();
	}

	@GetMapping("/largest")
	public List<ReportService.LargestPaymentReport> getLargestPayments(
			@RequestParam(name = "limit", required = false) Integer limit
	) {
		return limit != null ? reportService.getLargestPayments(limit) : reportService.getLargestPayments();
	}

	@GetMapping("/failures")
	public List<ReportService.FailureReport> getFailureReasons(
			@RequestParam(name = "limit", required = false) Integer limit
	) {
		return limit != null ? reportService.getFailureReasons(limit) : reportService.getFailureReasons();
	}
}
