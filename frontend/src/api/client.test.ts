import { AxiosError, AxiosHeaders, type AxiosAdapter, type InternalAxiosRequestConfig } from 'axios';
import { describe, expect, it, vi } from 'vitest';
import { createApiClient } from './client';
import { tokenStorage } from './tokenStorage';
import type { User } from './types';

const user: User = {
  id: 2,
  fullName: 'Agent',
  email: 'agent@test.com',
  role: 'AGENT',
  enabled: true,
  createdAt: '2026-01-01T00:00:00Z',
};

function respond(config: InternalAxiosRequestConfig, status: number, data: unknown) {
  const response = { data, status, statusText: String(status), headers: {}, config };
  if (status >= 400) {
    return Promise.reject(new AxiosError('error', String(status), config, null, response));
  }
  return Promise.resolve(response);
}

/**
 * Fake backend: accepts only "fresh" access tokens and exchanges refresh token "r1" once.
 */
function fakeBackend(options: { refreshSucceeds: boolean }) {
  const refreshCalls = vi.fn();
  const adapter: AxiosAdapter = async (config) => {
    if (config.url?.endsWith('/auth/refresh')) {
      refreshCalls(JSON.parse(config.data as string));
      return options.refreshSucceeds
        ? respond(config, 200, {
            success: true,
            data: { accessToken: 'fresh', refreshToken: 'r2', tokenType: 'Bearer', expiresIn: 900, user },
          })
        : respond(config, 401, { success: false, errorCode: 'INVALID_TOKEN' });
    }
    const auth = AxiosHeaders.from(config.headers).get('Authorization');
    return auth === 'Bearer fresh'
      ? respond(config, 200, { success: true, data: config.url })
      : respond(config, 401, { success: false, errorCode: 'UNAUTHORIZED' });
  };
  return { adapter, refreshCalls };
}

describe('api client token refresh', () => {
  it('refreshes once for concurrent 401s and replays every request', async () => {
    tokenStorage.set({ accessToken: 'expired', refreshToken: 'r1', user });
    const { adapter, refreshCalls } = fakeBackend({ refreshSucceeds: true });
    const client = createApiClient('/api');
    client.defaults.adapter = adapter;

    const results = await Promise.all([client.get('/a'), client.get('/b'), client.get('/c')]);

    expect(results.map((r) => r.data.data)).toEqual(['/a', '/b', '/c']);
    expect(refreshCalls).toHaveBeenCalledTimes(1);
    expect(refreshCalls).toHaveBeenCalledWith({ refreshToken: 'r1' });
    expect(tokenStorage.get()).toMatchObject({ accessToken: 'fresh', refreshToken: 'r2' });
  });

  it('clears the session when the refresh token is rejected', async () => {
    tokenStorage.set({ accessToken: 'expired', refreshToken: 'r1', user });
    const { adapter } = fakeBackend({ refreshSucceeds: false });
    const client = createApiClient('/api');
    client.defaults.adapter = adapter;
    const listener = vi.fn();
    const unsubscribe = tokenStorage.subscribe(listener);

    await expect(client.get('/a')).rejects.toBeDefined();

    expect(tokenStorage.get()).toBeNull();
    expect(listener).toHaveBeenLastCalledWith(null);
    unsubscribe();
  });

  it('does not try to refresh when the login call itself fails', async () => {
    const { adapter, refreshCalls } = fakeBackend({ refreshSucceeds: true });
    const client = createApiClient('/api');
    client.defaults.adapter = adapter;

    await expect(client.post('/auth/login', {})).rejects.toMatchObject({ response: { status: 401 } });
    expect(refreshCalls).not.toHaveBeenCalled();
  });
});
