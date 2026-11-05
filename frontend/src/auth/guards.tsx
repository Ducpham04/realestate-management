import type { ReactNode } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import type { Role } from '@/api/types';
import { useAuth } from './AuthContext';

/** Redirects anonymous users to the login page, remembering where they wanted to go. */
export function RequireAuth() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();
  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}

/** Route-level role check. */
export function RequireRole({ roles }: { roles: Role[] }) {
  const { hasRole } = useAuth();
  return hasRole(...roles) ? <Outlet /> : <Navigate to="/403" replace />;
}

/** Renders children only for the given roles (UI-level permission). */
export function RoleGate({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { hasRole } = useAuth();
  return hasRole(...roles) ? <>{children}</> : null;
}
