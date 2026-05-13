import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { MeResponse } from './types';

const STORAGE_ACCESS = 'afya_access_token';
const STORAGE_REFRESH = 'afya_refresh_token';

function getBaseURL(): string {
  return import.meta.env.VITE_API_BASE_URL ?? '';
}

export const api = axios.create({
  baseURL: getBaseURL(),
  headers: { 'Content-Type': 'application/json' },
});

export function getStoredAccessToken(): string | null {
  return sessionStorage.getItem(STORAGE_ACCESS);
}

export function getStoredRefreshToken(): string | null {
  return sessionStorage.getItem(STORAGE_REFRESH);
}

export function persistTokens(access: string, refresh: string): void {
  sessionStorage.setItem(STORAGE_ACCESS, access);
  sessionStorage.setItem(STORAGE_REFRESH, refresh);
}

export function clearTokens(): void {
  sessionStorage.removeItem(STORAGE_ACCESS);
  sessionStorage.removeItem(STORAGE_REFRESH);
}

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getStoredAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const refresh = getStoredRefreshToken();
  if (!refresh) return null;
  try {
    const { data } = await axios.post<import('./types').TokenResponse>(
      `${getBaseURL()}/api/v1/auth/refresh`,
      { refreshToken: refresh },
      { headers: { 'Content-Type': 'application/json' } }
    );
    persistTokens(data.accessToken, data.refreshToken);
    if (data.me) {
      window.dispatchEvent(new CustomEvent<MeResponse>('afya-auth-profile', { detail: data.me }));
    }
    return data.accessToken;
  } catch {
    clearTokens();
    return null;
  }
}

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
    if (
      error.response?.status === 401 &&
      original &&
      !original._retry &&
      !original.url?.includes('/auth/login') &&
      !original.url?.includes('/auth/refresh')
    ) {
      original._retry = true;
      if (!refreshPromise) {
        refreshPromise = refreshAccessToken().finally(() => {
          refreshPromise = null;
        });
      }
      const newAccess = await refreshPromise;
      if (newAccess) {
        original.headers.Authorization = `Bearer ${newAccess}`;
        return api(original);
      }
    }
    return Promise.reject(error);
  }
);
