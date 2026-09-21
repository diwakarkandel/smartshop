import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { useAuthStore } from '../stores/authStore';
import type { AuthUser, Sale, Tax, TaxRate, TaxType } from '../types';

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
      if (!original.url?.includes('/auth/')) {
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

export interface TaxRatePayload {
  rate: number;
  validFrom: string;
  validTo?: string | null;
}

export interface TaxPayload {
  shopId: string;
  name: string;
  type: TaxType;
  description?: string;
  isActive: boolean;
  rates: TaxRatePayload[];
}

export async function listTaxes(shopId: string, activeOnly = false): Promise<Tax[]> {
  const res = await api.get<{ data: Tax[] }>('/taxes', { params: { shopId, activeOnly } });
  return res.data.data;
}

export async function getTax(id: string): Promise<Tax> {
  const res = await api.get<{ data: Tax }>(`/taxes/${id}`);
  return res.data.data;
}

export async function createTax(payload: TaxPayload): Promise<Tax> {
  const res = await api.post<{ data: Tax }>('/taxes', payload);
  return res.data.data;
}

export async function updateTax(id: string, payload: TaxPayload): Promise<Tax> {
  const res = await api.put<{ data: Tax }>(`/taxes/${id}`, payload);
  return res.data.data;
}

export async function addTaxRate(taxId: string, payload: TaxRatePayload): Promise<TaxRate> {
  const res = await api.post<{ data: TaxRate }>(`/taxes/${taxId}/rates`, payload);
  return res.data.data;
}

export async function listTaxRates(taxId: string): Promise<TaxRate[]> {
  const res = await api.get<TaxRate[]>(`/taxes/${taxId}/rates`);
  return res.data;
}

/**
 * Hard delete — `TaxService.delete` calls `taxRepository.delete(tax)`, it does not
 * soft-deactivate. Prefer toggling `isActive` unless the row is genuinely unused.
 */
export async function deleteTax(id: string): Promise<void> {
  await api.delete(`/taxes/${id}`);
}

export async function getSale(id: string): Promise<Sale> {
  const res = await api.get<{ data: Sale }>(`/sales/${id}`);
  return res.data.data;
}

export async function downloadDocument(url: string, fallbackName: string): Promise<void> {
  const res = await api.get<Blob>(url, { responseType: 'blob' });
  const disposition = res.headers['content-disposition'] as string | undefined;
  const match = disposition?.match(/filename="?([^"]+)"?/);
  const blobUrl = URL.createObjectURL(res.data);
  const link = document.createElement('a');
  link.href = blobUrl;
  link.download = match?.[1] ?? fallbackName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(blobUrl);
}

export default api;