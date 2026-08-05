# Payment Processing System
# Frontend Context

---

## Project Overview

This frontend is developed for a **Payment Processing System**.

The application is intended for a **single administrator**.

There is **NO authentication**.

There is **NO login/logout**.

The frontend communicates with the Spring Boot REST APIs using JavaScript Fetch API.

---

# Technology Stack

Frontend

- HTML5
- CSS3
- JavaScript (ES6)
- Fetch API
- Chart.js
- Font Awesome

Backend

- Spring Boot
- REST APIs

Database

- MySQL

Deployment

- Nginx
- Docker
- Jenkins

---

# Folder Structure

frontend/

│── index.html
│── dashboard.html
│── payments.html
│── reports.html
│── payment-details.html
│
├── css/
│      style.css
│      dashboard.css
│      payments.css
│      reports.css
│
├── js/
│      api.js
│      dashboard.js
│      payments.js
│      reports.js
│      common.js
│
├── assets/
│      images/
│      icons/

This folder structure should remain unchanged unless discussed by the team.

---

# Theme

Application Theme

Modern Banking Dashboard

Primary Color

#1E3A8A

Secondary

#2563EB

Background

#F8FAFC

Card

White

Success

Green

#22C55E

Failed

Red

#EF4444

Pending

Orange

#F59E0B

Text

#1F2937

Border

#E5E7EB

Font

Poppins

---

# Navigation

Every page should have the same navigation.

Dashboard

Payments

Reports

Payment Details

No Login Page

No Logout Button

No Registration

---

# index.html

Purpose

Landing page.

Redirect user to Dashboard or act as the home page.

---

# dashboard.html

Purpose

Display overall summary of the payment system.

Features

- Total Payments

- Completed Payments

- Failed Payments

- Pending Payments

- Recent Payments

- Status Distribution Chart

- Daily Payment Chart

Backend APIs

Dashboard Summary

Recent Payments

---

# payments.html

Purpose

Display all payments.

Features

Search Payment ID

Status Filter

Refresh Button

Payment Table

View Button

Payment Table Columns

Payment ID

Sender

Receiver

Amount

Currency

Status

Created Date

Action

When user clicks

View

↓

Open

payment-details.html

Backend APIs

GET /payments

GET /payments?status=

GET /payments?id=

---

# payment-details.html

Purpose

Display complete information about one payment.

Features

Payment Information

Sender Details

Receiver Details

Transaction Details

Current Status

Payment Timeline

Failure Information

Timeline Example

CREATED

↓

VALIDATED

↓

SENT

↓

COMPLETED

or

CREATED

↓

FAILED

Backend APIs

GET /payments/{id}

GET /payments/{id}/history

---

# reports.html

Purpose

Display payment analytics.

Features

Summary Cards

Top Sender

Top Receiver

Daily Payments

Hourly Payments

Largest Payments

Failure Reasons

Charts

Pie Chart

Bar Chart

Line Chart

Backend APIs

GET /reports/summary

GET /reports/top-senders

GET /reports/top-receivers

GET /reports/daily

GET /reports/status

GET /reports/hourly

GET /reports/largest

GET /reports/failures

---

# CSS Files

style.css

Purpose

Global styles.

Contains

Header

Navigation

Buttons

Cards

Tables

Forms

Typography

Colors

Common classes

Do not duplicate styles in page CSS.

---

dashboard.css

Only dashboard-specific styles.

---

payments.css

Only payment page styles.

---

reports.css

Only reports page styles.

---

# JavaScript Files

api.js

Very Important

This file should contain all backend API calls.

Example

getPayments()

getPaymentById()

getPaymentHistory()

getDashboardSummary()

getReports()

No other JS file should directly use fetch().

---

dashboard.js

Load dashboard data.

Load summary cards.

Load recent payments.

Load dashboard charts.

---

payments.js

Load payment table.

Search payments.

Filter payments.

Open payment details.

---

reports.js

Load report data.

Render charts.

Display analytics.

---

common.js

Contains reusable JavaScript.

Examples

Date formatting

Currency formatting

Status badge colors

Utility functions

Shared event handlers

---

# Common UI Rules

Every page should have

Same Header

Same Navigation

Same Theme

Same Colors

Same Fonts

Same Button Style

Same Card Style

Same Table Style

Status Colors

Completed

Green

Failed

Red

Pending

Orange

Created

Blue

Validated

Purple

Sent

Dark Blue

---

# Coding Standards

Separate HTML CSS JavaScript.

No inline CSS.

No inline JavaScript.

Use meaningful variable names.

Comment important logic.

Keep functions small.

Reuse code whenever possible.

---

# API Communication Rules

HTML

↓

JavaScript

↓

api.js

↓

Spring Boot REST API

↓

Database

Never call backend APIs directly from HTML.

All backend communication should happen through api.js.

---

# Team Guidelines

Keep UI consistent.

Follow same naming convention.

Use same colors.

Do not change folder structure.

Commit after completing each feature.

Test every page before pushing.

---

# Final Goal

Build a clean and professional Payment Processing Dashboard capable of

- Viewing payment summaries

- Viewing all payments

- Viewing payment lifecycle

- Viewing reports

- Monitoring payment processing

using HTML, CSS and JavaScript with Spring Boot backend integration.