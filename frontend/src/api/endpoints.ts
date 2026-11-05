import { api, cleanParams, unwrap } from './client';
import type {
  Appointment,
  AppointmentFilters,
  AppointmentInput,
  AppointmentStatus,
  AuthResponse,
  Customer,
  CustomerFilters,
  CustomerInput,
  DashboardSummary,
  Deal,
  DealFilters,
  DealInput,
  DealStatus,
  MonthlyRevenue,
  Page,
  PageParams,
  Property,
  PropertyFilters,
  PropertyInput,
  PropertyStatus,
  Role,
  StatusCount,
  User,
  UserInput,
  UserUpdateInput,
} from './types';

export const authApi = {
  login: (email: string, password: string) =>
    unwrap<AuthResponse>(api.post('/auth/login', { email, password })),
  logout: (refreshToken: string) => unwrap<void>(api.post('/auth/logout', { refreshToken })),
  me: () => unwrap<User>(api.get('/auth/me')),
};

export const propertyApi = {
  search: (filters: PropertyFilters) =>
    unwrap<Page<Property>>(api.get('/properties', { params: cleanParams(filters) })),
  get: (id: number) => unwrap<Property>(api.get(`/properties/${id}`)),
  create: (input: PropertyInput) => unwrap<Property>(api.post('/properties', input)),
  update: (id: number, input: PropertyInput) => unwrap<Property>(api.put(`/properties/${id}`, input)),
  updateStatus: (id: number, status: PropertyStatus) =>
    unwrap<Property>(api.patch(`/properties/${id}/status`, { status })),
  remove: (id: number) => unwrap<void>(api.delete(`/properties/${id}`)),
};

export const customerApi = {
  search: (filters: CustomerFilters) =>
    unwrap<Page<Customer>>(api.get('/customers', { params: cleanParams(filters) })),
  create: (input: CustomerInput) => unwrap<Customer>(api.post('/customers', input)),
  update: (id: number, input: CustomerInput) => unwrap<Customer>(api.put(`/customers/${id}`, input)),
  remove: (id: number) => unwrap<void>(api.delete(`/customers/${id}`)),
};

export const appointmentApi = {
  search: (filters: AppointmentFilters) =>
    unwrap<Page<Appointment>>(api.get('/appointments', { params: cleanParams(filters) })),
  create: (input: AppointmentInput) => unwrap<Appointment>(api.post('/appointments', input)),
  update: (id: number, input: AppointmentInput) =>
    unwrap<Appointment>(api.put(`/appointments/${id}`, input)),
  changeStatus: (id: number, status: AppointmentStatus, note?: string) =>
    unwrap<Appointment>(api.patch(`/appointments/${id}/status`, { status, note })),
  remove: (id: number) => unwrap<void>(api.delete(`/appointments/${id}`)),
};

export const dealApi = {
  search: (filters: DealFilters) => unwrap<Page<Deal>>(api.get('/deals', { params: cleanParams(filters) })),
  create: (input: DealInput) => unwrap<Deal>(api.post('/deals', input)),
  update: (id: number, input: DealInput) => unwrap<Deal>(api.put(`/deals/${id}`, input)),
  changeStatus: (id: number, status: DealStatus) => unwrap<Deal>(api.patch(`/deals/${id}/status`, { status })),
  remove: (id: number) => unwrap<void>(api.delete(`/deals/${id}`)),
};

export const dashboardApi = {
  summary: () => unwrap<DashboardSummary>(api.get('/dashboard/summary')),
  propertiesByStatus: () => unwrap<StatusCount[]>(api.get('/dashboard/properties-by-status')),
  revenueByMonth: (months = 12) =>
    unwrap<MonthlyRevenue[]>(api.get('/dashboard/revenue-by-month', { params: { months } })),
};

export const userApi = {
  search: (params: PageParams & { keyword?: string; role?: Role }) =>
    unwrap<Page<User>>(api.get('/users', { params: cleanParams(params) })),
  agents: () => unwrap<Page<User>>(api.get('/users/agents', { params: { size: 100 } })),
  create: (input: UserInput) => unwrap<User>(api.post('/users', input)),
  update: (id: number, input: UserUpdateInput) => unwrap<User>(api.put(`/users/${id}`, input)),
  changePassword: (currentPassword: string, newPassword: string) =>
    unwrap<void>(api.put('/users/me/password', { currentPassword, newPassword })),
};
