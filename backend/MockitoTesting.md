# Mockito Testing Guide

This project uses JUnit 5 + Mockito for unit tests and MockMvc for controller tests.

## Test Folder Structure

```text
src/test/java/com/neueda/__Code_Titans/
  ApplicationTests.java
  controller/
	PaymentControllerTest.java
  service/
	PaymentServiceTest.java
  repo/
	AccountRepoTest.java
	PaymentRepoTest.java
	PaymentHistoryRepoTest.java
```

## What Is Covered

### Controller tests (`PaymentControllerTest`)
- JSON response envelope validation (`success`, `message`, `data`)
- Success/failure behavior of `POST /payments`
- Not-found behavior for `GET /payments/{paymentId}`
- Bad-request behavior for invalid status updates
- History endpoint response for `GET /payments/{paymentId}/history`

### Service tests (`PaymentServiceTest`)
- Payment creation success path with audit entry
- Validation failure path (duplicate idempotency key)
- Allowed status transition with audit entry
- Rejected transition validation
- Missing payment behavior for status update
- History retrieval delegation to `AuditService`

### Repository tests (`repo/*Test`)
- `PaymentRepo`: find/count/save (insert + update path)
- `AccountRepo`: exists/findById/save generated key assignment
- `PaymentHistoryRepo`: latest record lookup/count/save generated key assignment

## Run Tests

```powershell
Set-Location "C:\Users\Administrator\106-Code_Titans\backend"
.\mvnw.cmd test
```

## Notes

- Repository tests use mocked `JdbcTemplate` so they are fast and isolated.
- Service tests verify business rules and audit calls without real DB access.
- Controller tests validate HTTP status and JSON contract using `MockMvc`.

