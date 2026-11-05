import type { User } from './types';

const KEY = 're.auth';

export interface StoredSession {
  accessToken: string;
  refreshToken: string;
  user: User;
}

type Listener = (session: StoredSession | null) => void;
const listeners = new Set<Listener>();

/**
 * Single source of truth for the session. Persisted in localStorage so a reload keeps the user
 * logged in, and observable so the React tree reacts when the HTTP layer refreshes or clears it.
 */
export const tokenStorage = {
  get(): StoredSession | null {
    try {
      const raw = localStorage.getItem(KEY);
      return raw ? (JSON.parse(raw) as StoredSession) : null;
    } catch {
      return null;
    }
  },
  set(session: StoredSession): void {
    localStorage.setItem(KEY, JSON.stringify(session));
    listeners.forEach((l) => l(session));
  },
  clear(): void {
    localStorage.removeItem(KEY);
    listeners.forEach((l) => l(null));
  },
  subscribe(listener: Listener): () => void {
    listeners.add(listener);
    return () => listeners.delete(listener);
  },
};
