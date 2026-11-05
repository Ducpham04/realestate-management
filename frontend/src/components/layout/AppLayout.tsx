import { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  Building2,
  CalendarDays,
  Handshake,
  LayoutDashboard,
  LogOut,
  Menu,
  UserCog,
  Users,
  X,
  type LucideIcon,
} from 'lucide-react';
import type { Role } from '@/api/types';
import { useAuth } from '@/auth/AuthContext';
import { roleLabels } from '@/lib/labels';
import { cn } from '@/lib/cn';

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  roles?: Role[];
}

const NAV: NavItem[] = [
  { to: '/', label: 'Tổng quan', icon: LayoutDashboard },
  { to: '/properties', label: 'Bất động sản', icon: Building2 },
  { to: '/customers', label: 'Khách hàng', icon: Users },
  { to: '/appointments', label: 'Lịch hẹn', icon: CalendarDays },
  { to: '/deals', label: 'Giao dịch', icon: Handshake },
  { to: '/users', label: 'Nhân viên', icon: UserCog, roles: ['ADMIN'] },
];

export function AppLayout() {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const items = NAV.filter((item) => !item.roles || hasRole(...item.roles));

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  const sidebar = (
    <nav className="flex h-full flex-col">
      <div className="flex h-16 items-center gap-2 px-6">
        <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-white">
          <Building2 className="h-5 w-5" />
        </div>
        <div>
          <p className="text-sm font-semibold leading-tight text-slate-900">RealEstate</p>
          <p className="text-xs text-slate-500">Manager</p>
        </div>
      </div>
      <ul className="flex-1 space-y-1 px-3 py-4">
        {items.map(({ to, label, icon: Icon }) => (
          <li key={to}>
            <NavLink
              to={to}
              end={to === '/'}
              onClick={() => setOpen(false)}
              className={({ isActive }) =>
                cn(
                  'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900',
                )
              }
            >
              <Icon className="h-5 w-5" />
              {label}
            </NavLink>
          </li>
        ))}
      </ul>
      <div className="border-t border-slate-200 p-4">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-slate-200 text-sm font-semibold text-slate-700">
            {user?.fullName.charAt(0)}
          </div>
          <div className="min-w-0 flex-1">
            <p className="truncate text-sm font-medium text-slate-900">{user?.fullName}</p>
            <p className="truncate text-xs text-slate-500">{user && roleLabels[user.role]}</p>
          </div>
          <button
            onClick={handleLogout}
            className="rounded-lg p-2 text-slate-500 hover:bg-slate-100 hover:text-slate-900"
            aria-label="Đăng xuất"
            title="Đăng xuất"
          >
            <LogOut className="h-4 w-4" />
          </button>
        </div>
      </div>
    </nav>
  );

  return (
    <div className="min-h-screen">
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-slate-200 bg-white lg:block">
        {sidebar}
      </aside>

      {open && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-slate-900/40" onClick={() => setOpen(false)} />
          <aside className="absolute inset-y-0 left-0 w-64 bg-white shadow-xl">
            <button className="absolute right-3 top-4 p-1" onClick={() => setOpen(false)} aria-label="Đóng menu">
              <X className="h-5 w-5" />
            </button>
            {sidebar}
          </aside>
        </div>
      )}

      <div className="lg:pl-64">
        <header className="sticky top-0 z-20 flex h-16 items-center border-b border-slate-200 bg-white/80 px-4 backdrop-blur lg:hidden">
          <button onClick={() => setOpen(true)} className="rounded-lg p-2 hover:bg-slate-100" aria-label="Mở menu">
            <Menu className="h-5 w-5" />
          </button>
          <span className="ml-2 font-semibold">RealEstate Manager</span>
        </header>
        <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
