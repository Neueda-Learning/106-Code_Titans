# Payment Processing System - Project Context

## Project Overview

This project is a Payment Processing System built using Spring Boot, MySQL, and React.

The application simulates the complete lifecycle of a financial payment without integrating with a real banking network.

The goal is to demonstrate:

- REST API development
- Payment lifecycle management
- Validation
- Audit history
- Reporting and analytics
- Clean layered architecture

---

# Tech Stack

Backend
- Java 17
- Spring Boot
- Spring Data JPA
- MySQL
- Maven

Frontend
- React
- AG Grid
- Chart.js / Recharts

Tools
- Git
- GitHub
- GitHub Copilot
- Postman

---

# Layered Architecture

Frontend (React)
│
▼
Controllers
│
▼
Services
│
▼
Repositories
│
▼
MySQL Database

Responsibilities

Frontend
- UI
- Forms
- Dashboard
- Reports

Controller
- Receives HTTP requests
- Calls service methods
- Returns responses

Service
- Business logic
- Validation
- Payment lifecycle
- Reporting logic

Repository
- Database operations
- CRUD

Database
- Stores all application data

---

# Database Tables

## accounts

Purpose

Stores dummy bank accounts.

Columns

- account_id
- account_number
- account_holder_name
- bank_name
- balance
- currency
- account_status
- created_at

---

## payments

Purpose

Stores current payment information.

Columns

- payment_id
- source_account_id
- destination_account_id
- amount
- currency
- reference
- status
- error_code
- error_message
- idempotency_key
- created_at
- updated_at

---

## payment_history

Purpose

Stores every payment status transition.

Columns

- history_id
- payment_id
- old_status
- new_status
- changed_at
- changed_by
- remarks

---

# Java Enums

Package

com.payment.enums

PaymentStatus

- CREATED
- VALIDATED
- SENT
- COMPLETED
- FAILED

AccountStatus

- ACTIVE
- BLOCKED
- CLOSED

ErrorCode

- INVALID_ACCOUNT
- INVALID_AMOUNT
- INSUFFICIENT_FUNDS
- SAME_ACCOUNT
- DUPLICATE_PAYMENT
- NETWORK_ERROR

Enums will be stored in MySQL using

@Enumerated(EnumType.STRING)

Database columns remain VARCHAR.

---

# Payment Lifecycle

Successful Payment

CREATED

↓

VALIDATED

↓

SENT

↓

COMPLETED

Failed Validation

CREATED

↓

FAILED

Failed Processing

CREATED

↓

VALIDATED

↓

SENT

↓

FAILED

---

# Validation Rules

The service layer performs validations.

Rules

1. Source account exists.

2. Destination account exists.

3. Source != Destination.

4. Amount > 0.

5. Currency is supported.

6. Sufficient balance.

7. Duplicate payment detection using idempotency key.

If any validation fails

Status

FAILED

ErrorCode is stored.

Example

INSUFFICIENT_FUNDS

---

# Status Changes

Every status update must

1. Update payment.status

2. Insert a row into payment_history

Example

CREATED

↓

VALIDATED

↓

SENT

↓

COMPLETED

Payment table stores only the latest status.

payment_history stores all transitions.

---

# Dummy Data

Initially

5 dummy accounts.

Example

Rahul

Anant

Alice

Bob

Charlie

Each account has

- Account Number
- Balance
- Currency

Frontend dropdown fetches these accounts using

GET /accounts

---

# Planned REST APIs

Account APIs

GET /accounts

GET /accounts/{id}

POST /accounts

Payment APIs

POST /payments

GET /payments

GET /payments/{id}

PUT /payments/{id}/status

Report APIs

GET /reports/summary

GET /reports/top-senders

GET /reports/top-receivers

GET /reports/status

GET /reports/daily

GET /reports/hourly

GET /reports/largest

GET /reports/failures

---

# Reports

Dashboard

- Payments Today
- Completed Payments
- Failed Payments
- Pending Payments
- Total Amount

Analytics

- Top Sender
- Top Receiver
- Daily Payments
- Hourly Payments
- Largest Payments
- Success Rate
- Failure Reasons
- Total Money Transferred
- Most Active User

---

# Suggested Project Structure

com.payment

controller

service

service.impl

repository

entity

dto

mapper

exception

config

enums

util

---

# Development Roadmap

Phase 1

✔ Setup Spring Boot

Phase 2

✔ Create MySQL tables

Phase 3

Create Enums

Phase 4

Create Entity classes

Account

Payment

PaymentHistory

Phase 5

Create Repositories

Phase 6

Insert Dummy Accounts

Phase 7

Build Account API

GET /accounts

Phase 8

Build Payment Creation API

Status = CREATED

Phase 9

Insert payment history

Phase 10

Implement Validation

Phase 11

Implement Status Lifecycle

Phase 12

Update Account Balances

Phase 13

Build Reports

Phase 14

Build React Frontend

---

# Git Strategy

Commit after every feature.

Example

Initial project setup

↓

Added entities

↓

Added repositories

↓

Added account APIs

↓

Added payment APIs

↓

Added validation

↓

Added lifecycle

↓

Added reports

↓

Frontend

---

# GitHub Copilot Guidelines

Use Copilot as a pair programmer.

Good prompts

- Create JPA Entity from SQL table.
- Generate Spring Data Repository.
- Generate Service interface.
- Generate DTO.
- Generate Controller.
- Generate JPQL query for reports.

Never accept generated code without understanding it.

---

# Current Progress

Completed

✔ Spring Boot Skeleton

✔ MySQL Tables

Next Task

1. Create Enums

2. Create Entity Classes

3. Create Repositories

4. Insert Dummy Data

5. Build Account API