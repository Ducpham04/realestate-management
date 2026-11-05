import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { authApi } from '@/api/endpoints';
import { tokenStorage, type StoredSession } from '@/api/tokenStorage';
import type { Role, User } from '@/api/types';

interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (email: string, password: string) => Promise<User>;
  logout: () => Promise<void>;
  hasRole: (...roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [session, setSession] = useState<StoredSession | null>(() => tokenStorage.get());

  // Keeps React in sync when the HTTP layer refreshes tokens or clears an expired session.
  useEffect(() => tokenStorage.subscribe(setSession), []);

  // Drop cached data whenever the user logs out or the session expires.
  useEffect(() => {
    if (!session) queryClient.clear();
  }, [session, queryClient]);

  const login = useCallback(async (email: string, password: string) => {
    const res = await authApi.login(email, password);
    tokenStorage.set({ accessToken: res.accessToken, refreshToken: res.refreshToken, user: res.user });
    return res.user;
  }, []);

  const logout = useCallback(async () => {
    const refreshToken = tokenStorage.get()?.refreshToken;
    tokenStorage.clear();
    if (refreshToken) {
      await authApi.logout(refreshToken).catch(() => undefined);
    }
  }, []);

  const value = useMemo<AuthContextValue>(() => {
    const user = session?.user ?? null;
    return {
      user,
      isAuthenticated: !!user,
      isAdmin: user?.role === 'ADMIN',
      login,
      logout,
      hasRole: (...roles) => !!user && roles.includes(user.role),
    };
  }, [session, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
