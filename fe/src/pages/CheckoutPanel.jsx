import { useState } from 'react';
import Section from '../components/Section';

export default function CheckoutPanel({ cartStore }) {
  const [message, setMessage] = useState('');

  const submit = async () => {
    const rs = await cartStore.checkout();
    setMessage(rs.ok ? `✅ Order created: ${rs.orderId}` : `❌ ${rs.message}`);
  };

  return (
    <Section title="Checkout">
      <p className="muted">Cart is local in FE. Backend only receives <code>courseIds</code> and <code>paymentMethod</code>.</p>
      <p><strong>{cartStore.items.length}</strong> course(s) in cart</p>
      <div className="row">
        <select value={cartStore.paymentMethod} onChange={(e) => cartStore.setPaymentMethod(e.target.value)}>
          {cartStore.PAYMENT_METHODS.map((m) => (
            <option key={m} value={m}>{m}</option>
          ))}
        </select>
        <button disabled={cartStore.loading || cartStore.items.length === 0} onClick={submit}>Place order</button>
        <button className="secondary" onClick={cartStore.clearCart}>Clear cart</button>
      </div>
      {message && <p>{message}</p>}
    </Section>
  );
}
