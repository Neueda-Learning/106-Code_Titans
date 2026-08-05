const API_BASE_URL = window.API_BASE_URL || "http://localhost:8082";
const LOCAL_PAYMENTS_KEY = "pps_created_payments";

async function parseApiResponse(response) {
	let payload = null;

	try {
		payload = await response.json();
	} catch {
		payload = null;
	}

	if (!response.ok) {
		const error = new Error(payload?.message || `API request failed: ${response.status}`);
		error.status = response.status;
		error.payload = payload;
		throw error;
	}

	return payload;
}

async function apiGet(path, params = {}) {
	const url = new URL(`${API_BASE_URL}${path}`);

	Object.entries(params).forEach(([key, value]) => {
		if (value !== undefined && value !== null && value !== "") {
			url.searchParams.set(key, value);
		}
	});

	const response = await fetch(url.toString(), {
		method: "GET",
		headers: {
			"Content-Type": "application/json"
		}
	});

	return parseApiResponse(response);
}

async function apiPost(path, payload = {}) {
	const response = await fetch(`${API_BASE_URL}${path}`, {
		method: "POST",
		headers: {
			"Content-Type": "application/json"
		},
		body: JSON.stringify(payload)
	});

	return parseApiResponse(response);
}

async function apiPut(path, payload = {}) {
	const response = await fetch(`${API_BASE_URL}${path}`, {
		method: "PUT",
		headers: {
			"Content-Type": "application/json"
		},
		body: JSON.stringify(payload)
	});

	return parseApiResponse(response);
}

function normalizeArrayResponse(payload) {
	if (Array.isArray(payload)) {
		return payload;
	}

	if (Array.isArray(payload?.data)) {
		return payload.data;
	}

	if (Array.isArray(payload?.content)) {
		return payload.content;
	}

	return [];
}

function normalizeObjectResponse(payload) {
	if (!payload || typeof payload !== "object") {
		return payload;
	}

	if (payload.data && typeof payload.data === "object" && !Array.isArray(payload.data)) {
		return payload.data;
	}

	return payload;
}

function getLocalCreatedPayments() {
	try {
		const raw = window.localStorage.getItem(LOCAL_PAYMENTS_KEY);
		const parsed = raw ? JSON.parse(raw) : [];
		return Array.isArray(parsed) ? parsed : [];
	} catch {
		return [];
	}
}

function saveLocalCreatedPayments(payments) {
	window.localStorage.setItem(LOCAL_PAYMENTS_KEY, JSON.stringify(payments));
}

function upsertLocalPayment(payment) {
	const current = getLocalCreatedPayments();
	const paymentId = payment?.paymentId || payment?.id;

	if (!paymentId) {
		return payment;
	}

	const next = current.filter((item) => {
		const itemId = item?.paymentId || item?.id;
		return String(itemId) !== String(paymentId);
	});

	next.unshift({ ...payment, _local: true });
	saveLocalCreatedPayments(next);
	return payment;
}

function updateLocalPayment(paymentId, updater) {
	const current = getLocalCreatedPayments();
	const next = current.map((item) => {
		const itemId = item?.paymentId || item?.id;
		if (String(itemId) !== String(paymentId)) {
			return item;
		}
		return updater({ ...item });
	});
	saveLocalCreatedPayments(next);
}

function mergePaymentRecord(apiRecord, localRecord) {
	if (!apiRecord && !localRecord) {
		return null;
	}

	if (!apiRecord) {
		return localRecord;
	}

	if (!localRecord) {
		return apiRecord;
	}

	const merged = { ...apiRecord };
	Object.entries(localRecord).forEach(([key, value]) => {
		if (merged[key] === undefined || merged[key] === null || merged[key] === "") {
			merged[key] = value;
		}
	});

	if (Array.isArray(localRecord.history)) {
		merged.history = localRecord.history;
	}

	if (localRecord._simulationMode) {
		merged._simulationMode = localRecord._simulationMode;
	}

	return merged;
}

function mergePaymentsById(apiPayments, localPayments) {
	const mergedMap = new Map();

	(apiPayments || []).forEach((payment) => {
		const id = payment?.paymentId || payment?.id;
		if (id !== undefined && id !== null) {
			mergedMap.set(String(id), payment);
		}
	});

	(localPayments || []).forEach((payment) => {
		const id = payment?.paymentId || payment?.id;
		if (id === undefined || id === null) {
			return;
		}

		const key = String(id);
		const existing = mergedMap.get(key);
		mergedMap.set(key, mergePaymentRecord(existing, payment));
	});

	return Array.from(mergedMap.values());
}

function createPaymentId() {
	const now = new Date();
	const datePart = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, "0")}${String(now.getDate()).padStart(2, "0")}`;
	const randomPart = Math.floor(Math.random() * 9000 + 1000);
	return `PAY-${datePart}-${randomPart}`;
}

function createHistoryEvent({ oldStatus = null, newStatus, changedAt, changedBy = "system", remarks = "" }) {
	return {
		oldStatus,
		newStatus,
		changedAt: changedAt || new Date().toISOString(),
		changedBy,
		remarks,
		status: newStatus,
		timestamp: changedAt || new Date().toISOString(),
		note: remarks || `Moved to ${newStatus}`
	};
}

function getLocalPaymentById(paymentId) {
	const localPayments = getLocalCreatedPayments();
	return localPayments.find((item) => {
		const id = item?.paymentId || item?.id;
		return String(id) === String(paymentId);
	}) || null;
}

async function getPayments(params = {}) {
	const localPayments = getLocalCreatedPayments();

	try {
		const payload = await apiGet("/payments", params);
		const apiPayments = normalizeArrayResponse(payload);
		return mergePaymentsById(apiPayments, localPayments);
	} catch {
		return localPayments;
	}
}

async function getAccounts() {
	const payload = await apiGet("/accounts");
	return normalizeArrayResponse(payload);
}

async function getAccountById(accountId) {
	if (!accountId) {
		throw new Error("Account ID is required.");
	}

	const payload = await apiGet(`/accounts/${encodeURIComponent(accountId)}`);
	return normalizeObjectResponse(payload);
}

async function getPaymentById(paymentId) {
	if (!paymentId) {
		throw new Error("Payment ID is required.");
	}

	const localRecord = getLocalPaymentById(paymentId);

	try {
		const payload = await apiGet(`/payments/${encodeURIComponent(paymentId)}`);
		const apiRecord = normalizeObjectResponse(payload);
		return mergePaymentRecord(apiRecord, localRecord);
	} catch {
		if (localRecord) {
			return localRecord;
		}
		throw new Error("Payment not found.");
	}
}

function mapHistoryEntry(entry) {
	return {
		historyId: entry?.historyId,
		oldStatus: entry?.oldStatus || entry?.old_status || null,
		newStatus: entry?.newStatus || entry?.new_status || entry?.status || "CREATED",
		changedAt: entry?.changedAt || entry?.changed_at || entry?.timestamp || entry?.createdAt || null,
		changedBy: entry?.changedBy || entry?.changed_by || entry?.actor || "system",
		remarks: entry?.remarks || entry?.note || entry?.message || "Status transition recorded.",
		status: entry?.newStatus || entry?.new_status || entry?.status || "CREATED",
		timestamp: entry?.changedAt || entry?.changed_at || entry?.timestamp || entry?.createdAt || null,
		note: entry?.remarks || entry?.note || entry?.message || "Status transition recorded."
	};
}

async function getPaymentHistory(paymentId) {
	if (!paymentId) {
		throw new Error("Payment ID is required.");
	}

	try {
		const payload = await apiGet(`/payments/${encodeURIComponent(paymentId)}/history`);
		const rows = normalizeArrayResponse(payload).map(mapHistoryEntry);
		if (rows.length > 0) {
			return rows;
		}
	} catch {
		// fall back to local history below
	}

	const localPayment = getLocalPaymentById(paymentId) || await getPaymentById(paymentId);

	if (Array.isArray(localPayment?.history)) {
		return localPayment.history.map(mapHistoryEntry);
	}

	return [];
}

async function createPayment(paymentInput) {
	const nowIso = new Date().toISOString();
	const generatedId = paymentInput.paymentId || createPaymentId();
	const sourceAccountId = Number(paymentInput.sourceAccountId);
	const destinationAccountId = Number(paymentInput.destinationAccountId);
	const sourceAccount = paymentInput.sourceAccount || null;
	const destinationAccount = paymentInput.destinationAccount || null;
	const simulationMode = paymentInput.processingScenario || "AUTO_SUCCESS";
	const payload = {
		sourceAccountId,
		destinationAccountId,
		amount: Number(paymentInput.amount),
		currency: paymentInput.currency,
		reference: paymentInput.reference || `REF-${generatedId}`,
		idempotencyKey: paymentInput.idempotencyKey || generatedId
	};

	const displayPayload = {
		paymentId: generatedId,
		sourceAccountId,
		destinationAccountId,
		amount: Number(paymentInput.amount),
		currency: paymentInput.currency,
		reference: payload.reference,
		idempotencyKey: payload.idempotencyKey,
		status: paymentInput.status || "CREATED",
		createdAt: nowIso,
		updatedAt: nowIso,
		senderName: sourceAccount?.accountHolderName,
		senderAccount: sourceAccount?.accountNumber,
		senderBank: sourceAccount?.bankName,
		senderCountry: sourceAccount?.currency,
		receiverName: destinationAccount?.accountHolderName,
		receiverAccount: destinationAccount?.accountNumber,
		receiverBank: destinationAccount?.bankName,
		receiverCountry: destinationAccount?.currency,
		paymentMethod: paymentInput.paymentMethod || "Account Transfer",
		purpose: paymentInput.reference || "General transfer",
		channel: "Frontend Demo",
		_simulationMode: simulationMode,
		history: [createHistoryEvent({ oldStatus: null, newStatus: "CREATED", changedAt: nowIso, changedBy: "system", remarks: "Payment created" })]
	};

	try {
		const apiResponse = await apiPost("/payments", payload);
		const apiRecord = normalizeObjectResponse(apiResponse);
		const merged = {
			...displayPayload,
			...apiRecord,
			paymentId: apiRecord?.paymentId || apiRecord?.id || displayPayload.paymentId,
			status: apiRecord?.status || displayPayload.status
		};

		merged.history = [
			createHistoryEvent({
				oldStatus: null,
				newStatus: merged.status || "CREATED",
				changedAt: merged.createdAt || nowIso,
				changedBy: "system",
				remarks: merged.status === "FAILED" ? (merged.errorMessage || "Payment validation failed") : "Payment created"
			})
		];

		upsertLocalPayment(merged);
		return merged;
	} catch (error) {
		const failedRecord = normalizeObjectResponse(error.payload) || {};
		const merged = {
			...displayPayload,
			...failedRecord,
			paymentId: failedRecord?.paymentId || failedRecord?.id || displayPayload.paymentId,
			status: failedRecord?.status || "FAILED",
			errorCode: failedRecord?.errorCode || "REQUEST_FAILED",
			errorMessage: failedRecord?.errorMessage || error.message
		};

		merged.history = [
			createHistoryEvent({
				oldStatus: null,
				newStatus: merged.status,
				changedAt: merged.createdAt || nowIso,
				changedBy: "system",
				remarks: merged.errorMessage || "Payment validation failed"
			})
		];

		upsertLocalPayment(merged);
		return merged;
	}
}

async function updatePaymentStatus(paymentId, status, options = {}) {
	if (!paymentId) {
		throw new Error("Payment ID is required.");
	}

	const nowIso = new Date().toISOString();
	const payload = {
		status,
		changedBy: options.changedBy || "frontend-demo",
		remarks: options.remarks || `Status moved to ${status}`
	};

	let updatedRecord;
	try {
		const response = await apiPut(`/payments/${encodeURIComponent(paymentId)}/status`, payload);
		updatedRecord = normalizeObjectResponse(response) || {};
	} catch (error) {
		if (error.payload?.data) {
			updatedRecord = normalizeObjectResponse(error.payload);
		} else {
			throw error;
		}
	}

	const localExisting = getLocalPaymentById(paymentId) || {};
	const currentStatus = localExisting.status || updatedRecord.status || "CREATED";
	const merged = {
		...localExisting,
		...updatedRecord,
		paymentId: updatedRecord?.paymentId || updatedRecord?.id || localExisting.paymentId || paymentId,
		status,
		updatedAt: updatedRecord?.updatedAt || nowIso
	};

	if (options.errorCode) {
		merged.errorCode = options.errorCode;
	}
	if (options.errorMessage) {
		merged.errorMessage = options.errorMessage;
	}
	if (options.failureStage) {
		merged.failureStage = options.failureStage;
	}
	if (options.retryable !== undefined) {
		merged.retryable = options.retryable;
	}

	const existingHistory = Array.isArray(localExisting.history) ? localExisting.history : [];
	merged.history = [
		...existingHistory,
		createHistoryEvent({
			oldStatus: currentStatus,
			newStatus: status,
			changedAt: nowIso,
			changedBy: payload.changedBy,
			remarks: payload.remarks
		})
	];

	upsertLocalPayment(merged);
	return merged;
}

window.apiGet = apiGet;
window.apiPost = apiPost;
window.apiPut = apiPut;
window.getPayments = getPayments;
window.getAccounts = getAccounts;
window.getAccountById = getAccountById;
window.getPaymentById = getPaymentById;
window.getPaymentHistory = getPaymentHistory;
window.createPayment = createPayment;
window.updatePaymentStatus = updatePaymentStatus;
window.getLocalCreatedPayments = getLocalCreatedPayments;
