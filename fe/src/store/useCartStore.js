import { useMemo, useState } from 'react';
import { ordersApi } from '../api/orders.api';
import { ApiError } from '../api/http';

const PAYMENT_METHODS = ['CARD', 'MOMO', 'BANK_TRANSFER'];

export function useCartStore() {
  const [items, setItems] = useState([]);
  const [paymentMethod, setPaymentMethod] = useState('CARD');
  const [loading, setLoading] = useState(false);

  const addToCart = (course) => setItems((prev) => (prev.find((x) => x.id === course.id) ? prev : [...prev, course]));
  const removeFromCart = (id) => setItems((prev) => prev.filter((x) => x.id !== id));
  const clearCart = () => setItems([]);

  const checkout = async () => {
    if (!PAYMENT_METHODS.includes(paymentMethod)) {
      return { ok: false, message: 'Invalid payment method' };
    }

    setLoading(true);
    try {
      const payload = {
        courseIds: items.map((x) => x.id),
        paymentMethod,
      };
      const response = await ordersApi.checkout(payload);
      const orderId = response?.data?.id;
      clearCart();
      return { ok: true, orderId, message: response?.message || 'Checkout success' };
    } catch (error) {
      if (error instanceof ApiError && error.status === 429) {
        return { ok: false, message: `Checkout is rate-limited. Retry in ${error.retryAfter || 60}s.` };
      }
      return { ok: false, message: error.message || 'Checkout failed' };
    } finally {
      setLoading(false);
    }
  };

  return useMemo(
    () => ({ items, paymentMethod, setPaymentMethod, addToCart, removeFromCart, clearCart, checkout, loading, PAYMENT_METHODS }),
    [items, paymentMethod, loading],
  );
}
