import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import { tokenStorage } from './tokenStorage';
import type { ApiResponse, AuthResponse } from './types';

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';

const AUTH_PATHS = ['/auth/login', '/auth/refresh', '/auth/logout'];

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

/**
 * Creates the API client. Every request carries the access token; a 401 triggers exactly one
 * refresh call even when many requests fail concurrently, after which the queued requests are
 * replayed with the new token. If refreshing fails, the session is cleared (which logs the user out).
 */
export function createApiClient(baseURL: string = API_BASE_URL): AxiosInstance {
  const instance = axios.create({ baseURL, headers: { 'Content-Type': 'application/json' } });
  let refreshPromise: Promise<string> | null = null;

  const refreshAccessToken = async (): Promise<string> => {
    const session = tokenStorage.get();
    if (!session?.refreshToken) throw new Error('No refresh token');
    // Use a bare request so this call never goes through the 401 interceptor itself.
    const { data } = await axios.post<ApiResponse<AuthResponse>>(
      `${baseURL}/auth/refresh`,
      { refreshToken: session.refreshToken },
      { adapter: instance.defaults.adapter },
    );
    const { accessToken, refreshToken, user } = data.data;
    tokenStorage.set({ accessToken, refreshToken, user });
    return accessToken;
  };

  instance.interceptors.request.use((config) => {
    const token = tokenStorage.get()?.accessToken;
    if (token && !config.headers.Authorization) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  });

  instance.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
      const original = error.config as RetriableConfig | undefined;
      const isAuthCall = AUTH_PATHS.some((p) => original?.url?.includes(p));
      if (error.response?.status !== 401 || !original || original._retry || isAuthCall) {
        return Promise.reject(error);
      }
      original._retry = true;
      try {
        refreshPromise ??= refreshAccessToken().finally(() => {
          refreshPromise = null;
        });
        const newToken = await refreshPromise;
        original.headers.Authorization = `Bearer ${newToken}`;
        return instance(original);
      } catch (refreshError) {
        tokenStorage.clear();
        return Promise.reject(refreshError);
      }
    },
  );

  return instance;
}

export const api = createApiClient();

/** Unwraps the `data` field of the unified response envelope. */
export async function unwrap<T>(promise: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  const response = await promise;
  return response.data.data;
}

/** Drops empty values so they are not sent as query parameters. */
export function cleanParams<T extends object>(params: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== ''),
  ) as Partial<T>;
}
