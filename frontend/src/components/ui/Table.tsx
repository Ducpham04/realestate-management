import type { ReactNode, TdHTMLAttributes, ThHTMLAttributes } from 'react';
import { ArrowDown, ArrowUp, ArrowUpDown } from 'lucide-react';
import { cn } from '@/lib/cn';

export function Table({ children }: { children: ReactNode }) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-200 text-sm">{children}</table>
    </div>
  );
}

export function Th({ className, ...props }: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      className={cn('whitespace-nowrap px-4 py-3 text-left text-xs font-semibold uppercase tracking-wide text-slate-500', className)}
      {...props}
    />
  );
}

export function Td({ className, ...props }: TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={cn('whitespace-nowrap px-4 py-3 text-slate-700', className)} {...props} />;
}

interface SortableThProps {
  field: string;
  sort: string;
  onSort: (sort: string) => void;
  children: ReactNode;
  className?: string;
}

/** Header cell that toggles `field,asc` / `field,desc` in the Spring Data sort format. */
export function SortableTh({ field, sort, onSort, children, className }: SortableThProps) {
  const [current, dir] = sort.split(',');
  const active = current === field;
  const Icon = !active ? ArrowUpDown : dir === 'asc' ? ArrowUp : ArrowDown;
  return (
    <Th className={className}>
      <button
        type="button"
        className={cn('inline-flex items-center gap-1 uppercase', active && 'text-slate-900')}
        onClick={() => onSort(`${field},${active && dir === 'desc' ? 'asc' : 'desc'}`)}
      >
        {children}
        <Icon className="h-3.5 w-3.5" />
      </button>
    </Th>
  );
}
