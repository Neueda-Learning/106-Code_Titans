// reports.js — Live data binding for reports.html
// Depends on: api.js (loaded before this file in reports.html)
// Rule: never call fetch() directly — all API access goes through api.js functions.

document.addEventListener('DOMContentLoaded', loadReportsData);

// ─── Entry point ───────────────────────────────────────────────────────────

async function loadReportsData() {
	showLoading(true);
	try {
		const [payments, accounts] = await Promise.all([
			getPayments(),
			getAccounts().catch(() => [])
		]);
		const accountIndex = buildAccountIndex(accounts);
		const stats = computeStats(payments);

		updateSummaryCards(stats);
		updateStatusChart(stats);
		updateDailyChart(payments);
		updateHourlyChart(payments);
		updateSendersTable(payments, accountIndex);
		updateReceiversTable(payments, accountIndex);
		updateLargestTable(payments, accountIndex);
		updateFailureTable(payments);
	} catch (err) {
		console.error("Reports data load failed:", err);
	} finally {
		showLoading(false);
	}
}

// ─── Aggregations ──────────────────────────────────────────────────────────

function computeStats(payments) {
	const total = payments.length;
	const completed = payments.filter((p) => p.status === "COMPLETED").length;
	const failed = payments.filter((p) => p.status === "FAILED").length;
	const pending = payments.filter(
		(p) => p.status === "PENDING" || p.status === "CREATED"
	).length;
	return { total, completed, failed, pending };
}

// ─── Summary Cards ─────────────────────────────────────────────────────────

function updateSummaryCards({ total, completed, failed, pending }) {
	setText("total-count", total.toLocaleString());
	setText("total-unit", "transactions");
	setText("completed-count", completed.toLocaleString());
	setText("completed-unit", pct(completed, total) + "% success rate");
	setText("failed-count", failed.toLocaleString());
	setText("failed-unit", pct(failed, total) + "% failure rate");
	setText("pending-count", pending.toLocaleString());
	setText("pending-unit", pct(pending, total) + "% in progress");
}

// ─── Charts ────────────────────────────────────────────────────────────────

// Update the doughnut chart in-place (no destroy/recreate)
function updateStatusChart({ completed, failed, pending }) {
	const chart = getChartInstance("statusChart");
	if (!chart) return;
	chart.data.datasets[0].data = [completed, failed, pending];
	chart.update();
}

// Group payments by calendar day over the last 7 days → update bar chart
function updateDailyChart(payments) {
	const chart = getChartInstance("dailyChart");
	if (!chart) return;

	const labels = last7DayLabels();
	const completed = Array(7).fill(0);
	const failed = Array(7).fill(0);
	const now = new Date();

	payments.forEach((p) => {
		const d = new Date(p.createdAt);
		const diffDays = Math.floor((now - d) / 86_400_000);
		if (diffDays < 0 || diffDays >= 7) return;
		const idx = 6 - diffDays;
		if (p.status === "COMPLETED") completed[idx]++;
		else if (p.status === "FAILED") failed[idx]++;
	});

	chart.data.labels = labels;
	chart.data.datasets[0].data = completed;
	chart.data.datasets[1].data = failed;
	chart.update();
}

// Bucket payments by 2-hour window → update line chart
function updateHourlyChart(payments) {
	const chart = getChartInstance("hourlyChart");
	if (!chart) return;

	const buckets = Array(12).fill(0); // index 0 = 00:00–01:59, 1 = 02:00–03:59 …
	payments.forEach((p) => {
		const h = new Date(p.createdAt).getHours();
		buckets[Math.floor(h / 2)]++;
	});

	chart.data.datasets[0].data = buckets;
	chart.update();
}

// ─── Tables ────────────────────────────────────────────────────────────────

function updateSendersTable(payments, accountIndex) {
	const map = groupByResolvedParty(payments, "sender", accountIndex);
	const rows = topN(map, 5);
	setTableRows(
		"senders-tbody",
		rows.map(([name, list], i) =>
			`<tr>
        <td>${rankBadge(i)}</td>
        <td>${esc(name)}</td>
        <td>${list.length.toLocaleString()}</td>
        <td class="amount">${formatMoney(sumAmount(list))}</td>
        <td>${formatMoney(avg(list))}</td>
      </tr>`
		)
	);
}

function updateReceiversTable(payments, accountIndex) {
	const map = groupByResolvedParty(payments, "receiver", accountIndex);
	const rows = topN(map, 5);
	setTableRows(
		"receivers-tbody",
		rows.map(([name, list], i) =>
			`<tr>
        <td>${rankBadge(i)}</td>
        <td>${esc(name)}</td>
        <td>${list.length.toLocaleString()}</td>
        <td class="amount">${formatMoney(sumAmount(list))}</td>
        <td>${formatMoney(avg(list))}</td>
      </tr>`
		)
	);
}

function updateLargestTable(payments, accountIndex) {
	const sorted = payments
		.filter((p) => p.status === "COMPLETED")
		.slice()
		.sort((a, b) => b.amount - a.amount)
		.slice(0, 10);

	setTableRows(
		"largest-tbody",
		sorted.map((p) =>
			`<tr>
        <td>${esc(p.paymentId)}</td>
		<td>${esc(resolvePartyName(p, "sender", accountIndex))}</td>
		<td>${esc(resolvePartyName(p, "receiver", accountIndex))}</td>
        <td class="amount">${formatMoney(p.amount)}</td>
        <td>${statusBadge(p.status)}</td>
        <td>${formatDate(p.createdAt)}</td>
      </tr>`
		)
	);
}

function updateFailureTable(payments) {
	const failed = payments.filter((p) => p.status === "FAILED");
	const total = failed.length || 1;

	// Group by errorCode; fall back to errorMessage then generic bucket
	const reasonMap = {};
	failed.forEach((p) => {
		const key = p.errorCode || p.errorMessage || "Unknown Error";
		reasonMap[key] = (reasonMap[key] || 0) + 1;
	});

	const sorted = Object.entries(reasonMap)
		.sort((a, b) => b[1] - a[1])
		.slice(0, 5);

	setTableRows(
		"failure-tbody",
		sorted.length > 0
			? sorted.map(([reason, count]) =>
					`<tr>
          <td>${esc(reason)}</td>
          <td><span class="badge danger">${count}</span></td>
          <td>${pct(count, total)}%</td>
        </tr>`
				)
			: [
					`<tr><td colspan="3" style="text-align:center;color:#22C55E;padding:20px;">
          <i class="fas fa-check-circle" style="margin-right:6px;"></i>No failures recorded
        </td></tr>`,
				]
	);
}

// ─── DOM Helpers ───────────────────────────────────────────────────────────

function setText(id, text) {
	const el = document.getElementById(id);
	if (el) el.textContent = text;
}

function setTableRows(tbodyId, htmlRows) {
	const el = document.getElementById(tbodyId);
	if (!el) return;
	if (!htmlRows || htmlRows.length === 0) {
		el.innerHTML =
			'<tr><td colspan="10" style="text-align:center;color:#9CA3AF;padding:24px;">No data available</td></tr>';
	} else {
		el.innerHTML = htmlRows.join("");
	}
}

function showLoading(visible) {
	const el = document.getElementById("reports-loading");
	if (el) el.style.display = visible ? "block" : "none";
}

// Retrieve an existing Chart.js instance by canvas element id
function getChartInstance(canvasId) {
	const canvas = document.getElementById(canvasId);
	if (!canvas) return null;
	// Chart.getChart() is available in Chart.js v3+
	return typeof Chart !== "undefined" ? Chart.getChart(canvas) : null;
}

// ─── Data Helpers ──────────────────────────────────────────────────────────

function groupBy(arr, key) {
	return arr.reduce((map, item) => {
		const k = item[key] || "Unknown";
		if (!map[k]) map[k] = [];
		map[k].push(item);
		return map;
	}, {});
}

function groupByResolvedParty(payments, role, accountIndex) {
	return (payments || []).reduce((map, payment) => {
		const resolved = resolvePartyName(payment, role, accountIndex);
		const key = resolved && resolved !== "—" ? resolved : "Unknown";
		if (!map[key]) map[key] = [];
		map[key].push(payment);
		return map;
	}, {});
}

function buildAccountIndex(accounts) {
	const byId = new Map();
	const byNumber = new Map();

	(accounts || []).forEach((account) => {
		const label =
			account.accountHolderName ||
			account.holderName ||
			account.name ||
			account.accountNumber ||
			"Unknown";

		const id = account.accountId ?? account.id;
		if (id !== undefined && id !== null) {
			byId.set(String(id), label);
		}

		const number = account.accountNumber ?? account.number;
		if (number !== undefined && number !== null && number !== "") {
			byNumber.set(String(number), label);
		}
	});

	return { byId, byNumber };
}

function resolvePartyName(payment, role, accountIndex) {
	if (role === "sender") {
		const direct = payment.senderName || payment.sourceAccountName;
		if (direct) return direct;

		const id = payment.sourceAccountId ?? payment.senderAccountId ?? payment.senderId;
		if (id !== undefined && id !== null) {
			const mappedById = accountIndex?.byId?.get(String(id));
			if (mappedById) return mappedById;
		}

		const number = payment.senderAccount ?? payment.sourceAccountNumber;
		if (number) {
			const mappedByNumber = accountIndex?.byNumber?.get(String(number));
			if (mappedByNumber) return mappedByNumber;
			return `Account ${number}`;
		}

		if (id !== undefined && id !== null) {
			return `Account ${id}`;
		}

		return "—";
	}

	const direct = payment.receiverName || payment.destinationAccountName;
	if (direct) return direct;

	const id = payment.destinationAccountId ?? payment.receiverAccountId ?? payment.receiverId;
	if (id !== undefined && id !== null) {
		const mappedById = accountIndex?.byId?.get(String(id));
		if (mappedById) return mappedById;
	}

	const number = payment.receiverAccount ?? payment.destinationAccountNumber;
	if (number) {
		const mappedByNumber = accountIndex?.byNumber?.get(String(number));
		if (mappedByNumber) return mappedByNumber;
		return `Account ${number}`;
	}

	if (id !== undefined && id !== null) {
		return `Account ${id}`;
	}

	return "—";
}

// Sort map entries by list length descending, return top n
function topN(map, n) {
	return Object.entries(map)
		.sort((a, b) => b[1].length - a[1].length)
		.slice(0, n);
}

function sumAmount(list) {
	return list.reduce((s, p) => s + (Number(p.amount) || 0), 0);
}

function avg(list) {
	return list.length ? sumAmount(list) / list.length : 0;
}

function pct(part, total) {
	if (!total) return "0.0";
	return ((part / total) * 100).toFixed(1);
}

// ─── Formatting Helpers ────────────────────────────────────────────────────

function formatMoney(n) {
	return (
		"$" +
		Number(n).toLocaleString("en-US", {
			minimumFractionDigits: 0,
			maximumFractionDigits: 0,
		})
	);
}

function formatDate(iso) {
	if (!iso) return "—";
	return new Date(iso).toLocaleDateString("en-US", {
		month: "short",
		day: "numeric",
		year: "numeric",
	});
}

// Safe HTML escape — prevents XSS from server-supplied strings
function esc(str) {
	const d = document.createElement("div");
	d.textContent = str != null ? String(str) : "";
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
		COMPLETED: "completed",
		FAILED: "failed",
		PENDING: "pending",
		CREATED: "created",
	};
	const cls = map[status] || "created";
	const label = status
		? status.charAt(0) + status.slice(1).toLowerCase()
		: "—";
	return `<span class="status-badge ${cls}">${label}</span>`;
}

// Returns labels like ['Mon', 'Tue', …] for the last 7 calendar days
function last7DayLabels() {
	const days = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
	return Array.from({ length: 7 }, (_, i) => {
		const d = new Date();
		d.setDate(d.getDate() - (6 - i));
		return days[d.getDay()];
	});
}
