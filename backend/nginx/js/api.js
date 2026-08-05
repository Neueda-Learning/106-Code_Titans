const API_BASE_URL = window.API_BASE_URL || "http://localhost:8082";

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

async function getPayments(params = {}) {
	const payload = await apiGet("/payments", params);
	return normalizeArrayResponse(payload);
}

async function getPaymentById(paymentId) {
	if (!paymentId) {
		throw new Error("Payment ID is required.");
	}

	return apiGet(`/payments/${encodeURIComponent(paymentId)}`);
}

async function getPaymentHistory(paymentId) {
	if (!paymentId) {
		throw new Error("Payment ID is required.");
	}

	const payload = await apiGet(`/payments/${encodeURIComponent(paymentId)}/history`);
	return normalizeArrayResponse(payload);
}

window.apiGet = apiGet;
window.getPayments = getPayments;
window.getPaymentById = getPaymentById;
window.getPaymentHistory = getPaymentHistory;
