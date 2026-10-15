import type { Eligibility, Order } from '../api/types';

export const eligibility = (overrides: Partial<Eligibility> = {}): Eligibility => ({
  eligible: true,
  score: 100,
  reasons: [],
  warnings: [],
  decision: 'ELIGIBLE',
  blockingReasons: [],
  findings: [],
  evaluatedAt: '2026-10-15T12:00:00Z',
  ...overrides,
});

export const order = (overrides: Partial<Order> = {}): Order => ({
  orderId: 'ORD-1',
  customer: { name: 'Acme', tier: 'GOLD' },
  items: [{ sku: 'SKU-1', name: 'Widget', quantity: 1, unitPrice: 10 }],
  totalAmount: 10,
  shipping: { country: 'US', addressValidated: true },
  riskFlags: [],
  inventoryStatus: { available: true, lastCheckedAt: '2026-10-15T11:59:00Z' },
  status: 'NEW',
  createdAt: '2026-10-15T10:00:00Z',
  expeditedApproval: null,
  version: 0,
  eligibility: eligibility(),
  canApproveExpedited: true,
  ...overrides,
});
