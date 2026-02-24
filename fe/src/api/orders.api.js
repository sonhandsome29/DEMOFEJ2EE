import { request } from './http';

export const ordersApi = {
  checkout: (data) => request('/orders', { method: 'POST', body: data, auth: true }),
  getMyOrders: () => request('/orders/me', { auth: true }),
  getById: (orderId) => request(`/orders/${orderId}`, { auth: true }),
  getAdminOrders: (params = {}) => {
    const safeSize = Math.min(Math.max(Number(params.size || 10), 1), 100);
    const safePage = Math.max(Number(params.page || 0), 0);
    return request('/admin/orders', { auth: true, params: { ...params, size: safeSize, page: safePage } });
  },
  updateAdminOrderStatus: (id, status) =>
    request(`/admin/orders/${id}/status`, { method: 'PATCH', body: { status }, auth: true }),
};
