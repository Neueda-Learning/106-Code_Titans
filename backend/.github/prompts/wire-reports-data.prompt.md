---
description: "Wire reports.html to fetch live payment data from the Spring Boot backend via api.js and update all UI components (cards, Chart.js charts, tables) with real data."
name: "Wire Reports Page to Backend Data"
argument-hint: "Optional: specific section to wire (cards | charts | tables | all)"
agent: "agent"
tools: [read_file, replace_string_in_file, create_file, grep_search]
---

# Wire Reports Page to Live Backend Data

You are wiring `nginx/reports.html` and `nginx/js/reports.js` so every UI element
is driven by real payment data fetched from the Spring Boot backend through `nginx/js/api.js`.

## Context Files — Read These First

- [api.js](../../nginx/js/api.js) — the only allowed API layer. Use `getPayments()`.
- [reports.html](../../nginx/reports.html) — the page to update.
- [css/reports.css](../../nginx/css/reports.css) — existing status/badge class names.

## Payment Object Shape (from api.js `getPayments()`)

```js
{
  paymentId:    string,          // e.g. "PAY-20260805-1234"
  status:       "COMPLETED" | "FAILED" | "PENDING" | "CREATED",
  amount:       number,          // raw numeric value
  currency:     string,          // "USD", "EUR", etc.
  senderName:   string,
  receiverName: string,
  createdAt:    ISO-8601 string  // "2026-08-05T14:30:00Z"
}
```

---

## Step 1 — Add IDs to reports.html for DOM Binding

Replace the hardcoded values in each section with the IDs below.
**Do NOT change layout, CSS classes, or structure — only add `id` attributes.**

### Summary cards
| Element | id |
|---|---|
| Total count `<div class="value">` | `total-count` |
| Total unit text `<div class="unit">` | `total-unit` |
| Completed count | `completed-count` |
| Completed unit | `completed-unit` |
| Failed count | `failed-count` |
| Failed unit | `failed-unit` |
| Pending count | `pending-count` |
| Pending unit | `pending-unit` |

### Tables
| Table `<tbody>` | id |
|---|---|
| Top Senders | `senders-tbody` |
| Top Receivers | `receivers-tbody` |
| Largest Payments | `largest-tbody` |
| Failure Reasons | `failure-tbody` |

### Loading overlay (add inside `<div class="reports-container">` at the top)
```html
<div id="reports-loading" style="display:none;text-align:center;padding:40px;font-size:15px;color:#6B7280;">
  <i class="fas fa-spinner fa-spin" style="margin-right:8px;"></i>Loading live data…
</div>
```

---

## Step 2 — Create nginx/js/reports.js

Create the file with this exact structure. Every section must update the DOM.
All API calls go through `getPayments()` — never call `fetch()` directly.

```js
// reports.js — Live data binding for reports.html
// Depends on: api.js (must be loaded first in reports.html)

document.addEventListener('DOMContentLoaded', loadReportsData);

async function loadReportsData() {
  showLoading(true);
  try {
    const payments = await getPayments();
    const stats    = computeStats(payments);

    updateSummaryCards(stats);
    updateStatusChart(stats);
    updateDailyChart(payments);
    updateHourlyChart(payments);
    updateSendersTable(payments);
    updateReceiversTable(payments);
    updateLargestTable(payments);
    updateFailureTable(payments);
  } catch (err) {
    console.error('Reports data load failed:', err);
  } finally {
    showLoading(false);
  }
}

/* ── Aggregations ── */

function computeStats(payments) {
  const total     = payments.length;
  const completed = payments.filter(p => p.status === 'COMPLETED').length;
  const failed    = payments.filter(p => p.status === 'FAILED').length;
  const pending   = payments.filter(p =>
    p.status === 'PENDING' || p.status === 'CREATED').length;
  return { total, completed, failed, pending };
}

/* ── Summary Cards ── */

function updateSummaryCards({ total, completed, failed, pending }) {
  setText('total-count',     total.toLocaleString());
  setText('total-unit',      'transactions');
  setText('completed-count', completed.toLocaleString());
  setText('completed-unit',  pct(completed, total) + '% success rate');
  setText('failed-count',    failed.toLocaleString());
  setText('failed-unit',     pct(failed, total) + '% failure rate');
  setText('pending-count',   pending.toLocaleString());
  setText('pending-unit',    pct(pending, total) + '% in progress');
}

/* ── Charts ── */

// Update doughnut chart in-place without destroying it
function updateStatusChart({ completed, failed, pending }) {
  const chart = getChart('statusChart');
  if (!chart) return;
  chart.data.datasets[0].data = [completed, failed, pending];
  chart.update();
}

// Group by weekday (last 7 days) and update bar chart
function updateDailyChart(payments) {
  const chart = getChart('dailyChart');
  if (!chart) return;

  const days = last7Days();                          // ['Mon','Tue',…]
  const completed = Array(7).fill(0);
  const failed    = Array(7).fill(0);

  const now = new Date();
  payments.forEach(p => {
    const d = new Date(p.createdAt);
    const diffDays = Math.floor((now - d) / 86400000);
    if (diffDays < 0 || diffDays >= 7) return;
    const idx = 6 - diffDays;
    if (p.status === 'COMPLETED') completed[idx]++;
    else if (p.status === 'FAILED') failed[idx]++;
  });

  chart.data.labels              = days;
  chart.data.datasets[0].data   = completed;
  chart.data.datasets[1].data   = failed;
  chart.update();
}

// Group by hour-of-day and update line chart
function updateHourlyChart(payments) {
  const chart = getChart('hourlyChart');
  if (!chart) return;

  const buckets = Array(12).fill(0);
  payments.forEach(p => {
    const h = new Date(p.createdAt).getHours();
    const idx = Math.floor(h / 2);                  // 0=00:00, 1=02:00 … 11=22:00
    buckets[idx]++;
  });

  chart.data.datasets[0].data = buckets;
  chart.update();
}

/* ── Tables ── */

function updateSendersTable(payments) {
  const map = groupBy(payments, 'senderName');
  const rows = topN(map, 5);
  setTableRows('senders-tbody', rows.map(([ name, list ], i) =>
    `<tr>
      <td>${rankBadge(i)}</td>
      <td>${esc(name)}</td>
      <td>${list.length.toLocaleString()}</td>
      <td class="amount">${formatMoney(sumAmount(list))}</td>
      <td>${formatMoney(sumAmount(list) / list.length)}</td>
    </tr>`
  ));
}

function updateReceiversTable(payments) {
  const map = groupBy(payments, 'receiverName');
  const rows = topN(map, 5);
  setTableRows('receivers-tbody', rows.map(([ name, list ], i) =>
    `<tr>
      <td>${rankBadge(i)}</td>
      <td>${esc(name)}</td>
      <td>${list.length.toLocaleString()}</td>
      <td class="amount">${formatMoney(sumAmount(list))}</td>
      <td>${formatMoney(sumAmount(list) / list.length)}</td>
    </tr>`
  ));
}

function updateLargestTable(payments) {
  const sorted = [...payments].sort((a, b) => b.amount - a.amount).slice(0, 10);
  setTableRows('largest-tbody', sorted.map(p =>
    `<tr>
      <td>${esc(p.paymentId)}</td>
      <td>${esc(p.senderName   || '—')}</td>
      <td>${esc(p.receiverName || '—')}</td>
      <td class="amount">${formatMoney(p.amount)}</td>
      <td>${statusBadge(p.status)}</td>
      <td>${formatDate(p.createdAt)}</td>
    </tr>`
  ));
}

function updateFailureTable(payments) {
  const failed = payments.filter(p => p.status === 'FAILED');
  const total  = failed.length || 1;

  // Group by errorCode → fall back to a generic bucket label
  const reasonMap = {};
  failed.forEach(p => {
    const key = p.errorCode || p.errorMessage || 'Unknown Error';
    reasonMap[key] = (reasonMap[key] || 0) + 1;
  });

  const sorted = Object.entries(reasonMap)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5);

  setTableRows('failure-tbody', sorted.map(([ reason, count ]) =>
    `<tr>
      <td>${esc(reason)}</td>
      <td><span class="badge danger">${count}</span></td>
      <td>${pct(count, total)}%</td>
      <td>—</td>
    </tr>`
  ));
}

/* ── Helpers ── */

function setText(id, text) {
  const el = document.getElementById(id);
  if (el) el.textContent = text;
}

function setTableRows(tbodyId, htmlRows) {
  const el = document.getElementById(tbodyId);
  if (!el) return;
  if (htmlRows.length === 0) {
    el.innerHTML = '<tr><td colspan="10" style="text-align:center;color:#9CA3AF;padding:24px">No data available</td></tr>';
  } else {
    el.innerHTML = htmlRows.join('');
  }
}

function showLoading(visible) {
  const el = document.getElementById('reports-loading');
  if (el) el.style.display = visible ? 'block' : 'none';
}

// Get an existing Chart.js instance by canvas id
function getChart(canvasId) {
  const canvas = document.getElementById(canvasId);
  if (!canvas) return null;
  return Chart.getChart(canvas);
}

function groupBy(arr, key) {
  return arr.reduce((map, item) => {
    const k = item[key] || 'Unknown';
    if (!map[k]) map[k] = [];
    map[k].push(item);
    return map;
  }, {});
}

function topN(map, n) {
  return Object.entries(map)
    .sort((a, b) => b[1].length - a[1].length)
    .slice(0, n);
}

function sumAmount(list) {
  return list.reduce((s, p) => s + (Number(p.amount) || 0), 0);
}

function pct(part, total) {
  if (!total) return '0.0';
  return ((part / total) * 100).toFixed(1);
}

function formatMoney(n) {
  return '$' + Number(n).toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 });
}

function formatDate(iso) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

function esc(str) {
  const d = document.createElement('div');
  d.textContent = str ?? '';
  return d.innerHTML;
}

function rankBadge(i) {
  if (i === 0) return '<span class="rank-badge rank-1">1</span>';
  if (i === 1) return '<span class="rank-badge rank-2">2</span>';
  if (i === 2) return '<span class="rank-badge rank-3">3</span>';
  return `<span class="badge">${i + 1}</span>`;
}

function statusBadge(status) {
  const map = {
    COMPLETED: 'completed',
    FAILED:    'failed',
    PENDING:   'pending',
    CREATED:   'created'
  };
  const cls = map[status] || 'created';
  const label = status ? status.charAt(0) + status.slice(1).toLowerCase() : '—';
  return `<span class="status-badge ${cls}">${label}</span>`;
}

function last7Days() {
  const days = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() - (6 - i));
    return days[d.getDay()];
  });
}
```

---

## Step 3 — Add script tag to reports.html

Inside `reports.html`, **before** the closing `</body>` tag, ensure these two scripts are present **in this order**:

```html
<script src="js/api.js"></script>
<script src="js/reports.js"></script>
```

Remove any inline `initCharts()` call from the `DOMContentLoaded` listener **only if** `reports.js` already calls it. Keep the Chart.js initialization code that creates the three Chart instances — `reports.js` calls `Chart.getChart()` to update them in-place.

---

## Step 4 — Verify

After all changes, confirm:

1. `reports.html` has `id` attributes on every card value, every `<tbody>`, and the loading div.
2. `nginx/js/reports.js` exists and is loadable without errors (no missing functions).
3. `api.js` is included **before** `reports.js`.
4. Charts still render with static data on first paint; live data overlays them once `getPayments()` resolves.
5. Tables fall back to "No data available" when the API returns an empty list.

---

## Constraints

- **Never** call `fetch()` or `XMLHttpRequest` directly — always use `getPayments()` from `api.js`.
- **Never** modify `api.js`.
- **Never** change layout, CSS classes, or colour variables.
- Keep all existing Chart.js `new Chart(…)` calls intact — `reports.js` only calls `.update()` on existing instances.
- Use `esc()` to sanitise all user-supplied strings before inserting into the DOM.
