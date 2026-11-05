import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Select } from './Form';

interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onPageChange: (page: number) => void;
  onSizeChange?: (size: number) => void;
}

export function Pagination({ page, totalPages, totalElements, size, onPageChange, onSizeChange }: PaginationProps) {
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  return (
    <div className="flex flex-col items-center justify-between gap-3 border-t border-slate-200 px-4 py-3 text-sm sm:flex-row">
      <p className="text-slate-500">
        Hiển thị <span className="font-medium text-slate-700">{from}</span>–
        <span className="font-medium text-slate-700">{to}</span> trên{' '}
        <span className="font-medium text-slate-700">{totalElements}</span>
      </p>
      <div className="flex items-center gap-2">
        {onSizeChange && (
          <Select
            aria-label="Số dòng mỗi trang"
            className="w-auto py-1"
            value={size}
            onChange={(e) => onSizeChange(Number(e.target.value))}
            options={[10, 20, 50].map((n) => ({ value: n, label: `${n} / trang` }))}
          />
        )}
        <button
          className="rounded-lg border border-slate-300 p-1.5 disabled:opacity-40"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 0}
          aria-label="Trang trước"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        <span className="min-w-[4rem] text-center text-slate-600">
          {totalPages === 0 ? 0 : page + 1} / {totalPages}
        </span>
        <button
          className="rounded-lg border border-slate-300 p-1.5 disabled:opacity-40"
          onClick={() => onPageChange(page + 1)}
          disabled={page + 1 >= totalPages}
          aria-label="Trang sau"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}
