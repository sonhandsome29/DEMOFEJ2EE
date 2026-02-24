import { useState } from 'react';
import Section from '../components/Section';

export default function CheckoutPanel({ cartStore }) {
  const [message, setMessage] = useState('');

  const submit = async () => {
    const rs = await cartStore.checkout();
    setMessage(rs.ok ? `Order created: ${rs.orderId}` : rs.message);
  };

  return (
    <Section title="Checkout">
      <p>Local cart items: {cartStore.items.length}</p>
      <select value={cartStore.paymentMethod} onChange={(e) => cartStore.setPaymentMethod(e.target.value)}>
        {cartStore.PAYMENT_METHODS.map((m) => (
          <option key={m} value={m}>{m}</option>
        ))}
      </select>
      <div>
        <button onClick={submit}>Checkout with backend</button>
      </div>
      {message && <p>{message}</p>}
    </Section>
  );
}
