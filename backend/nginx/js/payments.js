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

function shouldAutoProcessFromQuery() {
	const query = new URLSearchParams(window.location.search);
	return query.get("autoProcess") === "1";
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

function formatAccountLabel(account) {
	if (!account) {
		return "Account";
	}

	const accountNumber = account.accountNumber || account.account_number || "Unknown";
	const holderName = account.accountHolderName || account.account_holder_name || "Unknown Holder";
	const bankName = account.bankName || account.bank_name || "Unknown Bank";
	const currency = account.currency || "--";

	return `${accountNumber} | ${holderName} | ${bankName} | ${currency}`;
}

function populateAccountSelect(selectElement, accounts, placeholder) {
	if (!selectElement) {
		return;
	}

	selectElement.innerHTML = `<option value="">${placeholder}</option>`;
	accounts.forEach((account) => {
		const option = document.createElement("option");
		option.value = String(account.accountId ?? account.account_id ?? "");
		option.textContent = formatAccountLabel(account);
		selectElement.appendChild(option);
	});
}

function showCreatePaymentMessage(type, message) {
	const messageElement = document.getElementById("createPaymentMessage");
	if (!messageElement) {
		return;
	}

	messageElement.textContent = message;
	messageElement.className = `form-message ${type}`;
	messageElement.hidden = false;
}

function clearCreatePaymentMessage() {
	const messageElement = document.getElementById("createPaymentMessage");
	if (!messageElement) {
		return;
	}

	messageElement.hidden = true;
	messageElement.textContent = "";
	messageElement.className = "form-message";
}

function readCreatePaymentForm(form) {
	const formData = new FormData(form);

	return {
		sourceAccountId: String(formData.get("sourceAccountId") || "").trim(),
		destinationAccountId: String(formData.get("destinationAccountId") || "").trim(),
		amount: String(formData.get("amount") || "").trim(),
		currency: String(formData.get("currency") || "").trim(),
		reference: String(formData.get("reference") || "").trim(),
		idempotencyKey: String(formData.get("idempotencyKey") || "").trim(),
		processingScenario: String(formData.get("processingScenario") || "AUTO_SUCCESS").trim()
	};
}

function validateCreatePaymentInput(input) {
	const requiredFields = [
		"sourceAccountId",
		"destinationAccountId",
		"amount",
		"currency",
		"reference"
	];

	for (const field of requiredFields) {
		if (!input[field]) {
			return `Please fill ${field}.`;
		}
	}

	const amount = Number(input.amount);
	if (!Number.isFinite(amount) || amount <= 0) {
		return "Amount must be a valid number greater than 0.";
	}

	if (input.sourceAccountId === input.destinationAccountId) {
		return "Sender and receiver accounts must be different.";
	}

	return "";
}

function generateIdempotencyKey() {
	if (window.crypto && typeof window.crypto.randomUUID === "function") {
		return window.crypto.randomUUID();
	}

	return `idem-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function normalizeStatus(status) {
	return String(status || "").trim().toUpperCase();
}

function isTerminalStatus(status) {
	const normalized = normalizeStatus(status);
	return normalized === "COMPLETED" || normalized === "FAILED";
}

function isPendingStatus(status) {
	const normalized = normalizeStatus(status);
	return normalized === "CREATED" || normalized === "VALIDATED" || normalized === "SENT" || normalized === "PENDING";
}

function delay(ms) {
	return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function resolvePaymentParty(payment, role, accountMap) {
	const isSource = role === "source";
	const idKeys = isSource ? ["sourceAccountId"] : ["destinationAccountId"];
	const nameKeys = isSource ? ["senderName", "sender"] : ["receiverName", "receiver"];
	const accountKeys = isSource ? ["senderAccount", "fromAccount"] : ["receiverAccount", "toAccount"];
	const bankKeys = isSource ? ["senderBank", "fromBank"] : ["receiverBank", "toBank"];

	const accountId = resolveField(payment, idKeys, "");
	const mappedAccount = accountMap.get(String(accountId));

	return {
		accountId,
		name: resolveField(payment, nameKeys, mappedAccount?.accountHolderName || mappedAccount?.account_holder_name || "--"),
		accountNumber: resolveField(payment, accountKeys, mappedAccount?.accountNumber || mappedAccount?.account_number || "--"),
		bank: resolveField(payment, bankKeys, mappedAccount?.bankName || mappedAccount?.bank_name || "--"),
		currency: mappedAccount?.currency || resolveField(payment, ["currency"], "--")
	};
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
	const toggleCreateButton = document.getElementById("toggleCreatePaymentBtn");
	const closeCreateButton = document.getElementById("closeCreatePaymentBtn");
	const createPaymentPanel = document.getElementById("createPaymentPanel");
	const createPaymentForm = document.getElementById("createPaymentForm");
	const createPaymentSubmitButton = document.getElementById("createPaymentSubmitBtn");
	const sourceAccountSelect = document.getElementById("sourceAccountId");
	const destinationAccountSelect = document.getElementById("destinationAccountId");
	let createPanelOpen = false;
	let availableAccounts = [];
	let accountMap = new Map();
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
			const status = normalizeStatus(resolveField(payment, ["status"], ""));
			if (status === "COMPLETED") totals.completed += 1;
			if (status === "FAILED") totals.failed += 1;
			if (isPendingStatus(status)) totals.pending += 1;
		});

		setTextById("kpiTotalPayments", String(totals.total));
		setTextById("kpiCompletedPayments", String(totals.completed));
		setTextById("kpiPendingPayments", String(totals.pending));
		setTextById("kpiFailedPayments", String(totals.failed));
	}

	function renderRows(rows) {
		tableBody.innerHTML = rows.map((payment) => {
			const id = resolveField(payment, ["paymentId", "id"], "--");
			const sender = resolvePaymentParty(payment, "source", accountMap);
			const receiver = resolvePaymentParty(payment, "destination", accountMap);
			const amount = resolveField(payment, ["amount"], "--");
			const currency = resolveField(payment, ["currency"], "--");
			const status = resolveField(payment, ["status"], "CREATED");
			const createdAt = resolveField(payment, ["createdAt", "createdDate", "createdOn"], "--");
			const reference = resolveField(payment, ["reference"], "--");

			return `
				<tr>
					<td>${escapeHtml(id)}</td>
					<td>
						<strong>${escapeHtml(sender.name)}</strong><br>
						<small>${escapeHtml(sender.accountNumber)}</small>
					</td>
					<td>
						<strong>${escapeHtml(receiver.name)}</strong><br>
						<small>${escapeHtml(receiver.accountNumber)}</small>
					</td>
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
			const reference = String(resolveField(payment, ["reference"], "")).toLowerCase();
			const status = normalizeStatus(resolveField(payment, ["status"], ""));
			const searchMatch = !searchTerm || id.includes(searchTerm) || reference.includes(searchTerm);
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
			const [payments, accounts] = await Promise.all([
				getPayments(),
				getAccounts().catch(() => [])
			]);

			allPayments = Array.isArray(payments) ? payments : [];
			if (Array.isArray(accounts) && accounts.length > 0) {
				availableAccounts = accounts;
				accountMap = new Map(accounts.map((account) => [String(account.accountId ?? account.account_id), account]));
			}
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

	function setCreatePanelVisible(visible) {
		createPanelOpen = visible;
		if (createPaymentPanel) {
			createPaymentPanel.hidden = !visible;
		}
		if (toggleCreateButton) {
			toggleCreateButton.setAttribute("aria-expanded", String(visible));
		}
		if (visible) {
			createPaymentPanel?.scrollIntoView({ behavior: "smooth", block: "start" });
		}
	}

	function toggleCreatePanel() {
		setCreatePanelVisible(!createPanelOpen);
	}

	function resetCreateFormState() {
		createPaymentForm?.reset();
		clearCreatePaymentMessage();
	}

	async function handleCreatePaymentSubmit(event) {
		event.preventDefault();
		clearCreatePaymentMessage();

		if (createPaymentSubmitButton) {
			createPaymentSubmitButton.disabled = true;
			createPaymentSubmitButton.innerHTML = '<i class="fas fa-spinner fa-spin" aria-hidden="true"></i> Creating...';
		}

		try {
			const input = readCreatePaymentForm(createPaymentForm);
			if (!input.idempotencyKey) {
				input.idempotencyKey = generateIdempotencyKey();
			}

			const validationError = validateCreatePaymentInput(input);
			if (validationError) {
				showCreatePaymentMessage("error", validationError);
				return;
			}

			const sourceAccount = availableAccounts.find((account) => String(account.accountId ?? account.account_id) === input.sourceAccountId);
			const destinationAccount = availableAccounts.find((account) => String(account.accountId ?? account.account_id) === input.destinationAccountId);

			const createdPayment = await createPayment({
				...input,
				sourceAccount,
				destinationAccount,
				paymentMethod: "Account Transfer"
			});
			const paymentId = createdPayment?.paymentId || createdPayment?.id;
			const status = normalizeStatus(createdPayment?.status || "CREATED");

			showCreatePaymentMessage(
				status === "FAILED" ? "error" : "success",
				status === "FAILED"
					? `Payment ${paymentId} failed validation. Opening details page...`
					: `Payment ${paymentId} created successfully. Opening lifecycle view...`
			);

			resetCreateFormState();
			setCreatePanelVisible(true);
			await loadPaymentsData();

			window.setTimeout(() => {
				window.location.href = `payment_details.html?id=${encodeURIComponent(paymentId)}&autoProcess=1`;
			}, 900);
		} catch (error) {
			console.error("Create payment failed:", error);
			showCreatePaymentMessage("error", error.message || "Unable to create payment right now. Please try again.");
		} finally {
			if (createPaymentSubmitButton) {
				createPaymentSubmitButton.disabled = false;
				createPaymentSubmitButton.innerHTML = '<i class="fas fa-plus" aria-hidden="true"></i> Create Payment';
			}
		}
	}

	async function loadAccountsForCreatePayment() {
		try {
			availableAccounts = await getAccounts();
			accountMap = new Map(availableAccounts.map((account) => [String(account.accountId ?? account.account_id), account]));
			populateAccountSelect(sourceAccountSelect, availableAccounts, "Select sender account");
			populateAccountSelect(destinationAccountSelect, availableAccounts, "Select receiver account");
		} catch (error) {
			console.error("Error loading accounts:", error);
			showCreatePaymentMessage("error", "Unable to load accounts for payment creation.");
		}
	}

	toggleCreateButton?.addEventListener("click", toggleCreatePanel);
	closeCreateButton?.addEventListener("click", () => setCreatePanelVisible(false));
	createPaymentForm?.addEventListener("submit", handleCreatePaymentSubmit);

	await loadAccountsForCreatePayment();
	await loadPaymentsData();
}

function buildHistoryFallback(payment) {
	if (Array.isArray(payment?.history) && payment.history.length > 0) {
		return payment.history;
	}

	const createdDate = resolveField(payment, ["createdAt", "createdDate", "createdOn"], null);
	const status = resolveField(payment, ["status"], "CREATED");
	const updatedDate = resolveField(payment, ["updatedAt", "updatedDate", "lastUpdated"], null);

	const timeline = [];
	if (createdDate) {
		timeline.push({ oldStatus: null, newStatus: "CREATED", changedAt: createdDate, remarks: "Payment created." });
	}

	if (status && normalizeStatus(status) !== "CREATED") {
		timeline.push({ oldStatus: "CREATED", newStatus: status, changedAt: updatedDate || createdDate, remarks: "Status updated." });
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
		const status = resolveField(event, ["newStatus", "status", "stage"], "CREATED");
		const timestamp = resolveField(event, ["changedAt", "timestamp", "createdAt", "eventTime", "date"], "--");
		const note = resolveField(event, ["remarks", "note", "message", "description"], "Status transition recorded.");

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

function updateLifecycleCard(payment, isProcessing) {
	const card = document.getElementById("lifecycleSimulationCard");
	const message = document.getElementById("lifecycleCurrentMessage");
	const hint = document.getElementById("lifecycleScenarioHint");

	if (!card || !message || !hint) {
		return;
	}

	card.hidden = false;

	const status = normalizeStatus(resolveField(payment, ["status"], "CREATED"));
	const scenario = resolveField(payment, ["_simulationMode"], "AUTO_SUCCESS");

	if (isProcessing) {
		message.textContent = `Payment is currently being processed. Current stage: ${status}.`;
	} else if (status === "COMPLETED") {
		message.textContent = "Payment reached COMPLETED. Funds are considered settled in this demo flow.";
	} else if (status === "FAILED") {
		message.textContent = `Payment finished as FAILED. ${resolveField(payment, ["errorMessage"], "Review the timeline for details.")}`;
	} else {
		message.textContent = `Payment is waiting at ${status}.`;
	}

	const scenarioMap = {
		AUTO_SUCCESS: "Scenario selected: Auto Success — the page will move the payment to COMPLETED.",
		NETWORK_FAIL: "Scenario selected: Gateway Timeout / Network Failure — the page will fail the payment after SENT.",
		BANK_FAIL: "Scenario selected: Bank Rejection — the page will fail the payment at the completion stage."
	};

	hint.textContent = scenarioMap[String(scenario).toUpperCase()] || "This page can automatically step a new payment through the demo lifecycle.";
}

async function enrichPartyDetails(payment) {
	const [sourceAccount, destinationAccount] = await Promise.all([
		payment.sourceAccountId ? getAccountById(payment.sourceAccountId).catch(() => null) : Promise.resolve(null),
		payment.destinationAccountId ? getAccountById(payment.destinationAccountId).catch(() => null) : Promise.resolve(null)
	]);

	return { sourceAccount, destinationAccount };
}

function populatePaymentDetails(payment, sourceAccount, destinationAccount) {
	const status = normalizeStatus(resolveField(payment, ["status"], "CREATED"));
	const amount = resolveField(payment, ["amount"], "--");
	const currency = resolveField(payment, ["currency"], "--");
	const fee = resolveField(payment, ["fee", "transactionFee"], 0);
	const netAmount = Number(amount) - Number(fee);

	setTextById("detailPaymentId", resolveField(payment, ["paymentId", "id"]));
	setTextById("detailReferenceId", resolveField(payment, ["reference", "referenceId", "transactionId", "externalRef"]));
	setTextById("detailCreatedAt", formatDateTime(resolveField(payment, ["createdAt", "createdDate", "createdOn"], null)));
	setTextById("detailUpdatedAt", formatDateTime(resolveField(payment, ["updatedAt", "updatedDate", "lastUpdated"], null)));
	setTextById("detailPaymentMethod", resolveField(payment, ["paymentMethod", "method", "type"], "Account Transfer"));

	setTextById("senderName", resolveField(payment, ["senderName"], sourceAccount?.accountHolderName || sourceAccount?.account_holder_name || "--"));
	setTextById("senderAccount", resolveField(payment, ["senderAccount"], sourceAccount?.accountNumber || sourceAccount?.account_number || resolveField(payment, ["sourceAccountId"], "--")));
	setTextById("senderBank", resolveField(payment, ["senderBank"], sourceAccount?.bankName || sourceAccount?.bank_name || "--"));
	setTextById("senderCountry", resolveField(payment, ["senderCountry"], sourceAccount?.currency || "--"));

	setTextById("receiverName", resolveField(payment, ["receiverName"], destinationAccount?.accountHolderName || destinationAccount?.account_holder_name || "--"));
	setTextById("receiverAccount", resolveField(payment, ["receiverAccount"], destinationAccount?.accountNumber || destinationAccount?.account_number || resolveField(payment, ["destinationAccountId"], "--")));
	setTextById("receiverBank", resolveField(payment, ["receiverBank"], destinationAccount?.bankName || destinationAccount?.bank_name || "--"));
	setTextById("receiverCountry", resolveField(payment, ["receiverCountry"], destinationAccount?.currency || "--"));

	setTextById("detailAmount", formatAmount(amount));
	setTextById("detailCurrency", currency);
	setTextById("detailFee", formatAmount(fee));
	setTextById("detailNetAmount", Number.isFinite(netAmount) ? formatAmount(netAmount) : "--");
	setTextById("detailPurpose", resolveField(payment, ["purpose", "description", "paymentPurpose", "reference"], "General transfer"));
	setTextById("detailChannel", resolveField(payment, ["channel", "source", "origin"], "Frontend Demo"));

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

function getSimulationOutcome(payment) {
	const scenario = normalizeStatus(resolveField(payment, ["_simulationMode"], "AUTO_SUCCESS"));
	if (scenario === "NETWORK_FAIL") {
		return {
			status: "FAILED",
			errorCode: "NETWORK_ERROR",
			errorMessage: "Gateway timeout while sending payment to processor.",
			failureStage: "SENT",
			retryable: true,
			remarks: "Gateway timeout while sending payment to processor."
		};
	}

	if (scenario === "BANK_FAIL") {
		return {
			status: "FAILED",
			errorCode: "BANK_REJECTED",
			errorMessage: "Receiving bank rejected the transaction during final settlement.",
			failureStage: "COMPLETION",
			retryable: false,
			remarks: "Receiving bank rejected the transaction during final settlement."
		};
	}

	return {
		status: "COMPLETED",
		remarks: "Payment completed successfully. Funds have been settled."
	};
}

async function drivePaymentLifecycle(paymentId) {
	let payment = await getPaymentById(paymentId);
	updateLifecycleCard(payment, true);

	if (isTerminalStatus(payment.status)) {
		updateLifecycleCard(payment, false);
		return payment;
	}

	if (normalizeStatus(payment.status) === "CREATED") {
		await delay(1200);
		payment = await updatePaymentStatus(paymentId, "VALIDATED", {
			changedBy: "processor",
			remarks: "Validation checks passed."
		});
		updateLifecycleCard(payment, true);
	}

	if (normalizeStatus(payment.status) === "VALIDATED") {
		await delay(1400);
		payment = await updatePaymentStatus(paymentId, "SENT", {
			changedBy: "processor",
			remarks: "Payment sent to gateway for settlement."
		});
		updateLifecycleCard(payment, true);
	}

	if (normalizeStatus(payment.status) === "SENT") {
		await delay(1800);
		const outcome = getSimulationOutcome(payment);
		payment = await updatePaymentStatus(paymentId, outcome.status, {
			changedBy: "processor",
			remarks: outcome.remarks,
			errorCode: outcome.errorCode,
			errorMessage: outcome.errorMessage,
			failureStage: outcome.failureStage,
			retryable: outcome.retryable
		});
	}

	updateLifecycleCard(payment, false);
	return payment;
}

async function initializePaymentDetailsPage() {
	const detailsRoot = document.getElementById("detailsContent");
	if (!detailsRoot) {
		return;
	}

	const loading = document.getElementById("detailsLoading");
	const error = document.getElementById("detailsError");
	const refreshButton = document.getElementById("refreshLifecycleBtn");
	const paymentId = readPaymentIdFromQuery();
	const autoProcess = shouldAutoProcessFromQuery();

	async function loadAndRender() {
		const payment = await getPaymentById(paymentId);
		const history = await getPaymentHistory(paymentId).catch(() => []);
		const parties = await enrichPartyDetails(payment);

		populatePaymentDetails(payment || {}, parties.sourceAccount, parties.destinationAccount);
		renderTimeline(history.length ? history : buildHistoryFallback(payment || {}));
		updateLifecycleCard(payment || {}, false);

		if (detailsRoot) detailsRoot.hidden = false;
		if (error) error.hidden = true;
		return payment;
	}

	if (!paymentId) {
		if (loading) loading.hidden = true;
		if (error) error.hidden = false;
		return;
	}

	refreshButton?.addEventListener("click", async () => {
		try {
			await loadAndRender();
		} catch (apiError) {
			console.error("Error refreshing payment details:", apiError);
			if (error) error.hidden = false;
		}
	});

	try {
		let payment = await loadAndRender();

		if (autoProcess && !isTerminalStatus(payment.status)) {
			updateLifecycleCard(payment, true);
			payment = await drivePaymentLifecycle(paymentId);
			await loadAndRender();
		}
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
