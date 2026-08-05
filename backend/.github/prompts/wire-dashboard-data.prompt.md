---
description: "Wire dashboard.html/dashboard.js to fetch live payment data from the Spring Boot backend via api.js and update all UI components (summary cards, Chart.js charts, recent-payments table) replacing all mock/hardcoded data."
name: "Wire Dashboard Page to Backend Data"
argument-hint: "Optional: specific section to wire (cards | charts | table | all)"
agent: "agent"
tools: [read_file, replace_string_in_file, grep_search]
---

# Wire Dashboard Page to Live Backend Data

You are replacing all `dashboardMockData` references in `nginx/js/dashboard.js`
so every UI element is driven by real payment data from the Spring Boot backend
fetched exclusively through `nginx/js/api.js`.

## Context Files — Read These First

- [api.js](../../nginx/js/api.js) — the only allowed API layer. Use `getPayments()`.
- [dashboard.js](../../nginx/js/dashboard.js) — file to update.
- [dashboard.html](../../nginx/dashboard.html) — DOM IDs used by dashboard.js.

## Payment Object Shape (from api.js `getPayments()`)

```js
{
  paymentId:    string,    // e.g. "PAY-20260805-1234"
  status:       "COMPLETED" | "FAILED" | "PENDING" | "CREATED" | "VALIDATED" | "SENT",
  amount:       number,
  currency:     string,    // "USD", "EUR", …
  senderName:   string,
  receiverName: string,
  createdAt:    ISO-8601   // "2026-08-05T14:30:00Z"
}
```

## DOM IDs in dashboard.html

| ID | Element | Updated by |
|---|---|---|
| `totalPayments` | `<h2>` — total count | `renderSummaryCards()` |
| `completedPayments` | `<h2>` — completed count | `renderSummaryCards()` |
| `pendingPayments` | `<h2>` — pending count | `renderSummaryCards()` |
| `failedPayments` | `<h2>` — failed count | `renderSummaryCards()` |
| `statusChart` | `<canvas>` — doughnut chart | `renderStatusChart()` |
| `dailyChart` | `<canvas>` — line chart | `renderDailyChart()` |
| `recentPaymentsBody` | `<tbody>` — recent payments | `renderRecentPayments()` |
| `lastUpdated` | `<p>` — refresh timestamp | `updateLastRefresh()` |

---

## Steps

### 1 — Remove `dashboardMockData`

Delete the entire `const dashboardMockData = { … };` block from the top of
`dashboard.js`. It will be replaced by computed values derived from the API.

### 2 — Add a top-level async loader

Replace the `document.addEventListener("DOMContentLoaded", …)` block at the
bottom of `dashboard.js` with:

```js
document.addEventListener("DOMContentLoaded", () => {
  bindDashboardActions();
  loadDashboardData();
});

async function loadDashboardData() {
  try {
    const payments = await getPayments();
    renderDashboard(payments);
  } catch (err) {
    console.error("Dashboard data load failed:", err);
    renderDashboard([]);   // render with empty state — never crash the page
  }
}
```

### 3 — Update `renderDashboard(payments)`

Change the signature so it accepts the live payments array and passes it down:

```js
function renderDashboard(payments) {
  renderSummaryCards(payments);
  renderRecentPayments(payments);
  renderStatusChart(payments);
  renderDailyChart(payments);
  updateLastRefresh();
}
```

### 4 — Update `renderSummaryCards(payments)`

```js
function renderSummaryCards(payments) {
  const total     = payments.length;
  const completed = payments.filter(p => p.status === "COMPLETED").length;
  const pending   = payments.filter(p =>
    p.status === "PENDING" || p.status === "CREATED" ||
    p.status === "VALIDATED" || p.status === "SENT").length;
  const failed    = payments.filter(p => p.status === "FAILED").length;

  document.getElementById("totalPayments").textContent     = total.toLocaleString("en-US");
  document.getElementById("completedPayments").textContent = completed.toLocaleString("en-US");
  document.getElementById("pendingPayments").textContent   = pending.toLocaleString("en-US");
  document.getElementById("failedPayments").textContent    = failed.toLocaleString("en-US");
}
```

### 5 — Update `renderStatusChart(payments)`

Compute counts from the payments array; update the existing chart instance
in-place using `chart.data.datasets[0].data = …; chart.update()` if it already
exists, or create it on first render:

```js
function renderStatusChart(payments) {
  if (!window.Chart) return;
  const canvas = document.getElementById("statusChart");
  if (!canvas) return;

  const completed = payments.filter(p => p.status === "COMPLETED").length;
  const pending   = payments.filter(p =>
    p.status === "PENDING" || p.status === "CREATED" ||
    p.status === "VALIDATED" || p.status === "SENT").length;
  const failed    = payments.filter(p => p.status === "FAILED").length;

  if (statusChartInstance) {
    statusChartInstance.data.datasets[0].data = [completed, pending, failed];
    statusChartInstance.update();
    return;
  }

  statusChartInstance = new Chart(canvas, {
    type: "doughnut",
    data: {
      labels: ["Completed", "Pending", "Failed"],
      datasets: [{
        data: [completed, pending, failed],
        backgroundColor: ["#22c55e", "#f59e0b", "#ef4444"],
        borderColor: "#ffffff",
        borderWidth: 4,
        hoverOffset: 4
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false, cutout: "68%",
      plugins: {
        legend: {
          position: "bottom",
          labels: {
            usePointStyle: true, pointStyle: "circle",
            boxWidth: 10, padding: 16, color: "#334155",
            font: { family: "Poppins", weight: "600" }
          }
        }
      }
    }
  });
}
```

### 6 — Update `renderDailyChart(payments)`

Group payments into the last 7 calendar days:

```js
function renderDailyChart(payments) {
  if (!window.Chart) return;
  const canvas = document.getElementById("dailyChart");
  if (!canvas) return;

  // Build last-7-days labels and per-day counts
  const days = ["Sun","Mon","Tue","Wed","Thu","Fri","Sat"];
  const labels = Array.from({ length: 7 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() - (6 - i));
    return days[d.getDay()];
  });
  const counts = Array(7).fill(0);
  const now = new Date();
  payments.forEach(p => {
    const diff = Math.floor((now - new Date(p.createdAt)) / 86_400_000);
    if (diff >= 0 && diff < 7) counts[6 - diff]++;
  });

  if (dailyChartInstance) {
    dailyChartInstance.data.labels            = labels;
    dailyChartInstance.data.datasets[0].data  = counts;
    dailyChartInstance.update();
    return;
  }

  dailyChartInstance = new Chart(canvas, {
    type: "line",
    data: {
      labels,
      datasets: [{
        label: "Payments Processed",
        data: counts,
        borderColor: "#2563eb",
        backgroundColor: "rgba(37,99,235,0.18)",
        fill: true, tension: 0.34, borderWidth: 3,
        pointRadius: 4, pointHoverRadius: 5,
        pointBackgroundColor: "#ffffff",
        pointBorderColor: "#2563eb", pointBorderWidth: 2
      }]
    },
    options: {
      responsive: true, maintainAspectRatio: false,
      scales: {
        x: { grid: { display: false }, ticks: { color: "#334155", font: { family: "Poppins", weight: "500" } } },
        y: { beginAtZero: true, ticks: { color: "#334155", font: { family: "Poppins", weight: "500" } }, grid: { color: "rgba(148,163,184,0.28)" } }
      },
      plugins: {
        legend: { display: false },
        tooltip: { backgroundColor: "#0f172a", titleFont: { family: "Poppins", weight: "600" }, bodyFont: { family: "Poppins" } }
      }
    }
  });
}
```

### 7 — Update `renderRecentPayments(payments)`

Show the 10 most-recent payments sorted by `createdAt` descending:

```js
function renderRecentPayments(payments) {
  const body = document.getElementById("recentPaymentsBody");
  if (!body) return;

  const recent = [...payments]
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .slice(0, 10);

  if (recent.length === 0) {
    body.innerHTML = `<tr><td colspan="6" style="text-align:center;color:#9CA3AF;padding:24px;">No payments found</td></tr>`;
    return;
  }

  body.innerHTML = recent.map(p => `
    <tr>
      <td>${esc(p.paymentId)}</td>
      <td>${esc(p.senderName   || "—")}</td>
      <td>${esc(p.receiverName || "—")}</td>
      <td>${formatCurrency(p.amount, p.currency || "USD")}</td>
      <td><span class="status-badge ${formatStatusClass(p.status)}">${esc(p.status)}</span></td>
      <td>${formatDate(p.createdAt)}</td>
    </tr>`).join("");
}

// XSS-safe escape helper (add once at the top of dashboard.js)
function esc(str) {
  const d = document.createElement("div");
  d.textContent = str != null ? String(str) : "";
  return d.innerHTML;
}
```

---

## Constraints

- **Never** call `fetch()` or `XMLHttpRequest` directly — always use `getPayments()`.
- **Never** modify `api.js`.
- **Never** change `dashboard.html`, CSS classes, or layout.
- Use `esc()` on all server-supplied strings before inserting into innerHTML.
- Keep `formatCurrency`, `formatDate`, `formatStatusClass`, `updateLastRefresh`,
  and `bindDashboardActions` exactly as-is — only update the data-source sections.
