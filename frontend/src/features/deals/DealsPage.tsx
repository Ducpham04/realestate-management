import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Pencil, Plus, Trash2 } from 'lucide-react';
import { dealApi } from '@/api/endpoints';
import type { Deal, DealFilters, DealStatus } from '@/api/types';
import { useAuth } from '@/auth/AuthContext';
import { RoleGate } from '@/auth/guards';
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  ErrorState,
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
import { formatCompactVnd, formatCurrency, formatDate } from '@/lib/format';
import { dealStatusLabels, dealStatusTones, dealTransitions, options } from '@/lib/labels';
import { DealFormModal } from './DealFormModal';

export default function DealsPage() {
  const { isAdmin } = useAuth();
  const agentOptions = useAgentOptions();
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<DealFilters>({ page: 0, size: 10, sort: 'createdAt,desc' });
  const [editing, setEditing] = useState<Deal | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [deleting, setDeleting] = useState<Deal | null>(null);
  const [confirmClose, setConfirmClose] = useState<Deal | null>(null);

  const query = useQuery({
    queryKey: ['deals', filters],
    queryFn: () => dealApi.search(filters),
    placeholderData: keepPreviousData,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['deals'] });
    queryClient.invalidateQueries({ queryKey: ['properties'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard'] });
  };

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: DealStatus }) => dealApi.changeStatus(id, status),
    onSuccess: (d) => {
      toast.success(`Giao dịch chuyển sang "${dealStatusLabels[d.status]}"`);
      invalidate();
      setConfirmClose(null);
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => dealApi.remove(id),
    onSuccess: () => {
      toast.success('Đã xóa giao dịch');
      invalidate();
      setDeleting(null);
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const update = (patch: Partial<DealFilters>) => setFilters((f) => ({ ...f, page: 0, ...patch }));

  const changeStatus = (deal: Deal, status: DealStatus) => {
    if (status === 'CLOSED') setConfirmClose(deal);
    else statusMutation.mutate({ id: deal.id, status });
  };

  return (
    <>
      <PageHeader
        title="Giao dịch"
        description="Theo dõi pipeline từ đàm phán, đặt cọc đến chốt giao dịch"
        actions={
          <Button
            onClick={() => {
              setEditing(null);
              setFormOpen(true);
            }}
          >
            <Plus className="h-4 w-4" /> Tạo giao dịch
          </Button>
        }
      />

      <Card>
        <div className="grid gap-3 border-b border-slate-200 p-4 sm:grid-cols-3">
          <Select
            aria-label="Trạng thái"
            placeholder="Mọi trạng thái"
            options={options(dealStatusLabels)}
            value={filters.status ?? ''}
            onChange={(e) => update({ status: (e.target.value || undefined) as DealStatus | undefined })}
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
          <EmptyState title="Chưa có giao dịch" />
        ) : (
          <>
            <Table>
              <thead className="bg-slate-50">
                <tr>
                  <Th>Bất động sản</Th>
                  <Th>Khách hàng</Th>
                  <Th>Môi giới</Th>
                  <SortableTh field="amount" sort={filters.sort!} onSort={(sort) => update({ sort })}>
                    Giá trị
                  </SortableTh>
                  <SortableTh field="commission" sort={filters.sort!} onSort={(sort) => update({ sort })}>
                    Hoa hồng
                  </SortableTh>
                  <Th>Trạng thái</Th>
                  <SortableTh field="createdAt" sort={filters.sort!} onSort={(sort) => update({ sort })}>
                    Ngày tạo
                  </SortableTh>
                  <SortableTh field="closedAt" sort={filters.sort!} onSort={(sort) => update({ sort })}>
                    Ngày chốt
                  </SortableTh>
                  <Th className="text-right">Thao tác</Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {query.data.content.map((d) => (
                  <tr key={d.id} className="hover:bg-slate-50">
                    <Td className="max-w-[16rem] truncate font-medium text-slate-900">{d.property.title}</Td>
                    <Td>
                      <div>{d.customer.fullName}</div>
                      <div className="text-xs text-slate-500">{d.customer.phone}</div>
                    </Td>
                    <Td>{d.agent.fullName}</Td>
                    <Td title={formatCurrency(d.amount)}>{formatCompactVnd(d.amount)}</Td>
                    <Td title={formatCurrency(d.commission)}>{formatCompactVnd(d.commission)}</Td>
                    <Td>
                      {dealTransitions[d.status].length ? (
                        <Select
                          aria-label="Đổi trạng thái"
                          className="w-auto py-1 text-xs"
                          value={d.status}
                          disabled={statusMutation.isPending}
                          onChange={(e) => changeStatus(d, e.target.value as DealStatus)}
                          options={[d.status, ...dealTransitions[d.status]].map((s) => ({
                            value: s,
                            label: dealStatusLabels[s],
                          }))}
                        />
                      ) : (
                        <Badge tone={dealStatusTones[d.status]}>{dealStatusLabels[d.status]}</Badge>
                      )}
                    </Td>
                    <Td>{formatDate(d.createdAt)}</Td>
                    <Td>{formatDate(d.closedAt)}</Td>
                    <Td className="text-right">
                      {dealTransitions[d.status].length > 0 && (
                        <Button
                          size="sm"
                          variant="ghost"
                          aria-label="Sửa"
                          onClick={() => {
                            setEditing(d);
                            setFormOpen(true);
                          }}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                      )}
                      <RoleGate roles={['ADMIN']}>
                        {d.status !== 'CLOSED' && (
                          <Button
                            size="sm"
                            variant="ghost"
                            aria-label="Xóa"
                            className="text-red-600"
                            onClick={() => setDeleting(d)}
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        )}
                      </RoleGate>
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

      <DealFormModal open={formOpen} deal={editing} onClose={() => setFormOpen(false)} />
      <ConfirmDialog
        open={!!confirmClose}
        title="Chốt giao dịch"
        confirmLabel="Chốt giao dịch"
        message={`Xác nhận chốt giao dịch "${confirmClose?.property.title}" với giá trị ${formatCurrency(
          confirmClose?.amount,
        )}? Bất động sản sẽ chuyển sang Đã bán / Đã cho thuê và không thể hoàn tác.`}
        loading={statusMutation.isPending}
        onConfirm={() => confirmClose && statusMutation.mutate({ id: confirmClose.id, status: 'CLOSED' })}
        onClose={() => setConfirmClose(null)}
      />
      <ConfirmDialog
        open={!!deleting}
        title="Xóa giao dịch"
        confirmLabel="Xóa giao dịch"
        message="Xóa giao dịch này? Bất động sản đang giữ chỗ sẽ được mở bán lại."
        loading={deleteMutation.isPending}
        onConfirm={() => deleting && deleteMutation.mutate(deleting.id)}
        onClose={() => setDeleting(null)}
      />
    </>
  );
}
