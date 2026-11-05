const vnd = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
const number = new Intl.NumberFormat('vi-VN');

export const formatCurrency = (value?: number | null) => (value == null ? '—' : vnd.format(value));

/** Compact Vietnamese money notation: 5.2 tỷ, 850 triệu. */
export function formatCompactVnd(value?: number | null): string {
  if (value == null) return '—';
  const abs = Math.abs(value);
  if (abs >= 1e9) return `${trim(value / 1e9)} tỷ`;
  if (abs >= 1e6) return `${trim(value / 1e6)} triệu`;
  if (abs >= 1e3) return `${trim(value / 1e3)} nghìn`;
  return number.format(value);
}

const trim = (n: number) => number.format(Math.round(n * 100) / 100);

export const formatNumber = (value?: number | null) => (value == null ? '—' : number.format(value));

export const formatArea = (value?: number | null) => (value == null ? '—' : `${number.format(value)} m²`);

export function formatDateTime(iso?: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function formatDate(iso?: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('vi-VN');
}

/** "2026-03" -> "T3/26" */
export function formatMonth(month: string): string {
  const [y, m] = month.split('-');
  return `T${Number(m)}/${y.slice(2)}`;
}

/** Converts an ISO instant to the value expected by <input type="datetime-local"> in local time. */
export function toLocalInputValue(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
