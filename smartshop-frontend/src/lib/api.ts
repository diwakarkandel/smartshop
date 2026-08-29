import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '../stores/authStore';
import type { AuthUser } from '../types';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().user?.accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshing: Promise<boolean> | null = null;

async function refreshAccessToken(): Promise<boolean> {
  try {
    const res = await axios.post<{ data: AuthUser }>(
      '/api/v1/auth/refresh',
      {},
      { withCredentials: true },
    );
    const auth = res.data?.data;
    if (auth?.accessToken) {
      useAuthStore.getState().setSession(auth);
    }
    return true;
  } catch {
    return false;
  }
}

interface RetryConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetryConfig | undefined;
    if (error.response?.status === 401 && original && !original._retry) {
      original._retry = true;
      if (!refreshing) {
        refreshing = refreshAccessToken();
      }
      const ok = await refreshing;
      refreshing = null;
      if (ok) {
        return api(original);
      }
      useAuthStore.getState().setSession(null);
      if (!original.url?.includes('/auth/login')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);

export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as
      | { message?: string; errors?: { message: string }[] }
      | undefined;
    if (data?.message) return data.message;
    if (data?.errors?.length) return data.errors.map((e) => e.message).join(', ');
    return error.message;
  }
  return error instanceof Error ? error.message : 'Something went wrong';
}

export default api;