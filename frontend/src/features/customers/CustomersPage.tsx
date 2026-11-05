import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Pencil, Plus, Search, Trash2 } from 'lucide-react';
import { customerApi } from '@/api/endpoints';
import type { Customer, CustomerFilters, CustomerType } from '@/api/types';
import { useAuth } from '@/auth/AuthContext';
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  ErrorState,
  Input,
  PageHeader,
  Pagination,
  Select,
  SortableTh,
  Spinner,
  Table,
  Td,
  Th,
} from '@/components/ui';
import { useAgentOptions } from '@/features/shared/useOptions';
import { getErrorMessage } from '@/lib/errors';
import { formatCompactVnd, formatDate } from '@/lib/format';
import { useDebounce } from '@/lib/hooks';
import { customerTypeLabels, options } from '@/lib/labels';
import { CustomerFormModal } from './CustomerFormModal';

export default function CustomersPage() {
  const { isAdmin } = useAuth();
  const agentOptions = useAgentOptions();
  const queryClient = useQueryClient();
  const [keyword, setKeyword] = useState('');
  const [filters, setFilters] = useState<CustomerFilters>({ page: 0, size: 10, sort: 'createdAt,desc' });
  const [editing, setEditing] = useState<Customer | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [deleting, setDeleting] = useState<Customer | null>(null);
  const debouncedKeyword = useDebounce(keyword);

  const params = { ...filters, keyword: debouncedKeyword || undefined };
  const query = useQuery({
    queryKey: ['customers', params],
    queryFn: () => customerApi.search(params),
    placeholderData: keepPreviousData,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => customerApi.remove(id),
    onSuccess: () => {
      toast.success('Đã xóa khách hàng');
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      setDeleting(null);
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const update = (patch: Partial<CustomerFilters>) => setFilters((f) => ({ ...f, page: 0, ...patch }));
  const budget = (c: Customer) =>
    c.budgetMin == null && c.budgetMax == null
      ? '—'
      : `${formatCompactVnd(c.budgetMin ?? 0)} – ${c.budgetMax != null ? formatCompactVnd(c.budgetMax) : '∞'}`;

  return (
    <>
      <PageHeader
        title="Khách hàng"
        description={isAdmin ? 'Toàn bộ khách hàng của công ty' : 'Khách hàng bạn đang phụ trách'}
        actions={
          <Button
            onClick={() => {
              setEditing(null);
              setFormOpen(true);
            }}
          >
            <Plus className="h-4 w-4" /> Thêm khách hàng
          </Button>
        }
      />

      <Card>
        <div className="grid gap-3 border-b border-slate-200 p-4 sm:grid-cols-3">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <Input
              aria-label="Tìm kiếm"
              placeholder="Tên, email, số điện thoại..."
              className="pl-9"
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value);
                setFilters((f) => ({ ...f, page: 0 }));
              }}
            />
          </div>
          <Select
            aria-label="Nhu cầu"
            placeholder="Mọi nhu cầu"
            options={options(customerTypeLabels)}
            value={filters.type ?? ''}
            onChange={(e) => update({ type: (e.target.value || undefined) as CustomerType | undefined })}
          />
          {isAdmin && (
            <Select
              aria-label="Môi giới"
              placeholder="Mọi môi giới"
              options={agentOptions}
              value={filters.agentId ?? ''}
              onChange={(e) => update({ agentId: e.target.value ? Number(e.target.value) : undefined })}
            />
          )}
        </div>

        {query.isLoading ? (
          <Spinner />
        ) : query.isError ? (
          <ErrorState message={getErrorMessage(query.error)} onRetry={() => query.refetch()} />
        ) : !query.data?.content.length ? (
          debouncedKeyword || filters.type || filters.agentId ? (
            <EmptyState
              title="Không tìm thấy khách hàng phù hợp"
              description="Kiểm tra lại tên, email hoặc số điện thoại."
            />
          ) : (
            <EmptyState
              title="Chưa có khách hàng nào"
              description="Thêm khách hàng để đặt lịch xem nhà và tạo giao dịch."
              action={
                <Button
                  onClick={() => {
                    setEditing(null);
                    setFormOpen(true);
                  }}
                >
                  Thêm khách hàng
                </Button>
              }
            />
          )
        ) : (
          <>
            <Table>
              <thead className="bg-slate-50">
                <tr>
                  <SortableTh field="fullName" sort={filters.sort!} onSort={(sort) => update({ sort })}>
                    Khách hàng
                  </SortableTh>
                  <Th>Liên hệ</Th>
                  <SortableTh field="type" sort={filters.sort!} onSort={(sort) => update({ sort })}>
                    Nhu cầu
                  </SortableTh>
                  <Th>Ngân sách</Th>
                  <Th className="hidden 2xl:table-cell">Khu vực</Th>
                  {isAdmin && <Th>Môi giới</Th>}
                  <SortableTh field="createdAt" sort={filters.sort!} onSort={(sort) => update({ sort })}>
                    Ngày tạo
                  </SortableTh>
                  <Th className="text-right">Thao tác</Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {query.data.content.map((c) => (
                  <tr key={c.id} className="hover:bg-slate-50">
                    <Td className="font-medium text-slate-900">{c.fullName}</Td>
                    <Td>
                      <div>{c.phone}</div>
                      <div className="text-xs text-slate-500">{c.email}</div>
                    </Td>
                    <Td>
                      <Badge tone="slate">{customerTypeLabels[c.type]}</Badge>
                    </Td>
                    <Td>{budget(c)}</Td>
                    <Td className="hidden max-w-[12rem] truncate 2xl:table-cell">{c.preferredLocation || '—'}</Td>
                    {isAdmin && <Td>{c.agent.fullName}</Td>}
                    <Td>{formatDate(c.createdAt)}</Td>
                    <Td className="text-right">
                      <Button
                        size="sm"
                        variant="ghost"
                        aria-label="Sửa"
                        onClick={() => {
                          setEditing(c);
                          setFormOpen(true);
                        }}
                      >
                        <Pencil className="h-4 w-4" />
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        aria-label="Xóa"
                        className="text-red-600"
                        onClick={() => setDeleting(c)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </Td>
                  </tr>
                ))}
              </tbody>
            </Table>
            <Pagination
              page={query.data.page}
              size={query.data.size}
              totalPages={query.data.totalPages}
              totalElements={query.data.totalElements}
              onPageChange={(page) => setFilters((f) => ({ ...f, page }))}
              onSizeChange={(size) => update({ size })}
            />
          </>
        )}
      </Card>

      <CustomerFormModal open={formOpen} customer={editing} onClose={() => setFormOpen(false)} />
      <ConfirmDialog
        open={!!deleting}
        title="Xóa khách hàng"
        confirmLabel="Xóa khách hàng"
        message={`Xóa khách hàng "${deleting?.fullName}"? Khách hàng đã có lịch hẹn hoặc giao dịch sẽ không thể xóa.`}
        loading={deleteMutation.isPending}
        onConfirm={() => deleting && deleteMutation.mutate(deleting.id)}
        onClose={() => setDeleting(null)}
      />
    </>
  );
}
