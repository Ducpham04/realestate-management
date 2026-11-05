import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { appointmentApi } from '@/api/endpoints';
import type { Appointment, AppointmentInput } from '@/api/types';
import { useAuth } from '@/auth/AuthContext';
import { Button, Field, Input, Modal, Select, Textarea } from '@/components/ui';
import { useAgentOptions, useCustomerOptions, usePropertyOptions } from '@/features/shared/useOptions';
import { getErrorMessage } from '@/lib/errors';
import { toLocalInputValue } from '@/lib/format';

const schema = z.object({
  propertyId: z.string().min(1, 'Chọn bất động sản'),
  customerId: z.string().min(1, 'Chọn khách hàng'),
  scheduledAt: z
    .string()
    .min(1, 'Chọn thời gian')
    .refine((v) => new Date(v).getTime() > Date.now(), 'Thời gian phải ở tương lai'),
  durationMinutes: z.coerce.number().int().min(15, 'Tối thiểu 15 phút').max(480, 'Tối đa 8 giờ'),
  note: z.string().max(1000).optional(),
  agentId: z.string().optional(),
});
type FormValues = z.infer<typeof schema>;

const toFormValues = (a?: Appointment | null): FormValues => ({
  propertyId: a ? String(a.property.id) : '',
  customerId: a ? String(a.customer.id) : '',
  scheduledAt: a ? toLocalInputValue(a.scheduledAt) : '',
  durationMinutes: a ? Math.round((Date.parse(a.endAt) - Date.parse(a.scheduledAt)) / 60000) : 60,
  note: a?.note ?? '',
  agentId: a ? String(a.agent.id) : '',
});

export function AppointmentFormModal({
  open,
  appointment,
  onClose,
}: {
  open: boolean;
  appointment: Appointment | null;
  onClose: () => void;
}) {
  const { isAdmin } = useAuth();
  const queryClient = useQueryClient();
  const agentOptions = useAgentOptions();
  const customerOptions = useCustomerOptions();
  const propertyOptions = usePropertyOptions().filter(
    (p) => p.status === 'AVAILABLE' || p.status === 'RESERVED' || p.value === appointment?.property.id,
  );
  const isEdit = !!appointment;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: toFormValues(appointment) });

  useEffect(() => {
    if (open) reset(toFormValues(appointment));
  }, [open, appointment, reset]);

  const mutation = useMutation({
    mutationFn: (input: AppointmentInput) =>
      isEdit ? appointmentApi.update(appointment!.id, input) : appointmentApi.create(input),
    onSuccess: () => {
      toast.success(isEdit ? 'Đã cập nhật lịch hẹn' : 'Đã đặt lịch hẹn');
      queryClient.invalidateQueries({ queryKey: ['appointments'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      onClose();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const onSubmit = (v: FormValues) =>
    mutation.mutate({
      propertyId: Number(v.propertyId),
      customerId: Number(v.customerId),
      scheduledAt: new Date(v.scheduledAt).toISOString(),
      durationMinutes: v.durationMinutes,
      note: v.note || null,
      agentId: isAdmin && v.agentId ? Number(v.agentId) : null,
    });

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="lg"
      title={isEdit ? 'Đổi lịch hẹn' : 'Đặt lịch xem nhà'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" form="appointment-form" loading={mutation.isPending}>
            {isEdit ? 'Lưu thay đổi' : 'Đặt lịch'}
          </Button>
        </>
      }
    >
      <form id="appointment-form" onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-2" noValidate>
        <Field label="Bất động sản" htmlFor="propertyId" error={errors.propertyId?.message} required className="sm:col-span-2">
          <Select
            id="propertyId"
            placeholder="— Chọn bất động sản —"
            options={propertyOptions}
            invalid={!!errors.propertyId}
            {...register('propertyId')}
          />
        </Field>
        <Field label="Khách hàng" htmlFor="customerId" error={errors.customerId?.message} required className="sm:col-span-2">
          <Select
            id="customerId"
            placeholder="— Chọn khách hàng —"
            options={customerOptions}
            invalid={!!errors.customerId}
            {...register('customerId')}
          />
        </Field>
        <Field label="Thời gian" htmlFor="scheduledAt" error={errors.scheduledAt?.message} required>
          <Input id="scheduledAt" type="datetime-local" invalid={!!errors.scheduledAt} {...register('scheduledAt')} />
        </Field>
        <Field label="Thời lượng (phút)" htmlFor="durationMinutes" error={errors.durationMinutes?.message} required>
          <Input id="durationMinutes" type="number" min={15} step={15} {...register('durationMinutes')} />
        </Field>
        {isAdmin && (
          <Field
            label="Môi giới"
            htmlFor="agentId"
            hint="Để trống: giao cho môi giới phụ trách khách hàng"
            className="sm:col-span-2"
          >
            <Select id="agentId" placeholder="— Tự động —" options={agentOptions} {...register('agentId')} />
          </Field>
        )}
        <Field label="Ghi chú" htmlFor="note" className="sm:col-span-2">
          <Textarea id="note" rows={3} {...register('note')} />
        </Field>
      </form>
    </Modal>
  );
}
