import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError, ordersApi } from '../api/client';
import type { Order } from '../api/types';

/**
 * Loads all orders and re-polls them. Eligibility depends on time (inventory freshness, order age),
 * so the list is refreshed periodically and after every mutation to stay in sync with the backend.
 */
export function useOrders(pollIntervalMs = 10_000) {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  // Only the most recent request may update state, so a slow poll cannot overwrite a newer reload.
  const latestRequest = useRef(0);

  const reload = useCallback(async () => {
    const requestId = ++latestRequest.current;
    try {
      const data = await ordersApi.list();
      if (requestId !== latestRequest.current) return;
      setOrders(data);
      setError(null);
      setLastUpdated(new Date());
    } catch (e) {
      if (requestId !== latestRequest.current) return;
      setError(e instanceof ApiError ? e.message : 'Unexpected error while loading orders');
    } finally {
      if (requestId === latestRequest.current) setLoading(false);
    }
  }, []);

  /** Replaces one order in place (e.g. after a mutation returned it) without waiting for the next poll. */
  const upsert = useCallback((order: Order) => {
    setOrders((current) => current.map((o) => (o.orderId === order.orderId ? order : o)));
  }, []);

  useEffect(() => {
    void reload();
    if (pollIntervalMs <= 0) return;
    const id = window.setInterval(() => void reload(), pollIntervalMs);
    return () => window.clearInterval(id);
  }, [reload, pollIntervalMs]);

  return { orders, loading, error, lastUpdated, reload, upsert };
}
