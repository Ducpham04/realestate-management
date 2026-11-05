import { AxiosError } from 'axios';
import type { ApiResponse } from '@/api/types';

/** Extracts a human-readable message from an API error. */
export function getErrorMessage(error: unknown, fallback = 'Đã có lỗi xảy ra, vui lòng thử lại'): string {
  if (error instanceof AxiosError) {
    const body = error.response?.data as Partial<ApiResponse<unknown>> | undefined;
    if (body?.errors?.length) {
      return body.errors.map((e) => `${e.field}: ${e.message}`).join('; ');
    }
    if (body?.message) return body.message;
    if (!error.response) return 'Không thể kết nối tới máy chủ';
  }
  if (error instanceof Error && error.message) return error.message;
  return fallback;
}

/** Field-level validation errors keyed by field name. */
export function getFieldErrors(error: unknown): Record<string, string> {
  if (error instanceof AxiosError) {
    const body = error.response?.data as Partial<ApiResponse<unknown>> | undefined;
    return Object.fromEntries((body?.errors ?? []).map((e) => [e.field, e.message]));
  }
  return {};
}
