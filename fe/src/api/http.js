import { authStorage } from '../utils/auth';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export class ApiError extends Error {
  constructor(status, message, data, retryAfter) {
    super(message);
    this.status = status;
    this.data = data;
    this.retryAfter = retryAfter;
  }
}

export async function request(path, { method = 'GET', body, auth = false, params } = {}) {
  const url = new URL(`${API_BASE_URL}${path}`);
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') url.searchParams.set(key, value);
    });
  }

  const headers = { 'Content-Type': 'application/json' };
  if (auth) {
    const token = authStorage.getToken();
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(url.toString(), {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  let payload;
  try {
    payload = await response.json();
  } catch {
    payload = { success: false, message: 'Invalid response from server' };
  }

  if (!response.ok) {
    const retryAfter = response.headers.get('Retry-After');
    throw new ApiError(response.status, payload?.message || 'Request failed', payload?.data, retryAfter);
  }

  return payload;
}
