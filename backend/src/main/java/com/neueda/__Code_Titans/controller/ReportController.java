package com.neueda.__Code_Titans.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.neueda.__Code_Titans.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getSummary() {
        return buildResponse(HttpStatus.OK, "Summary report fetched successfully", reportService.getSummary());
    }

    @GetMapping(value = "/top-senders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getTopSenders(
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        List<ReportService.PartyReport> data =
                limit != null ? reportService.getTopSenders(limit) : reportService.getTopSenders();
        return buildResponse(HttpStatus.OK, "Top senders fetched successfully", data);
    }

    @GetMapping(value = "/top-receivers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getTopReceivers(
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        List<ReportService.PartyReport> data =
                limit != null ? reportService.getTopReceivers(limit) : reportService.getTopReceivers();
        return buildResponse(HttpStatus.OK, "Top receivers fetched successfully", data);
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStatusBreakdown() {
        return buildResponse(HttpStatus.OK, "Status breakdown fetched successfully", reportService.getStatusBreakdown());
    }

    @GetMapping(value = "/daily", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getDailyPayments(
            @RequestParam(name = "days", required = false) Integer days
    ) {
        List<ReportService.DailyReport> data =
                days != null ? reportService.getDailyPayments(days) : reportService.getDailyPayments();
        return buildResponse(HttpStatus.OK, "Daily payments report fetched successfully", data);
    }

    @GetMapping(value = "/hourly", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getHourlyPayments(
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<ReportService.HourlyReport> data =
                date != null ? reportService.getHourlyPayments(date) : reportService.getHourlyPayments();
        return buildResponse(HttpStatus.OK, "Hourly payments report fetched successfully", data);
    }

    @GetMapping(value = "/largest", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getLargestPayments(
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        List<ReportService.LargestPaymentReport> data =
                limit != null ? reportService.getLargestPayments(limit) : reportService.getLargestPayments();
        return buildResponse(HttpStatus.OK, "Largest payments fetched successfully", data);
    }

    @GetMapping(value = "/failures", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getFailureReasons(
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        List<ReportService.FailureReport> data =
                limit != null ? reportService.getFailureReasons(limit) : reportService.getFailureReasons();
        return buildResponse(HttpStatus.OK, "Failure reasons fetched successfully", data);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message, Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        body.put("data", data);
        return ResponseEntity.status(status).body(body);
    }
}
