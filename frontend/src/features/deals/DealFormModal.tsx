import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { dealApi } from '@/api/endpoints';
import type { Deal, DealInput } from '@/api/types';
import { useAuth } from '@/auth/AuthContext';
import { Button, Field, Input, Modal, Select, Textarea } from '@/components/ui';
import { useAgentOptions, useCustomerOptions, usePropertyOptions } from '@/features/shared/useOptions';
import { getErrorMessage } from '@/lib/errors';

const schema = z
  .object({
    propertyId: z.string().min(1, 'Chọn bất động sản'),
    customerId: z.string().min(1, 'Chọn khách hàng'),
    amount: z.coerce.number({ invalid_type_error: 'Không hợp lệ' }).positive('Giá trị phải lớn hơn 0'),
    commission: z.coerce.number({ invalid_type_error: 'Không hợp lệ' }).min(0, 'Không được âm'),
    notes: z.string().max(5000).optional(),
    agentId: z.string().optional(),
  })
  .refine((v) => v.commission <= v.amount, { message: 'Hoa hồng không được lớn hơn giá trị', path: ['commission'] });
type FormValues = z.infer<typeof schema>;

const toFormValues = (d?: Deal | null): FormValues => ({
  propertyId: d ? String(d.property.id) : '',
  customerId: d ? String(d.customer.id) : '',
  amount: d?.amount ?? ('' as unknown as number),
  commission: d?.commission ?? ('' as unknown as number),
  notes: d?.notes ?? '',
  agentId: d ? String(d.agent.id) : '',
});

export function DealFormModal({ open, deal, onClose }: { open: boolean; deal: Deal | null; onClose: () => void }) {
  const { isAdmin } = useAuth();
  const queryClient = useQueryClient();
  const isEdit = !!deal;
  const agentOptions = useAgentOptions();
  const customerOptions = useCustomerOptions();
  const availableProperties = usePropertyOptions('AVAILABLE');
  const propertyOptions = isEdit ? [{ value: deal.property.id, label: deal.property.title }] : availableProperties;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: toFormValues(deal) });

  useEffect(() => {
    if (open) reset(toFormValues(deal));
  }, [open, deal, reset]);

  const mutation = useMutation({
    mutationFn: (input: DealInput) => (isEdit ? dealApi.update(deal!.id, input) : dealApi.create(input)),
    onSuccess: () => {
      toast.success(isEdit ? 'Đã cập nhật giao dịch' : 'Đã tạo giao dịch, bất động sản chuyển sang giữ chỗ');
      queryClient.invalidateQueries({ queryKey: ['deals'] });
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      onClose();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const onSubmit = (v: FormValues) =>
    mutation.mutate({
      propertyId: Number(v.propertyId),
      customerId: Number(v.customerId),
      amount: v.amount,
      commission: v.commission,
      notes: v.notes || null,
      agentId: isAdmin && v.agentId ? Number(v.agentId) : null,
    });

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="lg"
      title={isEdit ? 'Cập nhật giao dịch' : 'Tạo giao dịch'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" form="deal-form" loading={mutation.isPending}>
            {isEdit ? 'Lưu thay đổi' : 'Tạo giao dịch'}
          </Button>
        </>
      }
    >
      <form id="deal-form" onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-2" noValidate>
        <Field
          label="Bất động sản"
          htmlFor="propertyId"
          error={errors.propertyId?.message}
          required
          hint={isEdit ? undefined : 'Chỉ hiển thị bất động sản đang mở bán'}
          className="sm:col-span-2"
        >
          <Select
            id="propertyId"
            placeholder="— Chọn bất động sản —"
            options={propertyOptions}
            disabled={isEdit}
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
        <Field label="Giá trị giao dịch (VND)" htmlFor="amount" error={errors.amount?.message} required>
          <Input id="amount" type="number" min={0} step="1000000" invalid={!!errors.amount} {...register('amount')} />
        </Field>
        <Field label="Hoa hồng (VND)" htmlFor="commission" error={errors.commission?.message} required>
          <Input id="commission" type="number" min={0} invalid={!!errors.commission} {...register('commission')} />
        </Field>
        {isAdmin && (
          <Field label="Môi giới" htmlFor="agentId" hint="Để trống: môi giới phụ trách khách hàng" className="sm:col-span-2">
            <Select id="agentId" placeholder="— Tự động —" options={agentOptions} {...register('agentId')} />
          </Field>
        )}
        <Field label="Ghi chú" htmlFor="notes" className="sm:col-span-2">
          <Textarea id="notes" rows={3} {...register('notes')} />
        </Field>
      </form>
    </Modal>
  );
}
