// Mock data removed — all data is now fetched live from the backend via api.js

let statusChartInstance;
let dailyChartInstance;

function formatCurrency(amount, currency) {
	return new Intl.NumberFormat("en-US", {
		style: "currency",
		currency,
		maximumFractionDigits: 0
	}).format(amount);
}

function formatDate(isoDate) {
	const date = new Date(isoDate);
	return date.toLocaleString("en-US", {
		month: "short",
		day: "2-digit",
		year: "numeric",
		hour: "2-digit",
		minute: "2-digit"
	});
}

function formatStatusClass(status) {
	return `status-${status.toLowerCase()}`;
}

function updateLastRefresh() {
	const stamp = document.getElementById("lastUpdated");
	if (!stamp) {
		return;
	}

	const now = new Date();
	stamp.textContent = `Last updated: ${now.toLocaleString("en-US", {
		month: "short",
		day: "2-digit",
		year: "numeric",
		hour: "2-digit",
		minute: "2-digit"
	})}`;
}

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

function esc(str) {
	const d = document.createElement("div");
	d.textContent = str != null ? String(str) : "";
	return d.innerHTML;
}

function renderStatusChart(payments) {
	if (!window.Chart) {
		return;
	}

	const canvas = document.getElementById("statusChart");
	if (!canvas) {
		return;
	}

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
			datasets: [
				{
					data: [completed, pending, failed],
					backgroundColor: ["#22c55e", "#f59e0b", "#ef4444"],
					borderColor: "#ffffff",
					borderWidth: 4,
					hoverOffset: 4
				}
			]
		},
		options: {
			responsive: true,
			maintainAspectRatio: false,
			cutout: "68%",
			plugins: {
				legend: {
					position: "bottom",
					labels: {
						usePointStyle: true,
						pointStyle: "circle",
						boxWidth: 10,
						padding: 16,
						color: "#334155",
						font: {
							family: "Poppins",
							weight: "600"
						}
					}
				}
			}
		}
	});
}

function renderDailyChart(payments) {
	if (!window.Chart) {
		return;
	}

	const canvas = document.getElementById("dailyChart");
	if (!canvas) {
		return;
	}

	// Build last-7-days labels and per-day counts from live data
	const dayNames = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
	const labels = Array.from({ length: 7 }, (_, i) => {
		const d = new Date();
		d.setDate(d.getDate() - (6 - i));
		return dayNames[d.getDay()];
	});
	const counts = Array(7).fill(0);
	const now = new Date();
	payments.forEach(p => {
		const diff = Math.floor((now - new Date(p.createdAt)) / 86_400_000);
		if (diff >= 0 && diff < 7) counts[6 - diff]++;
	});

	if (dailyChartInstance) {
		dailyChartInstance.data.labels           = labels;
		dailyChartInstance.data.datasets[0].data = counts;
		dailyChartInstance.update();
		return;
	}

	dailyChartInstance = new Chart(canvas, {
		type: "line",
		data: {
			labels,
			datasets: [
				{
					label: "Payments Processed",
					data: counts,
					borderColor: "#2563eb",
					backgroundColor: "rgba(37, 99, 235, 0.18)",
					fill: true,
					tension: 0.34,
					borderWidth: 3,
					pointRadius: 4,
					pointHoverRadius: 5,
					pointBackgroundColor: "#ffffff",
					pointBorderColor: "#2563eb",
					pointBorderWidth: 2
				}
			]
		},
		options: {
			responsive: true,
			maintainAspectRatio: false,
			scales: {
				x: {
					grid: {
						display: false
					},
					ticks: {
						color: "#334155",
						font: {
							family: "Poppins",
							weight: "500"
						}
					}
				},
				y: {
					beginAtZero: true,
					ticks: {
						color: "#334155",
						font: {
							family: "Poppins",
							weight: "500"
						}
					},
					grid: {
						color: "rgba(148, 163, 184, 0.28)"
					}
				}
			},
			plugins: {
				legend: {
					display: false
				},
				tooltip: {
					backgroundColor: "#0f172a",
					titleFont: {
						family: "Poppins",
						weight: "600"
					},
					bodyFont: {
						family: "Poppins"
					}
				}
			}
		}
	});
}

function bindDashboardActions() {
	const refreshBtn = document.getElementById("refreshDashboardBtn");
	const exportBtn = document.getElementById("exportDashboardBtn");

	if (refreshBtn) {
		refreshBtn.addEventListener("click", () => {
			loadDashboardData();
		});
	}

	if (exportBtn) {
		exportBtn.addEventListener("click", () => {
			window.alert("Static frontend preview: export integration will be added with backend/API in the next phase.");
		});
	}
}

function renderDashboard(payments) {
	renderSummaryCards(payments);
	renderRecentPayments(payments);
	renderStatusChart(payments);
	renderDailyChart(payments);
	updateLastRefresh();
}

document.addEventListener("DOMContentLoaded", () => {
	bindDashboardActions();
	loadDashboardData();
});

async function loadDashboardData() {
	// Show spinner and disable button immediately so the user sees feedback
	const refreshBtn = document.getElementById("refreshDashboardBtn");
	if (refreshBtn) {
		refreshBtn.disabled = true;
		refreshBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Refreshing…';
	}

	try {
		// Payments are fetched first — they always resolve (api.js has internal fallback)
		const payments = await getPayments();

		// Enrich with account names; give the /accounts endpoint 3 s max so a
		// slow or missing endpoint never blocks the refresh button indefinitely
		let accountMap = {};
		try {
			const accounts = await Promise.race([
				getAccounts(),
				new Promise((_, reject) =>
					setTimeout(() => reject(new Error("accounts timeout")), 3000)
				)
			]);
			accounts.forEach(acc => {
				const id = acc.accountId ?? acc.id;
				if (id != null) {
					accountMap[String(id)] = acc.accountHolderName || acc.name || `Account ${id}`;
				}
			});
		} catch {
			// /accounts unavailable or timed-out — proceed without names
		}

		// Merge resolved names into each payment record
		const enriched = payments.map(p => ({
			...p,
			senderName:   p.senderName   || accountMap[String(p.sourceAccountId)]      || `Account ${p.sourceAccountId      ?? "?"}`,
			receiverName: p.receiverName || accountMap[String(p.destinationAccountId)] || `Account ${p.destinationAccountId ?? "?"}`
		}));

		renderDashboard(enriched);
	} catch (err) {
		console.error("Dashboard data load failed:", err);
		renderDashboard([]);   // render empty state — never crash the page
	} finally {
		// Always restore the button regardless of success or failure
		if (refreshBtn) {
			refreshBtn.disabled = false;
			refreshBtn.innerHTML = '<i class="fas fa-rotate-right"></i> Refresh Snapshot';
		}
	}
}
