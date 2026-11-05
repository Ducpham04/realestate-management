import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Ban, CheckCircle2, Pencil, Plus, Trash2, UserX } from 'lucide-react';
import { appointmentApi } from '@/api/endpoints';
import type { Appointment, AppointmentFilters, AppointmentStatus } from '@/api/types';
import { useAuth } from '@/auth/AuthContext';
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  ErrorState,
  Field,
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
import { formatDateTime } from '@/lib/format';
import { appointmentStatusLabels, appointmentStatusTones, options } from '@/lib/labels';
import { AppointmentFormModal } from './AppointmentFormModal';

const startOfDay = (d: string) => (d ? new Date(`${d}T00:00:00`).toISOString() : undefined);
const endOfDay = (d: string) => (d ? new Date(`${d}T23:59:59.999`).toISOString() : undefined);

export default function AppointmentsPage() {
  const { isAdmin } = useAuth();
  const agentOptions = useAgentOptions();
  const queryClient = useQueryClient();
  const [filters, setFilters] = useState<AppointmentFilters>({ page: 0, size: 10, sort: 'scheduledAt,desc' });
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [editing, setEditing] = useState<Appointment | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [deleting, setDeleting] = useState<Appointment | null>(null);

  const params = { ...filters, from: startOfDay(fromDate), to: endOfDay(toDate) };
  const query = useQuery({
    queryKey: ['appointments', params],
    queryFn: () => appointmentApi.search(params),
    placeholderData: keepPreviousData,
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['appointments'] });
    queryClient.invalidateQueries({ queryKey: ['dashboard'] });
  };

  const statusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: AppointmentStatus }) => appointmentApi.changeStatus(id, status),
    onSuccess: (a) => {
      toast.success(`Đã chuyển sang "${appointmentStatusLabels[a.status]}"`);
      invalidate();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => appointmentApi.remove(id),
    onSuccess: () => {
      toast.success('Đã xóa lịch hẹn');
      invalidate();
      setDeleting(null);
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const update = (patch: Partial<AppointmentFilters>) => setFilters((f) => ({ ...f, page: 0, ...patch }));
  const isPast = (a: Appointment) => Date.parse(a.scheduledAt) <= Date.now();

  return (
    <>
      <PageHeader
        title="Lịch hẹn xem nhà"
        description="Lên lịch, đổi lịch và cập nhật kết quả các buổi xem nhà"
        actions={
          <Button
            onClick={() => {
              setEditing(null);
              setFormOpen(true);
            }}
          >
            <Plus className="h-4 w-4" /> Đặt lịch
          </Button>
        }
      />

      <Card>
        <div className="grid gap-3 border-b border-slate-200 p-4 sm:grid-cols-2 lg:grid-cols-4">
          <Field label="Trạng thái" htmlFor="filter-status">
            <Select
              id="filter-status"
              placeholder="Tất cả"
              options={options(appointmentStatusLabels)}
              value={filters.status ?? ''}
              onChange={(e) => update({ status: (e.target.value || undefined) as AppointmentStatus | undefined })}
            />
          </Field>
          <Field label="Từ ngày" htmlFor="filter-from">
            <Input
              id="filter-from"
              type="date"
              value={fromDate}
              onChange={(e) => {
                setFromDate(e.target.value);
                update({});
              }}
            />
          </Field>
          <Field label="Đến ngày" htmlFor="filter-to">
            <Input
              id="filter-to"
              type="date"
              value={toDate}
              onChange={(e) => {
                setToDate(e.target.value);
                update({});
              }}
            />
          </Field>
          {isAdmin && (
            <Field label="Môi giới" htmlFor="filter-agent">
              <Select
                id="filter-agent"
                placeholder="Tất cả"
                options={agentOptions}
                value={filters.agentId ?? ''}
                onChange={(e) => update({ agentId: e.target.value ? Number(e.target.value) : undefined })}
              />
            </Field>
          )}
        </div>

        {query.isLoading ? (
          <Spinner />
        ) : query.isError ? (
          <ErrorState message={getErrorMessage(query.error)} onRetry={() => query.refetch()} />
        ) : !query.data?.content.length ? (
          <EmptyState title="Không có lịch hẹn" />
        ) : (
          <>
            <Table>
              <thead className="bg-slate-50">
                <tr>
                  <SortableTh field="scheduledAt" sort={filters.sort!} onSort={(sort) => update({ sort })}>
                    Thời gian
                  </SortableTh>
                  <Th>Bất động sản</Th>
                  <Th>Khách hàng</Th>
                  <Th>Môi giới</Th>
                  <SortableTh field="status" sort={filters.sort!} onSort={(sort) => update({ sort })}>
                    Trạng thái
                  </SortableTh>
                  <Th>Ghi chú</Th>
                  <Th className="text-right">Thao tác</Th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 bg-white">
                {query.data.content.map((a) => (
                  <tr key={a.id} className="hover:bg-slate-50">
                    <Td className="font-medium text-slate-900">{formatDateTime(a.scheduledAt)}</Td>
                    <Td className="max-w-[16rem] truncate">{a.property.title}</Td>
                    <Td>
                      <div>{a.customer.fullName}</div>
                      <div className="text-xs text-slate-500">{a.customer.phone}</div>
                    </Td>
                    <Td>{a.agent.fullName}</Td>
                    <Td>
                      <Badge tone={appointmentStatusTones[a.status]}>{appointmentStatusLabels[a.status]}</Badge>
                    </Td>
                    <Td className="max-w-[12rem] truncate text-slate-500">{a.note || '—'}</Td>
                    <Td className="text-right">
                      {a.status === 'SCHEDULED' && (
                        <>
                          {isPast(a) && (
                            <>
                              <Button
                                size="sm"
                                variant="ghost"
                                title="Hoàn thành"
                                aria-label="Hoàn thành"
                                className="text-green-600"
                                onClick={() => statusMutation.mutate({ id: a.id, status: 'COMPLETED' })}
                              >
                                <CheckCircle2 className="h-4 w-4" />
                              </Button>
                              <Button
                                size="sm"
                                variant="ghost"
                                title="Khách vắng mặt"
                                aria-label="Khách vắng mặt"
                                onClick={() => statusMutation.mutate({ id: a.id, status: 'NO_SHOW' })}
                              >
                                <UserX className="h-4 w-4" />
                              </Button>
                            </>
                          )}
                          <Button
                            size="sm"
                            variant="ghost"
                            title="Đổi lịch"
                            aria-label="Đổi lịch"
                            onClick={() => {
                              setEditing(a);
                              setFormOpen(true);
                            }}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            title="Hủy lịch"
                            aria-label="Hủy lịch"
                            className="text-amber-600"
                            onClick={() => statusMutation.mutate({ id: a.id, status: 'CANCELLED' })}
                          >
                            <Ban className="h-4 w-4" />
                          </Button>
                        </>
                      )}
                      <Button
                        size="sm"
                        variant="ghost"
                        title="Xóa"
                        aria-label="Xóa"
                        className="text-red-600"
                        onClick={() => setDeleting(a)}
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

      <AppointmentFormModal open={formOpen} appointment={editing} onClose={() => setFormOpen(false)} />
      <ConfirmDialog
        open={!!deleting}
        title="Xóa lịch hẹn"
        confirmLabel="Xóa lịch hẹn"
        message="Bạn chắc chắn muốn xóa lịch hẹn này?"
        loading={deleteMutation.isPending}
        onConfirm={() => deleting && deleteMutation.mutate(deleting.id)}
        onClose={() => setDeleting(null)}
      />
    </>
  );
}
