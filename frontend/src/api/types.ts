export type Role = 'ADMIN' | 'AGENT';
export type PropertyType = 'APARTMENT' | 'HOUSE' | 'VILLA' | 'TOWNHOUSE' | 'LAND' | 'OFFICE' | 'SHOPHOUSE';
export type ListingType = 'SALE' | 'RENT';
export type PropertyStatus = 'AVAILABLE' | 'RESERVED' | 'SOLD' | 'RENTED' | 'INACTIVE';
export type CustomerType = 'BUYER' | 'TENANT' | 'SELLER' | 'LANDLORD';
export type AppointmentStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';
export type DealStatus = 'NEGOTIATING' | 'DEPOSITED' | 'CLOSED' | 'CANCELLED';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  errorCode?: string;
  errors?: { field: string; message: string }[];
  timestamp: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface PageParams {
  page?: number;
  size?: number;
  sort?: string;
}

export interface User {
  id: number;
  fullName: string;
  email: string;
  phone?: string;
  role: Role;
  enabled: boolean;
  createdAt: string;
}

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface Address {
  street: string;
  ward?: string;
  district: string;
  city: string;
}

export interface Property {
  id: number;
  title: string;
  description?: string;
  type: PropertyType;
  listingType: ListingType;
  status: PropertyStatus;
  price: number;
  area: number;
  bedrooms?: number;
  bathrooms?: number;
  address: Address;
  images: string[];
  agent: UserSummary;
  createdAt: string;
  updatedAt: string;
}

export interface PropertySummary {
  id: number;
  title: string;
  status: PropertyStatus;
  price: number;
}

export interface PropertyInput {
  title: string;
  description?: string;
  type: PropertyType;
  listingType: ListingType;
  status?: PropertyStatus;
  price: number;
  area: number;
  bedrooms?: number | null;
  bathrooms?: number | null;
  address: Address;
  images: string[];
  agentId?: number | null;
}

export interface PropertyFilters extends PageParams {
  keyword?: string;
  type?: PropertyType;
  listingType?: ListingType;
  status?: PropertyStatus;
  city?: string;
  minPrice?: number;
  maxPrice?: number;
  minArea?: number;
  maxArea?: number;
  minBedrooms?: number;
  agentId?: number;
}

export interface Customer {
  id: number;
  fullName: string;
  email?: string;
  phone: string;
  type: CustomerType;
  budgetMin?: number;
  budgetMax?: number;
  preferredLocation?: string;
  notes?: string;
  agent: UserSummary;
  createdAt: string;
  updatedAt: string;
}

export interface CustomerSummary {
  id: number;
  fullName: string;
  phone: string;
}

export interface CustomerInput {
  fullName: string;
  email?: string | null;
  phone: string;
  type: CustomerType;
  budgetMin?: number | null;
  budgetMax?: number | null;
  preferredLocation?: string | null;
  notes?: string | null;
  agentId?: number | null;
}

export interface CustomerFilters extends PageParams {
  keyword?: string;
  type?: CustomerType;
  agentId?: number;
}

export interface Appointment {
  id: number;
  property: PropertySummary;
  customer: CustomerSummary;
  agent: UserSummary;
  scheduledAt: string;
  endAt: string;
  status: AppointmentStatus;
  note?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AppointmentInput {
  propertyId: number;
  customerId: number;
  scheduledAt: string;
  durationMinutes: number;
  note?: string | null;
  agentId?: number | null;
}

export interface AppointmentFilters extends PageParams {
  status?: AppointmentStatus;
  propertyId?: number;
  customerId?: number;
  agentId?: number;
  from?: string;
  to?: string;
}

export interface Deal {
  id: number;
  property: PropertySummary;
  customer: CustomerSummary;
  agent: UserSummary;
  amount: number;
  commission: number;
  status: DealStatus;
  closedAt?: string;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DealInput {
  propertyId: number;
  customerId: number;
  amount: number;
  commission: number;
  notes?: string | null;
  agentId?: number | null;
}

export interface DealFilters extends PageParams {
  status?: DealStatus;
  agentId?: number;
  minAmount?: number;
  maxAmount?: number;
}

export interface DashboardSummary {
  totalProperties: number;
  availableProperties: number;
  totalCustomers: number;
  upcomingAppointments: number;
  activeDeals: number;
  closedDealsThisMonth: number;
  revenueThisMonth: number;
  commissionThisMonth: number;
}

export interface StatusCount {
  status: PropertyStatus;
  count: number;
}

export interface MonthlyRevenue {
  month: string;
  deals: number;
  revenue: number;
  commission: number;
}

export interface UserInput {
  fullName: string;
  email: string;
  password: string;
  phone?: string | null;
  role: Role;
}

export interface UserUpdateInput {
  fullName: string;
  phone?: string | null;
  role: Role;
  enabled: boolean;
}
