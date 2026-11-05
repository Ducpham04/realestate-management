import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { customerApi } from '@/api/endpoints';
import type { Customer, CustomerInput } from '@/api/types';
import { useAuth } from '@/auth/AuthContext';
import { Button, Field, Input, Modal, Select, Textarea } from '@/components/ui';
import { useAgentOptions } from '@/features/shared/useOptions';
import { getErrorMessage } from '@/lib/errors';
import { customerTypeLabels, options } from '@/lib/labels';

const optionalMoney = z.preprocess(
  (v) => (v === '' || v == null ? null : Number(v)),
  z.number({ invalid_type_error: 'Không hợp lệ' }).min(0, 'Không được âm').nullable(),
);

const schema = z
  .object({
    fullName: z.string().trim().min(1, 'Vui lòng nhập họ tên').max(100),
    email: z.union([z.literal(''), z.string().email('Email không hợp lệ')]),
    phone: z.string().trim().regex(/^[0-9+\-\s]{8,20}$/, 'Số điện thoại không hợp lệ'),
    type: z.enum(['BUYER', 'TENANT', 'SELLER', 'LANDLORD']),
    budgetMin: optionalMoney,
    budgetMax: optionalMoney,
    preferredLocation: z.string().max(200).optional(),
    notes: z.string().max(5000).optional(),
    agentId: z.string().optional(),
  })
  .refine((v) => v.budgetMin == null || v.budgetMax == null || v.budgetMin <= v.budgetMax, {
    message: 'Ngân sách tối thiểu phải nhỏ hơn tối đa',
    path: ['budgetMax'],
  });

type FormValues = z.infer<typeof schema>;

const toFormValues = (c?: Customer | null): FormValues => ({
  fullName: c?.fullName ?? '',
  email: c?.email ?? '',
  phone: c?.phone ?? '',
  type: c?.type ?? 'BUYER',
  budgetMin: c?.budgetMin ?? null,
  budgetMax: c?.budgetMax ?? null,
  preferredLocation: c?.preferredLocation ?? '',
  notes: c?.notes ?? '',
  agentId: c?.agent.id ? String(c.agent.id) : '',
});

export function CustomerFormModal({
  open,
  customer,
  onClose,
}: {
  open: boolean;
  customer: Customer | null;
  onClose: () => void;
}) {
  const { isAdmin } = useAuth();
  const agentOptions = useAgentOptions();
  const queryClient = useQueryClient();
  const isEdit = !!customer;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: toFormValues(customer) });

  useEffect(() => {
    if (open) reset(toFormValues(customer));
  }, [open, customer, reset]);

  const mutation = useMutation({
    mutationFn: (input: CustomerInput) =>
      isEdit ? customerApi.update(customer!.id, input) : customerApi.create(input),
    onSuccess: () => {
      toast.success(isEdit ? 'Đã cập nhật khách hàng' : 'Đã thêm khách hàng');
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      onClose();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const onSubmit = (v: FormValues) =>
    mutation.mutate({
      fullName: v.fullName,
      email: v.email || null,
      phone: v.phone,
      type: v.type,
      budgetMin: v.budgetMin,
      budgetMax: v.budgetMax,
      preferredLocation: v.preferredLocation || null,
      notes: v.notes || null,
      agentId: isAdmin && v.agentId ? Number(v.agentId) : null,
    });

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="lg"
      title={isEdit ? 'Cập nhật khách hàng' : 'Thêm khách hàng'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" form="customer-form" loading={mutation.isPending}>
            {isEdit ? 'Lưu thay đổi' : 'Thêm mới'}
          </Button>
        </>
      }
    >
      <form id="customer-form" onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-2" noValidate>
        <Field label="Họ tên" htmlFor="fullName" error={errors.fullName?.message} required>
          <Input id="fullName" invalid={!!errors.fullName} {...register('fullName')} />
        </Field>
        <Field label="Số điện thoại" htmlFor="phone" error={errors.phone?.message} required>
          <Input id="phone" invalid={!!errors.phone} {...register('phone')} />
        </Field>
        <Field label="Email" htmlFor="email" error={errors.email?.message}>
          <Input id="email" type="email" invalid={!!errors.email} {...register('email')} />
        </Field>
        <Field label="Nhu cầu" htmlFor="type" required>
          <Select id="type" options={options(customerTypeLabels)} {...register('type')} />
        </Field>
        <Field label="Ngân sách từ (VND)" htmlFor="budgetMin" error={errors.budgetMin?.message}>
          <Input id="budgetMin" type="number" min={0} {...register('budgetMin')} />
        </Field>
        <Field label="Ngân sách đến (VND)" htmlFor="budgetMax" error={errors.budgetMax?.message}>
          <Input id="budgetMax" type="number" min={0} {...register('budgetMax')} />
        </Field>
        <Field label="Khu vực quan tâm" htmlFor="preferredLocation" className="sm:col-span-2">
          <Input id="preferredLocation" {...register('preferredLocation')} />
        </Field>
        {isAdmin && (
          <Field label="Môi giới phụ trách" htmlFor="agentId" className="sm:col-span-2">
            <Select id="agentId" placeholder="— Tôi —" options={agentOptions} {...register('agentId')} />
          </Field>
        )}
        <Field label="Ghi chú" htmlFor="notes" className="sm:col-span-2">
          <Textarea id="notes" rows={3} {...register('notes')} />
        </Field>
      </form>
    </Modal>
  );
}
