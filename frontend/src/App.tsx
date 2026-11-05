import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth, RequireRole } from '@/auth/guards';
import { AppLayout } from '@/components/layout/AppLayout';
import { Spinner } from '@/components/ui';
import { LoginPage } from '@/pages/LoginPage';
import { ForbiddenPage, NotFoundPage } from '@/pages/StatusPages';

const DashboardPage = lazy(() => import('@/features/dashboard/DashboardPage'));
const PropertiesPage = lazy(() => import('@/features/properties/PropertiesPage'));
const CustomersPage = lazy(() => import('@/features/customers/CustomersPage'));
const AppointmentsPage = lazy(() => import('@/features/appointments/AppointmentsPage'));
const DealsPage = lazy(() => import('@/features/deals/DealsPage'));
const UsersPage = lazy(() => import('@/features/users/UsersPage'));

export function App() {
  return (
    <Suspense fallback={<Spinner />}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route element={<RequireAuth />}>
          <Route element={<AppLayout />}>
            <Route index element={<DashboardPage />} />
            <Route path="properties" element={<PropertiesPage />} />
            <Route path="customers" element={<CustomersPage />} />
            <Route path="appointments" element={<AppointmentsPage />} />
            <Route path="deals" element={<DealsPage />} />
            <Route element={<RequireRole roles={['ADMIN']} />}>
              <Route path="users" element={<UsersPage />} />
            </Route>
            <Route path="403" element={<ForbiddenPage />} />
            <Route path="404" element={<NotFoundPage />} />
            <Route path="*" element={<Navigate to="/404" replace />} />
          </Route>
        </Route>
      </Routes>
    </Suspense>
  );
}
