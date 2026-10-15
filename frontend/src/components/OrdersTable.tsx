import type { Order } from '../api/types';
import { DecisionBadge, StatusBadge } from './Badges';

interface Props {
  orders: Order[];
  selectedId: string | null;
  onSelect: (orderId: string) => void;
}

export function OrdersTable({ orders, selectedId, onSelect }: Props) {
  return (
    <table className="orders-table">
      <thead>
        <tr>
          <th>Order</th>
          <th>Customer</th>
          <th>Status</th>
          <th>Expedited</th>
          <th className="num">Score</th>
        </tr>
      </thead>
      <tbody>
        {orders.map((order) => (
          <tr
            key={order.orderId}
            className={order.orderId === selectedId ? 'selected' : undefined}
            onClick={() => onSelect(order.orderId)}
            tabIndex={0}
            onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && onSelect(order.orderId)}
            aria-selected={order.orderId === selectedId}
          >
            <td className="mono">{order.orderId}</td>
            <td>
              {order.customer?.name ?? <em className="muted">unknown</em>}
              <span className="tier">{order.customer?.tier ?? 'NO TIER'}</span>
            </td>
            <td>
              <StatusBadge status={order.status} />
            </td>
            <td>
              <DecisionBadge decision={order.eligibility.decision} />
            </td>
            <td className="num">{order.eligibility.score}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
