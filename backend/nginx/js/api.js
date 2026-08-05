const API_BASE_URL = window.API_BASE_URL || "http://localhost:8080";
const LOCAL_PAYMENTS_KEY = "pps_created_payments";

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

	if (!response.ok) {
		throw new Error(`API request failed: ${response.status}`);
	}

	return response.json();
}

async function apiPost(path, payload = {}) {
	const response = await fetch(`${API_BASE_URL}${path}`, {
		method: "POST",
		headers: {
			"Content-Type": "application/json"
		},
		body: JSON.stringify(payload)
	});

	if (!response.ok) {
		throw new Error(`API request failed: ${response.status}`);
	}

	return response.json();
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

function addLocalCreatedPayment(payment) {
	const current = getLocalCreatedPayments();
	const paymentId = payment?.paymentId || payment?.id;

	if (!paymentId) {
		return;
	}

	const withoutDuplicate = current.filter((item) => {
		const itemId = item?.paymentId || item?.id;
		return itemId !== paymentId;
	});

	withoutDuplicate.unshift({ ...payment, _local: true });
	saveLocalCreatedPayments(withoutDuplicate);
}

function mergePaymentsById(apiPayments, localPayments) {
	const mergedMap = new Map();

	(apiPayments || []).forEach((payment) => {
		const id = payment?.paymentId || payment?.id;
		if (id) {
			mergedMap.set(id, payment);
		}
	});

	(localPayments || []).forEach((payment) => {
		const id = payment?.paymentId || payment?.id;
		if (id && !mergedMap.has(id)) {
			mergedMap.set(id, payment);
		}
	});

	return Array.from(mergedMap.values());
}

function createPaymentId() {
	const now = new Date();
	const datePart = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, "0")}${String(now.getDate()).padStart(2, "0")}`;
	const randomPart = Math.floor(Math.random() * 9000 + 1000);
	return `PAY-${datePart}-${randomPart}`;
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
	if (payload && typeof payload === "object" && Array.isArray(payload.data)) {
		return payload.data;
	}
	return payload?.data ?? payload;
}

async function getPaymentById(paymentId) {
	if (!paymentId) {
		throw new Error("Payment ID is required.");
	}

	try {
		return await apiGet(`/payments/${encodeURIComponent(paymentId)}`);
	} catch {
		const localPayments = getLocalCreatedPayments();
		const localRecord = localPayments.find((item) => {
			const id = item?.paymentId || item?.id;
			return id === paymentId;
		});

		if (localRecord) {
			return localRecord;
		}

		throw new Error("Payment not found.");
	}
}

async function getPaymentHistory(paymentId) {
	if (!paymentId) {
		throw new Error("Payment ID is required.");
	}

	try {
		const payload = await apiGet(`/payments/${encodeURIComponent(paymentId)}/history`);
		return normalizeArrayResponse(payload);
	} catch {
		const localPayment = await getPaymentById(paymentId);
		if (Array.isArray(localPayment?.history)) {
			return localPayment.history;
		}

		return [];
	}
}

async function createPayment(paymentInput) {
	const nowIso = new Date().toISOString();
	const generatedId = paymentInput.paymentId || createPaymentId();
	const sourceAccountId = Number(paymentInput.sourceAccountId);
	const destinationAccountId = Number(paymentInput.destinationAccountId);
	const sourceAccount = paymentInput.sourceAccount || null;
	const destinationAccount = paymentInput.destinationAccount || null;
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
		receiverCountry: destinationAccount?.currency
	};

	try {
		const apiResponse = await apiPost("/payments", payload);
		const merged = { ...displayPayload, ...apiResponse?.data, ...apiResponse };
		addLocalCreatedPayment(merged);
		return merged;
	} catch {
		addLocalCreatedPayment(displayPayload);
		return displayPayload;
	}
}

window.apiGet = apiGet;
window.apiPost = apiPost;
window.getPayments = getPayments;
window.getAccounts = getAccounts;
window.getAccountById = getAccountById;
window.getPaymentById = getPaymentById;
window.getPaymentHistory = getPaymentHistory;
window.createPayment = createPayment;
window.getLocalCreatedPayments = getLocalCreatedPayments;
