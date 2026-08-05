const dashboardMockData = {
	summary: {
		totalPayments: 1248,
		completedPayments: 1036,
		pendingPayments: 138,
		failedPayments: 74
	},
	statusDistribution: {
		labels: ["Completed", "Pending", "Failed"],
		values: [1036, 138, 74]
	},
	dailyPayments: {
		labels: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
		values: [176, 188, 162, 205, 214, 149, 154]
	},
	recentPayments: [
		{
			id: "PAY-2026-8711",
			sender: "Northwind Traders",
			receiver: "Silverline Retail",
			amount: 18450,
			currency: "USD",
			status: "COMPLETED",
			createdDate: "2026-08-05T08:14:00"
		},
		{
			id: "PAY-2026-8709",
			sender: "Aster Finance",
			receiver: "Nova Supplies",
			amount: 6320,
			currency: "USD",
			status: "PENDING",
			createdDate: "2026-08-05T07:52:00"
		},
		{
			id: "PAY-2026-8707",
			sender: "BlueRock Capital",
			receiver: "Trinity Health",
			amount: 22100,
			currency: "USD",
			status: "FAILED",
			createdDate: "2026-08-05T07:19:00"
		},
		{
			id: "PAY-2026-8704",
			sender: "Pioneer Foods",
			receiver: "Atlas Distribution",
			amount: 9750,
			currency: "USD",
			status: "COMPLETED",
			createdDate: "2026-08-05T06:47:00"
		},
		{
			id: "PAY-2026-8701",
			sender: "Lumina Logistics",
			receiver: "Helios Stores",
			amount: 4820,
			currency: "USD",
			status: "VALIDATED",
			createdDate: "2026-08-05T06:02:00"
		},
		{
			id: "PAY-2026-8698",
			sender: "Mercury Telecom",
			receiver: "Orbit Systems",
			amount: 15340,
			currency: "USD",
			status: "SENT",
			createdDate: "2026-08-05T05:40:00"
		}
	]
};

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

function renderSummaryCards() {
	const { summary } = dashboardMockData;
	document.getElementById("totalPayments").textContent = summary.totalPayments.toLocaleString("en-US");
	document.getElementById("completedPayments").textContent = summary.completedPayments.toLocaleString("en-US");
	document.getElementById("pendingPayments").textContent = summary.pendingPayments.toLocaleString("en-US");
	document.getElementById("failedPayments").textContent = summary.failedPayments.toLocaleString("en-US");
}

function renderRecentPayments() {
	const body = document.getElementById("recentPaymentsBody");
	if (!body) {
		return;
	}

	body.innerHTML = dashboardMockData.recentPayments
		.map(
			(payment) => `
			<tr>
				<td>${payment.id}</td>
				<td>${payment.sender}</td>
				<td>${payment.receiver}</td>
				<td>${formatCurrency(payment.amount, payment.currency)}</td>
				<td><span class="status-badge ${formatStatusClass(payment.status)}">${payment.status}</span></td>
				<td>${formatDate(payment.createdDate)}</td>
			</tr>
		`
		)
		.join("");
}

function renderStatusChart() {
	if (!window.Chart) {
		return;
	}

	const canvas = document.getElementById("statusChart");
	if (!canvas) {
		return;
	}

	if (statusChartInstance) {
		statusChartInstance.destroy();
	}

	statusChartInstance = new Chart(canvas, {
		type: "doughnut",
		data: {
			labels: dashboardMockData.statusDistribution.labels,
			datasets: [
				{
					data: dashboardMockData.statusDistribution.values,
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

function renderDailyChart() {
	if (!window.Chart) {
		return;
	}

	const canvas = document.getElementById("dailyChart");
	if (!canvas) {
		return;
	}

	if (dailyChartInstance) {
		dailyChartInstance.destroy();
	}

	dailyChartInstance = new Chart(canvas, {
		type: "line",
		data: {
			labels: dashboardMockData.dailyPayments.labels,
			datasets: [
				{
					label: "Payments Processed",
					data: dashboardMockData.dailyPayments.values,
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
			renderDashboard();
		});
	}

	if (exportBtn) {
		exportBtn.addEventListener("click", () => {
			window.alert("Static frontend preview: export integration will be added with backend/API in the next phase.");
		});
	}
}

function renderDashboard() {
	renderSummaryCards();
	renderRecentPayments();
	renderStatusChart();
	renderDailyChart();
	updateLastRefresh();
}

document.addEventListener("DOMContentLoaded", () => {
	bindDashboardActions();
	renderDashboard();
});
