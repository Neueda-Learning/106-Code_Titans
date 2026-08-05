function formatDateTime(value) {
	if (!value) {
		return "--";
	}

	const date = new Date(value);
	if (Number.isNaN(date.getTime())) {
		return String(value);
	}

	return date.toLocaleString("en-US", {
		year: "numeric",
		month: "short",
		day: "2-digit",
		hour: "2-digit",
		minute: "2-digit"
	});
}

function formatAmount(amount) {
	if (amount === null || amount === undefined || amount === "") {
		return "--";
	}

	const numeric = Number(amount);
	if (Number.isNaN(numeric)) {
		return String(amount);
	}

	return numeric.toLocaleString("en-US", {
		minimumFractionDigits: 2,
		maximumFractionDigits: 2
	});
}

function getStatusClass(status) {
	const normalized = String(status || "CREATED").toUpperCase();
	const map = {
		COMPLETED: "status-completed",
		FAILED: "status-failed",
		PENDING: "status-pending",
		CREATED: "status-created",
		VALIDATED: "status-validated",
		SENT: "status-sent"
	};

	return map[normalized] || "status-created";
}

function escapeHtml(value) {
	return String(value ?? "")
		.replace(/&/g, "&amp;")
		.replace(/</g, "&lt;")
		.replace(/>/g, "&gt;")
		.replace(/\"/g, "&quot;")
		.replace(/'/g, "&#39;");
}

function readPaymentIdFromQuery() {
	const query = new URLSearchParams(window.location.search);
	return query.get("id") || query.get("paymentId") || "";
}

function setTextById(elementId, value) {
	const element = document.getElementById(elementId);
	if (element) {
		element.textContent = value ?? "--";
	}
}

function resolveField(source, keys, fallback = "--") {
	for (const key of keys) {
		if (source && source[key] !== undefined && source[key] !== null && source[key] !== "") {
			return source[key];
		}
	}
	return fallback;
}

async function initializePaymentsPage() {
	const tableBody = document.getElementById("paymentsTableBody");
	if (!tableBody) {
		return;
	}

	const loadingBlock = document.getElementById("paymentsLoading");
	const errorBlock = document.getElementById("paymentsError");
	const emptyBlock = document.getElementById("paymentsEmpty");
	const tableSection = document.getElementById("payments-table-section");
	const countLabel = document.getElementById("paymentsCount");
	const searchInput = document.getElementById("paymentIdSearch");
	const statusFilter = document.getElementById("statusFilter");
	const refreshButton = document.getElementById("refreshPaymentsBtn");

	let allPayments = [];
	let filteredPayments = [];

	function updateKpiCards(rows) {
		const totals = {
			total: rows.length,
			completed: 0,
			pending: 0,
			failed: 0
		};

		rows.forEach((payment) => {
			const status = String(resolveField(payment, ["status"], "")).toUpperCase();
			if (status === "COMPLETED") totals.completed += 1;
			if (status === "PENDING") totals.pending += 1;
			if (status === "FAILED") totals.failed += 1;
		});

		setTextById("kpiTotalPayments", String(totals.total));
		setTextById("kpiCompletedPayments", String(totals.completed));
		setTextById("kpiPendingPayments", String(totals.pending));
		setTextById("kpiFailedPayments", String(totals.failed));
	}

	function renderRows(rows) {
		tableBody.innerHTML = rows.map((payment) => {
			const id = resolveField(payment, ["paymentId", "id"], "--");
			const sender = resolveField(payment, ["senderName", "sender", "fromAccount"], "--");
			const receiver = resolveField(payment, ["receiverName", "receiver", "toAccount"], "--");
			const amount = resolveField(payment, ["amount"], "--");
			const currency = resolveField(payment, ["currency"], "--");
			const status = resolveField(payment, ["status"], "CREATED");
			const createdAt = resolveField(payment, ["createdAt", "createdDate", "createdOn"], "--");

			return `
				<tr>
					<td>${escapeHtml(id)}</td>
					<td>${escapeHtml(sender)}</td>
					<td>${escapeHtml(receiver)}</td>
					<td>${escapeHtml(formatAmount(amount))}</td>
					<td>${escapeHtml(currency)}</td>
					<td><span class="status-badge ${getStatusClass(status)}">${escapeHtml(status)}</span></td>
					<td>${escapeHtml(formatDateTime(createdAt))}</td>
					<td><a class="btn" href="payment_details.html?id=${encodeURIComponent(id)}">View</a></td>
				</tr>
			`;
		}).join("");
	}

	function applyFilters() {
		const searchTerm = String(searchInput?.value || "").trim().toLowerCase();
		const selectedStatus = String(statusFilter?.value || "ALL").toUpperCase();

		filteredPayments = allPayments.filter((payment) => {
			const id = String(resolveField(payment, ["paymentId", "id"], "")).toLowerCase();
			const status = String(resolveField(payment, ["status"], "")).toUpperCase();
			const searchMatch = !searchTerm || id.includes(searchTerm);
			const statusMatch = selectedStatus === "ALL" || status === selectedStatus;
			return searchMatch && statusMatch;
		});

		updateKpiCards(filteredPayments);

		if (countLabel) {
			countLabel.textContent = `${filteredPayments.length} payment${filteredPayments.length === 1 ? "" : "s"} found`;
		}

		if (filteredPayments.length === 0) {
			if (tableSection) tableSection.hidden = true;
			if (emptyBlock) emptyBlock.hidden = false;
			return;
		}

		renderRows(filteredPayments);
		if (tableSection) tableSection.hidden = false;
		if (emptyBlock) emptyBlock.hidden = true;
	}

	async function loadPaymentsData() {
		if (loadingBlock) loadingBlock.hidden = false;
		if (errorBlock) errorBlock.hidden = true;
		if (emptyBlock) emptyBlock.hidden = true;
		if (tableSection) tableSection.hidden = true;

		try {
			allPayments = await getPayments();
			applyFilters();
		} catch (error) {
			console.error("Error loading payments:", error);
			if (errorBlock) errorBlock.hidden = false;
		} finally {
			if (loadingBlock) loadingBlock.hidden = true;
		}
	}

	searchInput?.addEventListener("input", applyFilters);
	statusFilter?.addEventListener("change", applyFilters);
	refreshButton?.addEventListener("click", loadPaymentsData);

	loadPaymentsData();
}

function buildHistoryFallback(payment) {
	const createdDate = resolveField(payment, ["createdAt", "createdDate", "createdOn"], null);
	const status = resolveField(payment, ["status"], "CREATED");
	const updatedDate = resolveField(payment, ["updatedAt", "updatedDate", "lastUpdated"], null);

	const timeline = [];
	if (createdDate) {
		timeline.push({ status: "CREATED", timestamp: createdDate, note: "Payment created." });
	}

	if (status && status !== "CREATED") {
		timeline.push({ status, timestamp: updatedDate || createdDate, note: "Status updated." });
	}

	return timeline;
}

function renderTimeline(historyRows) {
	const timeline = document.getElementById("paymentTimeline");
	if (!timeline) {
		return;
	}

	if (!historyRows.length) {
		timeline.innerHTML = `
			<li class="timeline-item">
				<div class="timeline-marker"></div>
				<div class="timeline-content">
					<p class="timeline-status">CREATED</p>
					<p class="timeline-time">--</p>
					<p class="timeline-note">No timeline events returned.</p>
				</div>
			</li>
		`;
		return;
	}

	timeline.innerHTML = historyRows.map((event) => {
		const status = resolveField(event, ["status", "stage"], "CREATED");
		const timestamp = resolveField(event, ["timestamp", "createdAt", "eventTime", "date"], "--");
		const note = resolveField(event, ["note", "message", "description", "remarks"], "Status transition recorded.");

		return `
			<li class="timeline-item">
				<div class="timeline-marker"></div>
				<div class="timeline-content">
					<p class="timeline-status">${escapeHtml(status)}</p>
					<p class="timeline-time">${escapeHtml(formatDateTime(timestamp))}</p>
					<p class="timeline-note">${escapeHtml(note)}</p>
				</div>
			</li>
		`;
	}).join("");
}

function populatePaymentDetails(payment) {
	const status = String(resolveField(payment, ["status"], "CREATED")).toUpperCase();
	const amount = resolveField(payment, ["amount"], "--");
	const currency = resolveField(payment, ["currency"], "--");
	const fee = resolveField(payment, ["fee", "transactionFee"], 0);
	const netAmount = Number(amount) - Number(fee);

	setTextById("detailPaymentId", resolveField(payment, ["paymentId", "id"]));
	setTextById("detailReferenceId", resolveField(payment, ["referenceId", "transactionId", "externalRef"]));
	setTextById("detailCreatedAt", formatDateTime(resolveField(payment, ["createdAt", "createdDate", "createdOn"], null)));
	setTextById("detailUpdatedAt", formatDateTime(resolveField(payment, ["updatedAt", "updatedDate", "lastUpdated"], null)));
	setTextById("detailPaymentMethod", resolveField(payment, ["paymentMethod", "method", "type"]));

	setTextById("senderName", resolveField(payment, ["senderName", "sender", "fromName"]));
	setTextById("senderAccount", resolveField(payment, ["senderAccount", "fromAccount"]));
	setTextById("senderBank", resolveField(payment, ["senderBank", "fromBank"]));
	setTextById("senderCountry", resolveField(payment, ["senderCountry", "fromCountry"]));

	setTextById("receiverName", resolveField(payment, ["receiverName", "receiver", "toName"]));
	setTextById("receiverAccount", resolveField(payment, ["receiverAccount", "toAccount"]));
	setTextById("receiverBank", resolveField(payment, ["receiverBank", "toBank"]));
	setTextById("receiverCountry", resolveField(payment, ["receiverCountry", "toCountry"]));

	setTextById("detailAmount", formatAmount(amount));
	setTextById("detailCurrency", currency);
	setTextById("detailFee", formatAmount(fee));
	setTextById("detailNetAmount", Number.isFinite(netAmount) ? formatAmount(netAmount) : "--");
	setTextById("detailPurpose", resolveField(payment, ["purpose", "description", "paymentPurpose"]));
	setTextById("detailChannel", resolveField(payment, ["channel", "source", "origin"]));

	const statusBadge = document.getElementById("detailStatusBadge");
	if (statusBadge) {
		statusBadge.textContent = status;
		statusBadge.className = `status-badge ${getStatusClass(status)}`;
	}

	const failureCard = document.getElementById("failureInfoCard");
	const failureReason = resolveField(payment, ["failureReason", "errorMessage"], "");
	const failureCode = resolveField(payment, ["failureCode", "errorCode"], "");
	const failureStage = resolveField(payment, ["failureStage", "failedAt"], "");
	const retryable = resolveField(payment, ["retryable"], "");

	if (status === "FAILED" || failureReason || failureCode || failureStage) {
		if (failureCard) {
			failureCard.hidden = false;
		}

		setTextById("detailFailureReason", failureReason || "Unavailable");
		setTextById("detailFailureCode", failureCode || "Unavailable");
		setTextById("detailFailureStage", failureStage || "Unavailable");
		setTextById("detailRetryable", retryable === "" ? "Unknown" : String(retryable));
	} else if (failureCard) {
		failureCard.hidden = true;
	}
}

async function initializePaymentDetailsPage() {
	const detailsRoot = document.getElementById("detailsContent");
	if (!detailsRoot) {
		return;
	}

	const loading = document.getElementById("detailsLoading");
	const error = document.getElementById("detailsError");
	const paymentId = readPaymentIdFromQuery();

	if (!paymentId) {
		if (loading) loading.hidden = true;
		if (error) error.hidden = false;
		return;
	}

	try {
		const [payment, history] = await Promise.all([
			getPaymentById(paymentId),
			getPaymentHistory(paymentId).catch(() => [])
		]);

		populatePaymentDetails(payment || {});
		renderTimeline(history.length ? history : buildHistoryFallback(payment || {}));

		if (detailsRoot) detailsRoot.hidden = false;
		if (error) error.hidden = true;
	} catch (apiError) {
		console.error("Error loading payment details:", apiError);
		if (error) error.hidden = false;
	} finally {
		if (loading) loading.hidden = true;
	}
}

document.addEventListener("DOMContentLoaded", () => {
	initializePaymentsPage();
	initializePaymentDetailsPage();
});
