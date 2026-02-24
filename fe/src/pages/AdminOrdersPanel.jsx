import { useState } from 'react';
import { ordersApi } from '../api/orders.api';
import Section from '../components/Section';

export default function AdminOrdersPanel() {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState('');

  const load = async () => {
    setError('');
    try {
      const response = await ordersApi.getAdminOrders({ page: 0, size: 20 });
      setOrders(response?.data?.orders || response?.data || []);
    } catch (e) {
      setError(e.message);
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
    <Section title="Admin Orders (real API)">
      <button onClick={load}>Load admin orders</button>
      {error && <p className="error">{error}</p>}
      {orders.map((o) => (
        <div key={o.id} className="row">
          <span>{o.id} - {o.status} - {o.paymentMethod}</span>
          <button onClick={() => updateStatus(o.id, 'COMPLETED')}>Set COMPLETED</button>
        </div>
      ))}
    </Section>
  );
}
