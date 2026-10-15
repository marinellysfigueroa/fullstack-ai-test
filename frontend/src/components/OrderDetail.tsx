import { useState } from 'react';
import { ApiError, ordersApi } from '../api/client';
import type { ApiErrorBody, Order } from '../api/types';
import { StatusBadge } from './Badges';
import { EligibilityPanel } from './EligibilityPanel';

interface Props {
  order: Order;
  /** Called after any successful mutation so the caller can re-sync with the backend. */
  onChanged: (updated?: Order) => Promise<void> | void;
}

type Feedback = { kind: 'success'; message: string } | { kind: 'error'; error: ApiErrorBody } | null;

const formatDate = (iso: string | null) => (iso ? new Date(iso).toLocaleString() : '—');
const show = (value: unknown) => (value === null || value === undefined ? '—' : String(value));

export function OrderDetail({ order, onChanged }: Props) {
  const [pending, setPending] = useState<'approve' | 'inventory' | null>(null);
  const [feedback, setFeedback] = useState<Feedback>(null);

  const approve = async () => {
    setPending('approve');
    setFeedback(null);
    try {
      const result = await ordersApi.approveExpedited(order.orderId);
      setFeedback({
        kind: 'success',
        message: result.alreadyApproved
          ? `${result.orderId} was already approved for expedited processing.`
          : `${result.orderId} approved for expedited processing (score ${result.eligibility?.score}).`,
      });
    } catch (e) {
      setFeedback({
        kind: 'error',
        error: e instanceof ApiError ? e.body : { status: 0, code: 'UNKNOWN', message: String(e) },
      });
    } finally {
      // Re-sync in every case: a refused approval usually means the data changed since the last poll.
      await onChanged();
      setPending(null);
    }
  };

  const refreshInventory = async () => {
    setPending('inventory');
    setFeedback(null);
    try {
      await onChanged(await ordersApi.refreshInventory(order.orderId));
    } catch (e) {
      setFeedback({ kind: 'error', error: e instanceof ApiError ? e.body : { status: 0, code: 'UNKNOWN', message: String(e) } });
    } finally {
      setPending(null);
    }
  };

  const items = order.items ?? [];

  return (
    <article className="order-detail">
      <header className="detail-header">
        <h2 className="mono">{order.orderId}</h2>
        <StatusBadge status={order.status} />
      </header>

      <EligibilityPanel eligibility={order.eligibility} />

      <div className="actions">
        <button
          type="button"
          className="primary"
          onClick={approve}
          disabled={!order.canApproveExpedited || pending !== null}
          title={order.canApproveExpedited ? undefined : 'The server reports this order cannot be approved'}
        >
          {pending === 'approve' ? 'Approving…' : order.status === 'APPROVED' ? 'Approved' : 'Approve expedited'}
        </button>
        <button type="button" onClick={refreshInventory} disabled={pending !== null}>
          {pending === 'inventory' ? 'Checking…' : 'Re-check inventory (simulated)'}
        </button>
      </div>

      {feedback?.kind === 'success' && (
        <div role="status" className="feedback feedback-success">
          {feedback.message}
        </div>
      )}
      {feedback?.kind === 'error' && (
        <div role="alert" className="feedback feedback-error">
          <strong>Approval refused ({feedback.error.code})</strong>: {feedback.error.message}
          {feedback.error.reasons && feedback.error.reasons.length > 0 && (
            <ul>
              {feedback.error.reasons.map((r) => (
                <li key={r}>{r}</li>
              ))}
            </ul>
          )}
        </div>
      )}

      {order.expeditedApproval && (
        <p className="approval-note">
          Approved at {formatDate(order.expeditedApproval.approvedAt)} with score {order.expeditedApproval.scoreAtApproval}.
        </p>
      )}

      <dl className="facts">
        <dt>Customer</dt>
        <dd>
          {show(order.customer?.name)} ({order.customer?.tier ?? 'no tier'})
        </dd>
        <dt>Total amount</dt>
        <dd>{show(order.totalAmount)}</dd>
        <dt>Shipping</dt>
        <dd>
          {show(order.shipping?.country)} · address validated: {show(order.shipping?.addressValidated)}
        </dd>
        <dt>Risk flags</dt>
        <dd>{order.riskFlags === null ? 'missing' : order.riskFlags.length ? order.riskFlags.join(', ') : 'none'}</dd>
        <dt>Inventory</dt>
        <dd>
          {order.inventoryStatus
            ? `${order.inventoryStatus.available ? 'available' : 'unavailable'} · checked ${formatDate(order.inventoryStatus.lastCheckedAt)}`
            : 'missing'}
        </dd>
        <dt>Created</dt>
        <dd>{formatDate(order.createdAt)}</dd>
      </dl>

      <h3>Items</h3>
      {items.length === 0 ? (
        <p className="muted">No items</p>
      ) : (
        <table className="items-table">
          <thead>
            <tr>
              <th>SKU</th>
              <th>Name</th>
              <th className="num">Qty</th>
              <th className="num">Unit price</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item, i) => (
              <tr key={`${item.sku}-${i}`}>
                <td className="mono">{show(item.sku)}</td>
                <td>{show(item.name)}</td>
                <td className="num">{show(item.quantity)}</td>
                <td className="num">{show(item.unitPrice)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </article>
  );
}
