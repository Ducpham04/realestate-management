import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { propertyApi } from '@/api/endpoints';
import type { Property, PropertyInput } from '@/api/types';
import { useAuth } from '@/auth/AuthContext';
import { Button, Field, Input, Modal, Select, Textarea } from '@/components/ui';
import { useAgentOptions } from '@/features/shared/useOptions';
import { getErrorMessage } from '@/lib/errors';
import { listingTypeLabels, options, propertyStatusLabels, propertyTypeLabels } from '@/lib/labels';

const optionalInt = z.preprocess(
  (v) => (v === '' || v == null || Number.isNaN(v) ? null : Number(v)),
  z.number().int().min(0, 'Không được âm').max(100).nullable(),
);

const schema = z.object({
  title: z.string().trim().min(1, 'Vui lòng nhập tiêu đề').max(200),
  description: z.string().max(5000).optional(),
  type: z.enum(['APARTMENT', 'HOUSE', 'VILLA', 'TOWNHOUSE', 'LAND', 'OFFICE', 'SHOPHOUSE']),
  listingType: z.enum(['SALE', 'RENT']),
  status: z.enum(['AVAILABLE', 'RESERVED', 'SOLD', 'RENTED', 'INACTIVE']),
  price: z.coerce.number({ invalid_type_error: 'Giá không hợp lệ' }).positive('Giá phải lớn hơn 0'),
  area: z.coerce.number({ invalid_type_error: 'Diện tích không hợp lệ' }).positive('Diện tích phải lớn hơn 0'),
  bedrooms: optionalInt,
  bathrooms: optionalInt,
  street: z.string().trim().min(1, 'Vui lòng nhập địa chỉ'),
  ward: z.string().optional(),
  district: z.string().trim().min(1, 'Vui lòng nhập quận/huyện'),
  city: z.string().trim().min(1, 'Vui lòng nhập tỉnh/thành phố'),
  images: z
    .string()
    .optional()
    .refine(
      (v) => !v || v.split('\n').map((s) => s.trim()).filter(Boolean).every((u) => /^https?:\/\/.+/.test(u)),
      'Mỗi dòng phải là một URL http(s)',
    ),
  agentId: z.string().optional(),
});

type FormValues = z.infer<typeof schema>;

const toFormValues = (p?: Property | null): FormValues => ({
  title: p?.title ?? '',
  description: p?.description ?? '',
  type: p?.type ?? 'APARTMENT',
  listingType: p?.listingType ?? 'SALE',
  status: p?.status ?? 'AVAILABLE',
  price: p?.price ?? ('' as unknown as number),
  area: p?.area ?? ('' as unknown as number),
  bedrooms: p?.bedrooms ?? null,
  bathrooms: p?.bathrooms ?? null,
  street: p?.address.street ?? '',
  ward: p?.address.ward ?? '',
  district: p?.address.district ?? '',
  city: p?.address.city ?? '',
  images: p?.images.join('\n') ?? '',
  agentId: p?.agent.id ? String(p.agent.id) : '',
});

interface Props {
  open: boolean;
  property: Property | null;
  onClose: () => void;
}

export function PropertyFormModal({ open, property, onClose }: Props) {
  const { isAdmin } = useAuth();
  const agentOptions = useAgentOptions();
  const queryClient = useQueryClient();
  const isEdit = !!property;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema), defaultValues: toFormValues(property) });

  useEffect(() => {
    if (open) reset(toFormValues(property));
  }, [open, property, reset]);

  const mutation = useMutation({
    mutationFn: (input: PropertyInput) =>
      isEdit ? propertyApi.update(property!.id, input) : propertyApi.create(input),
    onSuccess: () => {
      toast.success(isEdit ? 'Đã cập nhật bất động sản' : 'Đã thêm bất động sản');
      queryClient.invalidateQueries({ queryKey: ['properties'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      onClose();
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  });

  const onSubmit = (v: FormValues) =>
    mutation.mutate({
      title: v.title,
      description: v.description || undefined,
      type: v.type,
      listingType: v.listingType,
      status: v.status,
      price: v.price,
      area: v.area,
      bedrooms: v.bedrooms,
      bathrooms: v.bathrooms,
      address: { street: v.street, ward: v.ward || undefined, district: v.district, city: v.city },
      images: (v.images ?? '').split('\n').map((s) => s.trim()).filter(Boolean),
      agentId: isAdmin && v.agentId ? Number(v.agentId) : null,
    });

  return (
    <Modal
      open={open}
      onClose={onClose}
      size="xl"
      title={isEdit ? 'Cập nhật bất động sản' : 'Thêm bất động sản'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" form="property-form" loading={mutation.isPending}>
            {isEdit ? 'Lưu thay đổi' : 'Thêm mới'}
          </Button>
        </>
      }
    >
      <form id="property-form" onSubmit={handleSubmit(onSubmit)} className="grid gap-4 sm:grid-cols-2" noValidate>
        <Field label="Tiêu đề" htmlFor="title" error={errors.title?.message} required className="sm:col-span-2">
          <Input id="title" invalid={!!errors.title} {...register('title')} />
        </Field>
        <Field label="Loại hình" htmlFor="type" required>
          <Select id="type" options={options(propertyTypeLabels)} {...register('type')} />
        </Field>
        <Field label="Hình thức" htmlFor="listingType" required>
          <Select id="listingType" options={options(listingTypeLabels)} {...register('listingType')} />
        </Field>
        <Field label="Giá (VND)" htmlFor="price" error={errors.price?.message} required hint="Giá bán hoặc giá thuê/tháng">
          <Input id="price" type="number" min={0} step="1000000" invalid={!!errors.price} {...register('price')} />
        </Field>
        <Field label="Diện tích (m²)" htmlFor="area" error={errors.area?.message} required>
          <Input id="area" type="number" min={0} step="0.1" invalid={!!errors.area} {...register('area')} />
        </Field>
        <Field label="Phòng ngủ" htmlFor="bedrooms" error={errors.bedrooms?.message}>
          <Input id="bedrooms" type="number" min={0} {...register('bedrooms')} />
        </Field>
        <Field label="Phòng tắm" htmlFor="bathrooms" error={errors.bathrooms?.message}>
          <Input id="bathrooms" type="number" min={0} {...register('bathrooms')} />
        </Field>
        <Field label="Địa chỉ" htmlFor="street" error={errors.street?.message} required className="sm:col-span-2">
          <Input id="street" placeholder="Số nhà, tên đường" invalid={!!errors.street} {...register('street')} />
        </Field>
        <Field label="Phường/Xã" htmlFor="ward">
          <Input id="ward" {...register('ward')} />
        </Field>
        <Field label="Quận/Huyện" htmlFor="district" error={errors.district?.message} required>
          <Input id="district" invalid={!!errors.district} {...register('district')} />
        </Field>
        <Field label="Tỉnh/Thành phố" htmlFor="city" error={errors.city?.message} required>
          <Input id="city" invalid={!!errors.city} {...register('city')} />
        </Field>
        <Field label="Trạng thái" htmlFor="status">
          <Select id="status" options={options(propertyStatusLabels)} {...register('status')} />
        </Field>
        {isAdmin && (
          <Field label="Môi giới phụ trách" htmlFor="agentId" className="sm:col-span-2">
            <Select id="agentId" placeholder="— Tôi —" options={agentOptions} {...register('agentId')} />
          </Field>
        )}
        <Field label="Mô tả" htmlFor="description" className="sm:col-span-2">
          <Textarea id="description" rows={3} {...register('description')} />
        </Field>
        <Field
          label="Ảnh (mỗi dòng một URL)"
          htmlFor="images"
          error={errors.images?.message}
          className="sm:col-span-2"
        >
          <Textarea id="images" rows={3} placeholder="https://..." invalid={!!errors.images} {...register('images')} />
        </Field>
      </form>
    </Modal>
  );
}
