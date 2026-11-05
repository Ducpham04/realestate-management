import { render, screen } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { tokenStorage } from '@/api/tokenStorage';
import type { Role } from '@/api/types';
import { AuthProvider } from './AuthContext';
import { RequireAuth, RequireRole, RoleGate } from './guards';

function loginAs(role: Role) {
  tokenStorage.set({
    accessToken: 'a',
    refreshToken: 'r',
    user: { id: 1, fullName: 'Test', email: 't@test.com', role, enabled: true, createdAt: '' },
  });
}

function renderAt(path: string) {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<p>login page</p>} />
            <Route path="/403" element={<p>forbidden</p>} />
            <Route element={<RequireAuth />}>
              <Route
                path="/"
                element={
                  <>
                    <p>home</p>
                    <RoleGate roles={['ADMIN']}>
                      <button>delete</button>
                    </RoleGate>
                  </>
                }
              />
              <Route element={<RequireRole roles={['ADMIN']} />}>
                <Route path="/users" element={<p>users</p>} />
              </Route>
            </Route>
          </Routes>
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

describe('route guards', () => {
  it('redirects anonymous users to login', () => {
    renderAt('/');
    expect(screen.getByText('login page')).toBeInTheDocument();
  });

  it('hides admin-only UI and routes from agents', () => {
    loginAs('AGENT');
    renderAt('/');
    expect(screen.getByText('home')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'delete' })).not.toBeInTheDocument();
  });

  it('blocks agents from admin routes', () => {
    loginAs('AGENT');
    renderAt('/users');
    expect(screen.getByText('forbidden')).toBeInTheDocument();
  });

  it('shows admin-only UI to admins', () => {
    loginAs('ADMIN');
    renderAt('/');
    expect(screen.getByRole('button', { name: 'delete' })).toBeInTheDocument();
  });
});
