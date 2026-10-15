import { useState } from 'react';
import type { Order } from './api/types';
import { OrderDetail } from './components/OrderDetail';
import { OrdersTable } from './components/OrdersTable';
import { useOrders } from './hooks/useOrders';

export default function App() {
  const { orders, loading, error, lastUpdated, reload, upsert } = useOrders();
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const selected = orders.find((o) => o.orderId === selectedId) ?? null;

  const handleChanged = async (updated?: Order) => {
    if (updated) upsert(updated);
    await reload();
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>Expedited processing review</h1>
        <div className="sync">
          {lastUpdated && <span className="muted">Synced {lastUpdated.toLocaleTimeString()}</span>}
          <button type="button" onClick={() => void reload()}>
            Refresh
          </button>
        </div>
      </header>

      {error && (
        <div role="alert" className="feedback feedback-error">
          {error}
        </div>
      )}

      <main className="layout">
        <section className="list-pane">
          {loading ? <p>Loading orders…</p> : <OrdersTable orders={orders} selectedId={selectedId} onSelect={setSelectedId} />}
        </section>
        <section className="detail-pane">
          {selected ? (
            <OrderDetail key={selected.orderId} order={selected} onChanged={handleChanged} />
          ) : (
            <p className="muted placeholder">Select an order to review its eligibility.</p>
          )}
        </section>
      </main>
    </div>
  );
}
