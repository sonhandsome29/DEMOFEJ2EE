import { useMemo, useState } from 'react';
import { authApi } from '../api/auth.api';
import { ApiError } from '../api/http';
import { authStorage, isStrongPassword } from '../utils/auth';

export function useAuthStore() {
  const [user, setUser] = useState(authStorage.getUser());
  const [token, setToken] = useState(authStorage.getToken());
  const [loading, setLoading] = useState(false);

  const applyLogin = (payload) => {
    const nextToken = payload?.data?.token || payload?.data?.accessToken;
    const nextUser = payload?.data?.user || null;
    if (nextToken) {
      authStorage.setToken(nextToken);
      setToken(nextToken);
    }
    if (nextUser) {
      authStorage.setUser(nextUser);
      setUser(nextUser);
    }
  };

  const logout = () => {
    authStorage.clear();
    setToken(null);
    setUser(null);
    window.location.hash = '#/login';
  };

  const handleApiError = (error) => {
    if (error instanceof ApiError && error.status === 429) {
      const seconds = Number(error.retryAfter || 60);
      return `Too many requests. Try again in ${seconds} seconds.`;
    }
    return error.message || 'Operation failed';
  };

  const login = async (email, password) => {
    setLoading(true);
    try {
      const response = await authApi.login({ email, password });
      applyLogin(response);
      return { ok: true, message: response?.message || 'Login successful' };
    } catch (error) {
      return { ok: false, message: handleApiError(error) };
    } finally {
      setLoading(false);
    }
  };

  const register = async (fullName, email, password) => {
    if (!isStrongPassword(password)) {
      return { ok: false, message: 'Password must be 8-128 chars with upper/lower/digit/special and no spaces.' };
    }
    setLoading(true);
    try {
      const response = await authApi.register({ fullName, email, password });
      return { ok: true, message: response?.message || 'Register successful' };
    } catch (error) {
      return { ok: false, message: handleApiError(error) };
    } finally {
      setLoading(false);
    }
  };

  const forgotPassword = async (email) => {
    setLoading(true);
    try {
      const response = await authApi.forgotPassword({ email });
      return { ok: true, message: response?.message || 'Request submitted' };
    } catch (error) {
      return { ok: false, message: handleApiError(error) };
    } finally {
      setLoading(false);
    }
  };

  const resetPassword = async (tokenValue, newPassword) => {
    if (!isStrongPassword(newPassword)) return { ok: false, message: 'New password does not match policy.' };
    setLoading(true);
    try {
      const response = await authApi.resetPassword({ token: tokenValue, newPassword });
      logout();
      return { ok: true, message: response?.message || 'Password reset successful. Please login again.' };
    } catch (error) {
      return { ok: false, message: handleApiError(error) };
    } finally {
      setLoading(false);
    }
  };

  const changePassword = async (currentPassword, newPassword) => {
    if (!isStrongPassword(newPassword)) return { ok: false, message: 'New password does not match policy.' };
    setLoading(true);
    try {
      const response = await authApi.changePassword({ currentPassword, newPassword });
      logout();
      return { ok: true, message: response?.message || 'Password changed. Please login again.' };
    } catch (error) {
      return { ok: false, message: handleApiError(error) };
    } finally {
      setLoading(false);
    }
  };

  return useMemo(
    () => ({ user, token, loading, login, register, logout, forgotPassword, resetPassword, changePassword }),
    [user, token, loading],
  );
}
