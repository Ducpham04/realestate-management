import type {
  AppointmentStatus,
  CustomerType,
  DealStatus,
  ListingType,
  PropertyStatus,
  PropertyType,
  Role,
} from '@/api/types';

export type Tone = 'green' | 'amber' | 'blue' | 'slate' | 'red' | 'violet';

export const propertyTypeLabels: Record<PropertyType, string> = {
  APARTMENT: 'Căn hộ',
  HOUSE: 'Nhà riêng',
  VILLA: 'Biệt thự',
  TOWNHOUSE: 'Nhà liền kề',
  LAND: 'Đất nền',
  OFFICE: 'Văn phòng',
  SHOPHOUSE: 'Shophouse',
};

export const listingTypeLabels: Record<ListingType, string> = { SALE: 'Bán', RENT: 'Cho thuê' };

export const propertyStatusLabels: Record<PropertyStatus, string> = {
  AVAILABLE: 'Đang mở bán',
  RESERVED: 'Đang giữ chỗ',
  SOLD: 'Đã bán',
  RENTED: 'Đã cho thuê',
  INACTIVE: 'Ngừng giao dịch',
};

export const propertyStatusTones: Record<PropertyStatus, Tone> = {
  AVAILABLE: 'green',
  RESERVED: 'amber',
  SOLD: 'blue',
  RENTED: 'violet',
  INACTIVE: 'slate',
};

export const propertyStatusColors: Record<PropertyStatus, string> = {
  AVAILABLE: '#16a34a',
  RESERVED: '#f59e0b',
  SOLD: '#2563eb',
  RENTED: '#7c3aed',
  INACTIVE: '#94a3b8',
};

export const customerTypeLabels: Record<CustomerType, string> = {
  BUYER: 'Người mua',
  TENANT: 'Người thuê',
  SELLER: 'Người bán',
  LANDLORD: 'Chủ cho thuê',
};

export const appointmentStatusLabels: Record<AppointmentStatus, string> = {
  SCHEDULED: 'Đã lên lịch',
  COMPLETED: 'Đã hoàn thành',
  CANCELLED: 'Đã hủy',
  NO_SHOW: 'Khách vắng mặt',
};

export const appointmentStatusTones: Record<AppointmentStatus, Tone> = {
  SCHEDULED: 'blue',
  COMPLETED: 'green',
  CANCELLED: 'slate',
  NO_SHOW: 'red',
};

export const dealStatusLabels: Record<DealStatus, string> = {
  NEGOTIATING: 'Đang đàm phán',
  DEPOSITED: 'Đã đặt cọc',
  CLOSED: 'Đã chốt',
  CANCELLED: 'Đã hủy',
};

export const dealStatusTones: Record<DealStatus, Tone> = {
  NEGOTIATING: 'amber',
  DEPOSITED: 'violet',
  CLOSED: 'green',
  CANCELLED: 'slate',
};

/** Allowed next states for a deal, mirroring the backend state machine. */
export const dealTransitions: Record<DealStatus, DealStatus[]> = {
  NEGOTIATING: ['DEPOSITED', 'CLOSED', 'CANCELLED'],
  DEPOSITED: ['CLOSED', 'CANCELLED'],
  CLOSED: [],
  CANCELLED: [],
};

export const roleLabels: Record<Role, string> = { ADMIN: 'Quản trị viên', AGENT: 'Môi giới' };

export const options = <T extends string>(labels: Record<T, string>) =>
  (Object.keys(labels) as T[]).map((value) => ({ value, label: labels[value] }));
