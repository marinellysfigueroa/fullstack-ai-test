import type { ApiErrorBody, ApprovalResponse, Order } from './types';

const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');

/** Error raised for non-2xx responses, carrying the backend's structured body when present. */
export class ApiError extends Error {
  readonly status: number;
  readonly body: ApiErrorBody;

  constructor(body: ApiErrorBody) {
    super(body.message);
    this.name = 'ApiError';
    this.status = body.status;
    this.body = body;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...init,
      headers: { Accept: 'application/json', ...(init?.body ? { 'Content-Type': 'application/json' } : {}) },
    });
  } catch {
    throw new ApiError({ status: 0, code: 'NETWORK_ERROR', message: 'Cannot reach the orders API' });
  }

  const text = await response.text();
  const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new ApiError(
      data && typeof data === 'object' && 'code' in data
        ? (data as ApiErrorBody)
        : { status: response.status, code: 'HTTP_ERROR', message: `Request failed with status ${response.status}` },
    );
  }
  return data as T;
}

export const ordersApi = {
  list: () => request<Order[]>('/api/orders'),
  get: (orderId: string) => request<Order>(`/api/orders/${encodeURIComponent(orderId)}`),
  approveExpedited: (orderId: string) =>
    request<ApprovalResponse>(`/api/orders/${encodeURIComponent(orderId)}/approve-expedited`, { method: 'POST' }),
  refreshInventory: (orderId: string, available?: boolean) =>
    request<Order>(`/api/orders/${encodeURIComponent(orderId)}/inventory/refresh`, {
      method: 'POST',
      body: JSON.stringify(available === undefined ? {} : { available }),
    }),
};
