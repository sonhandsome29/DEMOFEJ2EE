import { useState } from 'react';
import { ordersApi } from '../api/orders.api';
import Section from '../components/Section';

export default function AdminOrdersPanel() {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await ordersApi.getAdminOrders({ page: 0, size: 20 });
      setOrders(response?.data?.orders || response?.data || []);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  };

  const updateStatus = async (id, status) => {
    try {
      await ordersApi.updateAdminOrderStatus(id, status);
      await load();
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <Section title="Admin Order Management">
      <p className="muted">Uses real backend APIs: <code>GET /api/admin/orders</code> and <code>PATCH /api/admin/orders/{'{id}'}/status</code></p>
      <button onClick={load} disabled={loading}>{loading ? 'Loading...' : 'Load admin orders'}</button>
      {error && <p className="status-error">{error}</p>}
      {orders.map((o) => (
        <div key={o.id} className="course-item">
          <div>
            <strong>{o.id}</strong>
            <div className="muted">Status: {o.status} • Payment: {o.paymentMethod}</div>
          </div>
          <div className="row">
            <button className="secondary" onClick={() => updateStatus(o.id, 'PENDING')}>PENDING</button>
            <button onClick={() => updateStatus(o.id, 'COMPLETED')}>COMPLETED</button>
          </div>
        </div>
      ))}
    </Section>
  );
}
